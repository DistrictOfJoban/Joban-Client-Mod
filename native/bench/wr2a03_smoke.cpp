/**
 * wr2a03_smoke.cpp — 端到端冒烟驱动：真实 ABI 驱动 wr2a03_lcd /
 * wr2a03_train_num 两个原生脚本库。
 *
 * 它做的事：
 *   1. LoadLibrary/GetProcAddress 打开脚本库（Windows；POSIX 走 dlopen）；
 *   2. 按 mtr_native.h 的布局合成 JcmVehicleSnapshot（6 节车厢、直线 10 站、
 *      环线 12 站、侧线名 "10010/01-02-03-04-05-06"）；
 *   3. 用宿主态 state 块调用 mtrCreate / mtrRender / mtrDispose；
 *   4. 从帧记录里重建上传的纹理（dirty rect → 整图），写 PPM 并做像素断言；
 *   5. 断言 ABI 版本、脚本 id/type、绘制记录数、纹理尺寸、内容颜色。
 *
 * 这是"脚本库能否被真实宿主驱动"的唯一自动化验证手段（本机无法启动 MC），
 * 因此 CI 与本地都跑它。
 *
 * 构建 (CMake 目标 wr2a03_smoke)：cmake --build build --target wr2a03_smoke
 * 运行：wr2a03_smoke.exe <脚本库所在目录>
 */
#include <mtr/mtr_native.h>

#include <chrono>
#include <cmath>
#include <cstdarg>
#include <cstdint>
#include <cstdio>
#include <cstdlib>
#include <cstring>
#include <map>
#include <string>
#include <vector>

#if defined(_WIN32)
#  include <windows.h>
#else
#  include <dlfcn.h>
#  include <sys/stat.h>
#  include <sys/types.h>
#endif

using namespace std::chrono;

/* ------------------------------------------------------------------ */
/* host service stubs                                                  */
/* ------------------------------------------------------------------ */

namespace {

int32_t g_next_texture = 1;
int32_t g_next_model = 100;
long    g_quad_models = 0;
long    g_textures = 0;
std::vector<std::string> g_model_paths;

int32_t hs_acquire_model(void*, const char* path) {
    g_model_paths.push_back(path ? path : "");
    return g_next_model++;
}
void hs_release_model(void*, int32_t) {}

int32_t hs_create_texture(void*, int32_t w, int32_t h) {
    g_textures++;
    std::printf("  [host] create_texture(%d x %d)\n", w, h);
    return g_next_texture++;
}
void hs_release_texture(void*, int32_t) {}

/* v4: 宿主构建四边形模型（DisplayHelper 等价物）。参考实现只做计数 +
   几何校验——真实宿主在这里用 RawMeshBuilder(4) 生成网格并交给 ModelJS。 */
int32_t hs_acquire_quad_model(void*, const float* verts, const float* uv,
                              int32_t vertex_count, int32_t render_stage,
                              int32_t texture_handle) {
    if (!verts || !uv || vertex_count != 4 || texture_handle < 0) return -1;
    /* 退化四边形检查：四个顶点必须互不相同 */
    bool degenerate = true;
    for (int i = 1; i < 4; i++) {
        if (std::fabs(verts[i * 3] - verts[0]) > 1e-6
            || std::fabs(verts[i * 3 + 1] - verts[1]) > 1e-6
            || std::fabs(verts[i * 3 + 2] - verts[2]) > 1e-6) {
            degenerate = false;
            break;
        }
    }
    if (degenerate) return -1;
    (void)render_stage;
    g_quad_models++;
    return g_next_model++;
}

int32_t hs_rasterize_text(void*, const char*, int32_t, int32_t x, int32_t y,
                          int32_t max_w, uint8_t r, uint8_t g, uint8_t b,
                          uint8_t* out, int32_t out_w, int32_t out_h) {
    if (max_w <= 0 || !out) return -1;
    const int32_t gw = max_w - 1;
    const int32_t gh = static_cast<int32_t>(max_w * 0.92);
    for (int32_t py = y; py < y + gh; py++) {
        if (py < 0 || py >= out_h) continue;
        for (int32_t px = x; px < x + gw; px++) {
            if (px < 0 || px >= out_w) continue;
            const size_t idx = (static_cast<size_t>(py) * out_w + px) * 4;
            out[idx + 0] = b;
            out[idx + 1] = g;
            out[idx + 2] = r;
            out[idx + 3] = 255;
        }
    }
    return 1;
}

void hs_log(void*, int32_t level, const char* utf8, int32_t len) {
    std::printf("  [script log %d] %.*s\n", level, len, utf8);
}

JcmHostServices make_host() {
    JcmHostServices hs{};
    hs.user = nullptr;
    hs.acquire_model = hs_acquire_model;
    hs.release_model = hs_release_model;
    hs.create_texture = hs_create_texture;
    hs.release_texture = hs_release_texture;
    hs.rasterize_text = hs_rasterize_text;
    hs.log = hs_log;
    hs.acquire_quad_model = hs_acquire_quad_model;
    return hs;
}

/* ------------------------------------------------------------------ */
/* snapshot builder (reimplementation of the host's marshalling)       */
/* ------------------------------------------------------------------ */

struct StopSpec {
    const char* name;
    double distance;
    int64_t station_id;
    const char* dest;
    std::vector<std::pair<const char*, uint32_t>> transfers;
    /* v5: 该 stop 所属 route 的 CircularState（JS: stop.route.getCircularState()） */
    uint8_t routeCircularState = 0;
};

struct CarSpec { float length = 25.0F; };

struct Snapshot {
    std::vector<uint8_t> buf;
    JcmVehicleSnapshot* hdr() {
        return reinterpret_cast<JcmVehicleSnapshot*>(buf.data());
    }
};

Snapshot build_snapshot(int carCount, const CarSpec* cars,
                        const std::vector<StopSpec>& stops,
                        int64_t gameTimeMillis, double doorValue, double speedMs,
                        int32_t nextStopIndex, const char* routeName,
                        uint32_t routeColor, uint8_t circularState,
                        const char* sidingName, bool reversed, bool onRoute) {
    Snapshot out;
    std::vector<uint8_t>& buf = out.buf;
    auto pad_to = [&](size_t align) {
        while (buf.size() % align != 0) buf.push_back(0);
    };

    buf.resize(sizeof(JcmVehicleSnapshot));
    pad_to(alignof(JcmCar));

    const int32_t carOffset = static_cast<int32_t>(buf.size());
    for (int i = 0; i < carCount; i++) {
        JcmCar c{};
        c.length = cars[i].length;
        c.width = 3.0F;
        c.left_door_open = doorValue > 0.5 ? 1 : 0;
        c.right_door_open = doorValue > 0.5 ? 1 : 0;
        c.rendered = 1;
        buf.insert(buf.end(), reinterpret_cast<const uint8_t*>(&c),
                   reinterpret_cast<const uint8_t*>(&c) + sizeof(c));
    }
    pad_to(alignof(JcmInterchange));

    const int32_t icOffset = static_cast<int32_t>(buf.size());
    for (const StopSpec& st : stops) {
        for (const auto& tr : st.transfers) {
            JcmInterchange ic{};
            ic.color = static_cast<int32_t>(tr.second);
            ic.route_name_offset = 0;
            ic.route_name_len = static_cast<int32_t>(std::strlen(tr.first));
            buf.insert(buf.end(), reinterpret_cast<const uint8_t*>(&ic),
                       reinterpret_cast<const uint8_t*>(&ic) + sizeof(ic));
        }
    }

    std::vector<std::pair<int32_t, int32_t>> icNameRefs;
    std::vector<std::pair<int32_t, int32_t>> stopNames, stopDests;
    int32_t routeNameOff = 0, sidingOff = 0, poolStart = 0;
    {
        for (const StopSpec& st : stops) {
            const int32_t off = static_cast<int32_t>(buf.size());
            buf.insert(buf.end(), st.name, st.name + std::strlen(st.name));
            stopNames.emplace_back(off, static_cast<int32_t>(std::strlen(st.name)));
            const char* d = st.dest ? st.dest : "";
            const int32_t doff = static_cast<int32_t>(buf.size());
            buf.insert(buf.end(), d, d + std::strlen(d));
            stopDests.emplace_back(doff, static_cast<int32_t>(std::strlen(d)));
        }
        for (const StopSpec& st : stops) {
            for (const auto& tr : st.transfers) {
                const int32_t off = static_cast<int32_t>(buf.size());
                buf.insert(buf.end(), tr.first, tr.first + std::strlen(tr.first));
                icNameRefs.emplace_back(off, static_cast<int32_t>(std::strlen(tr.first)));
            }
        }
        routeNameOff = static_cast<int32_t>(buf.size());
        buf.insert(buf.end(), routeName, routeName + std::strlen(routeName));
        sidingOff = static_cast<int32_t>(buf.size());
        buf.insert(buf.end(), sidingName, sidingName + std::strlen(sidingName));
        poolStart = stopNames.empty() ? 0 : stopNames[0].first;
    }

    pad_to(alignof(JcmStop));
    const int32_t stopOffset = static_cast<int32_t>(buf.size());
    {
        JcmInterchange* icBase = reinterpret_cast<JcmInterchange*>(buf.data() + icOffset);
        size_t k = 0;
        for (const StopSpec& st : stops) {
            for (size_t t = 0; t < st.transfers.size(); t++, k++) {
                icBase[k].route_name_offset = icNameRefs[k].first;
            }
        }
        int32_t icCursor = icOffset;
        for (size_t i = 0; i < stops.size(); i++) {
            JcmStop s{};
            s.route_id = 42;
            s.station_id = stops[i].station_id;
            s.platform_id = stops[i].station_id * 10;
            s.distance = stops[i].distance;
            s.dwell_time_millis = 15000;
            s.name_offset = stopNames[i].first;
            s.name_len = stopNames[i].second;
            s.destination_offset = stopDests[i].first;
            s.destination_len = stopDests[i].second;
            s.custom_destination_offset = -1;
            s.interchange_count = static_cast<int32_t>(stops[i].transfers.size());
            s.interchange_offset = stops[i].transfers.empty() ? 0 : icCursor;
            s.exit_count = 0;
            s.exit_offset = 0;
            s.route_circular_state = stops[i].routeCircularState;
            s.is_route_switchover = 0;
            if (!stops[i].transfers.empty()) {
                icCursor += static_cast<int32_t>(stops[i].transfers.size()
                                                 * sizeof(JcmInterchange));
            }
            buf.insert(buf.end(), reinterpret_cast<const uint8_t*>(&s),
                       reinterpret_cast<const uint8_t*>(&s) + sizeof(s));
        }
    }

    JcmVehicleSnapshot* hdr = out.hdr();
    hdr->vehicle_id = 123456789;
    hdr->siding_id = 7;
    hdr->this_route_id = 42;
    hdr->departure_index = 38;
    hdr->car_count = carCount;
    hdr->transport_mode = 0;
    hdr->speed_kmh = speedMs * 3.6;
    hdr->speed_ms = speedMs;
    hdr->rail_progress = 3400.0;
    hdr->door_value = doorValue;
    hdr->notch_level = 3;
    hdr->reversed = reversed ? 1 : 0;
    hdr->on_route = onRoute ? 1 : 0;
    hdr->door_opening = (doorValue > 0.05 && doorValue < 0.95) ? 1 : 0;
    hdr->any_car_rendered = 1;
    hdr->total_dwell_time_millis = 15000;
    hdr->elapsed_dwell_time_millis = 0;
    hdr->game_time_millis = gameTimeMillis;
    hdr->in_game_time = 6000;
    hdr->car_offset = carOffset;
    hdr->stop_count = static_cast<int32_t>(stops.size());
    hdr->stop_offset = stopOffset;
    hdr->this_route_stop_count = static_cast<int32_t>(stops.size());
    hdr->this_route_stop_offset = stopOffset;
    hdr->next_route_stop_count = 0;
    hdr->next_route_stop_offset = 0;
    hdr->next_stop_index = nextStopIndex;
    hdr->route_name_offset = routeNameOff;
    hdr->route_name_len = static_cast<int32_t>(std::strlen(routeName));
    hdr->route_color = static_cast<int32_t>(routeColor);
    hdr->circular_state = circularState;
    hdr->siding_name_offset = sidingOff;
    hdr->siding_name_len = static_cast<int32_t>(std::strlen(sidingName));
    hdr->string_pool_offset = poolStart;
    hdr->string_pool_len = static_cast<int32_t>(buf.size()) - poolStart;
    return out;
}

/* ------------------------------------------------------------------ */
/* module loader (Windows LoadLibrary / POSIX dlopen)                  */
/* ------------------------------------------------------------------ */

void* lib_open(const std::string& path) {
#if defined(_WIN32)
    return static_cast<void*>(LoadLibraryA(path.c_str()));
#else
    return dlopen(path.c_str(), RTLD_NOW | RTLD_LOCAL);
#endif
}
void* lib_sym(void* lib, const char* name) {
#if defined(_WIN32)
    return reinterpret_cast<void*>(GetProcAddress(static_cast<HMODULE>(lib), name));
#else
    return dlsym(lib, name);
#endif
}
void lib_close(void* lib) {
    if (!lib) return;
#if defined(_WIN32)
    FreeLibrary(static_cast<HMODULE>(lib));
#else
    dlclose(lib);
#endif
}

struct Module {
    void* handle = nullptr;
    uint32_t (*abi)(void) = nullptr;
    const char* (*type)(void) = nullptr;
    const char* (*id)(void) = nullptr;
    size_t (*state_size)(void) = nullptr;
    void (*init)(const JcmFrameInput*) = nullptr;
    int32_t (*create)(const JcmFrameInput*) = nullptr;
    int32_t (*render)(const JcmFrameInput*, JcmFrameOutput*) = nullptr;
    int32_t (*dispose)(const JcmFrameInput*) = nullptr;
    void* state = nullptr;
    std::string loadedPath;

    bool open(const std::string& path) {
        loadedPath = path;
        handle = lib_open(path);
        if (!handle) {
            std::printf("cannot load %s\n", path.c_str());
            return false;
        }
        abi = reinterpret_cast<uint32_t (*)(void)>(lib_sym(handle, "mtrNativeAbiVersion"));
        type = reinterpret_cast<const char* (*)(void)>(lib_sym(handle, "mtrScriptType"));
        id = reinterpret_cast<const char* (*)(void)>(lib_sym(handle, "mtrScriptId"));
        state_size = reinterpret_cast<size_t (*)(void)>(lib_sym(handle, "mtrStateSize"));
        init = reinterpret_cast<void (*)(const JcmFrameInput*)>(lib_sym(handle, "mtrInit"));
        create = reinterpret_cast<int32_t (*)(const JcmFrameInput*)>(lib_sym(handle, "mtrCreate"));
        render = reinterpret_cast<int32_t (*)(const JcmFrameInput*, JcmFrameOutput*)>(
            lib_sym(handle, "mtrRender"));
        dispose = reinterpret_cast<int32_t (*)(const JcmFrameInput*)>(
            lib_sym(handle, "mtrDispose"));
        if (!abi || !type || !id || !state_size || !create || !render || !dispose) {
            std::printf("module %s is missing mtr* exports (check the version still\n"
                        "exports the 5 mtr* functions and is not a stale build)\n",
                        path.c_str());
            lib_close(handle);
            handle = nullptr;
            return false;
        }
        return true;
    }
    ~Module() { lib_close(handle); }
};

/* ------------------------------------------------------------------ */
/* texture reconstruction + pixel helpers                              */
/* ------------------------------------------------------------------ */

struct ReconTex {
    int32_t w = 0, h = 0;
    std::vector<uint8_t> px;     /* little-endian ARGB => [B,G,R,A] */
};

struct FrameStats {
    int64_t models = 0;
    int64_t uploads = 0;
    int64_t uploadBytes = 0;
    /* per-car model draw counts: car index -> count */
    std::map<int, int> modelsPerCar;
};

void apply_frame(const JcmFrameOutput& out, std::map<int32_t, ReconTex>& texes,
                 FrameStats& stats) {
    const uint8_t* p = static_cast<const uint8_t*>(out.records);
    for (int32_t i = 0; i < out.record_count; i++) {
        const JcmRecordHeader* h = reinterpret_cast<const JcmRecordHeader*>(p);
        if (h->kind == JCM_DRAW_MODEL) {
            const JcmDrawModel* m = reinterpret_cast<const JcmDrawModel*>(p);
            stats.models++;
            stats.modelsPerCar[static_cast<int>(m->car)]++;
        } else if (h->kind == JCM_DRAW_TEXTURE_UPLOAD) {
            const JcmDrawTextureUpload* u = reinterpret_cast<const JcmDrawTextureUpload*>(p);
            stats.uploads++;
            stats.uploadBytes += u->pixel_data_len;
            if (u->texture_handle >= 0 && u->width > 0 && u->height > 0
                && u->dirty_w > 0 && u->dirty_h > 0) {
                ReconTex& t = texes[u->texture_handle];
                if (static_cast<int32_t>(t.px.size()) != u->width * u->height * 4) {
                    t.w = u->width;
                    t.h = u->height;
                    t.px.assign(static_cast<size_t>(u->width) * u->height * 4, 0);
                }
                for (int32_t y = 0; y < u->dirty_h; y++) {
                    const int64_t srcOff = u->pixel_data_offset
                        + static_cast<int64_t>(y) * u->dirty_w * 4;
                    std::memcpy(&t.px[(static_cast<size_t>(u->dirty_y + y) * u->width
                                       + u->dirty_x) * 4],
                                out.pixel_arena + srcOff,
                                static_cast<size_t>(u->dirty_w) * 4);
                }
            }
        }
        p += h->record_size;
    }
}

uint32_t sample_px(const ReconTex& t, int x, int y) {
    if (x < 0 || y < 0 || x >= t.w || y >= t.h) return 0;
    const size_t idx = (static_cast<size_t>(y) * t.w + x) * 4;
    return (static_cast<uint32_t>(t.px[idx + 2]) << 16)
         | (static_cast<uint32_t>(t.px[idx + 1]) << 8)
         | static_cast<uint32_t>(t.px[idx]);
}

uint32_t sample_alpha(const ReconTex& t, int x, int y) {
    if (x < 0 || y < 0 || x >= t.w || y >= t.h) return 0;
    return t.px[(static_cast<size_t>(y) * t.w + x) * 4 + 3];
}

bool close_to(uint32_t a, uint32_t b, int tol) {
    const int dr = std::abs(static_cast<int>((a >> 16) & 0xFF) - static_cast<int>((b >> 16) & 0xFF));
    const int dg = std::abs(static_cast<int>((a >> 8) & 0xFF) - static_cast<int>((b >> 8) & 0xFF));
    const int db = std::abs(static_cast<int>(a & 0xFF) - static_cast<int>(b & 0xFF));
    return dr <= tol && dg <= tol && db <= tol;
}

int count_color(const ReconTex& t, uint32_t want, int tol,
                int x0, int y0, int x1, int y1, bool needOpaque = false) {
    int n = 0;
    for (int y = y0; y < y1 && y < t.h; y++) {
        for (int x = x0; x < x1 && x < t.w; x++) {
            if (needOpaque && sample_alpha(t, x, y) < 200) continue;
            if (close_to(sample_px(t, x, y), want, tol)) n++;
        }
    }
    return n;
}

/* 环线/直线分支判据：环线画的是圆角矩形**轨道**，因此在 RING_LEFT_X /
   RING_RIGHT_X 处各有一段**竖直**轨道；直线只有一条水平轨道，这两处
   不应有线路色（站点圆点最多 2*K(14)=±23px，取 ±60px 的窄带即可区分）。
   采样带取 RING_TOP_Y..RING_BOTTOM_Y 的中段，避开上下两条水平轨道。 */
int rail_column_pixels(const ReconTex& t, uint32_t color, int tol, bool rightSide) {
    const double sx = t.w / 2800.0, sy = t.h / 800.0;
    const int band = static_cast<int>(std::lround(60 * sx));
    const int cx = static_cast<int>(std::lround((rightSide ? 2600 : 200) * sx));
    const int y0 = static_cast<int>(std::lround((400 + 80) * sy));
    const int y1 = static_cast<int>(std::lround((650 - 80) * sy));
    return count_color(t, color, tol, cx - band, y0, cx + band, y1);
}

int count_ink(const ReconTex& t) {
    int n = 0;
    for (size_t i = 0; i + 3 < t.px.size(); i += 4) {
        if (t.px[i + 3] >= 200 && t.px[i] < 70 && t.px[i + 1] < 70 && t.px[i + 2] < 70) n++;
    }
    return n;
}

void write_ppm(const std::string& path, const ReconTex& t) {
    FILE* f = std::fopen(path.c_str(), "wb");
    if (!f) return;
    std::fprintf(f, "P6\n%d %d\n255\n", t.w, t.h);
    for (size_t i = 0; i + 3 < t.px.size(); i += 4) {
        const uint8_t rgb[3] = {t.px[i + 2], t.px[i + 1], t.px[i]};
        std::fwrite(rgb, 1, 3, f);
    }
    std::fclose(f);
    std::printf("  wrote %s (%dx%d)\n", path.c_str(), t.w, t.h);
}

/* ------------------------------------------------------------------ */
/* scenes                                                              */
/* ------------------------------------------------------------------ */

std::vector<StopSpec> linear_stops() {
    const uint32_t c1 = 0xFFE60012;
    const uint32_t c2 = 0xFF00A651;
    return {
        {"火车站|Railway Station",   0,    101, "", {{"1号线|Line 1", c1}}},
        {"人民广场|People Square",   900,  102, "", {}},
        {"青年路|Qingnian Road",     1800, 103, "", {{"2号线|Line 2", c2}}},
        {"大南门|Dananmen",          2700, 104, "", {}},
        {"体育馆|Stadium",           3600, 105, "", {}},
        {"长风街|Changfeng Street",  4500, 106, "", {}},
        {"学府街|Xuefu Street",      5400, 107, "", {}},
        {"南中环|Nanzhonghuan",      6300, 108, "", {}},
        {"晋阳街|Jinyang Street",    7200, 109, "", {}},
        {"西桥|Xiqiao",              8100, 110, "西桥|Xiqiao", {}},
    };
}

std::vector<StopSpec> circular_stops() {
    static const char* names[] = {
        "西北延|Xibei Yan", "涧河路|Jianhe Road", "动物园|Zoo",
        "胜利桥东|Shengliqiaodong", "府西街|Fuxi Street", "迎泽公园|Yingze Park",
        "南内环|Nanneihuan", "长风街|Changfeng Street", "体育中心|Sports Center",
        "晋阳湖|Jinyang Lake", "兴华街|Xinghua Street", "玉门河|Yumenhe",
    };
    std::vector<StopSpec> s;
    for (int i = 0; i < 12; i++) {
        StopSpec st{};
        st.name = names[i];
        st.distance = i * 800.0;
        st.station_id = 200 + i;
        st.dest = "";
        /* v5: 该 stop 所属 route 为 CLOCKWISE 环线
           （JS: stop.route.getCircularState() === CLOCKWISE） */
        st.routeCircularState = 1;
        if (i == 4) st.transfers.push_back({"1号线|Line 1", 0xFFE60012});
        if (i == 7) st.transfers.push_back({"2号线|Line 2", 0xFF00A651});
        s.push_back(st);
    }
    /* 环线：末站绕回首站 */
    {
        StopSpec last{};
        last.name = "西北延|Xibei Yan";
        last.distance = 12 * 800.0;
        last.station_id = 200;
        last.dest = "";
        last.routeCircularState = 1;
        s.push_back(last);
    }
    return s;
}

/* ------------------------------------------------------------------ */
/* main                                                                */
/* ------------------------------------------------------------------ */

int run(int argc, char** argv) {
    const std::string dir = argc > 1 ? argv[1] : ".";
#if defined(_WIN32)
    const std::string lcdPath = dir + "\\wr2a03_lcd.dll";
    const std::string numPath = dir + "\\wr2a03_train_num.dll";
    /* Previews go into a SUBDIRECTORY, never next to the libraries: the usual
       way to run this is against a resource pack's natives/<platform> folder,
       and dumping five 9 MB PPMs in there silently bloats the pack by ~30 MB.
       Pass a second argument to put them somewhere else entirely. */
    const std::string outDir = argc > 2 ? std::string(argv[2]) : dir + "\\wr2a03_preview";
    const std::string outPrefix = outDir + "\\";
#elif defined(__APPLE__)
    /* CMake names shared modules lib<name>.dylib on macOS, not .so — keying the
       filename off "_WIN32 or everything else" made this tool unusable on the
       one platform where the ABI-6 state bug shows up. */
    const std::string lcdPath = dir + "/libwr2a03_lcd.dylib";
    const std::string numPath = dir + "/libwr2a03_train_num.dylib";
    const std::string outDir = argc > 2 ? std::string(argv[2]) : dir + "/wr2a03_preview";
    const std::string outPrefix = outDir + "/";
#else
    const std::string lcdPath = dir + "/libwr2a03_lcd.so";
    const std::string numPath = dir + "/libwr2a03_train_num.so";
    const std::string outDir = argc > 2 ? std::string(argv[2]) : dir + "/wr2a03_preview";
    const std::string outPrefix = outDir + "/";
#endif
#if defined(_WIN32)
    CreateDirectoryA(outDir.c_str(), nullptr);
#else
    mkdir(outDir.c_str(), 0755);
#endif

    std::printf("== wr2a03 native smoke ==\n\n");
    std::fflush(stdout);
    int failures = 0;
    auto check = [&](bool ok, const char* what) {
        std::printf("  [%s] %s\n", ok ? "PASS" : "FAIL", what);
        std::fflush(stdout);   /* 崩溃时也要看到已完成的断言 */
        if (!ok) failures++;
    };
    auto note = [&](const char* fmt, ...) {
        va_list ap;
        va_start(ap, fmt);
        std::vfprintf(stdout, fmt, ap);
        va_end(ap);
        std::fflush(stdout);
    };

    JcmHostServices host = make_host();
    const CarSpec cars[6] = {{25}, {25}, {25}, {25}, {25}, {25}};
    const char* siding = "10010/01-02-03-04-05-06";

    /* 一个"全新车辆实例"的场景运行器：分配一块干净的 state、create、
       跑 N 帧、返回重建出来的纹理，最后按需保留/释放。
       每个场景必须用独立 state —— 脚本只在 route id 变化时重建线路缓存
       （与 JS 的 cachedRouteId 短路一致），复用同一块 state 会让第二个场景
       沿用上一个场景的线路，测不出东西。 */
    struct SceneResult {
        std::map<int32_t, ReconTex> texes;
        FrameStats stats;
        bool created = false;
        bool ok = true;
    };
    auto run_scene = [&](Module& m, const Snapshot& snap, int frames) -> SceneResult {
        SceneResult r;
        void* st = std::calloc(1, m.state_size());
        JcmFrameInput in{};
        in.abi_version = MTR_NATIVE_ABI_VERSION;
        in.resource_kind = MTR_RESOURCE_VEHICLE;
        in.snapshot = snap.buf.data();
        in.state = st;
        in.state_size = m.state_size();
        in.host = &host;
        /* ABI 6 protocol: construct the state, then create, then render. */
        if (m.init) m.init(&in);
        r.created = (m.create(&in) == 0);
        JcmFrameOutput out{};
        for (int f = 0; f < frames; f++) {
            if (m.render(&in, &out) != 0) r.ok = false;
            apply_frame(out, r.texes, r.stats);
        }
        m.dispose(&in);
        std::free(st);
        return r;
    };

    /* ================= module 1: LCD ================= */
    {
        Module m;
        if (!m.open(lcdPath)) return 1;
        std::printf("wr2a03_lcd: id=%s type=%s abi=%u state=%zu bytes\n\n",
                    m.id(), m.type(), m.abi(), m.state_size());
        check(m.abi() == MTR_NATIVE_ABI_VERSION, "ABI version matches host (v6)");
        check(m.init != nullptr, "module exports mtrInit (ABI 6 state construction)");
        check(std::strcmp(m.type(), "vehicle") == 0, "script type is vehicle");
        check(std::strcmp(m.id(), "wr2a03:lcd") == 0, "script id is wr2a03:lcd");

        m.state = std::calloc(1, m.state_size());
        auto stops = linear_stops();
        Snapshot snap = build_snapshot(6, cars, stops, 1000000, 0.0, 17.0, 4,
                                       "10号线|Line 10", 0xFF009BC0, 0,
                                       siding, false, true);

        JcmFrameInput in{};
        in.abi_version = MTR_NATIVE_ABI_VERSION;
        in.resource_kind = MTR_RESOURCE_VEHICLE;
        in.snapshot = snap.buf.data();
        in.state = m.state;
        in.state_size = m.state_size();
        in.host = &host;

        /* ABI 6: the host constructs the state BEFORE create(). This is the
           step whose absence segfaults on libstdc++ (zeroed std::string). */
        if (m.init) m.init(&in);
        check(m.create(&in) == 0, "mtrCreate");
        check(g_textures == 12, "mtrCreate creates all LCD textures before first render");

        std::map<int32_t, ReconTex> texes;
        FrameStats stats;
        JcmFrameOutput out{};
        check(m.render(&in, &out) == 0, "mtrRender frame 1 (linear, full map)");
        apply_frame(out, texes, stats);
        check(g_textures == 12, "12 GraphicsTextures created (6 cars x 2 sides)");
        check(g_quad_models == 60, "60 host quads (6 cars x 2 sides x 5 positions)");
        check(stats.models == 60, "60 LCD draws with a separate texture for each car and side");
        for (int f = 0; f < 5; f++) {
            m.render(&in, &out);
            apply_frame(out, texes, stats);
        }
        check(texes.size() == 12, "all 12 textures uploaded");

        {
            const ReconTex& t = texes.begin()->second;
            std::printf("  texture size %dx%d\n", t.w, t.h);
            check(t.w == 3304, "texture width 3304 (JS SCR_W)");
            check(t.h == 944, "texture height 944 (SCR_W / LCD_ASPECT 3.5 — "
                              "NOT the JS runtime 3304/0.2857 = 11564)");
            check(close_to(sample_px(t, t.w / 2, 10), 0x009BC0, 40),
                  "header band is route color (cyan)");
            check(close_to(sample_px(t, t.w / 2, t.h - 30), 0xFFFFFF, 25),
                  "body background is white");
            const int dots =
                count_color(t, 0xED1C24, 60, 0, t.h * 50 / 100, t.w, t.h * 75 / 100)
              + count_color(t, 0x00C850, 60, 0, t.h * 50 / 100, t.w, t.h * 75 / 100);
            check(dots > 100, "route map station dots present (red/green)");
            const int glyphs = count_color(t, 0xFFFFFF, 30,
                                           t.w * 78 / 100, 4, t.w, t.h * 30 / 100);
            check(glyphs > 50, "车号 glass card glyphs present (white digits)");
            /* 直线分支：不应出现环线的竖直轨道 */
            const int railL = rail_column_pixels(t, 0x009BC0, 60, false);
            const int railR = rail_column_pixels(t, 0x009BC0, 60, true);
            check(railL < 200 && railR < 200,
                  "linear page has NO vertical ring rail (not mis-detected as 环线)");
            write_ppm(outPrefix + "wr2a03_lcd_linear.ppm", t);
        }

        /* ---- 开门页：大站名 + 到达文案（独立实例） ---- */
        {
            Snapshot open = build_snapshot(6, cars, stops, 1200000, 1.0, 0.0, 4,
                                           "10号线|Line 10", 0xFF009BC0, 0,
                                           siding, false, true);
            SceneResult r = run_scene(m, open, 4);
            check(!r.texes.empty(), "door-open page still renders");
            if (!r.texes.empty()) {
                const ReconTex& t = r.texes.begin()->second;
                /* 大站名用线路色，字号 K(150)=250px，占满屏中部 */
                const int bigName = count_color(t, 0x009BC0, 60,
                                                0, t.h * 30 / 100, t.w, t.h * 70 / 100);
                check(bigName > 500, "door-open page: big station name in route color");
                write_ppm(outPrefix + "wr2a03_lcd_door_open.ppm", t);
            }
        }

        /* ---- 环线：环形图（独立实例） ---- */
        {
            auto loop = circular_stops();
            Snapshot circ = build_snapshot(6, cars, loop, 2000000, 0.0, 12.0, 5,
                                           "环线|Loop Line", 0xFF009BC0, 1,
                                           siding, false, true);
            SceneResult r = run_scene(m, circ, 4);
            check(!r.texes.empty(), "circular page renders");
            if (!r.texes.empty()) {
                const ReconTex& t = r.texes.begin()->second;
                const int dots =
                    count_color(t, 0xED1C24, 60, 0, t.h * 45 / 100, t.w, t.h * 90 / 100)
                  + count_color(t, 0x00C850, 60, 0, t.h * 45 / 100, t.w, t.h * 90 / 100);
                check(dots > 100, "circular page: ring station dots present");
                /* 环线分支：圆角矩形轨道的左右两段竖直轨道必须存在 */
                const int railL = rail_column_pixels(t, 0x009BC0, 60, false);
                const int railR = rail_column_pixels(t, 0x009BC0, 60, true);
                check(railL > 500 && railR > 500,
                      "circular page draws both vertical ring rails (环线 branch taken)");
                write_ppm(outPrefix + "wr2a03_lcd_circular.ppm", t);
            }
        }

        /* ---- 无线路：清屏回白底（独立实例） ---- */
        {
            Snapshot none = build_snapshot(6, cars, {}, 3000000, 0.0, 0.0, 0,
                                           "", 0, 0, siding, false, false);
            SceneResult r = run_scene(m, none, 3);
            check(r.stats.models == 180, "no-route: still 60 model draws per frame");
            if (!r.texes.empty()) {
                const ReconTex& t = r.texes.begin()->second;
                check(close_to(sample_px(t, t.w / 2, t.h / 2), 0xFFFFFF, 25),
                      "no-route: screen cleared to white");
            }
        }

        check(m.dispose(&in) == 0, "mtrDispose");
        std::free(m.state);
    }

    /* ================= module 2: 车号 ================= */
    {
        Module m;
        if (!m.open(numPath)) return 1;
        std::printf("\nwr2a03_train_num: id=%s type=%s abi=%u state=%zu bytes\n\n",
                    m.id(), m.type(), m.abi(), m.state_size());
        check(m.abi() == MTR_NATIVE_ABI_VERSION, "ABI version matches host (v6)");
        check(m.init != nullptr, "module exports mtrInit (ABI 6 state construction)");
        check(std::strcmp(m.id(), "wr2a03:train_num") == 0, "script id is wr2a03:train_num");

        const int32_t texBefore = g_next_texture;
        const long quadBefore = g_quad_models;
        m.state = std::calloc(1, m.state_size());
        auto stops = linear_stops();
        Snapshot snap = build_snapshot(6, cars, stops, 1000000, 0.0, 17.0, 4,
                                       "10号线|Line 10", 0xFF009BC0, 0,
                                       siding, false, true);

        JcmFrameInput in{};
        in.abi_version = MTR_NATIVE_ABI_VERSION;
        in.resource_kind = MTR_RESOURCE_VEHICLE;
        in.snapshot = snap.buf.data();
        in.state = m.state;
        in.state_size = m.state_size();
        in.host = &host;

        if (m.init) m.init(&in);
        check(m.create(&in) == 0, "mtrCreate");
        /* 6 车厢 × 2 侧牌 + 头牌 + 尾牌 = 14 张纹理 */
        check(g_next_texture - texBefore == 14, "14 GraphicsTextures (6x2 plates + head + tail)");
        check(g_quad_models - quadBefore == 26, "26 host quads (6 cars x 4 side plates + head/tail)");

        std::map<int32_t, ReconTex> texes;
        FrameStats stats;
        JcmFrameOutput out{};
        check(m.render(&in, &out) == 0, "mtrRender frame 1");
        apply_frame(out, texes, stats);
        m.render(&in, &out);
        apply_frame(out, texes, stats);

        /* 6 cars x 2 side plates + head + tail = 14 model draws */
        check(stats.models == 52, "26 model draw records per frame");
        check(texes.size() == 14, "all 14 textures uploaded");

        {
            /* 找一张侧牌纹理（1120x240） */
            const ReconTex* side = nullptr;
            const ReconTex* head = nullptr;
            int sideCount = 0;
            for (const auto& kv : texes) {
                if (kv.second.w == 1120 && kv.second.h == 240) {
                    if (!side) side = &kv.second;
                    sideCount++;
                }
            }
            check(sideCount == 14, "all plates are 1120x240 (JS texSize)");
            if (side) {
                side = side; /* keep static analysis quiet */
                check(count_ink(*side) > 200, "plate ink present (black car number)");
                check(sample_alpha(*side, 5, 5) == 0,
                      "plate background is fully transparent (AlphaComposite.CLEAR)");
                write_ppm(outPrefix + "wr2a03_num_side.ppm", *side);
            }
            (void)head;
        }

        /* ---- 车厢数不匹配 → 红色错误提示 ---- */
        {
            note("  [step] mismatch scene: 6 cars vs 3 car numbers\n");
            std::map<int32_t, ReconTex> texes2;
            FrameStats stats2;
            Snapshot bad = build_snapshot(6, cars, stops, 1000000, 0.0, 17.0, 4,
                                          "10号线|Line 10", 0xFF009BC0, 0,
                                          "10010/01-02-03", false, true);
            in.snapshot = bad.buf.data();
            check(m.render(&in, &out) == 0, "mtrRender (car count mismatch)");
            apply_frame(out, texes2, stats2);
            note("  [step] mismatch frame applied (%d textures)\n",
                 static_cast<int>(texes2.size()));
            check(!texes2.empty(), "mismatched car count still renders");
            if (!texes2.empty()) {
                const ReconTex& t = texes2.begin()->second;
                note("  [step] mismatch texture %dx%d\n", t.w, t.h);
                const int red = count_color(t, 0xED1C24, 60, 0, 0, t.w, t.h, true);
                check(red > 200, "mismatch -> red error line drawn");
                write_ppm(outPrefix + "wr2a03_num_error.ppm", t);
            }
        }

        check(m.dispose(&in) == 0, "mtrDispose");
        std::free(m.state);
    }

    std::printf("\n%s (%d failure%s)\n", failures == 0 ? "SMOKE OK" : "SMOKE FAILED",
                failures, failures == 1 ? "" : "s");
    return failures == 0 ? 0 : 1;
}

} /* anonymous namespace */

int main(int argc, char** argv) {
    return run(argc, argv);
}
