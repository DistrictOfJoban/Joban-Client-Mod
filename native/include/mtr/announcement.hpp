/**
 * announcement.hpp — typed wrapper over JcmAnnouncementSnapshot.
 *
 * ABI 16 script kind driven by the JCM block-script bindings: one instance
 * per bound announcement speaker block (FangSu's announcement_speaker).
 * The snapshot carries the speaker's route/platform bindings, lead
 * seconds, template identity and the arrival pool; the script owns the
 * scheduling logic (which announcement fired already) and plays audio
 * through the unified sound records:
 *
 *   ctx.play_sound("mtr:announcement_next_train", volume, pitch) —
 *   positional, every player in range hears it at the speaker;
 *   ctx.play_local_sound(id, volume, pitch) — riding players only.
 *
 *   struct MyState { std::unordered_set<int64_t> announced; };
 *   struct MyScript : mtr::AnnouncementScript<MyState> {
 *       static constexpr auto ID = "demo:speaker";
 *       void render(mtr::AnnouncementContext& ctx, MyState& s,
 *                   const mtr::Announcement& an) override {
 *           auto arrivals = an.arrivals();
 *           for (int i = 0; i < arrivals.size(); i++) {
 *               const double remain = (arrivals[i].arrival_time()
 *                                      - an.game_time_millis()) / 1000.0;
 *               if (remain > 0 && remain <= an.lead_seconds()
 *                   && s.announced.insert(arrivals[i].departure_key()).second) {
 *                   ctx.play_sound("demo:chime", 1.0f, 1.0f);
 *               }
 *           }
 *       }
 *   };
 *   MTR_REGISTER_ANNOUNCEMENT_SCRIPT(MyScript)
 */
#pragma once

#include "mtr_native.h"
#include "frame.hpp"
#include "api.hpp"
#include "util.hpp"

#include <cstring>
#include <cstdint>

namespace mtr {

class AnnouncementArrival {
public:
    AnnouncementArrival(const JcmAnnouncementSnapshot* snap, const JcmAnnounceArrival* arr)
        : snap_(snap), arr_(arr) {}

    /* Host-corrected clock. */
    int64_t arrival_time() const { return arr_->arrival_millis; }
    int64_t departure_time() const { return arr_->departure_millis; }
    int64_t route_id() const { return arr_->route_id; }
    int64_t platform_id() const { return arr_->platform_id; }
    int32_t car_count() const { return arr_->car_count; }

    bool realtime() const { return arr_->realtime != 0; }
    bool terminating() const { return arr_->terminating != 0; }

    /* ABI 16: FangSu service flags — an express arrival may warrant a
       different announcement variant. */
    bool isExpress() const { return (arr_->flags & MTR_ARRIVAL_FLAG_EXPRESS) != 0; }
    bool isHiddenRoute() const { return (arr_->flags & MTR_ARRIVAL_FLAG_HIDDEN_ROUTE) != 0; }

    StrView destination() const { return pool(arr_->destination_offset, arr_->destination_len); }
    StrView route_name() const { return pool(arr_->route_name_offset, arr_->route_name_len); }

    /* Seconds until arrival, negative when already arrived. */
    double seconds_until_arrival() const {
        return static_cast<double>(arr_->arrival_millis - snap_->game_time_millis) / 1000.0;
    }

    bool arrived() const { return arr_->arrival_millis <= snap_->game_time_millis; }

    /* A stable per-departure identity for "already announced" sets. */
    int64_t departure_key() const {
        /* route + platform + scheduled arrival millis — enough to keep
           the same train from re-announcing, resilient to deviation. */
        return arr_->route_id * 31 + arr_->platform_id * 7 + arr_->arrival_millis;
    }

private:
    StrView pool(int32_t offset, int32_t len) const {
        if (len <= 0 || offset < 0) return StrView{};
        const uint8_t* base = reinterpret_cast<const uint8_t*>(snap_);
        return make_view(reinterpret_cast<const char*>(base + offset), len);
    }
    const JcmAnnouncementSnapshot* snap_;
    const JcmAnnounceArrival* arr_;
};

class AnnouncementArrivalList {
public:
    AnnouncementArrivalList(const JcmAnnouncementSnapshot* snap, int32_t count, int32_t offset)
        : snap_(snap), count_(count), offset_(offset) {}

    int32_t size() const { return count_; }
    bool empty() const { return count_ <= 0; }

    AnnouncementArrival at(int32_t i) const {
        if (i < 0 || i >= count_) throw std::out_of_range("AnnouncementArrival index");
        const uint8_t* base = reinterpret_cast<const uint8_t*>(snap_);
        const JcmAnnounceArrival* arr = reinterpret_cast<const JcmAnnounceArrival*>(base + offset_);
        return AnnouncementArrival(snap_, arr + i);
    }

    AnnouncementArrival operator[](int32_t i) const { return at(i); }

private:
    const JcmAnnouncementSnapshot* snap_;
    int32_t count_, offset_;
};

class Announcement {
public:
    explicit Announcement(const JcmAnnouncementSnapshot* snap) : snap_(snap) {}

    int64_t game_time_millis() const { return snap_->game_time_millis; }
    int64_t in_game_time() const { return snap_->in_game_time; }
    int64_t pos_x() const { return snap_->block_pos[0]; }
    int64_t pos_y() const { return snap_->block_pos[1]; }
    int64_t pos_z() const { return snap_->block_pos[2]; }

    /* The configured lead time in seconds (announcements fire when an
       arrival's remaining time drops inside this window). */
    int32_t lead_seconds() const { return snap_->lead_seconds; }

    /* Whether non-full services (limited stops) are announced too. */
    bool all_stops() const { return snap_->all_stops != 0; }

    /* The speaker's template / variant identity ("" when unset) — lets a
       script switch announcement styles without its own config. */
    StrView template_id() const { return pool(snap_->template_offset, snap_->template_len); }
    StrView variant() const { return pool(snap_->variant_offset, snap_->variant_len); }

    /* The bound (route_id, platform_id) pairs. binding_count <= 32
       (FangSu's cap); a pair of (0, 0) means "all platforms in range" in
       FangSu's model — the host forwards bindings verbatim. */
    int32_t binding_count() const { return snap_->binding_count; }
    int64_t binding_route(int32_t i) const {
        if (i < 0 || i >= snap_->binding_count) throw std::out_of_range("binding index");
        const uint8_t* base = reinterpret_cast<const uint8_t*>(snap_);
        const int64_t* pairs = reinterpret_cast<const int64_t*>(base + snap_->binding_offset);
        return pairs[i * 2];
    }
    int64_t binding_platform(int32_t i) const {
        if (i < 0 || i >= snap_->binding_count) throw std::out_of_range("binding index");
        const uint8_t* base = reinterpret_cast<const uint8_t*>(snap_);
        const int64_t* pairs = reinterpret_cast<const int64_t*>(base + snap_->binding_offset);
        return pairs[i * 2 + 1];
    }

    /* The arrival pool for every bound platform (already deduplicated
       per departure and sorted by arrival time). */
    AnnouncementArrivalList arrivals() const {
        return AnnouncementArrivalList(snap_, snap_->arrival_count, snap_->arrival_offset);
    }

    /* ABI 15 semantics: 0 clear, 1 rain, 2 thunder. */
    Weather weather() const { return static_cast<Weather>(snap_->weather); }
    StrView getWeather() const { return weather_name(snap_->weather); }

    /* 1 = the bound block is a FangSu speaker. */
    bool fangsu_present() const { return snap_->fangsu_present != 0; }

    const JcmAnnouncementSnapshot& raw() const { return *snap_; }

private:
    StrView pool(int32_t offset, int32_t len) const {
        if (len <= 0 || offset < 0) return StrView{};
        const uint8_t* base = reinterpret_cast<const uint8_t*>(snap_);
        return make_view(reinterpret_cast<const char*>(base + offset), len);
    }
    const JcmAnnouncementSnapshot* snap_;
};

/**
 * AnnouncementContext — the unified audio scheduling API. Both records
 * replay through the same ScriptSoundManager the PIDS path uses, so
 * volume/pitch semantics and distance culling match the JS wrappers.
 */
class AnnouncementContext {
public:
    AnnouncementContext(FrameRecorder& frame, const JcmFrameInput& in)
        : frame_(&frame), input_(&in) {}

    /* Positional sound at the speaker block — every player in range
       hears it (JCM_DRAW_SOUND record). */
    void play_sound(const char* sound, float volume, float pitch) {
        JcmDrawSound& r = frame_->push_sound(JCM_DRAW_SOUND);
        r.x = r.y = r.z = 0;
        r.volume = volume;
        r.pitch = pitch;
        r.sound_offset = frame_->intern(sound, -1);
        r.sound_len = static_cast<int32_t>(std::strlen(sound));
    }

    /* Riding-player announcement (JCM_DRAW_LOCAL_SOUND record) — the
       on-board channel, only the players inside a train hear it. */
    void play_local_sound(const char* sound, float volume, float pitch) {
        JcmDrawSound& r = frame_->push_sound(JCM_DRAW_LOCAL_SOUND);
        r.car = 0;
        r.volume = volume;
        r.pitch = pitch;
        r.sound_offset = frame_->intern(sound, -1);
        r.sound_len = static_cast<int32_t>(std::strlen(sound));
    }

    const JcmHostServices* host() const { return input_->host; }
    const JcmFrameInput& input() const { return *input_; }
    FrameRecorder& frame() { return *frame_; }

private:
    FrameRecorder* frame_;
    const JcmFrameInput* input_;
};

} /* namespace mtr */
