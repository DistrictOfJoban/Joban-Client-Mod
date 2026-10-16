/**
 * gfx.hpp — native GraphicsTexture + dot-matrix LCD canvas.
 *
 * The JS ecosystem paints vehicle LCDs through GraphicsTexture
 * (java.awt.Graphics2D — see mtrsteamloco/scripts/display_helper.js).
 * The native equivalent keeps the pixel buffer entirely inside the
 * shared library and ships dirty rects to the host through the frame
 * pixel arena, so per-pixel work never crosses the JVM boundary.
 *
 * LcdCanvas renders with the built-in 5x7 bitmap font (font5x7.hpp);
 * CJK glyphs fall back to host TTF rasterization via host services
 * (mirrors JCM FontManager TTF sets, e.g. "jsblock:shanghai").
 */
#pragma once

#include "mtr_native.h"
#include "frame.hpp"
#include "font5x7.hpp"
#include <cstdint>
#include <cstring>
#include <string>
#include <vector>

namespace mtr {

class GraphicsTexture {
public:
    static constexpr int32_t INVALID = -1;

    GraphicsTexture() = default;

    /* Mirrors `new GraphicsTexture(w, h)` + ModelManager upload. */
    void create(const JcmFrameInput& in, int32_t w, int32_t h) {
        width_ = w;
        height_ = h;
        pixels_.assign(static_cast<size_t>(w) * static_cast<size_t>(h), 0x00000000);
        if (in.host && in.host->create_texture) {
            handle_ = in.host->create_texture(in.host->user, w, h);
        }
        dirty_all();
    }

    ~GraphicsTexture() { close(nullptr); }

    GraphicsTexture(const GraphicsTexture&) = delete;
    GraphicsTexture& operator=(const GraphicsTexture&) = delete;
    GraphicsTexture(GraphicsTexture&& other) noexcept { swap(other); }
    GraphicsTexture& operator=(GraphicsTexture&& other) noexcept { swap(other); return *this; }

    void swap(GraphicsTexture& o) noexcept {
        std::swap(width_, o.width_);
        std::swap(height_, o.height_);
        std::swap(handle_, o.handle_);
        std::swap(pixels_, o.pixels_);
        std::swap(dirty_x_, o.dirty_x_);
        std::swap(dirty_y_, o.dirty_y_);
        std::swap(dirty_w_, o.dirty_w_);
        std::swap(dirty_h_, o.dirty_h_);
        std::swap(last_upload_bytes_, o.last_upload_bytes_);
    }

    int32_t handle() const { return handle_; }
    int32_t width()  const { return width_; }
    int32_t height() const { return height_; }
    int64_t texture_bytes() const {
        return static_cast<int64_t>(width_) * static_cast<int64_t>(height_) * 4;
    }
    int64_t last_upload_bytes() const { return last_upload_bytes_; }

    /* Row-major BGRA8 pixel storage, direct access from C++. */
    uint8_t* pixels() { return reinterpret_cast<uint8_t*>(pixels_.data()); }
    const uint8_t* pixels() const { return reinterpret_cast<const uint8_t*>(pixels_.data()); }
    uint32_t* pixels32() { return pixels_.data(); }

    void set_pixel(int32_t x, int32_t y, uint32_t argb) {
        if (x < 0 || y < 0 || x >= width_ || y >= height_) return;
        pixels_[static_cast<size_t>(y) * static_cast<size_t>(width_) + static_cast<size_t>(x)] = argb;
        mark_dirty(x, y);
    }

    void fill(uint32_t argb) {
        for (auto& p : pixels_) p = argb;
        dirty_all();
    }

    /* Dirty-rect bookkeeping. Pixels outside the texture must never
       widen the rect: JS paints with Graphics2D which simply clips,
       and a negative dirty_x / oversized dirty_w would make upload()
       read before the pixel buffer (and the host copy dirty_w*4 bytes
       per row). Callers may pass out-of-range coordinates — e.g. a
       centered string wider than the texture starts at x < 0 — so the
       sample is dropped here instead of being trusted. */
    void mark_dirty(int32_t x, int32_t y) {
        if (x < 0 || y < 0 || x >= width_ || y >= height_) return;
        if (dirty_w_ == 0) { dirty_x_ = x; dirty_y_ = y; dirty_w_ = dirty_h_ = 1; return; }
        const int32_t x0 = dirty_x_ < x ? dirty_x_ : x;
        const int32_t y0 = dirty_y_ < y ? dirty_y_ : y;
        const int32_t x1 = (dirty_x_ + dirty_w_ - 1) > x ? (dirty_x_ + dirty_w_ - 1) : x;
        const int32_t y1 = (dirty_y_ + dirty_h_ - 1) > y ? (dirty_y_ + dirty_h_ - 1) : y;
        dirty_x_ = x0; dirty_y_ = y0;
        dirty_w_ = x1 - x0 + 1; dirty_h_ = y1 - y0 + 1;
    }

    void dirty_all() {
        dirty_x_ = dirty_y_ = 0;
        dirty_w_ = width_; dirty_h_ = height_;
    }

    bool has_dirty() const { return dirty_w_ > 0; }

    /* Commit the dirty rect into the frame (mirrors texture.upload()).
       Call from render() after painting; draw_car_model() references
       this texture through its host handle. Records with handle == -1
       (no host texture, e.g. headless tests) are ignored by the host. */
    void upload(FrameRecorder& frame) {
        last_upload_bytes_ = 0;
        if (dirty_w_ <= 0 || dirty_h_ <= 0) return;
        /* Final clamp before the pointer arithmetic. mark_dirty() already
           refuses out-of-range samples, but the rect is also the source of a
           memcpy length (dirty_w*dirty_h*4), so re-establish the invariant
           here as well — a rect that escapes the texture must never turn into
           a wild copy inside the CRT. */
        if (dirty_x_ < 0) { dirty_w_ += dirty_x_; dirty_x_ = 0; }
        if (dirty_y_ < 0) { dirty_h_ += dirty_y_; dirty_y_ = 0; }
        if (dirty_x_ + dirty_w_ > width_)  dirty_w_ = width_  - dirty_x_;
        if (dirty_y_ + dirty_h_ > height_) dirty_h_ = height_ - dirty_y_;
        if (dirty_w_ <= 0 || dirty_h_ <= 0) {
            dirty_w_ = dirty_h_ = 0;
            return;
        }

        JcmDrawTextureUpload& r = frame.push_texture_upload();
        r.texture_handle = handle_;
        r.width = width_;
        r.height = height_;
        r.dirty_x = dirty_x_;
        r.dirty_y = dirty_y_;
        r.dirty_w = dirty_w_;
        r.dirty_h = dirty_h_;
        r.pixel_data_len = static_cast<int64_t>(dirty_w_) * dirty_h_ * 4;
        last_upload_bytes_ = r.pixel_data_len;

        const uint8_t* base = pixels() + (static_cast<size_t>(dirty_y_) * width_ + dirty_x_) * 4;
        if (dirty_w_ == width_) {
            r.pixel_data_offset = frame.push_pixels(base, r.pixel_data_len);
        } else {
            // A cropped rectangle has the full texture's row stride, whereas
            // the frame upload is tightly packed. Copy each row separately.
            r.pixel_data_offset = frame.push_pixels(base, static_cast<int64_t>(dirty_w_) * 4);
            for (int32_t row = 1; row < dirty_h_; row++) {
                frame.push_pixels(base + static_cast<size_t>(row) * width_ * 4,
                                  static_cast<int64_t>(dirty_w_) * 4);
            }
        }
        dirty_w_ = 0; /* clear */
    }

    void close(const JcmFrameInput* in) {
        if (handle_ != INVALID && in && in->host && in->host->release_texture) {
            in->host->release_texture(in->host->user, handle_);
        }
        handle_ = INVALID;
        pixels_.clear();
        pixels_.shrink_to_fit();
    }

private:
    int32_t width_ = 0, height_ = 0;
    int32_t handle_ = INVALID;
    std::vector<uint32_t> pixels_;   /* ARGB little-endian storage */
    int32_t dirty_x_ = 0, dirty_y_ = 0, dirty_w_ = 0, dirty_h_ = 0;
    int64_t last_upload_bytes_ = 0;
};

/**
 * LcdCanvas — dot-matrix painter over a GraphicsTexture.
 * 5x7 glyphs at 2px dot pitch => 10x10 cell per char + 1px spacing.
 * This is the native replacement for java.awt.Graphics2D.drawString
 * in dynamic texture rendering.
 */
class LcdCanvas {
public:
    static constexpr int CELL_W = 6;   /* 5 px glyph + 1 px space */
    static constexpr int CELL_H = 8;   /* 7 px glyph + 1 px space */
    static constexpr int DOT = 1;      /* dot scale (canvas px per dot) */

    LcdCanvas(GraphicsTexture& tex, uint32_t on_color = 0xFFFFB000,
              uint32_t off_color = 0xFF100800)
        : tex_(tex), on_(on_color), off_(off_color) {}

    void set_colors(uint32_t on, uint32_t off) { on_ = on; off_ = off; }

    void clear() {
        for (int32_t y = 0; y < tex_.height(); y += CELL_H) {
            for (int32_t x = 0; x < tex_.width(); x += CELL_W) {
                dot_cell(x, y, 5, 7, false);
            }
        }
        tex_.dirty_all();
    }

    /* Draw one ASCII line at dot coordinates. Returns dot width used. */
    int32_t draw_text(int32_t x_dot, int32_t y_dot, const char* utf8) {
        int32_t x = x_dot;
        for (const char* p = utf8; *p; ++p) {
            const char c = *p;
            if (c == ' ') { x += CELL_W; continue; }
            const uint8_t* glyph = font5x7_glyph(static_cast<uint8_t>(c));
            if (!glyph) { x += CELL_W; continue; }
            for (int col = 0; col < 5; col++) {
                const uint8_t bits = glyph[col];
                for (int row = 0; row < 7; row++) {
                    /* classic 5x7 tables: bit 0 (LSB) = top pixel */
                    const bool on = (bits >> row) & 1;
                    dot(x + col, y_dot + row, on);
                }
            }
            x += CELL_W;
        }
        return x - x_dot;
    }

    /* Mirrored glyphs for the opposite side of the car. */
    int32_t draw_text_mirrored(int32_t x_dot, int32_t y_dot, const char* utf8) {
        int32_t x = x_dot;
        for (const char* p = utf8; *p; ++p) {
            const char c = *p;
            if (c == ' ') { x += CELL_W; continue; }
            const uint8_t* glyph = font5x7_glyph(static_cast<uint8_t>(c));
            if (!glyph) { x += CELL_W; continue; }
            for (int col = 0; col < 5; col++) {
                const uint8_t bits = glyph[4 - col];
                for (int row = 0; row < 7; row++) {
                    const bool on = (bits >> row) & 1;
                    dot(x + col, y_dot + row, on);
                }
            }
            x += CELL_W;
        }
        return x - x_dot;
    }

    /* Solid dot / bar primitives (arrows, separators). */
    void dot(int32_t x, int32_t y, bool on) {
        tex_.set_pixel(x, y, on ? on_ : off_);
    }

    void dot_cell(int32_t x, int32_t y, int w, int h, bool on) {
        for (int dy = 0; dy < h; dy++) {
            for (int dx = 0; dx < w; dx++) {
                tex_.set_pixel(x + dx, y + dy, on ? on_ : off_);
            }
        }
    }

    void fill_rect(int32_t x, int32_t y, int32_t w, int32_t h, uint32_t argb) {
        for (int dy = 0; dy < h; dy++) {
            for (int dx = 0; dx < w; dx++) {
                tex_.set_pixel(x + dx, y + dy, argb);
            }
        }
    }

    /* Blink helper: door arrows etc. */
    static bool blink(int64_t game_millis, int64_t period_millis) {
        return (game_millis / period_millis) % 2 == 0;
    }

    static int32_t text_width(const char* s) {
        int32_t n = 0;
        for (const char* p = s; *p; ++p) n += CELL_W;
        return n;
    }

    /* CJK fallback: ask the host to rasterize with TTF fonts. */
    bool draw_text_host(const JcmFrameInput& in, int32_t x, int32_t y,
                        const char* utf8, int32_t len,
                        uint8_t r, uint8_t g, uint8_t b) {
        if (!in.host || !in.host->rasterize_text) return false;
        return in.host->rasterize_text(in.host->user, utf8, len, x, y,
                                       tex_.width(), r, g, b,
                                       tex_.pixels(), tex_.width(), tex_.height()) >= 0;
    }

    GraphicsTexture& texture() { return tex_; }

private:
    GraphicsTexture& tex_;
    uint32_t on_, off_;
};

} /* namespace mtr */
