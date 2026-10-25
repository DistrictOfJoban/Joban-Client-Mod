/**
 * pids.hpp — typed wrapper over JcmPidsSnapshot.
 *
 * Mirrors the JS objects exposed to PIDS scripts:
 *   com.lx862.jcm.mod.scripting.pids.PIDSWrapper
 *   + ArrivalsWrapper + ArrivalWrapper (see jsblock scripts).
 */
#pragma once

#include "mtr_native.h"
#include "frame.hpp"
#include "api.hpp"
#include "text.hpp"
#include "util.hpp"

namespace mtr {

class Arrival {
public:
    Arrival(const JcmPidsSnapshot* snap, const JcmArrival* arr)
        : snap_(snap), arr_(arr) {}

    StrView destination() const { return pool(arr_->destination_offset, arr_->destination_len); }
    StrView route_name() const { return pool(arr_->route_name_offset, arr_->route_name_len); }
    StrView route_number() const { return pool(arr_->route_number_offset, arr_->route_number_len); }
    StrView platform_name() const { return pool(arr_->platform_name_offset, arr_->platform_name_len); }

    int64_t arrival_time() const { return arr_->arrival_epoch_millis; }   /* millis epoch, offset-corrected */
    int64_t departure_time() const { return arr_->departure_epoch_millis; }
    int64_t deviation() const { return arr_->deviation_millis; }
    int64_t departure_index() const { return arr_->departure_index; }
    int64_t route_id() const { return arr_->route_id; }
    int64_t platform_id() const { return arr_->platform_id; }
    int32_t route_color() const { return arr_->route_color; }
    int32_t car_count() const { return arr_->car_count; }

    bool arrived() const { return arrival_time() <= now(); }
    bool departed() const { return departure_time() <= now(); }
    bool realtime() const { return arr_->realtime != 0; }
    bool terminating() const { return arr_->is_terminating != 0; }

    /* CircularState: 0 NONE, 1 CLOCKWISE, 2 ANTI_CLOCKWISE. */
    int32_t circular_state() const { return arr_->circular_state; }

private:
    int64_t now() const { return snap_->game_time_millis; }
    StrView pool(int32_t offset, int32_t len) const {
        const uint8_t* base = reinterpret_cast<const uint8_t*>(snap_);
        return make_view(reinterpret_cast<const char*>(base + offset), len);
    }
    const JcmPidsSnapshot* snap_;
    const JcmArrival* arr_;
};

class Arrivals {
public:
    Arrivals(const JcmPidsSnapshot* snap, int32_t count, int32_t offset)
        : snap_(snap), count_(count), offset_(offset) {}

    int32_t size() const { return count_; }

    /* pids.arrivals().get(i) — returns nullptr-equivalent via valid(). */
    Arrival at(int32_t i) const {
        if (i < 0 || i >= count_) throw std::out_of_range("Arrival index");
        const uint8_t* base = reinterpret_cast<const uint8_t*>(snap_);
        const JcmArrival* arr = reinterpret_cast<const JcmArrival*>(base + offset_);
        return Arrival(snap_, arr + i);
    }

    /* Arrival for a given platform (mirrors forEach(platformId, cb)).
       The cursor is caller-owned (and default-initialized to 0 by value
       callers); clamp it instead of trusting it — every sibling accessor
       here is bounds-checked, and a negative cursor would otherwise read
       before the pool. */
    bool next_for_platform(int64_t platform_id, Arrival& out, int32_t& cursor) const {
        if (cursor < 0) cursor = 0;
        const uint8_t* base = reinterpret_cast<const uint8_t*>(snap_);
        const JcmArrival* arr = reinterpret_cast<const JcmArrival*>(base + offset_);
        for (int32_t i = cursor; i < count_; i++) {
            if (arr[i].platform_id == platform_id) {
                out = Arrival(snap_, arr + i);
                cursor = i + 1;
                return true;
            }
        }
        return false;
    }

    /* arrivals().mixedCarLength() */
    bool mixed_car_length() const {
        if (count_ <= 0) return false;
        const uint8_t* base = reinterpret_cast<const uint8_t*>(snap_);
        const JcmArrival* arr = reinterpret_cast<const JcmArrival*>(base + offset_);
        const int32_t first = arr[0].car_count;
        for (int32_t i = 1; i < count_; i++) {
            if (arr[i].car_count != first) return true;
        }
        return false;
    }

private:
    const JcmPidsSnapshot* snap_;
    int32_t count_, offset_;
};

/**
 * Pids — mirrors PIDSWrapper (pids.height / pids.width / pids.arrivals()
 * / pids.getCustomMessage(i) / pids.isRowHidden(i) ...).
 */
class Pids {
public:
    explicit Pids(const JcmPidsSnapshot* snap) : snap_(snap) {}

    int32_t width() const { return snap_->width; }
    int32_t height() const { return snap_->height; }
    int32_t rows() const { return snap_->rows; }
    int64_t station_id() const { return snap_->station_id; }
    bool is_key_block() const { return snap_->key_block != 0; }
    bool is_platform_number_hidden() const { return snap_->platform_number_hidden != 0; }
    int64_t game_time_millis() const { return snap_->game_time_millis; }
    int64_t in_game_time() const { return snap_->in_game_time; }

    bool is_row_hidden(int32_t i) const {
        if (i < 0 || i >= 32) return false;
        return (snap_->row_hidden_bits >> i) & 1;
    }

    /* getCustomMessage(i): empty string when unset (JS returns null). */
    StrView get_custom_message(int32_t i) const {
        if (i < 0 || i >= snap_->custom_message_count) return StrView{};
        const uint8_t* base = reinterpret_cast<const uint8_t*>(snap_);
        const int32_t* refs = reinterpret_cast<const int32_t*>(base + snap_->custom_message_offset);
        /* refs: pairs of (offset, len) per message */
        return pool(refs[i * 2], refs[i * 2 + 1]);
    }

    Arrivals arrivals() const {
        return Arrivals(snap_, snap_->arrival_count, snap_->arrival_offset);
    }

    const JcmPidsSnapshot& raw() const { return *snap_; }

private:
    StrView pool(int32_t offset, int32_t len) const {
        if (len <= 0 || offset < 0) return StrView{};
        const uint8_t* base = reinterpret_cast<const uint8_t*>(snap_);
        return make_view(reinterpret_cast<const char*>(base + offset), len);
    }
    const JcmPidsSnapshot* snap_;
};

/**
 * PidsContext — mirrors PIDSScriptContext:
 *   ctx.draw(Text/Texture), ctx.setAutoZOrdering, ctx.setZOrderStep.
 */
class PidsContext {
public:
    PidsContext(FrameRecorder& frame, const JcmFrameInput& in)
        : frame_(&frame), input_(&in) {}

    void set_auto_z_ordering(bool on) {
        auto_z_ = on;
        if (input_->abi_version >= 8) Api(*input_).context().call("setAutoZOrdering", {on});
    }
    void set_z_order_step(double step) {
        z_step_ = step;
        if (input_->abi_version >= 8) Api(*input_).context().call("setZOrderStep", {step});
    }

    FrameRecorder& frame() { return *frame_; }
    const JcmHostServices* host() const { return input_->host; }
    const JcmFrameInput& input() const { return *input_; }

    int32_t alloc_z(JcmDrawText& rec, int32_t requested) {
        if (!auto_z_) return requested;
        const int32_t z = requested >= 0 ? requested : z_auto_++;
        rec.z_order = z;
        return z;
    }

    int32_t alloc_z(JcmDrawTexture& rec, int32_t requested) {
        if (!auto_z_) return requested;
        const int32_t z = requested >= 0 ? requested : z_auto_++;
        rec.z_order = z;
        return z;
    }

    void advance_auto_z() { z_auto_++; }

private:
    FrameRecorder* frame_;
    const JcmFrameInput* input_;
    bool auto_z_ = true;
    double z_step_ = 0.0002;
    int32_t z_auto_ = 0;
};

/* ---- Text::draw / Texture::draw (defined here: PidsContext complete) ---- */

inline void Text::draw(PidsContext& ctx) {
    FrameRecorder& f = ctx.frame();
    rec_.text_offset = f.intern(text_, text_len_);
    /* A Text built without .text() has a null pointer; strlen(nullptr)
       would crash the whole module out for a trivial omission. */
    if (!text_) { rec_.text_len = 0; } else {
        rec_.text_len = text_len_ < 0 ? static_cast<int32_t>(std::strlen(text_)) : text_len_;
    }
    if (font_) {
        rec_.font_id_offset = f.intern(font_, -1);
        rec_.font_id_len = static_cast<int32_t>(std::strlen(font_));
    }
    JcmDrawText& committed = f.push_text();
    const JcmRecordHeader hdr = committed.header; /* keep recorder header */
    committed = rec_;                             /* struct copy */
    committed.header = hdr;
}

inline void Texture::draw(PidsContext& ctx) {
    FrameRecorder& f = ctx.frame();
    JcmDrawTexture& committed = f.push_texture();
    const JcmRecordHeader hdr = committed.header;
    committed = rec_;
    committed.header = hdr;
    committed.texture_offset = f.intern(tex_, -1);
    committed.texture_len = static_cast<int32_t>(std::strlen(tex_));
}

} /* namespace mtr */
