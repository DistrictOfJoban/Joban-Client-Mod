/**
 * text.hpp / texture builders — 1:1 port of the JS PIDS draw-call
 * fluent API (jsblock `Text` / `Texture` / `Rect` classes, see
 * com.lx862.jcm.mod.scripting.pids.TextWrapper / TextureWrapper /
 * RectangleWrapper — the JS name is `Rect` since upstream
 * v2.3.0-prerelease.2; the Java class keeps its RectangleWrapper name).
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
#include <cstring>
#include <stdexcept>
#include <string>

namespace mtr {

class PidsContext; /* fwd */

/* renderType("LIGHT_2") — QueuedRenderLayer.valueOf(String) equivalent.
   The four MtrRenderLayer values condense the MC enum; the light variants
   share MTR_LAYER_LIGHT exactly like the previous layer(MtrRenderLayer)
   setter. Unknown names throw, mirroring IllegalArgumentException from
   valueOf. (LIGHT_1/LIGHT_3 are accepted as aliases of the light layer —
   the host maps the condensed layer back to LIGHT_2, the wrapper default.) */
inline MtrRenderLayer parse_render_layer(const char* name) {
    if (!name) throw std::invalid_argument("renderType: null");
    if (std::strcmp(name, "TEXT") == 0) return MTR_LAYER_TEXT;
    if (std::strcmp(name, "LIGHT") == 0 ||
        std::strcmp(name, "LIGHT_1") == 0 ||
        std::strcmp(name, "LIGHT_2") == 0 ||
        std::strcmp(name, "LIGHT_3") == 0) return MTR_LAYER_LIGHT;
    if (std::strcmp(name, "EXTERIOR") == 0) return MTR_LAYER_EXTERIOR;
    if (std::strcmp(name, "INTERIOR") == 0) return MTR_LAYER_INTERIOR;
    throw std::invalid_argument(std::string("No enum constant: ") + name);
}

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

    /* .naturalLight() — draw with the world light value instead of
       MAX_RENDER_LIGHT (mirrors TextWrapper.naturalLight(); carried in the
       record header flags so older hosts simply ignore it). */
    Text& natural_light()            { natural_light_ = true; return *this; }

    /* .renderType("LIGHT_1") — QueuedRenderLayer name (mirrors
       TextWrapper.renderType(String)). */
    Text& render_type(const char* layer) { rec_.layer = static_cast<int32_t>(parse_render_layer(layer)); return *this; }

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
    bool natural_light_ = false;
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

    /* .naturalLight() — see Text::natural_light(). */
    Texture& natural_light()         { natural_light_ = true; return *this; }
    Texture& render_type(const char* layer) { rec_.layer = static_cast<int32_t>(parse_render_layer(layer)); return *this; }

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
    bool natural_light_ = false;
};

/* ------------------------------------------------------------------ */
/* Rect (mirrors RectangleWrapper — exposed to JS as "Rect" since       */
/* upstream v2.3.0-prerelease.2; the class name is unchanged in Java)   */
/* ------------------------------------------------------------------ */

class Rect {
public:
    static Rect create(const char* /*comment*/ = nullptr) {
        return Rect();
    }

    /* Same defaults as RectangleWrapper: ARGB_WHITE tint on the shared
       white texture, 20x20 size, LIGHT_2 render type. */
    Rect& color(int32_t argb)        { color_ = argb; return *this; }
    Rect& natural_light()            { natural_light_ = true; return *this; }
    Rect& render_type(const char* layer) { layer_ = parse_render_layer(layer); return *this; }
    Rect& pos(double x, double y)    { x_ = x; y_ = y; return *this; }
    Rect& size(double w, double h)   { w_ = w; h_ = h; return *this; }
    Rect& z_order(int32_t z)         { z_ = z; return *this; }

    /* Rect draws a full-UV quad of mtr:white.png tinted with the color —
       exactly RectangleWrapper.drawTransformed(). Committed here (needs
       PidsContext complete, like Text/Texture::draw). */
    void draw(PidsContext& ctx);

private:
    Rect() = default;
    int32_t color_ = static_cast<int32_t>(0xFFFFFFFFu); /* ARGB_WHITE */
    MtrRenderLayer layer_ = MTR_LAYER_LIGHT;            /* LIGHT_2 */
    double x_ = 0, y_ = 0, w_ = 20, h_ = 20;            /* PIDSDrawCall(20, 20) */
    int32_t z_ = -1;
    bool natural_light_ = false;
};

} /* namespace mtr */
