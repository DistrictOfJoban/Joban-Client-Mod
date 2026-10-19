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
#include <algorithm>
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
    /* ---- shared record defaults ----
       ONE source of truth for the "unset field" values, used BOTH by the
       push_* emitters and by the fluent builders (Text/Texture in text.hpp):
       the builders used to carry a zero-initialised record whose struct copy
       clobbered these defaults, so an unset .color() drew fully transparent
       text, unset .scale() drew zero-size text, unset .uv() degenerated the
       quad and unset .align() centred instead of left-aligning. */

    static JcmDrawText default_text_record() {
        JcmDrawText r{};
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

    static JcmDrawTexture default_texture_record() {
        JcmDrawTexture r{};
        r.layer = MTR_LAYER_LIGHT;
        r.color = 0xFFFFFFFF;
        r.x = r.y = r.w = r.h = 0;
        r.u1 = 0; r.v1 = 0; r.u2 = 1; r.v2 = 1;
        r.z_order = -1;
        r.texture_offset = r.texture_len = 0;
        return r;
    }

    void begin_frame() {
        /* Deterministic reclaim first: a spike frame that grew the pixel
           arena must not pin native memory for the whole session (the old
           arena kept the session's LARGEST size forever — a Java-mode
           "keep it until GC" habit). Safe point: the previous frame's
           replay is complete (same discipline as grow_pixels). */
        shrink_pixels_if_idle();
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
        const JcmRecordHeader hdr = r.header; /* default_record zeroes it */
        r = default_text_record();
        r.header = hdr;
        return r;
    }

    JcmDrawTexture& push_texture() {
        JcmDrawTexture& r = push_record<JcmDrawTexture>(JCM_DRAW_TEXTURE);
        const JcmRecordHeader hdr = r.header;
        r = default_texture_record();
        r.header = hdr;
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
        if (static_cast<int64_t>(count) * static_cast<int64_t>(sizeof(float)) > float_cap_ - float_bytes_) throw std::length_error("Native shape arena capacity exceeded");
        int32_t off = float_bytes_;
        std::memcpy(reinterpret_cast<uint8_t*>(float_arena_) + float_bytes_, v,
                    sizeof(float) * static_cast<size_t>(count));
        float_bytes_ += static_cast<int32_t>(sizeof(float) * count);
        return off;
    }

    /* ---- pixel arena (BGRA8) — grows on demand, decays back when idle

       The host installs an initial static buffer (zero-cost BSS);
       frames that need more texture data transparently switch to a heap
       buffer. The steady-state hot path never allocates — and the heap
       arena is handed BACK to the allocator once the sustained demand no
       longer justifies its size (see shrink_pixels_if_idle), instead of
       sitting at the session's largest frame until the process exits. */
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
        /* Feed the decayed-usage tracker that drives the idle shrink
           (mutable bookkeeping on a const method, like a stats counter). */
        note_pixel_usage();
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

    /* Decay-driven idle shrink for the pixel arena. pixel_decay_ tracks a
       decaying peak of per-frame pixel usage (halved roughly every 34
       frames). The reclaim target is 2x that sustained demand, floored at
       the host-installed 1 MB arena; once the arena exceeds the target by
       more than 1.5x the excess is returned to the allocator at the start
       of the next frame. The 1.5x band is hysteresis: after a shrink the
       arena sits AT the target and cannot thrash per frame — the next
       possible shrink is a decay halving (~34 frames) away. Brief
       re-spikes just re-grow; a long quiet period collapses the arena
       back to the floor. */
    static constexpr double PIXEL_DECAY = 0.98;
    static constexpr int64_t PIXEL_SHRINK_FLOOR = 1LL << 20;
    static constexpr double PIXEL_SHRINK_MARGIN = 1.5;

    void shrink_pixels_if_idle() {
        if (!pixel_owned_ || pixel_cap_ <= PIXEL_SHRINK_FLOOR) return;
        const double sustained = std::max(pixel_decay_, 1.0);
        int64_t target = static_cast<int64_t>(2.0 * sustained);
        if (target < PIXEL_SHRINK_FLOOR) target = PIXEL_SHRINK_FLOOR;
        /* Idle (demand long gone): converge exactly to the floor. Otherwise
           only act beyond the hysteresis band so a steady workload never
           sees a shrink per frame. */
        const bool idle = pixel_decay_ < static_cast<double>(PIXEL_SHRINK_FLOOR) / 2.0;
        if (!idle && pixel_cap_ <= static_cast<int64_t>(PIXEL_SHRINK_MARGIN * static_cast<double>(target))) return;
        if (idle && pixel_cap_ <= target) return;
        uint8_t* shrunk = static_cast<uint8_t*>(std::malloc(static_cast<size_t>(target)));
        if (!shrunk) return;   /* allocation failure: keep the current arena */
        std::free(pixel_arena_);
        pixel_arena_ = shrunk;
        pixel_cap_ = target;
        /* Nothing to carry over: begin_frame resets pixel_bytes_ right
           after this call, so the smaller arena starts the frame empty. */
    }

    void note_pixel_usage() const {
        const double used = static_cast<double>(pixel_bytes_);
        pixel_decay_ = used > pixel_decay_ ? used : pixel_decay_ * PIXEL_DECAY;
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
    mutable double pixel_decay_ = 0.0;   /* decaying peak usage; drives idle shrink */
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
