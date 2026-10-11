/**
 * wr2a03_common.hpp — 若益宛 WR2-A03 车侧 LCD / 车号系统
 *                     C++ 移植的共享层
 *
 * 源脚本 (JS → 本文件 / wr2a03_*.cpp, 1:1):
 *   config.js          → 颜色 / 时间 / 布局常量
 *   util.js            → 文本拆分、侧线名解析、门/速度、当前站索引
 *   train_num_util.js  → parseSidingName / getCarDisplayNum
 *   mtr_util.js        → 列车状态机
 *   circular.js        → resolveCircularState (环线判定 + 锁定语义)
 *   data.js            → StationInfo / parseTransferRoute / 拼音首字母
 *   draw_num.js        → drawNum / drawError  (见 wr2a03_train_num.cpp)
 *   draw_common.js / draw_header.js / draw_circular.js / draw_linear.js
 *                      → 见 wr2a03_lcd.cpp
 *
 * 与 JS 的差异（全部为有意为之，逐条列明）：
 *   1. 纹理长宽比取 config.js 声明的 3.5（3304×944），而不是 main.js
 *      运行期算出的倒数 0.2857（3304×11564）——见下方 LCD_ASPECT_CONFIG
 *      的详细说明。这是唯一影响渲染结果的差异，可用
 *      WR2_LCD_USE_JS_ASPECT=1 切回 JS 的字面行为。
 *   2. 屏幕内容签名：JS 每帧重画全部车厢两侧；本移植对每块屏做内容哈希
 *      (pageMode / 闪烁 / 门 / 站序 / 线路 / 侧线名)，内容不变则跳过光栅化，
 *      仍每帧提交贴图与模型绘制——视觉与 JS 等价，CPU 占用更低。
 *   3. 字体与 JS 共用资源包内的 TTF/OTF，通过宿主 FontMetrics / drawString
 *      保留原始字形、字宽、基线和上下行部。无宿主的测试才使用内置字模。
 */
#pragma once

#include <mtr/script.hpp>
#include <mtr/gfx2d.hpp>
#include <mtr/vehicle.hpp>

#include <algorithm>
#include <cmath>
#include <cstdint>
#include <cstring>
#include <string>
#include <vector>

namespace wr2a03 {

using namespace mtr;

/* Same resources as config.js. Use the existing regular Han Sans for the
   error message (draw_num.js's source-han-sans-cn path is absent in the pack). */
inline constexpr const char* FONT_HAN_SANS = "mtr:wr2a03/fonts/source-han-sans/source-han-sans-medium.otf";
inline constexpr const char* FONT_HAN_REGULAR = "mtr:wr2a03/fonts/source-han-sans/source-han-sans-regular.otf";
inline constexpr const char* FONT_ARIAL = "mtr:wr2a03/fonts/arial/arial.ttf";
inline constexpr const char* FONT_ARIAL_BOLD = "mtr:wr2a03/fonts/arial/arialbd.ttf";
inline constexpr const char* FONT_SANS_LAO = "mtr:wr2a03/fonts/sans_lao/sans_lao.ttf";
inline constexpr const char* FONT_HONGLEI = "mtr:wr2a03/fonts/honglei/hongleixingshu.otf";

/* MSVC 在 /std:c++17 下不保证 <cmath> 暴露 M_PI（需要 _USE_MATH_DEFINES），
   这里自带一个常量，避免依赖平台宏。 */
inline constexpr double WR2_PI = 3.14159265358979323846;

/* ==================================================================== */
/* 物理四边形 (main.js 顶层常量, 与模型给定值一致)                        */
/* ==================================================================== */

/* LCD_POS_L / LCD_POS_R — 顶点序 v0→v1 (短边/竖直) → v2→v3 (长边/水平) */
inline constexpr double LCD_POS_L[4][3] = {
    { 0.8627472, 2.4004532, -10.5625},
    { 1.1772487, 2.1962136, -10.5625},
    { 1.1772487, 2.1962136,  -9.25  },
    { 0.8627472, 2.4004532,  -9.25  }
};
inline constexpr double LCD_POS_R[4][3] = {
    {-0.8627472, 2.4004532,  -9.4375},
    {-1.1772487, 2.1962136,  -9.4375},
    {-1.1772487, 2.1962136, -10.75  },
    {-0.8627472, 2.4004532, -10.75  }
};

/* 车号牌四边形: num_leftPos / num_rightPos (train_num.js) */
inline constexpr double NUM_LEFT_POS[4][3] = {
    { 1.470637, 1.353106,  1.75},
    { 1.479363, 0.353144,  1.75},
    { 1.479363, 0.353144, -1.75},
    { 1.470637, 1.353106, -1.75}
};
inline constexpr double NUM_RIGHT_POS[4][3] = {
    {-1.470637, 1.353106, -1.75},
    {-1.479363, 0.353144, -1.75},
    {-1.479363, 0.353144,  1.75},
    {-1.470637, 1.353106,  1.75}
};

/* ==================================================================== */
/* 几何: LCD 长宽比                                                      */
/* ==================================================================== */

/**
 * ★ 与 JS 的一处有意差异（这是整份移植里唯一改变渲染结果的改动）。
 *
 * JS main.js 用 computeLcdAspectFromSlot() 从 LCD 四边形的顶点距离反推比例:
 *     w = |p1 - p0|   // 顶点 v0→v1
 *     h = |p2 - p1|   // 顶点 v1→v2
 *     LCD_ASPECT = w / h
 * 代入上面两个四边形（顶点序 TL→BL→BR→TR）:
 *     w = 0.375,  h = 1.3125   →   LCD_ASPECT = 0.285714 (= 2/7)
 * 再被 main.js 用来覆盖 config.js 的 SCR_W/SCR_H/SCALE_X/SCALE_Y:
 *     SCR_H = round(3304 / 0.285714) = 11564     ← 与配置的 944 相差 12 倍
 *     SCALE_Y = 11564 / 800 = 14.455             ← 每个像素被拉长 14 倍
 *
 * 也就是说：JS 运行时用到的是**长短边的倒数**，而不是长宽比；这与
 * config.js 自己注释里写的 "物理 LCD 长宽比 = 21 : 6 = 3.5"、
 * 以及全部按 TEX_H = 800 / SCALE_X = SCALE_Y = 1.18 调好的布局常量
 * （HEADER_H 217、RING_TOP_Y 400、字号 32/26 …）互相矛盾。按 0.2857
 * 渲染出来的屏会是 3304×11564、纵向被拉伸 12 倍，既不是设计意图，
 * 也要为每节车厢每侧分配约 150 MB 的纹理。
 *
 * 因此这里采用**配置声明的长宽比 3.5**（= 3304×944，SCALE_X = SCALE_Y
 * = 1.18，等比缩放无拉伸），即 config.js 的设计意图，也是上游 jslcd
 * 移植在同样位置采用的取值（其 README「已修复的上游 JS bug」一节）。
 * 需要逐像素复刻 JS 运行期行为时，把 WR2_LCD_USE_JS_ASPECT 定义出来即可。
 */
inline constexpr double LCD_ASPECT_CONFIG = 3.5;

#ifndef WR2_LCD_USE_JS_ASPECT
#  define WR2_LCD_USE_JS_ASPECT 0
#endif

/** main.js computeLcdAspectFromSlot() 的字面等价物（倒数版，见上）。 */
inline double compute_lcd_aspect_js() {
    const double dx = LCD_POS_L[1][0] - LCD_POS_L[0][0];
    const double dy = LCD_POS_L[1][1] - LCD_POS_L[0][1];
    const double dz = LCD_POS_L[1][2] - LCD_POS_L[0][2];
    const double w = std::sqrt(dx * dx + dy * dy + dz * dz);

    const double ex = LCD_POS_L[2][0] - LCD_POS_L[1][0];
    const double ey = LCD_POS_L[2][1] - LCD_POS_L[1][1];
    const double ez = LCD_POS_L[2][2] - LCD_POS_L[1][2];
    const double h = std::sqrt(ex * ex + ey * ey + ez * ez);

    return h > 0.0 ? (w / h) : LCD_ASPECT_CONFIG;
}

inline double compute_lcd_aspect() {
#if WR2_LCD_USE_JS_ASPECT
    return compute_lcd_aspect_js();
#else
    return LCD_ASPECT_CONFIG;
#endif
}

/* ---- 纹理与逻辑绘图尺寸 (config.js) ---- */

inline constexpr int TEX_W = 2800;                       /* config.js */
inline constexpr int TEX_H = 800;                        /* = TEX_W / 3.5 */
inline constexpr double LAYOUT_K_V = TEX_H / 480.0;      /* ≈ 1.6667 */

inline constexpr int SCR_W = 3304;                       /* main.js 覆盖 */
/* SCR_H = round(SCR_W / LCD_ASPECT) = 944 */
inline int scr_h() { return static_cast<int>(std::lround(SCR_W / compute_lcd_aspect())); }
inline double scale_x() { return SCR_W / static_cast<double>(TEX_W); }     /* ≈ 1.18 */
inline double scale_y() { return scr_h() / static_cast<double>(TEX_H); }   /* ≈ 1.18 */

/* 车号牌纹理 (train_num.js num_slotCfg.texSize) */
inline constexpr int NUM_W = 1120;
inline constexpr int NUM_H = 240;

/* JS: Math.round(v * LAYOUT_K) — 全为正数, 半值向上 */
inline int K(double v) { return static_cast<int>(std::lround(v * LAYOUT_K_V)); }

/* ==================================================================== */
/* 颜色常量 (config.js)                                                  */
/* ==================================================================== */

inline constexpr uint32_t LINE_COLOR    = 0xFF009BC0u;   /* clr(0,155,192)   */
inline constexpr uint32_t GRAY_COLOR    = 0xFF768289u;   /* clr(118,130,137) */
inline constexpr uint32_t RED_COLOR     = 0xFFED1C24u;   /* clr(237,28,36)   */
inline constexpr uint32_t GREEN_COLOR   = 0xFF00C850u;   /* clr(0,200,80)    */
inline constexpr uint32_t WHITE_COLOR   = 0xFFFFFFFFu;
inline constexpr uint32_t BLACK_COLOR   = 0xFF000000u;
inline constexpr uint32_t DOT_GRAY      = 0xFFB4B4B4u;   /* clr(180,180,180) */
inline constexpr uint32_t DEEP_GRAY_DOT = 0xFF5A646Bu;   /* clr(90,100,107)  */
inline constexpr uint32_t BLINK_RED     = RED_COLOR;
inline constexpr uint32_t BLINK_GREEN   = GREEN_COLOR;

/* 透明 (AlphaComposite.CLEAR 等价物) */
inline constexpr uint32_t CLEAR_COLOR   = 0x00000000u;

/* ---- 时间常量 (config.js, ms) ---- */
inline constexpr int64_t CYCLE_FULL_MS     = 10000;
inline constexpr int64_t CYCLE_PARTIAL_MS  = 10000;
inline constexpr int64_t BLINK_INTERVAL_MS = 1000;
inline constexpr int64_t STOP_DEBOUNCE_MS  = 3000;

/* ---- 纵向布局常量 (config.js, 按 LAYOUT_K 缩放) ---- */
inline const int HEADER_H      = K(130);                      /* 217 */
inline const int RING_TOP_Y    = K(240);                      /* 400 */
inline const int RING_BOTTOM_Y = K(390);                      /* 650 */
inline const int RING_LEFT_X   = 200;
inline const int RING_RIGHT_X  = TEX_W - 200;                 /* 2600 */
inline const int RING_RADIUS   = (RING_BOTTOM_Y - RING_TOP_Y) / 2;  /* 125 */
inline const int RING_STROKE   = K(24);                       /* 40 */
inline const int RING_PADDING  = K(60);                       /* 100 */

/* ==================================================================== */
/* util.js                                                              */
/* ==================================================================== */

inline std::string get_non_extra_parts(const std::string& name) {
    const size_t idx = name.find("||");
    return idx == std::string::npos ? name : name.substr(0, idx);
}

inline bool is_interchange_route_kept(const std::string& name) {
    static const char* drop[] = {
        "巴士", "晋祠专线", "观光", "空中捷运", "快速直达专线", "公交"
    };
    for (const char* d : drop) {
        if (name.find(d) != std::string::npos) return false;
    }
    return true;
}

inline void append_utf8(std::string& out, uint32_t cp) {
    char buf[4];
    int n = 0;
    if (cp < 0x80) {
        buf[n++] = static_cast<char>(cp);
    } else if (cp < 0x800) {
        buf[n++] = static_cast<char>(0xC0 | (cp >> 6));
        buf[n++] = static_cast<char>(0x80 | (cp & 0x3F));
    } else {
        buf[n++] = static_cast<char>(0xE0 | (cp >> 12));
        buf[n++] = static_cast<char>(0x80 | ((cp >> 6) & 0x3F));
        buf[n++] = static_cast<char>(0x80 | (cp & 0x3F));
    }
    out.append(buf, static_cast<size_t>(n));
}

/* parse_decimal — 解析 [begin, end) 的十进制数字。
 *
 * 不能用 atol/strtol：glibc 2.38 起它们被改写为 __isoc23_strtol，于是二进制
 * 会带上 GLIBC_2.38 符号版本，在 Debian 12 / Ubuntu 22.04（glibc 2.36）上直接
 * 加载失败。这里手写十行，彻底不碰 libc 的数字转换。 */
inline int64_t parse_decimal(const std::string& s, size_t begin, size_t end) {
    int64_t v = 0;
    for (size_t k = begin; k < end && k < s.size(); k++) {
        const unsigned char ch = static_cast<unsigned char>(s[k]);
        if (ch < '0' || ch > '9') break;
        v = v * 10 + static_cast<int64_t>(ch - '0');
    }
    return v;
}

/* splitCjkNonCjk — TextUtil.getCjkParts/getNonCjkParts 等价物 */
inline void split_cjk_non_cjk(const std::string& full, std::string& cn, std::string& en) {
    cn.clear();
    en.clear();
    if (full.empty()) return;

    Utf8Cursor c(full.c_str(), static_cast<int32_t>(full.size()));
    while (!c.done()) {
        const uint32_t cp = c.next();
        if (cp == '|') continue;
        if (is_cjk_cp(cp)) append_utf8(cn, cp);
        else append_utf8(en, cp);
    }
    while (!en.empty() && en.front() == ' ') en.erase(en.begin());
    while (!en.empty() && en.back() == ' ') en.pop_back();

    if (cn.empty()) {
        const size_t bar = full.find('|');
        if (bar != std::string::npos) {
            cn = full.substr(0, bar);
            en = full.substr(bar + 1);
            while (!cn.empty() && cn.front() == ' ') cn.erase(cn.begin());
            while (!cn.empty() && cn.back() == ' ') cn.pop_back();
            while (!en.empty() && en.front() == ' ') en.erase(en.begin());
            while (!en.empty() && en.back() == ' ') en.pop_back();
        } else {
            cn = full;
            en.clear();
        }
    }
}

/* getTravelLeftSide — 模型侧 ↔ 行驶方向左侧 (isReversed 翻转) */
inline constexpr char TRAVEL_LEFT_MODEL_SIDE_WHEN_FORWARD = 'L';

inline char get_travel_left_side(const Train& train, bool& reversed) {
    reversed = train.reversed();
    const char forwardLeft = TRAVEL_LEFT_MODEL_SIDE_WHEN_FORWARD;
    return reversed ? (forwardLeft == 'L' ? 'R' : 'L') : forwardLeft;
}

inline double vehicle_speed_ms(const Train& train) {
    return std::fabs(train.speed_ms());
}

inline double vehicle_door_value(const Train& train) {
    return std::max(0.0, std::min(1.0, train.door_value()));
}

/* ==================================================================== */
/* train_num_util.js                                                    */
/* ==================================================================== */

struct SidingInfo {
    std::string vehicleNum;
    std::vector<std::string> carNums;
    std::string formation;
    std::string extra;
};

inline std::string trim_str(const std::string& s) {
    const size_t a = s.find_first_not_of(" \t\r\n");
    if (a == std::string::npos) return "";
    const size_t b = s.find_last_not_of(" \t\r\n");
    return s.substr(a, b - a + 1);
}

inline std::vector<std::string> split_str(const std::string& s, char sep) {
    std::vector<std::string> out;
    size_t start = 0;
    for (size_t i = 0; i <= s.size(); i++) {
        if (i == s.size() || s[i] == sep) {
            out.push_back(trim_str(s.substr(start, i - start)));
            start = i + 1;
        }
    }
    return out;
}

/**
 * parseSidingName — 侧线名 → 车号 / 车厢编号
 *   "10010/01-02-03-04-05-06" → 10010 + [01..06]
 *   "10010 Tc1 A1-1"          → 10010, 编组 Tc1, extra A1-1, carNums=[1,1]
 *   "01-02-03"                → 无车号, 仅车厢编号
 */
inline SidingInfo parse_siding_name(const std::string& sidingName) {
    SidingInfo result;
    if (sidingName.empty()) return result;

    const size_t slash = sidingName.find('/');
    if (slash != std::string::npos) {
        result.vehicleNum = trim_str(sidingName.substr(0, slash));
        result.carNums = split_str(sidingName.substr(slash + 1), '-');
        return result;
    }

    /* 按空白切分 (JS: sidingName.split(/\s+/) 并把空串滤掉) */
    std::vector<std::string> parts;
    {
        size_t i = 0;
        while (i < sidingName.size()) {
            while (i < sidingName.size() && std::isspace(static_cast<unsigned char>(sidingName[i]))) i++;
            const size_t start = i;
            while (i < sidingName.size() && !std::isspace(static_cast<unsigned char>(sidingName[i]))) i++;
            if (i > start) parts.push_back(sidingName.substr(start, i - start));
        }
    }
    if (parts.size() >= 3) {
        result.vehicleNum = parts[0];
        result.formation = parts[1];
        result.extra = parts[2];
        /* JS: extra.match(/\d+/g) → 连续数字串 */
        std::string run;
        for (char ch : result.extra) {
            if (ch >= '0' && ch <= '9') {
                run.push_back(ch);
            } else if (!run.empty()) {
                result.carNums.push_back(run);
                run.clear();
            }
        }
        if (!run.empty()) result.carNums.push_back(run);
        return result;
    }

    result.carNums = split_str(sidingName, '-');
    return result;
}

/** getCarDisplayNum — LCD 顶部信息栏的车号 ("10010 01") */
inline std::string get_car_display_num(const std::string& sidingName, int carIndex) {
    const SidingInfo parsed = parse_siding_name(sidingName);
    if (parsed.carNums.empty() || carIndex < 0
        || carIndex >= static_cast<int>(parsed.carNums.size())) {
        return parsed.vehicleNum;
    }
    const std::string carName = parsed.carNums[static_cast<size_t>(carIndex)];
    std::string suffix = carName;
    if (!parsed.vehicleNum.empty() && carName.rfind(parsed.vehicleNum, 0) == 0) {
        suffix = carName.substr(parsed.vehicleNum.size());
    }
    if (!parsed.vehicleNum.empty()) return parsed.vehicleNum + " " + suffix;
    return suffix;
}

/** getVehicleDisplayNum — 头/尾牌车号 ("10010 Tc1") */
inline std::string get_vehicle_display_num(const std::string& sidingName) {
    const SidingInfo parsed = parse_siding_name(sidingName);
    if (!parsed.formation.empty()) return parsed.vehicleNum + " " + parsed.formation;
    if (!parsed.vehicleNum.empty() && !parsed.carNums.empty()) {
        const std::string first = parsed.carNums[0];
        if (first.rfind(parsed.vehicleNum, 0) == 0) {
            const std::string suffix = first.substr(parsed.vehicleNum.size());
            if (!suffix.empty()) return parsed.vehicleNum + " " + suffix;
        }
    }
    return parsed.vehicleNum.empty() ? sidingName : parsed.vehicleNum;
}

/* ==================================================================== */
/* data.js                                                              */
/* ==================================================================== */

struct Transfer {
    std::string name;
    uint32_t color = 0xFF646464u;
};

struct ExitInfo {
    std::string name;
    std::vector<std::string> destinations;
};

struct StationInfo {
    std::string id;
    std::string nameCn;
    std::string nameEn;
    std::vector<Transfer> transfers;
    std::vector<ExitInfo> exits;
};

inline std::string get_station_id_str(const Stop& stop) {
    if (stop.station_id() != 0) return "s" + std::to_string(stop.station_id());
    if (stop.platform_id() != 0) return "p" + std::to_string(stop.platform_id());
    return "";
}

/* getStationsFromStops — thisRouteStops → StationInfo[] */
inline std::vector<StationInfo> get_stations_from_stops(const StopList& stops,
                                                        const std::string& thisRouteName) {
    std::vector<StationInfo> stations;
    const int32_t n = stops.size();
    if (n > 0) stations.reserve(static_cast<size_t>(n));

    for (int32_t i = 0; i < n; i++) {
        const Stop stop = stops.at(i);
        StationInfo st;
        const std::string fullName(stop.name().str());
        split_cjk_non_cjk(fullName, st.nameCn, st.nameEn);
        if (st.nameCn.empty() && !fullName.empty()) st.nameCn = fullName;
        st.id = get_station_id_str(stop);

        const int32_t ic = stop.interchange_count();
        for (int32_t j = 0; j < ic; j++) {
            const auto ir = stop.interchange(j);
            const std::string irName(ir.name.str());
            if (irName.empty()) continue;
            if (!is_interchange_route_kept(irName)) continue;
            if (get_non_extra_parts(irName) == thisRouteName) continue;
            Transfer t;
            t.name = irName;
            t.color = static_cast<uint32_t>(ir.color) | 0xFF000000u;
            st.transfers.push_back(t);
        }

        const int32_t ec = stop.exit_count();
        for (int32_t j = 0; j < ec; j++) {
            const Stop::Exit ex = stop.exit(j);
            ExitInfo ei;
            ei.name = std::string(ex.name.str());
            for (int32_t k = 0; k < ex.destination_count; k++) {
                ei.destinations.emplace_back(ex.destination(k).str());
            }
            if (!ei.destinations.empty()) st.exits.push_back(std::move(ei));
        }
        stations.push_back(std::move(st));
    }

    /* 环线：末站的"绕回首站"副本去重（data.js 同款守卫 + 原生侧收紧）：
       只有在首末站 id/名称相同、且该 route 确实为环线时才 pop，
       否则会把"起点与终点同名"的普通线路少画一站。 */
    if (stations.size() > 1) {
        const StationInfo& first = stations.front();
        const StationInfo& last = stations.back();
        const bool sameId = !first.id.empty() && first.id == last.id;
        const bool sameName = !first.nameCn.empty()
            && first.nameCn == last.nameCn && first.nameEn == last.nameEn;
        if (sameId || sameName) stations.pop_back();
    }
    return stations;
}

/* ---- parseTransferRoute + 排序 (data.js) ---- */

enum TransferType {
    TRANSFER_NUMBER = 0,
    TRANSFER_LETTER_NUMBER = 1,
    TRANSFER_CHINESE = 2
};

struct ParsedTransferRoute {
    std::string id, cn, en, letter;
    long num = 0;
    int type = TRANSFER_CHINESE;
    uint32_t color = 0xFF646464u;
};

inline ParsedTransferRoute parse_transfer_route(const std::string& name) {
    ParsedTransferRoute out;
    const std::string clean = get_non_extra_parts(name);
    const size_t bar = clean.find('|');
    if (bar != std::string::npos) {
        out.cn = trim_str(clean.substr(0, bar));
        out.en = trim_str(clean.substr(bar + 1));
    } else {
        split_cjk_non_cjk(clean, out.cn, out.en);
    }
    if (out.cn.empty() && !clean.empty()) out.cn = clean;

    size_t i = 0;
    while (i < out.cn.size() && std::isalpha(static_cast<unsigned char>(out.cn[i]))) i++;
    const std::string letter = out.cn.substr(0, i);
    size_t j = i;
    while (j < out.cn.size() && std::isdigit(static_cast<unsigned char>(out.cn[j]))) j++;
    if (j > i) {
        out.num = parse_decimal(out.cn, i, j);
        out.id = letter + out.cn.substr(i, j - i);
        out.letter = letter;
        out.type = letter.empty() ? TRANSFER_NUMBER : TRANSFER_LETTER_NUMBER;
    } else {
        out.id = out.cn;
        out.letter.clear();
        out.num = 0;
        out.type = TRANSFER_CHINESE;
    }
    return out;
}

/* getChinesePinyinInitial — data.js 查表 */
inline char pinyin_initial(uint32_t cp) {
    struct Ent { uint32_t cp; char py; };
    static const Ent map[] = {
        {0x4E00,'Y'},{0x4E8C,'E'},{0x4E09,'S'},{0x56DB,'S'},{0x4E94,'W'},
        {0x516D,'L'},{0x4E03,'Q'},{0x516B,'B'},{0x4E5D,'J'},{0x5341,'S'},
        {0x4E1C,'D'},{0x897F,'X'},{0x5357,'N'},{0x5317,'B'},{0x4E2D,'Z'},
        {0x4E0A,'S'},{0x4E0B,'X'},{0x5927,'D'},{0x5C0F,'X'},{0x65B0,'X'},
        {0x8001,'L'},{0x9AD8,'G'},{0x4F4E,'D'},{0x5FEB,'K'},{0x901F,'S'},
        {0x6162,'M'},{0x673A,'J'},{0x573A,'C'},{0x8F66,'C'},{0x7AD9,'Z'},
        {0x516C,'G'},{0x4EA4,'J'},{0x5DF4,'B'},{0x58EB,'S'},{0x89C2,'G'},
        {0x5149,'G'},{0x7A7A,'K'},{0x6377,'J'},{0x8FD0,'Y'},{0x5730,'D'},
        {0x94C1,'T'},{0x53F7,'H'},{0x7EBF,'X'},{0x652F,'Z'},{0x5EF6,'Y'},
        {0x957F,'C'},{0x73AF,'H'},{0x5185,'N'},{0x5916,'W'},{0x9996,'S'},
        {0x90FD,'D'},{0x56FD,'G'},{0x9645,'J'},{0x56ED,'Y'},{0x5C71,'S'},
        {0x6CB3,'H'},{0x6E56,'H'},{0x6C5F,'J'},{0x6D77,'H'},{0x6E7E,'W'},
        {0x6865,'Q'},{0x8DEF,'L'},{0x8857,'J'},{0x95E8,'M'},{0x53E3,'K'}
    };
    for (const Ent& e : map) {
        if (e.cp == cp) return e.py;
    }
    return '\x7F';
}

inline int compare_transfer_route(const ParsedTransferRoute& a, const ParsedTransferRoute& b) {
    if (a.type != b.type) return a.type < b.type ? -1 : 1;
    if (a.type == TRANSFER_NUMBER) return a.num < b.num ? -1 : (a.num > b.num ? 1 : 0);
    if (a.type == TRANSFER_LETTER_NUMBER) {
        if (a.letter != b.letter) return a.letter < b.letter ? -1 : 1;
        return a.num < b.num ? -1 : (a.num > b.num ? 1 : 0);
    }
    const uint32_t cpa = !a.cn.empty() ? Utf8Cursor(a.cn.c_str(), -1).next() : 0;
    const uint32_t cpb = !b.cn.empty() ? Utf8Cursor(b.cn.c_str(), -1).next() : 0;
    const char pa = pinyin_initial(cpa), pb = pinyin_initial(cpb);
    if (pa != pb) return pa < pb ? -1 : 1;
    return a.cn < b.cn ? -1 : (a.cn > b.cn ? 1 : 0);
}

/* normalizeLineId — "10号线"→"10", "S1线"→"S1" */
inline std::string normalize_line_id(const std::string& name) {
    if (name.empty()) return "";
    std::string clean = get_non_extra_parts(name);
    const size_t bar = clean.find('|');
    if (bar != std::string::npos) clean = clean.substr(0, bar);
    clean = trim_str(clean);

    size_t i = 0;
    while (i < clean.size() && std::isalpha(static_cast<unsigned char>(clean[i]))) i++;
    size_t j = i;
    while (j < clean.size() && std::isdigit(static_cast<unsigned char>(clean[j]))) j++;
    if (j > i) return clean.substr(0, j);

    auto strip_suffix = [](std::string& s, const char* suf) {
        const size_t n = std::strlen(suf);
        if (s.size() >= n && s.compare(s.size() - n, n, suf) == 0) s.erase(s.size() - n);
    };
    std::string cn = clean;
    strip_suffix(cn, "捷运");
    strip_suffix(cn, "号线");
    strip_suffix(cn, "线");
    cn = trim_str(cn);
    return cn.empty() ? clean : cn;
}

/* ==================================================================== */
/* circular.js — 环线判定                                                */
/* ==================================================================== */

struct CircularResult {
    bool isCircular = false;
    int circularState = 0;    /* 0 NONE, 1 CLOCKWISE, 2 ANTICLOCKWISE */
};

/**
 * resolveCircularState — circular.js 的等价实现。
 *
 * 判定方式（与 JS 一一对应）：
 *  (i)  当前 route 自身为环线（v2 快照的 circular_state，宿主由
 *       SimplifiedRoute.getCircularState() 写入）→ 环线并**锁定**
 *       （JS state._isCircularLocked：未重载前恒为环线）。
 *  (ii) 否则遍历列车将要经过的每一个 stop，读取 **该 stop 所属 route**
 *       的 CircularState（ABI 5：JcmStop.route_circular_state，等价于
 *       JS 的 stop.route.getCircularState()）。任意一个非 NONE 即为环线，
 *       **不锁定**（换到非环线后会重新判定）。
 *
 * 注意：JS 方式 (ii) 用"该 route 的站台数"作为步进量跳过同一条线路的
 * 连续站点；ABI 5 里每个 stop 都带自己的 route 状态，因此逐步遍历即可
 * 得到同样的结论，无需猜测步长 —— 早期版本用"同一 route 在 stop 列表里
 * 出现两次即视为环线"的形态判据，会把任何多站线路都误判成环线。
 */
inline CircularResult resolve_circular_state(const Train& train, bool& lockedFlag,
                                             int& storedState) {
    CircularResult result;
    if (lockedFlag) {
        result.isCircular = true;
        result.circularState = storedState;
        return result;
    }
    storedState = 0;

    /* ---------- 方式 (i)：当前 route ---------- */
    const uint8_t cs = train.circular_state();
    if (cs == 1 || cs == 2) {
        result.isCircular = true;
        result.circularState = cs;
        lockedFlag = true;
        storedState = cs;
        return result;
    }

    /* ---------- 方式 (ii)：遍历整个 stop 列表 ---------- */
    const StopList all = train.stops();
    const int64_t thisRoute = train.this_route_id();
    for (int32_t i = 0; i < all.size(); i++) {
        const Stop stop = all.at(i);
        const uint8_t sv = stop.route_circular_state();
        if (sv == 1 || sv == 2) {
            result.isCircular = true;
            result.circularState = sv;
            storedState = sv;              /* 不锁定 */
            return result;
        }
        /* 宿主未提供 per-stop 状态（老快照）时，退回到"当前 route 在
           列表中出现两次即绕回"的保守判据。 */
        if (sv == 0 && stop.route_id() == thisRoute) {
            const int32_t next = i + 1;
            if (next < all.size() && all.at(next).route_id() == thisRoute
                && all.at(next).station_id() == all.at(i).station_id()) {
                result.isCircular = true;
                result.circularState = 2;
                storedState = 2;
                return result;
            }
        }
    }
    return result;
}

inline const char* circ_cn(int cs) {
    if (cs == 1) return "内环";
    if (cs == 2) return "外环";
    return "环线";
}
inline const char* circ_en(int cs) {
    if (cs == 1) return "Inner Loop";
    if (cs == 2) return "Outer Loop";
    return "Loop";
}

/* ==================================================================== */
/* mtr_util.js — 列车状态机                                              */
/* ==================================================================== */

enum TrainStatus {
    STATUS_NO_ROUTE = 0,
    STATUS_WAITING_FOR_DEPARTURE,
    STATUS_LEAVING_DEPOT,
    STATUS_ON_ROUTE,
    STATUS_ARRIVED,
    STATUS_CHANGING_ROUTE,
    STATUS_RETURNING_TO_DEPOT
};

inline TrainStatus get_train_status(const Train& train) {
    const StopList all = train.stops();
    const StopList thisRoute = train.this_route_stops();
    if (all.size() == 0) return STATUS_NO_ROUTE;
    if (!train.on_route()) return STATUS_WAITING_FOR_DEPARTURE;
    const int32_t nextIdx = train.next_stop_index();
    if (nextIdx >= all.size()) return STATUS_RETURNING_TO_DEPOT;
    if (train.rail_progress() == all.at(nextIdx).distance()) return STATUS_ARRIVED;
    if (nextIdx == 0) return STATUS_LEAVING_DEPOT;
    if (thisRoute.size() > 0 && nextIdx >= thisRoute.size()) return STATUS_CHANGING_ROUTE;
    return STATUS_ON_ROUTE;
}

/* ==================================================================== */
/* util.js — 当前站索引                                                  */
/* ==================================================================== */

/**
 * getCurrentStationIdx:
 *   rawN = vehicle.getNextStopIndex(thisRouteStops, 0.5)  （快照里预计算）
 *   环线 : doorOpen ? rawN : rawN - 1   （取模）
 *   非环 : rawN >= n ? n-1 : (doorOpen ? rawN : max(0, rawN - 1))
 * 与 JS 完全一致；rawN 不可用时退回 _stopCountIdx。
 */
inline int get_current_station_idx(const Train& train, int stationCount,
                                   bool isCircular, bool isDoorOpen,
                                   int stopCountIdx) {
    if (stationCount <= 0) return 0;

    const int32_t raw = train.next_stop_index();
    if (raw < 0) return std::max(0, std::min(stopCountIdx, stationCount - 1));

    const int n = stationCount;
    if (isCircular) {
        const int m = ((raw % n) + n) % n;
        return isDoorOpen ? m : ((m - 1 + n) % n);
    }
    if (raw >= n) return n - 1;
    return isDoorOpen ? raw : std::max(0, raw - 1);
}

/* ==================================================================== */
/* 纹理尺寸容器                                                          */
/* ==================================================================== */

/** LCD 屏的纹理尺寸（运行期常量，create 时算一次） */
struct ScreenSize {
    int w = 0;
    int h = 0;
    double sx = 1.0;
    double sy = 1.0;
};

inline ScreenSize compute_screen_size() {
    ScreenSize s;
    s.w = SCR_W;
    s.h = static_cast<int>(scr_h());
    s.sx = scale_x();
    s.sy = scale_y();
    return s;
}

/* ==================================================================== */
/* 模型句柄：宿主构建的四边形（DisplayHelper 等价物）                     */
/* ==================================================================== */

/**
 * 把 JS slot 的 pos 数组交给宿主，换取一个四边形模型句柄。
 * 宿主不可用时返回 -1，调用方必须跳过绘制（与 JS 里
 * `dh.graphicsFor()` 返回 null 时 return 的守卫等价）。
 */
inline int32_t acquire_quad(const JcmHostServices* host, int32_t texture_handle,
                            const double pos[4][3], float u1, float v1,
                            float u2, float v2, int32_t render_stage = 1,
                            double offset_z = 0) {
    if (!host || !host->acquire_quad_model || texture_handle < 0) return -1;

    float verts[12];
    for (int i = 0; i < 4; i++) {
        verts[i * 3 + 0] = static_cast<float>(pos[i][0]);
        verts[i * 3 + 1] = static_cast<float>(pos[i][1]);
        verts[i * 3 + 2] = static_cast<float>(pos[i][2] + offset_z);
    }
    /* DisplayHelper: top-left, bottom-left, bottom-right, top-right. */
    const float uv[8] = {u1, v1, u1, v2, u2, v2, u2, v1};
    return host->acquire_quad_model(host->user, verts, uv, 4, render_stage,
                                    texture_handle);
}

inline void release_quad(const JcmHostServices* host, int32_t handle) {
    if (handle < 0 || !host || !host->release_model) return;
    host->release_model(host->user, handle);
}

/* ==================================================================== */
/* 屏幕内容签名（repaint-on-change）                                     */
/* ==================================================================== */

inline uint64_t fnv1a(uint64_t h, const void* data, size_t len) {
    const uint8_t* p = static_cast<const uint8_t*>(data);
    for (size_t i = 0; i < len; i++) {
        h ^= p[i];
        h *= 1099511628211ull;
    }
    return h;
}

inline uint64_t fnv_str(uint64_t h, const std::string& s) {
    return fnv1a(h, s.data(), s.size());
}

} /* namespace wr2a03 */
