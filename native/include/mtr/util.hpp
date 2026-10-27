/**
 * util.hpp — utility ports used by native scripts.
 *
 *  - CycleTracker   : mirrors com.lx862.mtrscripting.core.util.CycleTrackerJS
 *  - StateTracker   : mirrors com.lx862.mtrscripting.core.util.StateTrackerJS
 *                     (including the upstream v2.3.0-prerelease.2 change:
 *                     setState returns whether this is the first frame in
 *                     the new state)
 *  - PidsUtil       : mirrors jsblock:scripts/pids_util.js (JS object)
 *  - StrView helpers: zero-copy views over the snapshot string pool
 */
#pragma once

#include <cstdint>
#include <cstddef>
#include <cmath>
#include <string>
#include <vector>

namespace mtr {

/* Non-owning UTF-8 view into snapshot/frame string arenas. */
struct StrView {
    const char* data = nullptr;
    int32_t len = 0;

    bool valid() const { return data != nullptr && len > 0; }
    bool empty() const { return len <= 0; }

    bool operator==(const char* rhs) const {
        if (!rhs) return len == 0;
        for (int32_t i = 0; i < len; i++) {
            if (data[i] != rhs[i]) return false;
        }
        return rhs[len] == '\0';
    }
    bool operator!=(const char* rhs) const { return !(*this == rhs); }

    std::string str() const { return !data || len <= 0 ? std::string() : std::string(data, static_cast<size_t>(len)); }
};

inline StrView make_view(const char* data, int32_t len) {
    StrView v;
    v.data = data;
    v.len = len;
    return v;
}

/**
 * StateTracker — 1:1 port of StateTrackerJS. The JS class is constructed
 * with `new StateTracker()` and reads Timing.globalElapsed() itself; the
 * C++ port takes the clock explicitly (the snapshots' game_time_millis is
 * the natural equivalent) so it stays allocation- and host-free.
 *
 * JS (upstream v2.3.0-prerelease.2 semantics):
 *   const st = new StateTracker();
 *   if (st.setState("arriving")) { ... first frame in "arriving" ... }
 *   st.stateNow() / st.stateLast() / st.stateNowDuration()
 *   st.changedTo("arriving") / st.changedFromTo("idle", "arriving")
 */
template <typename T>
class StateTracker {
public:
    /* Returns true on the first frame in the new state (the upstream
       setState() boolean since v2.3.0-prerelease.2 — previously void). */
    bool set_state(const T& value, double now_millis) {
        if (!(value == current_)) {
            last_ = current_;
            current_ = value;
            current_since_ = now_millis;
            first_time_ = true;
        } else {
            first_time_ = false;
        }
        return first_time_;
    }

    const T& state_now() const { return current_; }
    const T& state_last() const { return last_; }
    double state_now_duration(double now_millis) const { return now_millis - current_since_; }
    bool state_now_first() const { return first_time_; }

    bool changed_to(const T& state) const {
        return first_time_ && current_ == state;
    }
    bool changed_from_to(const T& old_state, const T& new_state) const {
        return first_time_ && last_ == old_state && current_ == new_state;
    }

private:
    T last_{};
    T current_{};
    double current_since_ = 0.0;
    bool first_time_ = false;
};

/**
 * CycleTracker — mirrors CycleTrackerJS / TextUtil.cycleString:
 * a "A|B|C" string cycles its parts every `interval` game ticks.
 * Hot path does no allocation: view switches by offset math.
 */
class CycleTracker {
public:
    explicit CycleTracker(const char* packed, int32_t interval_ticks = 40)
        : packed_(packed), interval_(interval_ticks) {
        scan_parts();
    }

    /* Returns the currently active segment (view into `packed`). */
    StrView value(int64_t game_tick) const {
        if (part_count_ <= 1) {
            StrView v;
            v.data = packed_ ? packed_ + 0 : nullptr;
            v.len = packed_ ? static_cast<int32_t>(std::strlen(packed_)) : 0;
            return v;
        }
        const int32_t idx = static_cast<int32_t>(
            (game_tick / interval_) % part_count_);
        StrView v;
        v.data = packed_ + part_starts_[static_cast<size_t>(idx)];
        v.len = part_lens_[static_cast<size_t>(idx)];
        return v;
    }

    /* String-building variant for composing with extra text. */
    std::string value_str(int64_t game_tick) const {
        return value(game_tick).str();
    }

    int32_t interval() const { return interval_; }
    int32_t part_count() const { return part_count_; }

private:
    void scan_parts() {
        if (!packed_) { part_count_ = 0; return; }
        const size_t total = std::strlen(packed_);
        int32_t start = 0;
        for (size_t i = 0; i <= total; i++) {
            if (packed_[i] == '|' || packed_[i] == '\0') {
                part_starts_.push_back(start);
                part_lens_.push_back(static_cast<int32_t>(i) - start);
                start = static_cast<int32_t>(i) + 1;
            }
        }
        part_count_ = static_cast<int32_t>(part_starts_.size());
    }

    const char* packed_;
    int32_t interval_;
    std::vector<int32_t> part_starts_;
    std::vector<int32_t> part_lens_;
    int32_t part_count_ = 1;
};

/**
 * PidsUtil — direct port of pids_util.js so PIDS scripts port
 * 1:1 between JS and C++.
 */
namespace pids_util {

/* getETAText(arrival_millis) — e.g. "42 秒|42 sec" style. */
inline std::string eta_text(int64_t arrival_epoch_millis, int64_t now_millis,
                            const char* arrived_text = "") {
    const double will_arrive_in = static_cast<double>(arrival_epoch_millis - now_millis);
    const int64_t in_sec = static_cast<int64_t>(std::floor(will_arrive_in / 1000.0));
    char buf[64];
    if (in_sec <= 0) {
        return arrived_text ? std::string(arrived_text) : std::string();
    } else if (will_arrive_in <= 60000.0) {
        std::snprintf(buf, sizeof(buf), "%lld sec", static_cast<long long>(in_sec));
        return std::string(buf);
    } else {
        const int64_t mins = in_sec / 60;
        std::snprintf(buf, sizeof(buf), "%lld min%s",
                      static_cast<long long>(mins), mins > 1 ? "s" : "");
        return std::string(buf);
    }
}

/* getCarText(len) — "4-car" / "4 cars". */
inline std::string car_text(int32_t car_count) {
    char buf[32];
    if (car_count <= 1) {
        std::snprintf(buf, sizeof(buf), "%d-car", car_count);
    } else {
        std::snprintf(buf, sizeof(buf), "%d-cars", car_count);
    }
    return std::string(buf);
}

/* formatTime(inGameTime, padZero) — MC world time -> HH:mm. */
inline std::string format_time(int64_t in_game_time, bool pad_zero) {
    const int64_t time_now = in_game_time + 6000;
    const double hrs = std::fmod(static_cast<double>(time_now) / 1000.0, 24.0);
    const int64_t h = static_cast<int64_t>(hrs);
    const int64_t m = static_cast<int64_t>((hrs - static_cast<double>(h)) * 60.0) % 60;
    char buf[16];
    if (pad_zero) {
        std::snprintf(buf, sizeof(buf), "%02lld:%02lld", static_cast<long long>(h), static_cast<long long>(m));
    } else {
        std::snprintf(buf, sizeof(buf), "%lld:%02lld", static_cast<long long>(h), static_cast<long long>(m));
    }
    return std::string(buf);
}

} /* namespace pids_util */

} /* namespace mtr */
