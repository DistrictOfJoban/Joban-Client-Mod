/**
 * levelcrossing.hpp — typed wrapper over JcmLevelCrossingSnapshot.
 *
 * ABI 16 script kind driven by the JCM block-script bindings: one instance
 * per bound level crossing block (FangSu's level_crossing family). The
 * host derives the approaching-train pool from the client vehicle table
 * and the barrier/warning state from that pool; the script draws barrier
 * arms, lamps and signage through model draw records.
 *
 *   struct MyState { double barrier = 0; };
 *   struct MyScript : mtr::LevelCrossingScript<MyState> {
 *       static constexpr auto ID = "demo:crossing";
 *       void render(mtr::LevelCrossingContext& ctx, MyState& s,
 *                   const mtr::LevelCrossing& lc) override {
 *           if (lc.barrier_target()) s.barrier = ...;
 *       }
 *   };
 *   MTR_REGISTER_LEVEL_CROSSING_SCRIPT(MyScript)
 */
#pragma once

#include "mtr_native.h"
#include "frame.hpp"
#include "api.hpp"
#include "text.hpp"
#include "util.hpp"
#include "matrices.hpp"

namespace mtr {

/* Barrier / warning states, matching the header values. */
enum class CrossingWarning : uint8_t { IDLE = 0, WARNING = 1 };
enum class CrossingBarrier : uint8_t { OPEN = 0, CLOSED = 1 };

class CrossingApproach {
public:
    CrossingApproach(const JcmLevelCrossingSnapshot* snap, const JcmCrossingApproach* ap)
        : snap_(snap), ap_(ap) {}

    int64_t vehicle_id() const { return ap_->vehicle_id; }
    int64_t route_id() const { return ap_->route_id; }
    /* Host-corrected clock, 0 = unknown. */
    int64_t arrival_millis() const { return ap_->arrival_millis; }

    double distance_blocks() const { return ap_->distance_blocks; }
    double speed_kmh() const { return ap_->speed_kmh; }
    double seconds_to_arrival() const { return ap_->seconds_to_arrival; }
    int32_t car_count() const { return ap_->car_count; }

    StrView destination() const { return pool(ap_->destination_offset, ap_->destination_len); }

    bool arrived() const {
        return ap_->arrival_millis != 0 && ap_->arrival_millis <= snap_->game_time_millis;
    }

private:
    StrView pool(int32_t offset, int32_t len) const {
        if (len <= 0 || offset < 0) return StrView{};
        const uint8_t* base = reinterpret_cast<const uint8_t*>(snap_);
        return make_view(reinterpret_cast<const char*>(base + offset), len);
    }
    const JcmLevelCrossingSnapshot* snap_;
    const JcmCrossingApproach* ap_;
};

class CrossingApproachList {
public:
    CrossingApproachList(const JcmLevelCrossingSnapshot* snap, int32_t count, int32_t offset)
        : snap_(snap), count_(count), offset_(offset) {}

    int32_t size() const { return count_; }
    bool empty() const { return count_ <= 0; }

    CrossingApproach at(int32_t i) const {
        if (i < 0 || i >= count_) throw std::out_of_range("CrossingApproach index");
        const uint8_t* base = reinterpret_cast<const uint8_t*>(snap_);
        const JcmCrossingApproach* arr = reinterpret_cast<const JcmCrossingApproach*>(base + offset_);
        return CrossingApproach(snap_, arr + i);
    }

    CrossingApproach operator[](int32_t i) const { return at(i); }

private:
    const JcmLevelCrossingSnapshot* snap_;
    int32_t count_, offset_;
};

/**
 * LevelCrossing — the snapshot view. The host's barrier_target and
 * warning_state are advisory: the script owns the barrier animation
 * (its per-instance state), the host simply tells it the goal state.
 */
class LevelCrossing {
public:
    explicit LevelCrossing(const JcmLevelCrossingSnapshot* snap) : snap_(snap) {}

    int64_t game_time_millis() const { return snap_->game_time_millis; }
    int64_t in_game_time() const { return snap_->in_game_time; }
    /* The bound block's world position. */
    int64_t pos_x() const { return snap_->block_pos[0]; }
    int64_t pos_y() const { return snap_->block_pos[1]; }
    int64_t pos_z() const { return snap_->block_pos[2]; }

    /* Primary track yaw in degrees (atan2 style, -1 = unknown) — from
       FangSu's geometry snapshot when available. Barrier arms align to
       this. */
    double track_yaw_degrees() const { return snap_->track_yaw_degrees; }

    /* Host-derived warning state: 1 when at least one train approaches
       inside the warning radius. */
    CrossingWarning warningState() const { return static_cast<CrossingWarning>(snap_->warning_state); }
    bool isWarning() const { return snap_->warning_state != 0; }

    /* Host-derived barrier goal: 1 (lowered) while warning, 0 otherwise.
       The script animates its barrier model toward this target. */
    CrossingBarrier barrierTarget() const { return static_cast<CrossingBarrier>(snap_->barrier_target); }
    bool barrier_closed() const { return snap_->barrier_target != 0; }

    /* ABI 15 semantics: 0 clear, 1 rain, 2 thunder. */
    Weather weather() const { return static_cast<Weather>(snap_->weather); }
    StrView getWeather() const { return weather_name(snap_->weather); }

    /* 1 = the bound block is a FangSu level crossing. */
    bool fangsu_present() const { return snap_->fangsu_present != 0; }

    CrossingApproachList approaches() const {
        return CrossingApproachList(snap_, snap_->approach_count, snap_->approach_offset);
    }

    const JcmLevelCrossingSnapshot& raw() const { return *snap_; }

private:
    const JcmLevelCrossingSnapshot* snap_;
};

/**
 * LevelCrossingContext — model / sound / shape drawing for crossing
 * scripts. Mirrors EyeCandyContext's surface (the replay path is the
 * same eyecandy-style render manager), plus the styled model variant.
 */
class LevelCrossingContext {
public:
    LevelCrossingContext(FrameRecorder& frame, const JcmFrameInput& in)
        : frame_(&frame), input_(&in) {}

    void draw_model(int32_t model_handle, const Matrices* matrices) {
        draw_model(model_handle, matrices, 0, false, false);
    }

    /* ABI 16 styled variant — tint / emissive / translucent (barrier
       lamps: emissive; glass: translucent + alpha tint). */
    void draw_model(int32_t model_handle, const Matrices* matrices,
                    uint32_t tint_argb, bool emissive, bool translucent) {
        JcmDrawModel& r = frame_->push_model();
        r.model_handle = model_handle;
        r.car = 0;
        r.bogie = 0xFF;
        if (matrices) {
            r.pose_offset = matrices->compile();
            r.pose_count = 1;
        }
        r.tint = static_cast<int32_t>(tint_argb);
        if (emissive) r.header.flags |= MTR_RECORD_FLAG_EMISSIVE;
        if (translucent) r.header.flags |= MTR_RECORD_FLAG_TRANSLUCENT;
    }

    void play_sound(const char* sound, float volume, float pitch) {
        JcmDrawSound& r = frame_->push_sound(JCM_DRAW_SOUND);
        r.x = r.y = r.z = 0;
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
