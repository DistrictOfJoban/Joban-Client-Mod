/**
 * gfx2d.hpp — java.awt.Graphics2D subset, in C++, for native scripts.
 *
 * JavaScript scripts can paint through java.awt.Graphics2D
 * (fillRect / fillRoundRect / drawRoundRect / fillOval / drawOval /
 * drawLine / GeneralPath fill+stroke / drawString with derived TTF
 * fonts / clip / AlphaComposite). This header re-implements that exact
 * drawing subset as a deterministic software rasterizer straight onto
 * mtr::GraphicsTexture, so the JS files (config.js / draw_header.js /
 * draw_common.js / draw_circular.js / draw_linear.js / draw_num.js ...)
 * port 1:1 to C++ with only mechanical edits.
 *
 * Resource text: supplying font resource IDs to the constructor uses the
 * host's real FontMetrics and drawString for whole UTF-8 strings, preserving
 * advances, bearings, baseline, compression, alpha and device clipping.
 * resource supplies the same resource fonts as its JavaScript version.
 *
 * Headless / legacy text fallback:
 *   - ASCII/latin: built-in 5x7 font scaled to the requested size,
 *     proportional advances (narrow i/l/., wide m/W) — same numbers
 *     returned by text_width() and used when drawing, so JS layout
 *     math (adaptive font sizing, 5% step compression) ports exactly.
 *   - CJK: full-width 1.0 em advance; pixels come from the host TTF
 *     callback (JcmHostServices.rasterize_text, mirrors JCM
 *     FontManager) and degrade to a tofu outline box without a host.
 *   - ascent = 0.78 em, descent = 0.22 em (≈ Arial/HanSans metrics).
 *
 * Performance notes (vs. the Java2D route inside Rhino):
 *   - every primitive is a tight integer/float loop over its device
 *     bounding box with a src-over fast path for opaque colors —
 *     no Java2D pipeline, no GeneralPath object churn, no per-draw
 *     font derivation;
 *   - dirty tracking merges one bounding box per primitive, so the
 *     upload keeps shipping only what actually changed.
 */
#pragma once

#include "gfx.hpp"
#include "font5x7.hpp"
#include "util.hpp"
#include <algorithm>
#include <cmath>
#include <vector>

#ifndef M_PI
#define M_PI 3.14159265358979323846
#endif

namespace mtr {

/* ---- color helpers (java.awt.Color equivalents) ---- */

constexpr uint32_t gfx_clamp8(int v) {
    return v < 0 ? 0u : (v > 255 ? 255u : static_cast<uint32_t>(v));
}

/* clr(r,g,b) / clra(r,g,b,a) — JS config.js helpers. */
constexpr uint32_t clr(int r, int g, int b) {
    return 0xFF000000u | (gfx_clamp8(r) << 16) | (gfx_clamp8(g) << 8) | gfx_clamp8(b);
}
constexpr uint32_t clra(int r, int g, int b, int a) {
    return (gfx_clamp8(a) << 24) | (gfx_clamp8(r) << 16) | (gfx_clamp8(g) << 8) | gfx_clamp8(b);
}

/* UTF-8 codepoint walk + CJK detection (same ranges as the JS regex
   /[\u3000-\u303f\u4e00-\u9fff\uff00-\uffef]/ used by draw_common.js). */
struct Utf8Cursor {
    const char* p;
    const char* end;
    explicit Utf8Cursor(const char* s, int32_t len)
        : p(s), end(s + (len >= 0 ? len : static_cast<int32_t>(std::strlen(s)))) {}
    bool done() const { return p >= end || *p == '\0'; }
    uint32_t next() {
        const uint8_t c = static_cast<uint8_t>(*p++);
        if (c < 0x80) return c;
        if ((c & 0xE0) == 0xC0 && p < end) {
            const uint8_t c1 = static_cast<uint8_t>(*p++);
            const uint32_t cp = ((c & 0x1Fu) << 6) | (c1 & 0x3Fu);
            return cp;
        }
        if ((c & 0xF0) == 0xE0 && p + 1 < end) {
            const uint8_t c1 = static_cast<uint8_t>(*p++);
            const uint8_t c2 = static_cast<uint8_t>(*p++);
            const uint32_t cp = ((c & 0x0Fu) << 12)
                | ((c1 & 0x3Fu) << 6)
                | (c2 & 0x3Fu);
            return cp;
        }
        if ((c & 0xF8) == 0xF0 && p + 2 < end) {
            const uint8_t c1 = static_cast<uint8_t>(*p++);
            const uint8_t c2 = static_cast<uint8_t>(*p++);
            const uint8_t c3 = static_cast<uint8_t>(*p++);
            const uint32_t cp = ((c & 0x07u) << 18)
                | ((c1 & 0x3Fu) << 12)
                | ((c2 & 0x3Fu) << 6)
                | (c3 & 0x3Fu);
            return cp;
        }
        return 0xFFFDu;
    }
};

inline bool is_cjk_cp(uint32_t cp) {
    return (cp >= 0x3000 && cp <= 0x303F) || (cp >= 0x4E00 && cp <= 0x9FFF)
        || (cp >= 0xFF00 && cp <= 0xFFEF);
}

/** Proportional ASCII advance in em units (Arial-like). */
inline double char_advance_em(uint32_t cp) {
    if (is_cjk_cp(cp)) return 1.0;
    switch (cp) {
        case ' ': return 0.30;
        case 'i': case 'l': case 'j': case 't': case 'f':
        case '.': case ',': case ':': case ';': case '\'':
        case '|': case '!': return 0.28;
        case 'm': case 'M': case 'W': case 'w': return 0.85;
        case '@': return 0.90;
        default: return 0.55;
    }
}

/* ------------------------------------------------------------------ */
/* Gfx2D                                                               */
/* ------------------------------------------------------------------ */

class Gfx2D {
public:
    Gfx2D(GraphicsTexture& tex, const JcmHostServices* host = nullptr,
          const char* font_cjk = nullptr, const char* font_latin = nullptr)
        : tex_(tex), host_(host),
          font_cjk_(font_cjk), font_latin_(font_latin),
          base_(tex.pixels32()), stride_(tex.width()) {
        clip_x0_ = 0; clip_y0_ = 0;
        clip_x1_ = tex.width(); clip_y1_ = tex.height();
    }

    /* g.scale(SCALE_X, SCALE_Y) — logical→device scale, uniform-ish. */
    void set_scale(double sx, double sy) { sx_ = sx; sy_ = sy; }

    void set_color(uint32_t argb) { color_ = argb; }

    /* fullAA() / noAA() — port of GraphicsTexture.fullAA()/noAA()
       (upstream v2.3.0-prerelease.2): toggles shape anti-aliasing on the
       canvas. Java default (constructor) leaves KEY_ANTIALIASING unset —
       shapes are hard-edged until fullAA() — so the C++ default matches
       with aa_ = false and the previous hard-edged loops stay the fast
       path. Text is not affected here: the host TTF rasterization is
       always anti-aliased (Java keeps TEXT_ANTIALIAS ON in the
       constructor and fullAA()/noAA() only re-assert/clear it). */
    void full_aa() { aa_ = true; }
    void no_aa()   { aa_ = false; }
    bool aa() const { return aa_; }

    GraphicsTexture& texture() { return tex_; }

    /* ---- clip (g.clip(rect) / g.setClip(oldClip)) ---- */
    void push_clip(double lx, double ly, double lw, double lh) {
        const int32_t x0 = static_cast<int32_t>(std::floor(lx * sx_));
        const int32_t y0 = static_cast<int32_t>(std::floor(ly * sy_));
        const int32_t x1 = static_cast<int32_t>(std::ceil((lx + lw) * sx_));
        const int32_t y1 = static_cast<int32_t>(std::ceil((ly + lh) * sy_));
        saved_.push_back({clip_x0_, clip_y0_, clip_x1_, clip_y1_});
        clip_x0_ = std::max(clip_x0_, x0); clip_y0_ = std::max(clip_y0_, y0);
        clip_x1_ = std::min(clip_x1_, x1); clip_y1_ = std::min(clip_y1_, y1);
    }
    void pop_clip() {
        if (saved_.empty()) return;
        const Clip c = saved_.back(); saved_.pop_back();
        clip_x0_ = c.x0; clip_y0_ = c.y0; clip_x1_ = c.x1; clip_y1_ = c.y1;
    }

    /* ---- primitives (logical coords, java semantics) ---- */

    /* g.fillRect */
    void fill_rect(double x, double y, double w, double h) {
        int32_t x0 = static_cast<int32_t>(std::floor(x * sx_));
        int32_t y0 = static_cast<int32_t>(std::floor(y * sy_));
        int32_t x1 = static_cast<int32_t>(std::ceil((x + w) * sx_));
        int32_t y1 = static_cast<int32_t>(std::ceil((y + h) * sy_));
        x0 = std::max(x0, clip_x0_); y0 = std::max(y0, clip_y0_);
        x1 = std::min(x1, clip_x1_); y1 = std::min(y1, clip_y1_);
        if (x1 <= x0 || y1 <= y0) return;
        if ((color_ >> 24) == 0xFF) {
            /* opaque fast path: row fill, no per-pixel clip/blend checks */
            for (int32_t py = y0; py < y1; py++) {
                uint32_t* const row = base_ + static_cast<size_t>(py) * stride_;
                std::fill(row + x0, row + x1, color_);
            }
        } else {
            for (int32_t py = y0; py < y1; py++) {
                for (int32_t px = x0; px < x1; px++) plot(px, py);
            }
        }
        dirty_union(x0, y0, x1 - 1, y1 - 1);
    }

    /* g.fillRoundRect(x, y, w, h, arcW, arcH) — arc dims = corner ellipse
       diameters (java semantics; pill shape when arc == h). */
    void fill_round_rect(double x, double y, double w, double h,
                         double arc_w, double arc_h) {
        if (arc_w <= 0 || arc_h <= 0) { fill_rect(x, y, w, h); return; }
        const double dx = x * sx_, dy = y * sy_, dw = w * sx_, dh = h * sy_;
        const double arx = std::min(arc_w * sx_, dw) * 0.5;
        const double ary = std::min(arc_h * sy_, dh) * 0.5;
        const int32_t x0 = static_cast<int32_t>(std::floor(dx));
        const int32_t y0 = static_cast<int32_t>(std::floor(dy));
        const int32_t x1 = static_cast<int32_t>(std::ceil(dx + dw));
        const int32_t y1 = static_cast<int32_t>(std::ceil(dy + dh));
        const double cx0 = dx + arx, cx1 = dx + dw - arx;
        const double cy0 = dy + ary, cy1 = dy + dh - ary;
        for (int32_t py = y0; py < y1; py++) {
            const double fy = py + 0.5;
            for (int32_t px = x0; px < x1; px++) {
                const double fx = px + 0.5;
                double ex = 0, ey = 0;
                bool edge = true;
                if (fx < cx0 && fy < cy0) { ex = (cx0 - fx) / arx; ey = (cy0 - fy) / ary; }
                else if (fx > cx1 && fy < cy0) { ex = (fx - cx1) / arx; ey = (cy0 - fy) / ary; }
                else if (fx < cx0 && fy > cy1) { ex = (cx0 - fx) / arx; ey = (fy - cy1) / ary; }
                else if (fx > cx1 && fy > cy1) { ex = (fx - cx1) / arx; ey = (fy - cy1) / ary; }
                else edge = false;
                if (!edge) { plot(px, py); continue; }
                const double rr = std::sqrt(ex * ex + ey * ey);
                /* rr is normalized (1.0 = the boundary): convert to a signed
                   pixel distance ((rr - 1) * local radius) so edge_coverage
                   sees < 0 inside, ~0 on the boundary, > 0 outside. */
                if (aa_) { plot_cov(px, py, edge_coverage((rr - 1.0) * std::min(arx, ary), 1.0)); }
                else if (rr <= 1.0) plot(px, py);
            }
        }
        dirty_union(x0, y0, x1 - 1, y1 - 1);
    }

    /* g.drawRoundRect(x, y, w, h, arcW, arcH) with BasicStroke(lw).
       Scratch buffers are members: two heap vectors per call per frame in
       the render path were avoidable allocation churn. */
    void draw_round_rect(double x, double y, double w, double h,
                         double arc_w, double arc_h, double lw) {
        scratch_xs_.clear(); scratch_ys_.clear();
        append_round_rect_path(scratch_xs_, scratch_ys_, x, y, w, h, arc_w, arc_h, 12);
        stroke_polyline(scratch_xs_.data(), scratch_ys_.data(),
                        static_cast<int32_t>(scratch_xs_.size()), lw);
    }

    /* g.fillOval */
    void fill_oval(double x, double y, double w, double h) {
        const double dx = x * sx_, dy = y * sy_, dw = w * sx_, dh = h * sy_;
        const double rx = dw * 0.5, ry = dh * 0.5;
        const double cx = dx + rx, cy = dy + ry;
        const int32_t x0 = static_cast<int32_t>(std::floor(dx));
        const int32_t y0 = static_cast<int32_t>(std::floor(dy));
        const int32_t x1 = static_cast<int32_t>(std::ceil(dx + dw));
        const int32_t y1 = static_cast<int32_t>(std::ceil(dy + dh));
        for (int32_t py = y0; py < y1; py++) {
            const double ey = (py + 0.5 - cy) / ry;
            for (int32_t px = x0; px < x1; px++) {
                const double ex = (px + 0.5 - cx) / rx;
                if (aa_) {
                    plot_cov(px, py, edge_coverage((std::sqrt(ex * ex + ey * ey) - 1.0)
                                                    * std::min(rx, ry), 1.0));
                } else if (ex * ex + ey * ey <= 1.0) plot(px, py);
            }
        }
        dirty_union(x0, y0, x1 - 1, y1 - 1);
    }

    /* g.drawOval + BasicStroke(lw) */
    void draw_oval(double x, double y, double w, double h, double lw) {
        const double dx = x * sx_, dy = y * sy_, dw = w * sx_, dh = h * sy_;
        const double rx = dw * 0.5, ry = dh * 0.5;
        const double cx = dx + rx, cy = dy + ry;
        const double tol = (lw * std::max(sx_, sy_)) * 0.5 + 0.5;
        const double norm = std::max(rx, ry);
        const int32_t x0 = static_cast<int32_t>(std::floor(dx - tol));
        const int32_t y0 = static_cast<int32_t>(std::floor(dy - tol));
        const int32_t x1 = static_cast<int32_t>(std::ceil(dx + dw + tol));
        const int32_t y1 = static_cast<int32_t>(std::ceil(dy + dh + tol));
        for (int32_t py = y0; py < y1; py++) {
            for (int32_t px = x0; px < x1; px++) {
                const double ex = (px + 0.5 - cx) / rx;
                const double ey = (py + 0.5 - cy) / ry;
                const double d = std::fabs(std::sqrt(ex * ex + ey * ey) - 1.0) * norm;
                if (aa_) { plot_cov(px, py, edge_coverage(tol - d, 1.0)); }
                else if (d <= tol) plot(px, py);
            }
        }
        dirty_union(x0, y0, x1 - 1, y1 - 1);
    }

    /* g.drawLine + BasicStroke(lw, CAP_ROUND, JOIN_ROUND) — distance
       field ⇒ round caps for free. */
    void draw_line(double x1, double y1, double x2, double y2, double lw) {
        stroke_segment_device(x1 * sx_, y1 * sy_, x2 * sx_, y2 * sy_, lw * std::max(sx_, sy_));
    }

    /* GeneralPath stroke: moveTo/lineTo point list, CAP/JOIN round. */
    void stroke_polyline(const double* xs, const double* ys, int32_t n, double lw) {
        if (n < 2) return;
        const double lw_dev = lw * std::max(sx_, sy_);
        for (int32_t i = 1; i < n; i++) {
            stroke_segment_device(xs[i - 1] * sx_, ys[i - 1] * sy_,
                                   xs[i] * sx_, ys[i] * sy_, lw_dev);
        }
    }

    /* GeneralPath fill (even-odd, e.g. drawArrow triangles). */
    void fill_polygon(const double* xs, const double* ys, int32_t n) {
        if (n < 3) return;
        double min_x = 1e30, min_y = 1e30, max_x = -1e30, max_y = -1e30;
        scratch_xs_.clear(); scratch_ys_.clear();
        scratch_xs_.resize(static_cast<size_t>(n)); scratch_ys_.resize(static_cast<size_t>(n));
        double* dxs = scratch_xs_.data(); double* dys = scratch_ys_.data();
        for (int32_t i = 0; i < n; i++) {
            dxs[i] = xs[i] * sx_; dys[i] = ys[i] * sy_;
            min_x = std::min(min_x, dxs[i]); max_x = std::max(max_x, dxs[i]);
            min_y = std::min(min_y, dys[i]); max_y = std::max(max_y, dys[i]);
        }
        const int32_t y0 = std::max(clip_y0_, static_cast<int32_t>(std::floor(min_y)));
        const int32_t y1 = std::min(clip_y1_ - 1, static_cast<int32_t>(std::ceil(max_y)));
        scratch_hits_.clear();
        std::vector<double>& xhits = scratch_hits_;
        for (int32_t py = y0; py <= y1; py++) {
            const double fy = py + 0.5;
            xhits.clear();
            for (int32_t i = 0, j = n - 1; i < n; j = i++) {
                const double yi = dys[i], yj = dys[j];
                if ((yi <= fy && yj > fy) || (yj <= fy && yi > fy)) {
                    const double t = (fy - yi) / (yj - yi);
                    xhits.push_back(dxs[i] + t * (dxs[j] - dxs[i]));
                }
            }
            std::sort(xhits.begin(), xhits.end());
            for (size_t k = 0; k + 1 < xhits.size(); k += 2) {
                const double xa = xhits[k], xb = xhits[k + 1];
                const int32_t px0 = std::max(clip_x0_, static_cast<int32_t>(std::floor(xa)));
                const int32_t px1 = std::min(clip_x1_ - 1, static_cast<int32_t>(std::ceil(xb)));
                for (int32_t px = px0; px <= px1; px++) {
                    if (aa_) {
                        /* Horizontal box-filter coverage: the fraction of
                           [px, px+1) covered by the span [xa, xb]. */
                        const double cov = std::min(static_cast<double>(px + 1), xb)
                                         - std::max(static_cast<double>(px), xa);
                        plot_cov(px, py, cov);
                    } else {
                        plot(px, py);
                    }
                }
            }
        }
        dirty_union(static_cast<int32_t>(min_x), static_cast<int32_t>(min_y),
                    static_cast<int32_t>(max_x), static_cast<int32_t>(max_y));
    }

    /* ---- text (baseline semantics like drawString) ---- */

    static double text_ascent(double size) { return size * 0.78; }
    static double text_descent(double size) { return size * 0.22; }

    /* Resource font metrics are paired with the host drawString callback.
       The static text_* helpers below remain the headless bitmap fallback. */
    JcmFontMetrics font_metrics(double size, const char* utf8 = "",
                                 const char* font = nullptr) const {
        JcmFontMetrics result{};
        const char* path = font ? font : text_font(utf8, static_cast<int32_t>(std::strlen(utf8)));
        if (path && host_ && host_->measure_font_text &&
            host_->measure_font_text(host_->user, path, utf8,
                static_cast<int32_t>(std::strlen(utf8)), size, sx_, sy_, &result) == 0) return result;
        return {text_width(size, utf8), text_ascent(size), text_descent(size), size};
    }
    double font_width(double size, const char* utf8, const char* font = nullptr) const {
        return font_metrics(size, utf8, font).width;
    }
    double font_ascent(double size, const char* font = nullptr) const {
        return font_metrics(size, "", font).ascent;
    }
    double font_descent(double size, const char* font = nullptr) const {
        return font_metrics(size, "", font).descent;
    }

    /* Logical width — must match draw_text exactly (self-consistent
       replacement for g.getFontMetrics().stringWidth). */
    static double text_width(double size, const char* utf8) {
        Utf8Cursor c(utf8, -1);
        double w = 0;
        while (!c.done()) w += char_advance_em(c.next()) * size;
        return w;
    }
    static double text_width(double size, const StrView& s) {
        Utf8Cursor c(s.data, s.len);
        double w = 0;
        while (!c.done()) w += char_advance_em(c.next()) * size;
        return w;
    }

    /* measureMixedText — CJK segments at sizeCn, latin at sizeEn. */
    static double mixed_text_width(double size_cjk, double size_en, const char* utf8) {
        Utf8Cursor c(utf8, -1);
        double w = 0;
        while (!c.done()) {
            const uint32_t cp = c.next();
            w += char_advance_em(cp) * (is_cjk_cp(cp) ? size_cjk : size_en);
        }
        return w;
    }

    /* g.drawString(str, x, y) — baseline at y, current color. */
    void draw_text(double x, double y_baseline, double size, const char* utf8,
                   const char* font = nullptr) {
        draw_text_impl(x, y_baseline, size, size, utf8, static_cast<int32_t>(std::strlen(utf8)), 1.0, font);
    }
    void draw_text(double x, double y_baseline, double size, const StrView& s) {
        draw_text_impl(x, y_baseline, size, size, s.data, s.len, 1.0);
    }
    /* drawMixedText: CJK glyphs at size_cjk, latin at size_en. */
    void draw_mixed_text(double x, double y_baseline, double size_cjk,
                         double size_en, const char* utf8) {
        draw_text_impl(x, y_baseline, size_cjk, size_en, utf8,
                       static_cast<int32_t>(std::strlen(utf8)), 1.0);
    }
    /* drawAdaptiveCenteredText: centered + optional horizontal compression. */
    void draw_text_compressed(double cx, double y_baseline, double size,
                              const char* utf8, double compress, const char* font = nullptr) {
        draw_text_impl(cx, y_baseline, size, size, utf8,
                       static_cast<int32_t>(std::strlen(utf8)), compress, font);
    }

    /* drawCenteredText */
    void draw_text_centered(double cx, double y_baseline, double size, const char* utf8) {
        draw_text_impl(cx, y_baseline, size, size, utf8,
                       static_cast<int32_t>(std::strlen(utf8)), 1.0);
    }

    /* drawImage approximation for the route logo slot: JS loads a PNG;
       native scripts draw a route-colored rounded badge instead (the JS
       itself falls back to "no logo" when every path fails). */
    void draw_logo_badge(double x, double y, double w, double h, uint32_t bg,
                         const char* label, double label_size) {
        set_color(bg);
        fill_round_rect(x, y, w, h, h * 0.3, h * 0.3);
        const uint32_t fg = contrast_text(bg);
        set_color(fg);
        const double tw = font_width(label_size, label);
        draw_text(x + (w - tw) * 0.5, y + h * 0.5 + font_ascent(label_size) * 0.5,
                  label_size, label);
    }

    /* getContrastTextColor(bg) — JS config.js luma rule. */
    static uint32_t contrast_text(uint32_t bg) {
        const double r = (bg >> 16) & 0xFF, g = (bg >> 8) & 0xFF, b = bg & 0xFF;
        const double y = (0.299 * r + 0.587 * g + 0.114 * b) / 255.0;
        return y > 0.5 ? 0xFF000000u : 0xFFFFFFFFu;
    }

private:
    /* Reusable raster scratch (hot path stays allocation-free). */
    std::vector<double> scratch_xs_, scratch_ys_, scratch_hits_;
    struct Clip { int32_t x0, y0, x1, y1; };

    inline void plot(int32_t px, int32_t py) {
        if (px < clip_x0_ || py < clip_y0_ || px >= clip_x1_ || py >= clip_y1_) return;
        uint32_t* const row = base_ + static_cast<size_t>(py) * stride_;
        plot_blend(row + px, color_);
    }

    /* Coverage-blended plot for the AA paths: the source alpha is scaled
       by cov (0..1) first, then the same integer src-over as plot(). */
    inline void plot_cov(int32_t px, int32_t py, double cov) {
        if (cov <= 0.0) return;
        if (cov > 1.0) cov = 1.0;
        if (px < clip_x0_ || py < clip_y0_ || px >= clip_x1_ || py >= clip_y1_) return;
        uint32_t* const row = base_ + static_cast<size_t>(py) * stride_;
        const uint32_t src = color_;
        const uint32_t sa = static_cast<uint32_t>((src >> 24) * cov + 0.5);
        if (sa == 0) return;
        if (sa >= 255) { row[px] = src; return; }
        plot_blend(row + px, (src & 0x00FFFFFFu) | (sa << 24));
    }

    inline void plot_blend(uint32_t* dst_ptr, uint32_t src) {
        const uint32_t sa = src >> 24;
        if (sa == 0) return;
        if (sa == 0xFF) { *dst_ptr = src; return; }
        const uint32_t dst = *dst_ptr;
        const uint32_t da = dst >> 24;
        /* integer src-over */
        const uint32_t inv = 255 - sa;
        const uint32_t out_a = sa + ((inv * da + 127) >> 8);
        auto mix = [sa, inv](uint32_t sc, uint32_t dc) {
            return (sa * sc + inv * dc + 127) >> 8;
        };
        *dst_ptr = (out_a << 24) | (mix((src >> 16) & 0xFF, (dst >> 16) & 0xFF) << 16)
                      | (mix((src >> 8) & 0xFF, (dst >> 8) & 0xFF) << 8)
                      | mix(src & 0xFF, dst & 0xFF);
    }

    /* Analytic edge coverage for the AA paths: dist_px is a signed distance
       in PIXELS (<= 0 inside the shape / stroke band, > 0 outside) —
       normalized-space distances must be scaled back to pixels by the
       caller before reaching this function. One-pixel feather, the same
       visual softness as Java2D's default ANTIALIAS quality hint. */
    static inline double edge_coverage(double dist_px, double /*scale*/) {
        const double c = 0.5 - dist_px;
        return c <= 0.0 ? 0.0 : (c >= 1.0 ? 1.0 : c);
    }

    inline void dirty_union(int32_t x0, int32_t y0, int32_t x1, int32_t y1) {
        if (x1 < x0 || y1 < y0) return;
        tex_.mark_dirty(x0, y0);
        tex_.mark_dirty(std::min(x1, tex_.width() - 1), std::min(y1, tex_.height() - 1));
    }

    void stroke_segment_device(double x1, double y1, double x2, double y2, double lw) {
        const double r = lw * 0.5 + 0.35;
        const double min_x = std::min(x1, x2) - r, max_x = std::max(x1, x2) + r;
        const double min_y = std::min(y1, y2) - r, max_y = std::max(y1, y2) + r;
        const int32_t px0 = std::max(clip_x0_, static_cast<int32_t>(std::floor(min_x)));
        const int32_t py0 = std::max(clip_y0_, static_cast<int32_t>(std::floor(min_y)));
        const int32_t px1 = std::min(clip_x1_ - 1, static_cast<int32_t>(std::ceil(max_x)));
        const int32_t py1 = std::min(clip_y1_ - 1, static_cast<int32_t>(std::ceil(max_y)));
        const double dx = x2 - x1, dy = y2 - y1;
        const double len2 = dx * dx + dy * dy;
        for (int32_t py = py0; py <= py1; py++) {
            for (int32_t px = px0; px <= px1; px++) {
                double t = 0;
                if (len2 > 0) {
                    t = ((px + 0.5 - x1) * dx + (py + 0.5 - y1) * dy) / len2;
                    t = t < 0 ? 0 : (t > 1 ? 1 : t);
                }
                const double ex = px + 0.5 - (x1 + t * dx);
                const double ey = py + 0.5 - (y1 + t * dy);
                if (aa_) {
                    plot_cov(px, py, edge_coverage(r - std::sqrt(ex * ex + ey * ey), 1.0));
                } else if (ex * ex + ey * ey <= r * r) plot(px, py);
            }
        }
        dirty_union(px0, py0, px1, py1);
    }

    /* Java RoundRectangle2D outline as a polyline (line + arc steps). */
    static void append_round_rect_path(std::vector<double>& xs, std::vector<double>& ys,
                                       double x, double y, double w, double h,
                                       double arc_w, double arc_h, int steps) {
        const double rx = std::min(arc_w, w) * 0.5;
        const double ry = std::min(arc_h, h) * 0.5;
        auto pt = [&](double px, double py) { xs.push_back(px); ys.push_back(py); };
        auto arc = [&](double cx, double cy, double a0, double a1) {
            for (int s = 0; s <= steps; s++) {
                const double a = a0 + (a1 - a0) * s / steps;
                pt(cx + rx * std::cos(a), cy + ry * std::sin(a));
            }
        };
        pt(x + rx, y);
        pt(x + w - rx, y);
        arc(x + w - rx, y + ry, -M_PI / 2, 0);     /* TR */
        pt(x + w, y + h - ry);
        arc(x + w - rx, y + h - ry, 0, M_PI / 2);   /* BR */
        pt(x + rx, y + h);
        arc(x + rx, y + h - ry, M_PI / 2, M_PI);    /* BL */
        pt(x, y + ry);
        arc(x + rx, y + ry, M_PI, 3 * M_PI / 2);    /* TL, closes the path */
    }

    const char* text_font(const char* utf8, int32_t len) const {
        Utf8Cursor c(utf8, len);
        while (!c.done()) if (is_cjk_cp(c.next())) return font_cjk_;
        return font_latin_;
    }

    /* Shared text rasterizer: segments CJK vs latin, per-glyph size,
       optional X compression, deterministic advances. */
    void draw_text_impl(double x, double y_baseline, double size_cjk, double size_en,
                        const char* utf8, int32_t len, double compress, const char* font = nullptr) {
        if (size_cjk == size_en) {
            const char* path = font ? font : text_font(utf8, len);
            int32_t bounds[4]{};
            if (path && host_ && host_->draw_font_text &&
                host_->draw_font_text(host_->user, path, utf8, len, size_en,
                    x, y_baseline, sx_, sy_, compress, color_,
                    clip_x0_, clip_y0_, clip_x1_, clip_y1_,
                    tex_.pixels(), tex_.width(), tex_.height(), bounds) == 0) {
                if (bounds[2] > bounds[0] && bounds[3] > bounds[1])
                    dirty_union(bounds[0], bounds[1], bounds[2] - 1, bounds[3] - 1);
                return;
            }
        }
        Utf8Cursor c(utf8, len);
        double pen = x;   /* logical */
        while (!c.done()) {
            const uint32_t cp = c.next();
            const bool cjk = is_cjk_cp(cp);
            const double size = cjk ? size_cjk : size_en;
            const double adv = char_advance_em(cp) * size * compress;
            if (cp != ' ' && cp != 0xFFFD) {
                if (cjk) {
                    draw_cjk_glyph(pen, y_baseline, size, cp, compress);
                } else if (cp < 128) {
                    draw_ascii_glyph(pen, y_baseline, size, static_cast<char>(cp), compress);
                } else {
                    draw_cjk_glyph(pen, y_baseline, size, cp, compress); /* tofu/host */
                }
            }
            pen += adv;
        }
        const double w = pen - x;
        dirty_union(static_cast<int32_t>(std::floor(x * sx_)),
                    static_cast<int32_t>(std::floor((y_baseline - text_ascent(std::max(size_cjk, size_en))) * sy_)),
                    static_cast<int32_t>(std::ceil((x + w) * sx_)),
                    static_cast<int32_t>(std::ceil((y_baseline + text_descent(std::max(size_cjk, size_en))) * sy_)));
    }

    void draw_ascii_glyph(double x_logical, double y_baseline, double size,
                          char ch, double compress) {
        const uint8_t* glyph = font5x7_glyph(static_cast<uint8_t>(ch));
        if (!glyph) return;
        /* device geometry: glyph height = 7/8 of size, 5:7 aspect ratio,
           centered inside the proportional advance cell. */
        const double adv_dev = char_advance_em(static_cast<uint32_t>(ch)) * size * sx_ * compress;
        const double gh_dev = size * sy_ * (7.0 / 8.0);
        const double gw_dev = gh_dev * (5.0 / 7.0);
        const double x_dev = x_logical * sx_ + (adv_dev - gw_dev) * 0.5;
        const double gTop = (y_baseline - text_ascent(size)) * sy_ + size * sy_ * (1.0 / 16.0);
        for (int row = 0; row < 7; row++) {
            for (int col = 0; col < 5; col++) {
                if ((glyph[col] >> row) & 1) {
                    const double gx0 = x_dev + gw_dev * col / 5.0;
                    const double gy0 = gTop + gh_dev * row / 7.0;
                    fill_rect_dev(gx0, gy0, gw_dev / 5.0 + 0.5, gh_dev / 7.0 + 0.5);
                }
            }
        }
    }

    /* CJK glyph: host TTF callback (top-left origin), else tofu box. */
    void draw_cjk_glyph(double x_logical, double y_baseline, double size,
                        uint32_t cp, double compress) {
        char buf[8];
        int32_t n = 0;
        if (cp < 0x80) { buf[n++] = static_cast<char>(cp); }
        else if (cp < 0x800) {
            buf[n++] = static_cast<char>(0xC0 | (cp >> 6));
            buf[n++] = static_cast<char>(0x80 | (cp & 0x3F));
        } else {
            buf[n++] = static_cast<char>(0xE0 | (cp >> 12));
            buf[n++] = static_cast<char>(0x80 | ((cp >> 6) & 0x3F));
            buf[n++] = static_cast<char>(0x80 | (cp & 0x3F));
        }
        const double w_dev = size * sx_ * compress;
        const double x_dev = x_logical * sx_;
        const double y_top = (y_baseline - text_ascent(size)) * sy_;
        if (host_ && host_->rasterize_text) {
            const uint32_t col = color_;
            if (host_->rasterize_text(host_->user, buf, n,
                                      static_cast<int32_t>(x_dev), static_cast<int32_t>(y_top),
                                      static_cast<int32_t>(w_dev + 1),
                                      static_cast<uint8_t>((col >> 16) & 0xFF),
                                      static_cast<uint8_t>((col >> 8) & 0xFF),
                                      static_cast<uint8_t>(col & 0xFF),
                                      tex_.pixels(), tex_.width(), tex_.height()) >= 0) {
                return;   /* host covered the glyph */
            }
        }
        /* fallback: tofu outline box (keeps layout visible without host) */
        const double lw = std::max(1.0, size * 0.06 * std::max(sx_, sy_));
        const double inset = size * 0.08;
        const double box_y = y_baseline - text_ascent(size) + inset;
        const double box_h = size - inset * 2;
        stroke_rect_dev(x_dev + inset * sx_, box_y * sy_,
                        (size * compress - inset * 2) * sx_, box_h * sy_, lw);
    }

    void stroke_rect_dev(double x, double y, double w, double h, double lw) {
        if (w <= 0 || h <= 0) return;
        fill_rect_dev(x, y, w, lw);                       /* top */
        fill_rect_dev(x, y + h - lw, w, lw);              /* bottom */
        fill_rect_dev(x, y + lw, lw, h - 2 * lw);         /* left */
        fill_rect_dev(x + w - lw, y + lw, lw, h - 2 * lw);/* right */
    }

    void fill_rect_dev(double x, double y, double w, double h) {
        if (w <= 0 || h <= 0) return;
        int32_t x0 = std::max(clip_x0_, static_cast<int32_t>(std::floor(x)));
        int32_t y0 = std::max(clip_y0_, static_cast<int32_t>(std::floor(y)));
        int32_t x1 = std::min(clip_x1_, static_cast<int32_t>(std::ceil(x + w)));
        int32_t y1 = std::min(clip_y1_, static_cast<int32_t>(std::ceil(y + h)));
        if (x1 <= x0 || y1 <= y0) return;
        if ((color_ >> 24) == 0xFF) {
            for (int32_t py = y0; py < y1; py++) {
                uint32_t* const row = base_ + static_cast<size_t>(py) * stride_;
                std::fill(row + x0, row + x1, color_);
            }
        } else {
            for (int32_t py = y0; py < y1; py++) {
                for (int32_t px = x0; px < x1; px++) plot(px, py);
            }
        }
    }

    GraphicsTexture& tex_;
    const JcmHostServices* host_ = nullptr;
    const char* font_cjk_ = nullptr;
    const char* font_latin_ = nullptr;
    uint32_t* base_ = nullptr;        /* cached pixel base (no realloc while drawing) */
    int32_t stride_ = 0;
    uint32_t color_ = 0xFF000000u;
    double sx_ = 1.0, sy_ = 1.0;
    bool aa_ = false;                 /* fullAA()/noAA() — shape anti-aliasing */
    int32_t clip_x0_ = 0, clip_y0_ = 0, clip_x1_ = 0, clip_y1_ = 0;
    std::vector<Clip> saved_;
};

} /* namespace mtr */
