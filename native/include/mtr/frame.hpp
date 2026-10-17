/**
 * frame.hpp — bump-arena frame recorder used by native scripts.
 *
 * Mirrors JCM's ScriptRenderManager "capture, then replay" model:
 * while the script renders, draw calls are appended to an arena-backed
 * frame. When mtrRender returns, the host reads the frame and replays
 * the records on the render thread.
 *
 * Records are variable-size structs (JcmDrawText, JcmDrawModel, ...),
 * each beginning with a JcmRecordHeader { kind, record_size }; they
 * are appended SEQUENTIALLY and the host walks them with
 * pos += header.record_size — compact and branch-predictable.
 */
#pragma once

#include "mtr_native.h"
#include <cstring>
#include <cstdlib>
#include <new>
#include <stdexcept>

namespace mtr {

class FrameRecorder {
public:
    FrameRecorder() = default;
    FrameRecorder(const FrameRecorder&) = delete;
    FrameRecorder& operator=(const FrameRecorder&) = delete;
    ~FrameRecorder() { if (pixel_owned_) std::free(pixel_arena_); }
    void begin_frame() {
        record_count_ = 0;
        records_bytes_ = 0;
        matrix_bytes_ = 0;
        string_bytes_ = 0;
        pixel_bytes_ = 0;
        float_bytes_ = 0;
        z_auto_ = 0;
    }

    /* ---- record emitters (bump-allocated, no malloc on hot path) ---- */

    JcmDrawText& push_text() {
        JcmDrawText& r = push_record<JcmDrawText>(JCM_DRAW_TEXT);
        r.shadow = r.bold = r.italic = 0;
        r.layer = MTR_LAYER_TEXT;
        r.color = 0xFFFFFFFF;
        r.align = MTR_TEXT_ALIGN_LEFT;
        r.overflow = MTR_TEXT_OVERFLOW_NONE;
        r.z_order = -1;
        r.x = r.y = r.w = r.h = 0;
        r.scale = 1;
        r.marquee_duration = -1;
        r.marquee_progress = -1;
        r.font_id_offset = r.font_id_len = 0;
        r.text_offset = r.text_len = 0;
        return r;
    }

    JcmDrawTexture& push_texture() {
        JcmDrawTexture& r = push_record<JcmDrawTexture>(JCM_DRAW_TEXTURE);
        r.layer = MTR_LAYER_LIGHT;
        r.color = 0xFFFFFFFF;
        r.x = r.y = r.w = r.h = 0;
        r.u1 = 0; r.v1 = 0; r.u2 = 1; r.v2 = 1;
        r.z_order = -1;
        r.texture_offset = r.texture_len = 0;
        return r;
    }

    JcmDrawModel& push_model() {
        JcmDrawModel& r = push_record<JcmDrawModel>(JCM_DRAW_MODEL);
        r.car = 0;
        r.bogie = 0xFF;
        r.model_handle = -1;
        r.pose_offset = -1;
        r.pose_count = 0;
        return r;
    }

    JcmDrawSound& push_sound(uint8_t kind) {
        JcmDrawSound& r = push_record<JcmDrawSound>(kind);
        r.car = 0;
        r.x = r.y = r.z = 0;
        r.volume = 1.0f;
        r.pitch = 1.0f;
        r.sound_offset = r.sound_len = 0;
        return r;
    }

    JcmDrawTextureUpload& push_texture_upload() {
        JcmDrawTextureUpload& r = push_record<JcmDrawTextureUpload>(JCM_DRAW_TEXTURE_UPLOAD);
        r.texture_handle = -1;
        r.width = r.height = 0;
        r.dirty_x = r.dirty_y = 0;
        r.dirty_w = r.dirty_h = 0;
        r.pixel_data_offset = 0;
        r.pixel_data_len = 0;
        return r;
    }

    JcmDrawShape& push_shape() {
        JcmDrawShape& r = push_record<JcmDrawShape>(JCM_DRAW_OUTLINE_SHAPE);
        r.is_collision = 0;
        r.box_count = 0;
        r.box_offset = 0;
        return r;
    }

    /* Bound individual pixel writes to reject invalid arena sizes. */
    static constexpr int64_t MAX_PIXEL_BYTES = 256LL * 1024 * 1024;

    /* ---- string arena (UTF-8) ---- */

    int32_t intern(const char* s, int32_t len) {
        if (!s) return string_bytes_;
        if (len < 0) len = static_cast<int32_t>(std::strlen(s));
        if (len <= 0 || !s) return string_bytes_;
        if (len > string_cap_ - string_bytes_) throw std::length_error("Native string arena capacity exceeded");
        int32_t off = string_bytes_;
        std::memcpy(string_arena_ + string_bytes_, s, static_cast<size_t>(len));
        string_bytes_ += len;
        return off;
    }

    /* ---- matrix arena: JcmMat4 stream ---- */

    int32_t push_matrix(const JcmMat4& m) {
        if (sizeof(m) > static_cast<size_t>(matrix_cap_ - matrix_bytes_)) throw std::length_error("Native matrix arena capacity exceeded");
        int32_t idx = matrix_bytes_ / static_cast<int32_t>(sizeof(JcmMat4));
        std::memcpy(reinterpret_cast<uint8_t*>(matrix_arena_) + matrix_bytes_, m.m, sizeof(m));
        matrix_bytes_ += static_cast<int32_t>(sizeof(m));
        return idx;
    }

    /* ---- float arena (voxel boxes etc.) ---- */

    int32_t push_floats(const float* v, int32_t count) {
        if (count <= 0 || !v) return float_bytes_;
        if (static_cast<int64_t>(count) * sizeof(float) > float_cap_ - float_bytes_) throw std::length_error("Native shape arena capacity exceeded");
        int32_t off = float_bytes_;
        std::memcpy(reinterpret_cast<uint8_t*>(float_arena_) + float_bytes_, v,
                    sizeof(float) * static_cast<size_t>(count));
        float_bytes_ += static_cast<int32_t>(sizeof(float) * count);
        return off;
    }

    /* ---- pixel arena (BGRA8) — grows on demand ----

       The host installs an initial static buffer (zero-cost BSS);
       frames that need more texture data transparently switch to a heap buffer that persists for
       the module lifetime. The steady-state hot path never allocates. */
    int64_t push_pixels(const uint8_t* px, int64_t len) {
        /* Hard invariant. A negative/absurd len here reaches memcpy as a
           size_t and faults inside the CRT far away from the real mistake
           (observed in the field as an ACCESS_VIOLATION in VCRUNTIME140
           with the caller frame gone). Clamp + log instead of copying, so
           one bad frame cannot take the game down. */
        if (len <= 0) return pixel_bytes_;
        if (len > MAX_PIXEL_BYTES || !px) return pixel_bytes_;
        if (pixel_bytes_ + len > pixel_cap_) {
            grow_pixels(pixel_bytes_ + len);
        }
        if (pixel_bytes_ + len > pixel_cap_) return pixel_bytes_;  /* grow failed */
        int64_t off = pixel_bytes_;
        std::memcpy(pixel_arena_ + pixel_bytes_, px, static_cast<size_t>(len));
        pixel_bytes_ += len;
        return off;
    }

    int64_t pixel_capacity() const { return pixel_cap_; }

    /* Auto z-ordering, mirrors PIDSScriptContext. */
    int32_t next_auto_z() { return z_auto_++; }

    /* ---- output ---- */

    void write_output(JcmFrameOutput& out) const {
        out.record_count = record_count_;
        out.records_len = records_bytes_;
        out.matrix_arena_len = matrix_bytes_;
        out.string_arena_len = string_bytes_;
        out.pixel_arena_len = pixel_bytes_;
        out.float_arena_len = float_bytes_;
        out.records = records_;
        out.matrix_arena = matrix_arena_;
        out.string_arena = string_arena_;
        out.pixel_arena = pixel_arena_;
        out.float_arena = float_arena_;
        /* ABI 9: capacities let the host cache one ByteBuffer per arena and
           rewind its limit per frame instead of allocating five wrapper
           objects per vehicle per frame. */
        out.records_capacity = record_cap_;
        out.matrix_arena_capacity = matrix_cap_;
        out.string_arena_capacity = string_cap_;
        out.pixel_arena_capacity = pixel_cap_;
        out.float_arena_capacity = float_cap_;
    }

    /* Host installs external buffers before first use (zero shared
       state, zero per-frame allocation). Capacities are sized to the
       common frame: 256 draw calls, 128 matrices, 32KB strings,
       16MB pixels (2048x2048 RGBA), 1KB floats; the pixel arena
       grows if a frame needs more. */
    void attach(void* records, float* matrix_arena, char* string_arena,
                uint8_t* pixel_arena, int64_t pixel_cap, float* float_arena,
                int32_t record_cap = sizeof(JcmDrawText) * 256,
                int32_t matrix_cap = 128 * sizeof(JcmMat4),
                int32_t string_cap = 32 * 1024, int32_t float_cap = 1024 * sizeof(float)) {
        records_ = records;
        matrix_arena_ = matrix_arena;
        string_arena_ = string_arena;
        pixel_arena_ = pixel_arena;
        pixel_cap_ = pixel_cap;
        pixel_owned_ = false;
        float_arena_ = float_arena;
        record_cap_ = record_cap; matrix_cap_ = matrix_cap;
        string_cap_ = string_cap; float_cap_ = float_cap;
    }

private:
    void grow_pixels(int64_t need) {
        int64_t new_cap = pixel_cap_ > 0 ? pixel_cap_ : (1LL << 20);
        while (new_cap < need) new_cap *= 2;
        uint8_t* grown = static_cast<uint8_t*>(std::malloc(static_cast<size_t>(new_cap)));
        if (!grown) return;   /* allocation failure: drop safely */
        if (pixel_bytes_ > 0 && pixel_arena_) {
            std::memcpy(grown, pixel_arena_, static_cast<size_t>(pixel_bytes_));
        }
        if (pixel_owned_) std::free(pixel_arena_);
        pixel_arena_ = grown;
        pixel_cap_ = new_cap;
        pixel_owned_ = true;
    }

    template <typename T>
    T& push_record(uint8_t kind) {
        constexpr int32_t padded = (sizeof(T) + 7) & ~7;
        if (padded > record_cap_ - records_bytes_) throw std::length_error("Native draw record capacity exceeded");
        T* slot = reinterpret_cast<T*>(reinterpret_cast<uint8_t*>(records_) + records_bytes_);
        std::memset(slot, 0, padded);
        slot->header.kind = kind;
        slot->header.record_size = padded;
        records_bytes_ += padded;
        record_count_++;
        return *slot;
    }

    void* records_ = nullptr;
    float* matrix_arena_ = nullptr;
    char* string_arena_ = nullptr;
    uint8_t* pixel_arena_ = nullptr;
    float* float_arena_ = nullptr;

    int64_t pixel_cap_ = 0;
    bool pixel_owned_ = false;
    int32_t record_cap_ = 0, matrix_cap_ = 0, string_cap_ = 0, float_cap_ = 0;

    int32_t record_count_ = 0;
    int32_t records_bytes_ = 0;
    int32_t matrix_bytes_ = 0;
    int32_t string_bytes_ = 0;
    int64_t pixel_bytes_ = 0;
    int32_t float_bytes_ = 0;
    int32_t z_auto_ = 0;
};

} /* namespace mtr */
