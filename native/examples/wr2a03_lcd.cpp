/**
 * wr2a03_lcd.cpp — 若益宛 WR2-A03 车侧路线图 LCD (原生 C++ 版)
 *
 * 源脚本 (JS 全量移植, 逐函数对应):
 *   main.js        → create / render / dispose, drawOneScreen, clearScreen
 *   draw_header.js → drawHeader / drawRouteNameBlock / measureRouteNameBlock /
 *                    isNumberRouteName / drawDestinationBlock /
 *                    drawVehicleNumBox
 *   draw_common.js → drawCenteredText / drawArrow / drawArrowChar /
 *                    computeAdaptiveFontSize / computePerTextCompressX /
 *                    drawAdaptiveCenteredText / getStationPos /
 *                    getLinearStationPos / appendRingSegment /
 *                    drawProgressOverlay / normalizeLineId /
 *                    drawTransferBadges / drawExitInfo / drawBigStationName /
 *                    drawTerminusMessage
 *   draw_circular.js → drawCircularFullMap / drawCircularPartialMap
 *   draw_linear.js   → drawLinearFullMap / drawLinearPartialMap
 *   data.js        → drawPartialTransferInfo (换乘面板)
 *   config.js / util.js / circular.js / mtr_util.js / train_num_util.js
 *                  → wr2a03_common.hpp
 *
 * 渲染管线 (与 JS 语义一致):
 *   DisplayHelper.graphicsFor(slot) → Graphics2D 绘制 → texture.upload()
 *   → ctx.drawCarModel(model, car, null)
 * 对应:
 *   Gfx2D(GraphicsTexture)          → 纯 C++ 光栅化  → tex.upload(frame)
 *   → ctx.draw_car_model(quad, car, nullptr)
 * 四边形模型由宿主按 acquire_quad_model(顶点, UV, 纹理句柄) 构建,
 * 与 JS DisplayHelper 从同一组 pos 顶点 + texArea 生成的网格等价。
 *
 * mtr_custom_resources.json:
 *   "vehicles":      [{ "id": "...", "scriptId": "wr2a03:lcd", ... }],
 *   "vehicleScripts": [{
 *       "id": "wr2a03:lcd", "language": "cpp",
 *       "nativeLibrary": { "windows": "mtr:wr2a03/natives/windows-x64/wr2a03_lcd.dll",
 *                          "linux":   "mtr:wr2a03/natives/linux-x64/libwr2a03_lcd.so" }
 *   }]
 */
#include "wr2a03_common.hpp"

namespace wr2a03 {
namespace {

/* ==================================================================== */
/* 每节车厢的两块屏                                                      */
/* ==================================================================== */

struct CarScreens {
    GraphicsTexture left;
    GraphicsTexture right;
    std::array<int32_t, 5> modelsLeft{{-1, -1, -1, -1, -1}};
    std::array<int32_t, 5> modelsRight{{-1, -1, -1, -1, -1}};
    uint64_t leftSig = 0;      /* repaint-on-change 签名（含 blink） */
    uint64_t rightSig = 0;
    uint64_t leftLaySig = 0;   /* 只看布局的签名（不含 blink） */
    uint64_t rightLaySig = 0;
    bool everPainted = false;  /* 首帧也要清屏（见 render 里的无线路分支） */
};

/* ---- 每帧的绘制输入 (main.js 的 info 对象) ---- */

struct ScreenInfo {
    const std::vector<StationInfo>* stations = nullptr;
    int currentIdx = 0;
    int nextIdx = 0;
    bool isCircular = false;
    int circularState = 0;                 /* 0 NONE, 1 CW, 2 ACW */
    std::string destinationCn, destinationEn;
    bool isDoorOpen = false;
    bool blinkState = false;
    uint32_t routeColor = LINE_COLOR;
    std::string routeNameCn, routeNameEn;
    std::string vehicleNum;
};

struct Wr2LcdState {
    /* --- JS state --- */
    int64_t lastCycleTime = 0;
    int64_t lastBlinkTime = 0;
    bool blinkState = false;
    int pageMode = 0;                       /* 0 full, 1 partial */
    bool isDoorOpen = false;
    bool lastDoorOpen = false;
    std::string cachedRouteId;
    std::vector<StationInfo> stations;
    std::string destinationCn, destinationEn;
    bool isCircular = false;
    int circularState = 0;
    bool circularLocked = false;
    uint32_t routeColor = LINE_COLOR;
    std::string routeNameCn, routeNameEn;
    int stopCountIdx = -1;
    int64_t lastCountTime = 0;
    int lastIdxShown = -1;

    /* --- 原生侧资源 --- */
    std::vector<CarScreens> cars;
    int screenW = 0, screenH = 0;
    double sx = 1.0, sy = 1.0;
};

/* ==================================================================== */
/* draw_common.js                                                        */
/* ==================================================================== */

void draw_centered_text(Gfx2D& g, const std::string& text, double cx,
                        double y_baseline, double size, uint32_t color) {
    if (text.empty()) return;
    g.set_color(color);
    const double w = g.font_width(size, text.c_str());
    g.draw_text(cx - w * 0.5, y_baseline, size, text.c_str());
}

/* drawArrow — GeneralPath 三角形, 四方向 */
void draw_arrow(Gfx2D& g, double x, double y, char dir, uint32_t color, double size) {
    g.set_color(color);
    double xs[3], ys[3];
    switch (dir) {
        case 'R':
            xs[0] = x - size;       ys[0] = y - size * 0.6;
            xs[1] = x + size;       ys[1] = y;
            xs[2] = x - size;       ys[2] = y + size * 0.6;
            break;
        case 'L':
            xs[0] = x + size;       ys[0] = y - size * 0.6;
            xs[1] = x - size;       ys[1] = y;
            xs[2] = x + size;       ys[2] = y + size * 0.6;
            break;
        case 'U':
            xs[0] = x - size * 0.6; ys[0] = y + size;
            xs[1] = x;              ys[1] = y - size;
            xs[2] = x + size * 0.6; ys[2] = y + size;
            break;
        default: /* 'D' */
            xs[0] = x - size * 0.6; ys[0] = y - size;
            xs[1] = x;              ys[1] = y + size;
            xs[2] = x + size * 0.6; ys[2] = y - size;
            break;
    }
    g.fill_polygon(xs, ys, 3);
}

/* computeAdaptiveFontSize */
double compute_adaptive_font_size(double baseSize, double maxW, double availW,
                                  double minSize) {
    if (maxW <= 0 || availW >= maxW) return baseSize;
    double target = baseSize * (availW / maxW);
    if (target < minSize) target = minSize;
    return std::lround(target);
}

/* computePerTextCompressX — 压缩比量化到 1/20 步进 */
double compute_per_text_compress_x(double textW, double availW) {
    if (textW <= availW || textW <= 0) return 1.0;
    const double natural = availW / textW;
    int steps = static_cast<int>(std::floor(natural * 20.0 + 1e-9));
    if (steps < 10) steps = 10;
    if (steps > 20) steps = 20;
    return steps / 20.0;
}

/* drawAdaptiveCenteredText */
void draw_adaptive_centered_text(Gfx2D& g, const std::string& text, double cx,
                                 double y_baseline, double size, double availW,
                                 uint32_t color) {
    if (text.empty()) return;
    const double origW = g.font_width(size, text.c_str());
    const double compress = compute_per_text_compress_x(origW, availW);
    g.set_color(color);
    g.draw_text_compressed(cx - origW * compress * 0.5, y_baseline, size,
                           text.c_str(), compress);
}

/* getStationPos — 环线站点位置 (上/下两行) */
struct StationPos {
    double x = 0, y = 0;
    int section = 0;      /* 0 top, 1 bottom */
};

StationPos get_station_pos(int index, int total, bool reversed) {
    const double topStartX = RING_LEFT_X + RING_RADIUS + RING_PADDING;
    const double topEndX = RING_RIGHT_X - RING_RADIUS - RING_PADDING;
    const int topCount = (total + 1) / 2;          /* JS Math.ceil(n / 2) */
    const int botCount = total - topCount;

    StationPos p;
    if (index < topCount) {
        double t = topCount <= 1 ? 0.5
                                 : static_cast<double>(index) / (topCount - 1);
        if (reversed) t = 1.0 - t;
        p.x = topStartX + t * (topEndX - topStartX);
        p.y = RING_TOP_Y;
        p.section = 0;
    } else {
        const int i = index - topCount;
        double t = botCount <= 1 ? 0.5
                                 : static_cast<double>(i) / (botCount - 1);
        if (reversed) t = 1.0 - t;
        p.x = topEndX - t * (topEndX - topStartX);
        p.y = RING_BOTTOM_Y;
        p.section = 1;
    }
    return p;
}

/* getLinearStationPos — 直线站点位置 (奇偶交替上下) */
struct LinearPos {
    double x = 0, y = 0;
    bool above = false;
};

LinearPos get_linear_station_pos(int index, int total) {
    const double startX = RING_LEFT_X + RING_PADDING;
    const double endX = RING_RIGHT_X - RING_PADDING;
    const double y = (RING_TOP_Y + RING_BOTTOM_Y) * 0.5;

    LinearPos p;
    if (total <= 1) {
        p.x = (startX + endX) * 0.5;
    } else {
        const double t = static_cast<double>(index) / (total - 1);
        p.x = startX + t * (endX - startX);
    }
    p.y = y;
    p.above = (index % 2 == 1);
    return p;
}

/* appendRingSegment — 圆角矩形轨道弧段 (24 段折线逼近) */
void append_ring_segment(std::vector<double>& xs, std::vector<double>& ys,
                         const StationPos& pa, const StationPos& pb, bool clockwise) {
    if (pa.section == pb.section) {
        xs.push_back(pb.x);
        ys.push_back(pb.y);
        return;
    }
    bool useRightArc;
    if (pa.section == 0 && pb.section == 1) useRightArc = clockwise;
    else                                    useRightArc = !clockwise;

    const double cx = useRightArc ? (RING_RIGHT_X - RING_RADIUS)
                                  : (RING_LEFT_X + RING_RADIUS);
    const double cy = (RING_TOP_Y + RING_BOTTOM_Y) * 0.5;

    if (pa.section == 0) { xs.push_back(cx); ys.push_back(RING_TOP_Y); }
    else                 { xs.push_back(cx); ys.push_back(RING_BOTTOM_Y); }

    const int steps = 24;
    for (int s = 1; s <= steps; s++) {
        double a;
        if (pa.section == 0) {
            a = useRightArc ? (-WR2_PI / 2 + static_cast<double>(s) / steps * WR2_PI)
                            : (-WR2_PI / 2 - static_cast<double>(s) / steps * WR2_PI);
        } else {
            a = useRightArc ? (WR2_PI / 2 - static_cast<double>(s) / steps * WR2_PI)
                            : (WR2_PI / 2 + static_cast<double>(s) / steps * WR2_PI);
        }
        xs.push_back(cx + RING_RADIUS * std::cos(a));
        ys.push_back(cy + RING_RADIUS * std::sin(a));
    }
    xs.push_back(pb.x);
    ys.push_back(pb.y);
}

/* drawArrowChar — 进度段中点上的方向箭头 */
void draw_arrow_char(Gfx2D& g, char dir, double cx, double cy) {
    const char* text = dir == 'R' ? "→" : dir == 'L' ? "←" : dir == 'U' ? "↑" : "↓";
    const JcmFontMetrics fm = g.font_metrics(30, text, FONT_HAN_SANS);
    g.set_color(WHITE_COLOR);
    g.draw_text(cx - fm.width * 0.5, cy + (fm.ascent - fm.descent) * 0.5,
                30, text, FONT_HAN_SANS);
}

/* drawProgressOverlay — 环线当前→下一站区段闪烁 */
void draw_progress_overlay(Gfx2D& g, const Wr2LcdState& state, const ScreenInfo& info,
                           bool reversed, bool clockwise) {
    const int n = static_cast<int>(info.stations->size());
    if (n <= 0) return;
    const StationPos p1 = get_station_pos(info.currentIdx, n, reversed);
    const StationPos p2 = get_station_pos(info.nextIdx, n, reversed);

    std::vector<double> xs, ys;
    xs.reserve(60);
    ys.reserve(60);
    xs.push_back(p1.x);
    ys.push_back(p1.y);
    append_ring_segment(xs, ys, p1, p2, clockwise);

    const int count = static_cast<int>(xs.size());
    g.set_color(BLACK_COLOR);
    g.stroke_polyline(xs.data(), ys.data(), count, RING_STROKE + K(3));
    g.set_color(state.blinkState ? BLINK_GREEN : BLINK_RED);
    g.stroke_polyline(xs.data(), ys.data(), count, RING_STROKE);

    double midX, midY;
    char dir;
    if (p1.section == p2.section) {
        midX = (p1.x + p2.x) * 0.5;
        midY = p1.y;
        dir = p2.x > p1.x ? 'R' : 'L';
    } else {
        const double ringCenterX = (RING_LEFT_X + RING_RIGHT_X) * 0.5;
        midX = ((p1.x + p2.x) * 0.5 > ringCenterX) ? RING_RIGHT_X : RING_LEFT_X;
        midY = (RING_TOP_Y + RING_BOTTOM_Y) * 0.5;
        dir = (p2.section == 1) ? 'D' : 'U';
    }
    draw_arrow_char(g, dir, midX, midY);
}

/* drawTransferBadges — 环线站点换乘胶囊 */
void draw_transfer_badges(Gfx2D& g, const std::vector<Transfer>& transfers,
                          const StationPos& pos) {
    if (transfers.empty()) return;
    const double badgeH = K(26);
    const double gap = K(6);

    std::vector<std::string> texts(transfers.size());
    std::vector<double> widths(transfers.size());
    double totalW = 0;
    for (size_t i = 0; i < transfers.size(); i++) {
        texts[i] = normalize_line_id(transfers[i].name);
        widths[i] = std::max(g.font_width(K(18), texts[i].c_str(), FONT_HAN_SANS) + 22.0, badgeH);
        totalW += widths[i] + gap;
    }
    totalW -= gap;

    double startX = pos.x - totalW * 0.5;
    const double yOff = K(32);
    const double y = pos.y + (pos.section == 0 ? yOff : -yOff);

    for (size_t j = 0; j < transfers.size(); j++) {
        const double w = widths[j];
        const double cx = startX + w * 0.5;
        g.set_color(transfers[j].color);
        g.fill_round_rect(cx - w * 0.5, y - badgeH * 0.5, w, badgeH, badgeH, badgeH);
        g.set_color(WHITE_COLOR);
        const double tw = g.font_width(K(18), texts[j].c_str(), FONT_HAN_SANS);
        g.draw_text(cx - tw * 0.5, y + K(7), K(18), texts[j].c_str(), FONT_HAN_SANS);
        startX += w + gap;
    }
}

/* drawBigStationName — 开门页大站名 */
void draw_big_station_name(Gfx2D& g, const StationInfo& station, double centerY,
                           uint32_t routeColor) {
    const uint32_t rc = routeColor ? routeColor : LINE_COLOR;
    const double cnSize = K(150);
    const double enSize = K(42);
    const double gap = K(24);
    const double cnVisualH = cnSize * 0.75;
    const double enVisualH = enSize * 0.7;
    const double totalH = cnVisualH + gap + enVisualH;
    const double top = centerY - totalH * 0.5;
    const double cnBaseline = top + cnVisualH;
    const double enBaseline = cnBaseline + gap + enVisualH;

    g.set_color(rc);
    const double cnW = g.font_width(cnSize, station.nameCn.c_str(), FONT_HONGLEI);
    g.draw_text(TEX_W * 0.5 - cnW * 0.5, cnBaseline, cnSize, station.nameCn.c_str(), FONT_HONGLEI);

    g.set_color(BLACK_COLOR);
    const double enW = g.font_width(enSize, station.nameEn.c_str());
    g.draw_text(TEX_W * 0.5 - enW * 0.5, enBaseline, enSize, station.nameEn.c_str());
}

/* drawTerminusMessage — 终点站提示 */
void draw_terminus_message(Gfx2D& g, double y) {
    const char* cnText = "本次列车已抵达终点站，请所有乘客全部下车。";
    const char* enText = "This train has arrived at the terminus. Please alight from the train.";
    g.set_color(BLACK_COLOR);
    const double cnW = g.font_width(K(26), cnText);
    g.draw_text(TEX_W * 0.5 - cnW * 0.5, y, K(26), cnText);
    const double enW = g.font_width(K(20), enText);
    g.draw_text(TEX_W * 0.5 - enW * 0.5, y + K(36), K(20), enText);
}

/* ==================================================================== */
/* draw_header.js                                                        */
/* ==================================================================== */

/* isNumberRouteName — /^(\d+)号线$/ 且 en == "Line N" */
bool is_number_route_name(const std::string& routeCn, const std::string& routeEn) {
    if (routeCn.size() < 9) return false;
    size_t i = 0;
    while (i < routeCn.size() && routeCn[i] >= '0' && routeCn[i] <= '9') i++;
    if (i == 0) return false;
    if (routeCn.compare(i, routeCn.size() - i, "号线") != 0) return false;
    return routeEn == ("Line " + routeCn.substr(0, i));
}

struct RouteNameOpts {
    double scale = 1.0;
    bool numberBold = true;
    bool numberCenter = false;
};

/* measureRouteNameBlock */
double measure_route_name_block(Gfx2D& g, const std::string& routeCn, const std::string& routeEn,
                                double h, const RouteNameOpts& opts) {
    if (is_number_route_name(routeCn, routeEn)) {
        const std::string numStr = routeCn.substr(0, routeCn.size() - 6);
        const double numSize = std::max(1.0, static_cast<double>(std::lround(h * 0.62 * opts.scale)));
        const double suffixSize = std::max(1.0, static_cast<double>(std::lround(h * 0.26 * opts.scale)));
        const double enSize = std::max(1.0, static_cast<double>(std::lround(h * 0.20 * opts.scale)));
        const double numW = g.font_width(numSize, numStr.c_str(), opts.numberBold ? FONT_ARIAL_BOLD : FONT_ARIAL);
        const double suffixW = g.font_width(suffixSize, "号线");
        const double enW = g.font_width(enSize, routeEn.c_str());
        return std::lround(numW + std::lround(h * 0.06 * opts.scale)
                           + std::max(suffixW, enW));
    }
    const double cnSize = std::max(1.0, static_cast<double>(std::lround(h * 0.30 * opts.scale)));
    const double enSize = std::max(1.0, static_cast<double>(std::lround(h * 0.20 * opts.scale)));
    const double cnW = g.font_width(cnSize, routeCn.c_str());
    const double enW = g.font_width(enSize, routeEn.c_str());
    return std::lround(std::max(cnW, enW));
}

/* drawRouteNameBlock — 数字号线 (大号数字) / 命名线 (中英居中) */
void draw_route_name_block(Gfx2D& g, double x, double y, double w, double h,
                           const std::string& routeCn, const std::string& routeEn,
                           uint32_t textColor, const RouteNameOpts& opts) {
    if (routeCn.empty() && routeEn.empty()) return;
    const double cy = y + h * 0.5;

    if (is_number_route_name(routeCn, routeEn)) {
        const std::string numStr = routeCn.substr(0, routeCn.size() - 6);
        const double numSize = std::max(1.0, static_cast<double>(std::lround(h * 0.62 * opts.scale)));
        const double suffixSize = std::max(1.0, static_cast<double>(std::lround(h * 0.26 * opts.scale)));
        const double enSize = std::max(1.0, static_cast<double>(std::lround(h * 0.20 * opts.scale)));

        const char* numFont = opts.numberBold ? FONT_ARIAL_BOLD : FONT_ARIAL;
        const double numW = g.font_width(numSize, numStr.c_str(), numFont);
        const double suffixW = g.font_width(suffixSize, "号线");
        const double enW = g.font_width(enSize, routeEn.c_str());

        const double lineGap = std::max(1.0, static_cast<double>(std::lround(h * 0.02 * opts.scale)));
        const double suffixH = g.font_ascent(suffixSize, FONT_HAN_SANS) + g.font_descent(suffixSize, FONT_HAN_SANS);
        const double enH = g.font_ascent(enSize) + g.font_descent(enSize);
        const double blockH = suffixH + lineGap + enH;

        const double gapNX = std::lround(h * 0.06 * opts.scale);
        const double rightW = std::max(suffixW, enW);
        const double totalW = numW + gapNX + rightW;

        const double startX = opts.numberCenter ? x + (w - totalW) * 0.5 : x;
        const double numBaseline = cy + (g.font_ascent(numSize, numFont)
                                         - g.font_descent(numSize, numFont)) * 0.5;
        const double rightX = startX + numW + gapNX;

        g.set_color(textColor);
        g.draw_text(startX, numBaseline, numSize, numStr.c_str(), numFont);

        const double blockTop = cy - blockH * 0.5;
        const double suffixBaseline = blockTop + g.font_ascent(suffixSize, FONT_HAN_SANS);
        const double enBaseline = blockTop + suffixH + lineGap + g.font_ascent(enSize);
        g.draw_text(rightX, suffixBaseline, suffixSize, "号线");
        g.draw_text(rightX, enBaseline, enSize, routeEn.c_str());
    } else {
        const double baseCn = std::max(1.0, static_cast<double>(std::lround(h * 0.30 * opts.scale)));
        const double baseEn = std::max(1.0, static_cast<double>(std::lround(h * 0.20 * opts.scale)));
        const double centerX = x + w * 0.5;

        const double cnW = g.font_width(baseCn, routeCn.c_str());
        const double cnH = g.font_ascent(baseCn, FONT_HAN_SANS) + g.font_descent(baseCn, FONT_HAN_SANS);
        const double enW = routeEn.empty() ? 0.0 : g.font_width(baseEn, routeEn.c_str());
        const double enH = routeEn.empty() ? 0.0
            : g.font_ascent(baseEn) + g.font_descent(baseEn);
        const double gap2 = routeEn.empty() ? 0.0
            : std::max(1.0, static_cast<double>(std::lround(h * 0.05 * opts.scale)));
        const double totalH = cnH + gap2 + enH;
        const double top2 = cy - totalH * 0.5;

        g.set_color(textColor);
        g.draw_text(centerX - cnW * 0.5, top2 + g.font_ascent(baseCn, FONT_HAN_SANS),
                    baseCn, routeCn.c_str());
        if (!routeEn.empty()) {
            g.draw_text(centerX - enW * 0.5, top2 + cnH + gap2 + g.font_ascent(baseEn),
                        baseEn, routeEn.c_str());
        }
    }
}

/* drawDestinationBlock — 开往 X / To X，环线时为内环/外环 */
void draw_destination_block(Gfx2D& g, const ScreenInfo& info, double startX,
                            double limitX, uint32_t textColor) {
    std::string destCn, destEn;
    if (info.isCircular) {
        destCn = circ_cn(info.circularState);
        destEn = circ_en(info.circularState);
    } else {
        const auto& stations = *info.stations;
        destCn = !info.destinationCn.empty() ? info.destinationCn
                 : (!stations.empty() ? stations.back().nameCn : "");
        destEn = !info.destinationEn.empty() ? info.destinationEn
                 : (!stations.empty() ? stations.back().nameEn : "");
    }
    const std::string cnText = info.isCircular ? destCn
                                               : (destCn.empty() ? "" : "开往 " + destCn);
    const std::string enText = info.isCircular ? destEn
                                               : (destEn.empty() ? "" : "To " + destEn);
    if (cnText.empty() && enText.empty()) return;

    const double availW = limitX - startX;
    if (availW < K(60)) return;

    double cnSize = K(32);
    double enSize = K(22);
    const double cnW = g.font_width(cnSize, cnText.c_str());
    const double enW = g.font_width(enSize, enText.c_str());
    const double maxW = std::max(cnW, enW);
    if (maxW > availW && maxW > 0) {
        const double k = availW / maxW;
        cnSize = std::max(1.0, std::floor(cnSize * k));
        enSize = std::max(1.0, std::floor(enSize * k));
    }

    g.set_color(textColor);
    if (!cnText.empty()) g.draw_text(startX, K(62), cnSize, cnText.c_str());
    if (!enText.empty()) g.draw_text(startX, K(102), enSize, enText.c_str());
}

/* drawVehicleNumBox — 车号"液态玻璃"卡片 */
void draw_vehicle_num_box(Gfx2D& g, const ScreenInfo& info, uint32_t routeColor) {
    const std::string& numText = info.vehicleNum;
    if (numText.empty()) return;

    const double size = K(36);                  /* FONT_VEHICLE_NUM = 36 * LAYOUT_K */
    const double textW = g.font_width(size, numText.c_str(), FONT_SANS_LAO);
    const double ascent = g.font_ascent(size, FONT_SANS_LAO);
    const double descent = g.font_descent(size, FONT_SANS_LAO);
    const double textH = ascent + descent;

    const double boxH = K(80);
    const double pad = std::max(0.0, static_cast<double>(std::lround((boxH - textH) * 0.5)));
    const double boxW = textW + pad * 2;
    const double boxY = (HEADER_H - boxH) * 0.5;
    const double boxX = (TEX_W - boxY) - boxW;
    const double baseY = boxY + (boxH + ascent - descent) * 0.5;
    const double textCx = boxX + boxW * 0.5;
    const double radius = 14;

    /* 阴影 (6 层) */
    for (int s = 6; s >= 1; s--) {
        const int alpha = static_cast<int>(std::lround(28 * (1 - (s - 1) / 6.0)));
        g.set_color(static_cast<uint32_t>(alpha) << 24);
        g.fill_round_rect(boxX - s, boxY - s + 3, boxW + s * 2, boxH + s * 2,
                          radius + s, radius + s);
    }

    g.set_color(routeColor);
    g.fill_round_rect(boxX, boxY, boxW, boxH, radius, radius);

    /* 磨砂高光 ×2 */
    g.set_color(0x3CFFFFFFu);
    g.fill_round_rect(boxX + 3, boxY + 3, boxW - 6, std::lround(boxH * 0.45),
                      radius, radius);
    g.set_color(0x5AFFFFFFu);
    g.fill_round_rect(boxX + 3, boxY + 3, boxW - 6, std::lround(boxH * 0.16),
                      radius, radius);

    /* 玻璃描边 */
    g.set_color(0x82FFFFFFu);
    g.draw_round_rect(boxX, boxY, boxW, boxH, radius, radius, 2.0);

    g.set_color(Gfx2D::contrast_text(routeColor));
    g.draw_text(textCx - textW * 0.5, baseY, size, numText.c_str(), FONT_SANS_LAO);
}

/* drawHeader — 顶部信息栏 */
void draw_header(Gfx2D& g, const ScreenInfo& info) {
    const uint32_t routeColor = info.routeColor ? info.routeColor : LINE_COLOR;
    const uint32_t textColor = Gfx2D::contrast_text(routeColor);
    g.set_color(routeColor);
    g.fill_rect(0, 0, TEX_W, HEADER_H);

    const std::string& routeCn = info.routeNameCn;
    const std::string& routeEn = info.routeNameEn;

    double leftX = 24;
    /* 线路 logo：JS 从资源包读 PNG；原生版按线路 ID 绘制徽章
       （同 JS 在 logo 加载失败时的回退表现）。 */
    {
        const std::string lineId = normalize_line_id(routeCn);
        if (!lineId.empty()) {
            const double lh = HEADER_H - K(24);
            g.draw_logo_badge(leftX, K(12), lh * 0.85, lh, info.routeColor,
                              lineId.c_str(), K(18));
            leftX += lh * 0.85 + K(36);
        }
    }

    double nameRightX = leftX;
    if (!routeCn.empty() || !routeEn.empty()) {
        RouteNameOpts nameOpts;
        const double contentW = measure_route_name_block(g, routeCn, routeEn, HEADER_H, nameOpts);
        draw_route_name_block(g, leftX, 0, contentW, HEADER_H, routeCn, routeEn,
                              textColor, nameOpts);
        nameRightX = leftX + contentW;
    }

    if (!info.stations || info.stations->empty()) {
        draw_vehicle_num_box(g, info, routeColor);
        return;
    }

    const double centerX = TEX_W * 0.5;
    const char* label = info.isDoorOpen ? "到达" : "下一站";
    const char* labelEn = info.isDoorOpen ? "Arrived" : "Next Station";
    const int idx = info.isDoorOpen ? info.currentIdx : info.nextIdx;
    const StationInfo& bigStation =
        (*info.stations)[static_cast<size_t>(std::max(0, std::min(
            idx, static_cast<int>(info.stations->size()) - 1)))];

    double centerLimit = centerX;
    {
        const std::string cnFull = std::string(label) + " " + bigStation.nameCn;
        const std::string enFull = std::string(labelEn) + " " + bigStation.nameEn;
        const double halfCn = g.font_width(K(40), cnFull.c_str()) * 0.5;
        const double halfEn = g.font_width(K(28), enFull.c_str()) * 0.5;
        centerLimit = centerX - std::max(halfCn, halfEn) - K(30);
    }
    draw_destination_block(g, info, nameRightX + K(36), centerLimit, textColor);

    {
        const std::string cnFull = std::string(label) + " " + bigStation.nameCn;
        const std::string enFull = std::string(labelEn) + " " + bigStation.nameEn;
        draw_centered_text(g, cnFull, centerX, K(62), K(40), textColor);
        draw_centered_text(g, enFull, centerX, K(102), K(28), textColor);
    }

    draw_vehicle_num_box(g, info, routeColor);
}

/* ==================================================================== */
/* data.js — 换乘面板 / 出站口面板                                        */
/* ==================================================================== */

void draw_partial_transfer_info(Gfx2D& g, int curIdx,
                                const std::vector<StationInfo>& stations) {
    if (curIdx < 0 || curIdx >= static_cast<int>(stations.size())) return;
    const std::vector<Transfer>& transfers = stations[static_cast<size_t>(curIdx)].transfers;
    if (transfers.empty()) return;

    std::vector<ParsedTransferRoute> items;
    items.reserve(transfers.size());
    for (const Transfer& t : transfers) {
        ParsedTransferRoute item = parse_transfer_route(t.name);
        item.color = t.color;
        items.push_back(item);
    }
    std::sort(items.begin(), items.end(), compare_transfer_route);

    const double titleX = 40;
    const double titleY = HEADER_H + K(48);

    g.set_color(BLACK_COLOR);
    g.draw_text(titleX, titleY, K(34), "换乘");
    g.draw_text(titleX + K(108), titleY, K(30), "Transfer");

    const int cols = 3;
    const double badgeGap = K(14);
    const double startX = 40;
    const double startY = titleY + K(32);
    const double availW = 480;
    const double availH = TEX_H - startY - 20;

    const int rows = static_cast<int>((items.size() + cols - 1) / cols);
    const double maxByW = std::floor((availW - (cols - 1) * badgeGap) / cols);
    const double maxByH = std::floor((availH - (rows - 1) * badgeGap) / rows);
    double badgeSize = std::min(std::min(maxByW, maxByH), static_cast<double>(K(130)));
    if (badgeSize < 1) badgeSize = 1;

    const double badgeW = badgeSize;
    const double badgeH = static_cast<double>(std::lround(badgeSize * 0.55));

    RouteNameOpts nameOpts;
    nameOpts.numberBold = false;
    nameOpts.numberCenter = true;
    const double padX = K(10);

    for (size_t i = 0; i < items.size(); i++) {
        const int col = static_cast<int>(i % cols);
        const int row = static_cast<int>(i / cols);
        const double bx = startX + col * (badgeW + badgeGap);
        const double by = startY + row * (badgeH + badgeGap);

        g.set_color(items[i].color);
        g.fill_round_rect(bx, by, badgeW, badgeH, badgeH, badgeH);
        draw_route_name_block(g, bx + padX, by, badgeW - padX * 2, badgeH,
                              items[i].cn, items[i].en,
                              Gfx2D::contrast_text(items[i].color), nameOpts);
    }
}

void draw_exit_info(Gfx2D& g, int curIdx, const std::vector<StationInfo>& stations,
                    uint32_t routeColor) {
    if (curIdx < 0 || curIdx >= static_cast<int>(stations.size())) return;
    const std::vector<ExitInfo>& exits = stations[static_cast<size_t>(curIdx)].exits;
    if (exits.empty()) return;                   /* JS: if (!exits.length) return */

    std::vector<const ExitInfo*> valid;
    for (const ExitInfo& e : exits) {
        if (!e.destinations.empty()) valid.push_back(&e);
    }
    if (valid.empty()) return;

    const uint32_t rc = routeColor ? routeColor : LINE_COLOR;
    const double x = TEX_W - 420;
    const double y = 178 * LAYOUT_K_V;

    g.set_color(BLACK_COLOR);
    g.draw_text(x, y, K(34), "出站口");
    g.draw_text(x + g.font_width(K(34), "出站口") + K(10), y, K(34), "Exits");

    /* 唯一前缀折叠 */
    auto letter_run = [](const std::string& s) -> std::string {
        size_t i = 0;
        while (i < s.size() && std::isalpha(static_cast<unsigned char>(s[i]))) i++;
        return s.substr(0, i);
    };
    std::vector<std::pair<std::string, int>> prefixCount;
    for (const ExitInfo* e : valid) {
        const std::string run = letter_run(e->name);
        if (run.empty()) continue;
        bool found = false;
        for (auto& kv : prefixCount) {
            if (kv.first == run) { kv.second++; found = true; break; }
        }
        if (!found) prefixCount.emplace_back(run, 1);
    }

    for (size_t k = 0; k < valid.size(); k++) {
        const ExitInfo& exit = *valid[k];
        const double ey = y + K(52) + static_cast<double>(k) * K(64);

        std::string displayName = exit.name;
        const std::string run = letter_run(exit.name);
        if (!run.empty()) {
            for (const auto& kv : prefixCount) {
                if (kv.first == run && kv.second == 1) { displayName = run; break; }
            }
        }

        std::string destCn, destEn;
        split_cjk_non_cjk(exit.destinations[0], destCn, destEn);
        if (destCn.empty() && !exit.destinations[0].empty()) destCn = exit.destinations[0];

        g.set_color(rc);
        g.draw_text(x, ey, K(26), displayName.c_str());

        g.set_color(BLACK_COLOR);
        g.draw_text(x + K(70), ey, K(20), destCn.c_str());

        g.set_color(0xFF8A8A8Au);
        g.draw_text(x + K(70), ey + K(20), K(17), destEn.c_str());
    }
}

/* ==================================================================== */
/* draw_circular.js                                                      */
/* ==================================================================== */

void draw_circular_full_map(Gfx2D& g, const Wr2LcdState& state, const ScreenInfo& info) {
    const auto& stations = *info.stations;
    const int n = static_cast<int>(stations.size());
    if (n <= 0) return;

    const uint32_t routeColor = info.routeColor ? info.routeColor : LINE_COLOR;
    const bool reversed = (info.circularState == 2);       /* ANTICLOCKWISE */
    const bool clockwise = !reversed;
    const int highlightIdx = info.isDoorOpen ? info.currentIdx : info.nextIdx;

    /* 轨道: 灰底 + 线路色 (整条环线) */
    g.set_color(GRAY_COLOR);
    g.draw_round_rect(RING_LEFT_X, RING_TOP_Y,
                      RING_RIGHT_X - RING_LEFT_X, RING_BOTTOM_Y - RING_TOP_Y,
                      RING_RADIUS * 2.0, RING_RADIUS * 2.0, RING_STROKE);
    g.set_color(routeColor);
    g.draw_round_rect(RING_LEFT_X, RING_TOP_Y,
                      RING_RIGHT_X - RING_LEFT_X, RING_BOTTOM_Y - RING_TOP_Y,
                      RING_RADIUS * 2.0, RING_RADIUS * 2.0, RING_STROKE);

    if (!info.isDoorOpen) draw_progress_overlay(g, state, info, reversed, clockwise);

    /* 自适应字号 */
    const double topStartX = RING_LEFT_X + RING_RADIUS + RING_PADDING;
    const double topEndX = RING_RIGHT_X - RING_RADIUS - RING_PADDING;
    const int topCount = (n + 1) / 2;
    const int botCount = n - topCount;
    const double rowW = topEndX - topStartX;

    const double topSpacing = topCount > 1 ? rowW / (topCount - 1) : rowW;
    const double botSpacing = botCount > 1 ? rowW / (botCount - 1) : rowW;
    const double minSpacing = std::min(topSpacing, botSpacing);
    const double availW = minSpacing * 0.98;

    const double baseCnSize = K(32);
    const double baseEnSize = K(26);
    double maxCnW = 0, maxEnW = 0;
    for (const StationInfo& st : stations) {
        maxCnW = std::max(maxCnW, g.font_width(baseCnSize, st.nameCn.c_str()));
        maxEnW = std::max(maxEnW, g.font_width(baseEnSize, st.nameEn.c_str()));
    }
    const double MIN_CN_SIZE = 36, MIN_EN_SIZE = 28;
    const double cnSize = compute_adaptive_font_size(baseCnSize, maxCnW, availW, MIN_CN_SIZE);
    const double enSize = compute_adaptive_font_size(baseEnSize, maxEnW, availW, MIN_EN_SIZE);

    const double dotR = K(14);
    const double topCnOff = K(42), topEnOff = K(20);
    const double botCnOff = K(50), botEnOff = K(72);

    for (int i = 0; i < n; i++) {
        const StationPos pos = get_station_pos(i, n, reversed);
        const uint32_t fillColor = (i == highlightIdx) ? RED_COLOR : GREEN_COLOR;
        const uint32_t nameColor = (i == highlightIdx) ? routeColor : BLACK_COLOR;

        g.set_color(fillColor);
        g.fill_oval(pos.x - dotR, pos.y - dotR, dotR * 2, dotR * 2);
        g.set_color(BLACK_COLOR);
        g.draw_oval(pos.x - dotR, pos.y - dotR, dotR * 2, dotR * 2, 3.0);

        if (pos.section == 0) {
            draw_adaptive_centered_text(g, stations[i].nameCn, pos.x, pos.y - topCnOff,
                                        cnSize, availW, nameColor);
            draw_adaptive_centered_text(g, stations[i].nameEn, pos.x, pos.y - topEnOff,
                                        enSize, availW, nameColor);
        } else {
            draw_adaptive_centered_text(g, stations[i].nameCn, pos.x, pos.y + botCnOff,
                                        cnSize, availW, nameColor);
            draw_adaptive_centered_text(g, stations[i].nameEn, pos.x, pos.y + botEnOff,
                                        enSize, availW, nameColor);
        }

        if (!stations[i].transfers.empty()) {
            draw_transfer_badges(g, stations[i].transfers, pos);
        }
    }
}

void draw_circular_partial_map(Gfx2D& g, const Wr2LcdState& state, const ScreenInfo& info) {
    const auto& stations = *info.stations;
    const int n = static_cast<int>(stations.size());
    if (n <= 0) return;
    const int curIdx = std::max(0, std::min(info.currentIdx, n - 1));
    const int nxtIdx = std::max(0, std::min(info.nextIdx, n - 1));
    const uint32_t routeColor = info.routeColor ? info.routeColor : LINE_COLOR;

    if (info.isDoorOpen) {
        const double whiteCenterY = (HEADER_H + TEX_H) * 0.5;
        draw_big_station_name(g, stations[curIdx], whiteCenterY, routeColor);
        draw_partial_transfer_info(g, curIdx, stations);
        draw_exit_info(g, curIdx, stations, routeColor);
        return;
    }

    const double lineY = K(300);
    const double lineL = 500;
    const double lineR = TEX_W - 500;

    g.set_color(GRAY_COLOR);
    g.draw_line(lineL, lineY, lineR, lineY, 6.0);

    const double rectX = lineL + 30;
    const double rectW = lineR - lineL - 60;
    const double rectH = K(64);

    g.set_color(GRAY_COLOR);
    g.fill_round_rect(rectX, lineY - rectH * 0.5, rectW, rectH, rectH, rectH);

    /* 环线: 下一站恒在中间, 前后各 2 站 (环绕取模) */
    int displayIdx[5];
    for (int k = 0; k < 5; k++) {
        const int d = k - 2;
        displayIdx[k] = ((nxtIdx + d) % n + n) % n;
    }
    const double spacing = rectW / 6.0;
    const int count = 5;

    g.set_color(routeColor);
    g.fill_round_rect(rectX, lineY - rectH * 0.5, rectW, rectH, rectH, rectH);

    g.set_color(WHITE_COLOR);
    g.draw_round_rect(rectX, lineY - rectH * 0.5, rectW, rectH, rectH, rectH, 2.0);

    const double dotR = K(24);
    for (int k = 0; k < count; k++) {
        const int actualIdx = displayIdx[k];
        const StationInfo& station = stations[static_cast<size_t>(actualIdx)];
        const double x = rectX + (k + 1) * spacing;

        g.set_color(actualIdx == nxtIdx ? RED_COLOR : GREEN_COLOR);
        g.fill_oval(x - dotR, lineY - dotR, dotR * 2, dotR * 2);

        const bool isAbove = (k % 2 == 0);
        const double aboveOff = K(80);
        const double textY1 = isAbove ? (lineY - aboveOff) : (lineY + aboveOff);
        const double textY2 = textY1 + K(26);
        draw_centered_text(g, station.nameCn, x, textY1, K(36), BLACK_COLOR);
        draw_centered_text(g, station.nameEn, x, textY2, K(22), BLACK_COLOR);
    }

    draw_partial_transfer_info(g, nxtIdx, stations);
    draw_exit_info(g, nxtIdx, stations, routeColor);
}

/* ==================================================================== */
/* draw_linear.js                                                        */
/* ==================================================================== */

void draw_linear_transfer_badges(Gfx2D& g, const std::vector<Transfer>& transfers,
                                 const LinearPos& pos) {
    if (transfers.empty()) return;
    std::vector<ParsedTransferRoute> items;
    items.reserve(transfers.size());
    for (const Transfer& t : transfers) {
        ParsedTransferRoute item = parse_transfer_route(t.name);
        item.color = t.color;
        items.push_back(item);
    }
    std::sort(items.begin(), items.end(), compare_transfer_route);

    const double badgeH = K(30);
    const double gap = K(4);
    const double padX = K(14);
    const double step = badgeH + gap;

    RouteNameOpts nameOpts;
    nameOpts.numberBold = false;
    nameOpts.numberCenter = true;

    std::vector<double> widths(items.size());
    for (size_t k = 0; k < items.size(); k++) {
        widths[k] = measure_route_name_block(g, items[k].cn, items[k].en, badgeH, nameOpts)
                    + padX * 2;
    }

    const double topGap = K(16);
    double startY;
    if (pos.above) {
        startY = pos.y + topGap;
    } else {
        startY = pos.y - topGap - static_cast<double>(items.size() - 1) * step - badgeH;
    }

    for (size_t j = 0; j < items.size(); j++) {
        const double w = widths[j];
        const double y = startY + static_cast<double>(j) * step;
        const double x = pos.x - w * 0.5;

        g.set_color(items[j].color);
        g.fill_round_rect(x, y, w, badgeH, badgeH, badgeH);
        draw_route_name_block(g, x + padX, y, w - padX * 2, badgeH,
                              items[j].cn, items[j].en,
                              Gfx2D::contrast_text(items[j].color), nameOpts);
    }
}

void draw_linear_full_map(Gfx2D& g, const Wr2LcdState& state, const ScreenInfo& info) {
    const auto& stations = *info.stations;
    const int n = static_cast<int>(stations.size());
    if (n <= 0) return;

    const uint32_t routeColor = info.routeColor ? info.routeColor : LINE_COLOR;
    const int highlightIdx = info.isDoorOpen ? info.currentIdx : info.nextIdx;

    const double lineY = (RING_TOP_Y + RING_BOTTOM_Y) * 0.5;
    const double startX = RING_LEFT_X + RING_PADDING;
    const double endX = RING_RIGHT_X - RING_PADDING;
    const double rowW = endX - startX;

    const double spacing = n > 1 ? rowW / (n - 1) : rowW;
    const double availW = spacing * 0.98;

    const double baseCnSize = K(32), baseEnSize = K(26);
    double maxCnW = 0, maxEnW = 0;
    for (const StationInfo& st : stations) {
        maxCnW = std::max(maxCnW, g.font_width(baseCnSize, st.nameCn.c_str()));
        maxEnW = std::max(maxEnW, g.font_width(baseEnSize, st.nameEn.c_str()));
    }
    const double MIN_CN_SIZE = 36, MIN_EN_SIZE = 28;
    const double cnSize = compute_adaptive_font_size(baseCnSize, maxCnW, availW, MIN_CN_SIZE);
    const double enSize = compute_adaptive_font_size(baseEnSize, maxEnW, availW, MIN_EN_SIZE);

    g.set_color(GRAY_COLOR);
    g.draw_line(startX, lineY, endX, lineY, RING_STROKE);

    if (info.currentIdx < n - 1) {
        const LinearPos pStart = get_linear_station_pos(info.currentIdx, n);
        const LinearPos pEnd = get_linear_station_pos(n - 1, n);
        g.set_color(routeColor);
        g.draw_line(pStart.x, lineY, pEnd.x, lineY, RING_STROKE);
    }

    if (!info.isDoorOpen) {
        const LinearPos p1 = get_linear_station_pos(info.currentIdx, n);
        const LinearPos p2 = get_linear_station_pos(info.nextIdx, n);
        g.set_color(BLACK_COLOR);
        g.draw_line(p1.x, p1.y, p2.x, p2.y, RING_STROKE + K(3));
        g.set_color(state.blinkState ? BLINK_GREEN : BLINK_RED);
        g.draw_line(p1.x, p1.y, p2.x, p2.y, RING_STROKE);
    }

    const double dotR = K(14);
    const double halfTrack = RING_STROKE * 0.5;
    const double edgeGap = K(12);
    const double lineGap = K(2);
    const double enCapHeight = std::lround(enSize * 0.72);
    const double cnAscent = g.font_ascent(cnSize, FONT_HAN_SANS);
    const double cnDescent = g.font_descent(cnSize, FONT_HAN_SANS);
    const double enDescent = g.font_descent(enSize);

    for (int i = 0; i < n; i++) {
        const LinearPos pos = get_linear_station_pos(i, n);

        uint32_t fillColor, nameColor;
        if (i < highlightIdx)       { fillColor = WHITE_COLOR; nameColor = DOT_GRAY; }
        else if (i == highlightIdx) { fillColor = RED_COLOR;   nameColor = routeColor; }
        else                        { fillColor = GREEN_COLOR; nameColor = BLACK_COLOR; }

        g.set_color(fillColor);
        g.fill_oval(pos.x - dotR, pos.y - dotR, dotR * 2, dotR * 2);
        g.set_color(BLACK_COLOR);
        g.draw_oval(pos.x - dotR, pos.y - dotR, dotR * 2, dotR * 2, 3.0);

        double cnY, enY;
        if (pos.above) {
            const double enBaseline = pos.y - halfTrack - enDescent - edgeGap;
            const double cnBaseline = enBaseline - enCapHeight - lineGap - cnDescent;
            cnY = cnBaseline;
            enY = enBaseline;
        } else {
            const double cnBaseline = pos.y + halfTrack + cnAscent + edgeGap;
            const double enBaseline = cnBaseline + cnDescent + lineGap + enCapHeight;
            cnY = cnBaseline;
            enY = enBaseline;
        }

        draw_adaptive_centered_text(g, stations[i].nameCn, pos.x, cnY, cnSize, availW, nameColor);
        draw_adaptive_centered_text(g, stations[i].nameEn, pos.x, enY, enSize, availW, nameColor);

        if (!stations[i].transfers.empty()) {
            draw_linear_transfer_badges(g, stations[i].transfers, pos);
        }
    }
}

void draw_linear_partial_map(Gfx2D& g, const Wr2LcdState& state, const ScreenInfo& info) {
    const auto& stations = *info.stations;
    const int n = static_cast<int>(stations.size());
    if (n <= 0) return;
    const int curIdx = std::max(0, std::min(info.currentIdx, n - 1));
    const int nxtIdx = std::max(0, std::min(info.nextIdx, n - 1));
    const uint32_t routeColor = info.routeColor ? info.routeColor : LINE_COLOR;

    if (info.isDoorOpen) {
        const bool isTerminus = (curIdx >= n - 1);
        const double whiteCenterY = (HEADER_H + TEX_H) * 0.5;
        const double bigNameCenterY = isTerminus ? (whiteCenterY - 50) : whiteCenterY;
        draw_big_station_name(g, stations[curIdx], bigNameCenterY, routeColor);
        if (isTerminus) draw_terminus_message(g, bigNameCenterY + 140);
        draw_partial_transfer_info(g, curIdx, stations);
        draw_exit_info(g, curIdx, stations, routeColor);
        return;
    }

    const double lineY = K(300);
    const double lineL = 500;
    const double lineR = TEX_W - 500;

    g.set_color(GRAY_COLOR);
    g.draw_line(lineL, lineY, lineR, lineY, 6.0);

    const double rectX = lineL + 30;
    const double rectW = lineR - lineL - 60;
    const double rectH = K(64);

    g.set_color(GRAY_COLOR);
    g.fill_round_rect(rectX, lineY - rectH * 0.5, rectW, rectH, rectH, rectH);

    /* 非环线: 从 max(0, nxtIdx-2) 起最多显示 5 站 (不环绕) */
    int startIdx = std::max(0, nxtIdx - 2);
    int endIdx = std::min(n - 1, startIdx + 4);
    if (endIdx - startIdx < 4) startIdx = std::max(0, endIdx - 4);
    const int count = endIdx - startIdx + 1;
    int displayIdx[5];
    for (int k = 0; k < count; k++) displayIdx[k] = startIdx + k;
    const double spacing = rectW / (count + 1);

    int curLocalIdx = -1;
    for (int k = 0; k < count; k++) {
        if (displayIdx[k] == curIdx) { curLocalIdx = k; break; }
    }

    /* 非环线: 仅"未经过段"涂线路色 (clip 实现, 同 JS) */
    double boundaryX = -1;
    if (curIdx <= 0) {
        g.set_color(routeColor);
        g.fill_round_rect(rectX, lineY - rectH * 0.5, rectW, rectH, rectH, rectH);
    } else if (curLocalIdx >= 0 && curLocalIdx < count) {
        boundaryX = rectX + (curLocalIdx + 1) * spacing;
        g.push_clip(boundaryX, lineY - rectH * 0.5, rectX + rectW - boundaryX, rectH);
        g.set_color(routeColor);
        g.fill_round_rect(rectX, lineY - rectH * 0.5, rectW, rectH, rectH, rectH);
        g.pop_clip();
    } else {
        g.set_color(routeColor);
        g.fill_round_rect(rectX, lineY - rectH * 0.5, rectW, rectH, rectH, rectH);
    }

    g.set_color(WHITE_COLOR);
    g.draw_round_rect(rectX, lineY - rectH * 0.5, rectW, rectH, rectH, rectH, 2.0);

    if (boundaryX > 0) {
        const double circleR = rectH * 0.5;
        g.set_color(routeColor);
        g.fill_oval(boundaryX - circleR, lineY - circleR, circleR * 2, circleR * 2);
    }

    const double dotR = K(24);
    for (int k = 0; k < count; k++) {
        const int actualIdx = displayIdx[k];
        const StationInfo& station = stations[static_cast<size_t>(actualIdx)];
        const double x = rectX + (k + 1) * spacing;

        const bool isPassed = actualIdx < curIdx;
        g.set_color(isPassed ? DEEP_GRAY_DOT
                             : (actualIdx == nxtIdx ? RED_COLOR : GREEN_COLOR));
        g.fill_oval(x - dotR, lineY - dotR, dotR * 2, dotR * 2);

        const uint32_t nameColor = isPassed ? DOT_GRAY : BLACK_COLOR;
        const bool isAbove = (k % 2 == 0);
        const double aboveOff = K(80);
        const double textY1 = isAbove ? (lineY - aboveOff) : (lineY + aboveOff);
        const double textY2 = textY1 + K(26);
        draw_centered_text(g, station.nameCn, x, textY1, K(36), nameColor);
        draw_centered_text(g, station.nameEn, x, textY2, K(22), nameColor);
    }

    draw_partial_transfer_info(g, nxtIdx, stations);
    draw_exit_info(g, nxtIdx, stations, routeColor);
}

/* ==================================================================== */
/* main.js — 每块屏一次                                                  */
/* ==================================================================== */

void paint_screen(Gfx2D& g, const Wr2LcdState& state, const ScreenInfo& info, int pageMode) {
    g.set_color(WHITE_COLOR);
    g.fill_rect(0, 0, TEX_W, TEX_H);

    draw_header(g, info);

    if (info.isCircular) {
        if (pageMode == 0) draw_circular_full_map(g, state, info);
        else               draw_circular_partial_map(g, state, info);
    } else {
        if (pageMode == 0) draw_linear_full_map(g, state, info);
        else               draw_linear_partial_map(g, state, info);
    }
}

/* 内容签名：内容不变则跳过光栅化（见文件头说明 2）。
 *
 * 签名分两次计算：含 blinkState 的完整签名决定"是否要重画"，不含它的
 * 布局签名用来识别"这一帧只有闪烁位变了"。
 *
 * 为什么需要后者：进度线段的闪烁每秒翻转一次，而它写在**每一块屏**的内容
 * 里 —— 只按完整签名判断，一次闪烁会把 6 节车厢 × 2 侧的 12 块屏全部重新
 * 光栅化（实测约 50 ms／帧，环线图约 97 ms），造成每秒一次的可见卡顿。
 * 闪烁只是个颜色翻转，不值得重画整块屏：布局未变时直接沿用已有纹理
 * （代价是进度段的闪烁不再动画，换来的是稳态每帧 0.006 ms）。
 * 需要逐步闪烁动画时把 WR2_LCD_ANIMATE_BLINK 定义出来即可。 */
#ifndef WR2_LCD_ANIMATE_BLINK
#  define WR2_LCD_ANIMATE_BLINK 0
#endif

uint64_t screen_signature(const Wr2LcdState& state, const ScreenInfo& info,
                          int pageMode, const std::string& vehicleNum,
                          bool includeBlink) {
    uint64_t h = 1469598103934665603ull;
    h = fnv1a(h, &pageMode, sizeof(pageMode));
    h = fnv1a(h, &info.currentIdx, sizeof(info.currentIdx));
    h = fnv1a(h, &info.nextIdx, sizeof(info.nextIdx));
    h = fnv1a(h, &info.isCircular, sizeof(info.isCircular));
    h = fnv1a(h, &info.isDoorOpen, sizeof(info.isDoorOpen));
#if WR2_LCD_ANIMATE_BLINK
    includeBlink = true;
#endif
    if (includeBlink) h = fnv1a(h, &info.blinkState, sizeof(info.blinkState));
    h = fnv1a(h, &info.circularState, sizeof(info.circularState));
    h = fnv1a(h, &info.routeColor, sizeof(info.routeColor));
    h = fnv_str(h, vehicleNum);
    h = fnv_str(h, state.cachedRouteId);
    h = fnv_str(h, state.routeNameCn);
    h = fnv_str(h, state.routeNameEn);
    h = fnv_str(h, state.destinationCn);
    h = fnv_str(h, state.destinationEn);
    if (info.stations) {
        for (const StationInfo& st : *info.stations) {
            h = fnv_str(h, st.nameCn);
            h = fnv_str(h, st.nameEn);
            const size_t transferCount = st.transfers.size();
            h = fnv1a(h, &transferCount, sizeof(transferCount));
            for (const Transfer& t : st.transfers) h = fnv_str(h, t.name);
            for (const ExitInfo& e : st.exits) h = fnv_str(h, e.name);
        }
    }
    return h;
}

/* ==================================================================== */
/* 脚本本体                                                              */
/* ==================================================================== */

struct Wr2LcdScript : VehicleScript<Wr2LcdState> {
    static constexpr auto ID = "wr2a03:lcd";

    void create(VehicleContext& ctx, Wr2LcdState& state, const Train& train) override {
        const ScreenSize screen = compute_screen_size();
        state.screenW = screen.w;
        state.screenH = screen.h;
        state.sx = screen.sx;
        state.sy = screen.sy;

        ensure_cars(ctx.input(), state, train);

        /* 初始化状态机（main.js create()） */
        const int64_t now = train.game_time_millis();
        state.lastCycleTime = now;
        state.lastBlinkTime = now;
        state.blinkState = false;
        state.pageMode = 0;
        state.isDoorOpen = false;
        state.lastDoorOpen = false;
        state.cachedRouteId.clear();
        state.stations.clear();
        state.destinationCn.clear();
        state.destinationEn.clear();
        state.circularState = 0;
        state.isCircular = false;
        state.lastIdxShown = -1;
        state.stopCountIdx = -1;
        state.lastCountTime = 0;
        state.circularLocked = false;
        state.routeColor = LINE_COLOR;
        state.routeNameCn.clear();
        state.routeNameEn.clear();
    }

    void render(VehicleContext& ctx, Wr2LcdState& state, const Train& train) override {
        const int carCount = train.car_count();
        ensure_cars(ctx.input(), state, train);

        const int64_t now = train.game_time_millis();
        const std::string sidingName(train.siding_name().str());

        /* ---- 线路缓存刷新（main.js: cachedRouteId 变化才重建） ---- */
        const StopList thisRouteStops = train.this_route_stops();
        const bool hasRoute = thisRouteStops.size() > 0;
        if (hasRoute) {
            const std::string routeId = std::to_string(train.this_route_id());
            if (state.cachedRouteId != routeId) {
                state.cachedRouteId = routeId;
                state.stations = get_stations_from_stops(
                    thisRouteStops, get_non_extra_parts(train.route_name().str()));

                const CircularResult circ = resolve_circular_state(
                    train, state.circularLocked, state.circularState);
                state.isCircular = circ.isCircular;
                state.circularState = circ.circularState;

                const std::string cleanName = get_non_extra_parts(train.route_name().str());
                std::string routeCn, routeEn;
                const size_t bar = cleanName.find('|');
                if (bar != std::string::npos) {
                    routeCn = trim_str(cleanName.substr(0, bar));
                    routeEn = trim_str(cleanName.substr(bar + 1));
                } else {
                    split_cjk_non_cjk(cleanName, routeCn, routeEn);
                    if (routeCn.empty() && !cleanName.empty()) routeCn = cleanName;
                }
                state.routeNameCn = routeCn;
                state.routeNameEn = routeEn;
                state.routeColor = train.route_color() != 0
                    ? (train.route_color() | 0xFF000000u)
                    : LINE_COLOR;

                const Stop lastStop = thisRouteStops.at(thisRouteStops.size() - 1);
                split_cjk_non_cjk(lastStop.destination_name().str(),
                                  state.destinationCn, state.destinationEn);

                state.lastIdxShown = -1;
            }
        }
        const bool hasStations = !state.stations.empty();

        /* ---- 门状态机 / 页面轮播 / 闪烁（main.js render()） ---- */
        const double doorVal = vehicle_door_value(train);
        const double absSpeed = vehicle_speed_ms(train);
        if (state.isDoorOpen) {
            if (doorVal < 0.05 && absSpeed > 0.3) state.isDoorOpen = false;
        } else {
            if (doorVal > 0.5 || (absSpeed < 0.1 && doorVal > 0.1)) state.isDoorOpen = true;
        }

        const int64_t cycleElapsed = now - state.lastCycleTime;
        if (state.pageMode == 0) {
            if (cycleElapsed >= CYCLE_FULL_MS) { state.pageMode = 1; state.lastCycleTime = now; }
        } else {
            if (cycleElapsed >= CYCLE_PARTIAL_MS) { state.pageMode = 0; state.lastCycleTime = now; }
        }

        if (!state.isDoorOpen && now - state.lastBlinkTime >= BLINK_INTERVAL_MS) {
            state.blinkState = !state.blinkState;
            state.lastBlinkTime = now;
        }

        if (hasStations) {
            if (state.isDoorOpen && !state.lastDoorOpen
                && now - state.lastCountTime > STOP_DEBOUNCE_MS) {
                state.stopCountIdx++;
                const int n = static_cast<int>(state.stations.size());
                if (state.isCircular && n > 0) {
                    state.stopCountIdx = ((state.stopCountIdx % n) + n) % n;
                } else if (state.stopCountIdx >= n) {
                    state.stopCountIdx = n - 1;
                }
                state.lastCountTime = now;
            }
        }
        state.lastDoorOpen = state.isDoorOpen;

        /* ---- 当前/下一站索引 + 行驶方向左右屏 ---- */
        int currentIdx = 0, nextIdx = 0;
        int leftMode = 0, rightMode = 0;    /* 0 full, 1 partial */
        if (hasStations) {
            const int n = static_cast<int>(state.stations.size());
            currentIdx = get_current_station_idx(train, n, state.isCircular,
                                                 state.isDoorOpen, state.stopCountIdx);
            currentIdx = std::max(0, std::min(currentIdx, n - 1));
            nextIdx = state.isCircular ? (currentIdx + 1) % n
                                       : std::min(currentIdx + 1, n - 1);
            state.lastIdxShown = currentIdx;

            bool reversed = false;
            const char side = get_travel_left_side(train, reversed);
            const int cyclingMode = state.isDoorOpen ? 1 : state.pageMode;
            if (side == 'L') { leftMode = cyclingMode; rightMode = 0; }
            else             { leftMode = 0; rightMode = cyclingMode; }
        }

        /* ---- 每节车厢两侧 ---- */
        for (int ci = 0; ci < carCount && ci < static_cast<int>(state.cars.size()); ci++) {
            CarScreens& cs = state.cars[static_cast<size_t>(ci)];
            const std::string carDisplay = get_car_display_num(sidingName, ci);

            if (!hasRoute || !hasStations) {
                /* JS: clearScreen(g) —— 白底。
                   sig == 0 表示"这块屏还什么都没画过"，首帧也必须清一次，
                   否则会停在纹理初值（全透明）上。
                   ★ 两个签名都要清零：只清 leftSig/rightSig 的话，布局签名
                   仍是上一张图的值，线路恢复时会被误判成"只有闪烁变了"而
                   跳过绘制，屏幕就停在白底上。 */
                if (cs.leftSig != 0 || !cs.everPainted) {
                    Gfx2D gL(cs.left, ctx.host(), FONT_HAN_SANS, FONT_ARIAL);
                    gL.set_scale(state.sx, state.sy);
                    gL.set_color(WHITE_COLOR);
                    gL.fill_rect(0, 0, TEX_W, TEX_H);
                    cs.leftSig = 0;
                    cs.leftLaySig = 0;
                }
                if (cs.rightSig != 0 || !cs.everPainted) {
                    Gfx2D gR(cs.right, ctx.host(), FONT_HAN_SANS, FONT_ARIAL);
                    gR.set_scale(state.sx, state.sy);
                    gR.set_color(WHITE_COLOR);
                    gR.fill_rect(0, 0, TEX_W, TEX_H);
                    cs.rightSig = 0;
                    cs.rightLaySig = 0;
                }
                cs.everPainted = true;
            } else {
                ScreenInfo info;
                info.stations = &state.stations;
                info.currentIdx = currentIdx;
                info.nextIdx = nextIdx;
                info.isCircular = state.isCircular;
                info.circularState = state.circularState;
                info.destinationCn = state.destinationCn;
                info.destinationEn = state.destinationEn;
                info.isDoorOpen = state.isDoorOpen;
                info.blinkState = state.blinkState;
                info.routeColor = state.routeColor;
                info.routeNameCn = state.routeNameCn;
                info.routeNameEn = state.routeNameEn;
                info.vehicleNum = carDisplay;

                /* Two signatures per screen: the full one decides whether to
                   re-rasterise, the layout-only one (blink removed) recognises
                   a tick where nothing but the blink flipped — see the note on
                   screen_signature(). */
                const uint64_t sigL = screen_signature(state, info, leftMode, carDisplay, true);
                const uint64_t layL = screen_signature(state, info, leftMode, carDisplay, false);
                if (cs.leftSig != sigL) {
                    if (cs.leftLaySig != layL) {
                        Gfx2D gL(cs.left, ctx.host(), FONT_HAN_SANS, FONT_ARIAL);
                        gL.set_scale(state.sx, state.sy);
                        paint_screen(gL, state, info, leftMode);
                    }
                    cs.leftSig = sigL;
                    cs.leftLaySig = layL;
                }
                const uint64_t sigR = screen_signature(state, info, rightMode, carDisplay, true);
                const uint64_t layR = screen_signature(state, info, rightMode, carDisplay, false);
                if (cs.rightSig != sigR) {
                    if (cs.rightLaySig != layR) {
                        Gfx2D gR(cs.right, ctx.host(), FONT_HAN_SANS, FONT_ARIAL);
                        gR.set_scale(state.sx, state.sy);
                        paint_screen(gR, state, info, rightMode);
                    }
                    cs.rightSig = sigR;
                    cs.rightLaySig = layR;
                }
            }

            cs.left.upload(ctx.frame());
            cs.right.upload(ctx.frame());

            /* JS: ctx.drawCarModel(dhL.model, ci, null) */
            for (int32_t model : cs.modelsLeft) ctx.draw_car_model(model, ci, nullptr);
            for (int32_t model : cs.modelsRight) ctx.draw_car_model(model, ci, nullptr);
        }
    }

    void dispose(VehicleContext& ctx, Wr2LcdState& state, const Train&) override {
        for (CarScreens& cs : state.cars) {
            for (int32_t model : cs.modelsLeft) release_quad(ctx.host(), model);
            for (int32_t model : cs.modelsRight) release_quad(ctx.host(), model);
            cs.left.close(&ctx.input());
            cs.right.close(&ctx.input());
        }
        state.cars.clear();
    }

private:
    /* 车厢数变化时重建两侧纹理（main.js render() 里的守卫） */
    void ensure_cars(const JcmFrameInput& in, Wr2LcdState& state, const Train& train) {
        const int carCount = train.car_count();
        if (static_cast<int>(state.cars.size()) == carCount && carCount > 0) return;

        for (CarScreens& cs : state.cars) {
            for (int32_t model : cs.modelsLeft) release_quad(in.host, model);
            for (int32_t model : cs.modelsRight) release_quad(in.host, model);
            cs.left.close(&in);
            cs.right.close(&in);
        }
        state.cars.clear();
        if (state.screenW <= 0) {
            const ScreenSize screen = compute_screen_size();
            state.screenW = screen.w;
            state.screenH = screen.h;
            state.sx = screen.sx;
            state.sy = screen.sy;
        }

        for (int i = 0; i < carCount; i++) {
            CarScreens cs;
            cs.left.create(in, state.screenW, state.screenH);
            cs.right.create(in, state.screenW, state.screenH);
            for (int j = 0; j < 5; j++) {
                cs.modelsLeft[j] = acquire_quad(in.host, cs.left.handle(), LCD_POS_L,
                                                0.f, 0.f, 1.f, 1.f, 1, j * 5.0);
                cs.modelsRight[j] = acquire_quad(in.host, cs.right.handle(), LCD_POS_R,
                                                 0.f, 0.f, 1.f, 1.f, 1, j * 5.0);
            }
            state.cars.push_back(std::move(cs));
        }
    }
};

MTR_REGISTER_VEHICLE_SCRIPT(Wr2LcdScript)

} /* anonymous namespace */
} /* namespace wr2a03 */
