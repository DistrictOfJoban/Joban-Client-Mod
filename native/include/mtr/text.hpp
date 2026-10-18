/**
 * text.hpp / texture builders — 1:1 port of the JS PIDS draw-call
 * fluent API (jsblock `Text` / `Texture` classes, see
 * com.lx862.jcm.mod.scripting.pids.TextWrapper / TextureWrapper).
 *
 * JS:
 *   Text.create("Destination Text")
 *       .text("Tsim Sha Tsui")
 *       .scale(1.725).size(100, 9).stretchXY()
 *       .fontMC().color(0xFC9700)
 *       .pos(3, rowY).draw(ctx);
 *
 * C++:
 *   Text::create("Destination Text")
 *       .text("Tsim Sha Tsui")
 *       .scale(1.725).size(100, 9).stretch_xy()
 *       .font_mc().color(0xFC9700)
 *       .pos(3, row_y).draw(ctx);
 */
#pragma once

#include "mtr_native.h"
#include "frame.hpp"
#include <cstddef>

namespace mtr {

class PidsContext; /* fwd */

/* ------------------------------------------------------------------ */
/* Text (mirrors TextWrapper extends PIDSDrawCall<TextWrapper>)        */
/* ------------------------------------------------------------------ */

class Text {
public:
    static Text create(const char* /*comment*/ = nullptr) {
        return Text();
    }

    Text& text(const char* str, int32_t len = -1) {
        text_ = str;
        text_len_ = len;
        return *this;
    }

    Text& scale(double s)            { rec_.scale = s; return *this; }
    Text& left_align()               { rec_.align = MTR_TEXT_ALIGN_LEFT;   return *this; }
    Text& center_align()             { rec_.align = MTR_TEXT_ALIGN_CENTER; return *this; }
    Text& right_align()              { rec_.align = MTR_TEXT_ALIGN_RIGHT;  return *this; }
    Text& shadowed()                 { rec_.shadow = 1; return *this; }
    Text& bold()                     { rec_.bold = 1; return *this; }
    Text& italic()                   { rec_.italic = 1; return *this; }
    Text& stretch_xy()               { rec_.overflow = MTR_TEXT_OVERFLOW_STRETCH; return *this; }
    Text& scale_xy()                 { rec_.overflow = MTR_TEXT_OVERFLOW_SCALE;   return *this; }
    Text& wrap_text()                { rec_.overflow = MTR_TEXT_OVERFLOW_WRAP;    return *this; }
    Text& marquee(double duration = -1) {
        rec_.overflow = MTR_TEXT_OVERFLOW_MARQUEE;
        rec_.marquee_duration = duration;
        return *this;
    }
    Text& with_marquee_progress(double p) { rec_.marquee_progress = p; return *this; }

    /* font(id): custom font, e.g. .font("jsblock:shanghai") */
    Text& font(const char* font_id) { font_ = font_id; return *this; }
    Text& font_mc()                  { font_ = nullptr; return *this; }

    Text& color(int32_t argb)        { rec_.color = argb; return *this; }
    Text& pos(double x, double y)    { rec_.x = x; rec_.y = y; return *this; }
    Text& size(double w, double h)   { rec_.w = w; rec_.h = h; return *this; }
    Text& z_order(int32_t z)         { rec_.z_order = z; return *this; }
    Text& layer(MtrRenderLayer l)    { rec_.layer = static_cast<int32_t>(l); return *this; }

    /* Finalize: commit into the frame (mirrors .draw(ctx)). */
    void draw(PidsContext& ctx);

private:
    /* Same defaults as FrameRecorder::push_text — see default_text_record:
       a plain {} here used to overwrite scale/color/align/uv with zeros on
       the struct copy in draw(). */
    JcmDrawText rec_ = FrameRecorder::default_text_record();
    const char* text_ = nullptr;
    int32_t text_len_ = -1;
    const char* font_ = nullptr;
};

/* ------------------------------------------------------------------ */
/* Texture (mirrors TextureWrapper)                                    */
/* ------------------------------------------------------------------ */

class Texture {
public:
    static Texture create(const char* /*comment*/ = nullptr) {
        return Texture();
    }

    Texture& texture(const char* id) {
        tex_ = id;
        return *this;
    }

    Texture& color(int32_t argb)      { rec_.color = argb; return *this; }
    Texture& uv(float u1, float v1, float u2, float v2) {
        rec_.u1 = u1; rec_.v1 = v1; rec_.u2 = u2; rec_.v2 = v2;
        return *this;
    }
    Texture& pos(double x, double y)  { rec_.x = x; rec_.y = y; return *this; }
    Texture& size(double w, double h) { rec_.w = w; rec_.h = h; return *this; }
    Texture& z_order(int32_t z)       { rec_.z_order = z; return *this; }
    Texture& layer(MtrRenderLayer l)  { rec_.layer = static_cast<int32_t>(l); return *this; }

    void draw(PidsContext& ctx);

private:
    /* See default_text_record: matches FrameRecorder::push_texture defaults. */
    JcmDrawTexture rec_ = FrameRecorder::default_texture_record();
    const char* tex_ = nullptr;
};

} /* namespace mtr */
