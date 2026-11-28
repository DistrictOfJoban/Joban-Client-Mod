/**
 * faremachine.hpp — typed wrapper over JcmFareMachineSnapshot.
 *
 * ABI 16 script kind driven by the JCM block-script bindings: one instance
 * per bound ticket barrier or ticket machine block (FangSu's
 * ticket_barrier / ticket_machine family). The snapshot carries the
 * barrier state (open flag + host-derived wing animation), the configured
 * fares, the client player's balance and the nearest platform's arrivals,
 * so the script can render dynamic small-screen content — fare tables,
 * balance prompts, congestion hints — through its own model draws.
 *
 *   struct MyState { ... };
 *   struct MyScript : mtr::FareMachineScript<MyState> {
 *       static constexpr auto ID = "demo:barrier_screen";
 *       void render(mtr::FareMachineContext& ctx, MyState& s,
 *                   const mtr::FareMachine& fm) override { ... }
 *   };
 *   MTR_REGISTER_FARE_MACHINE_SCRIPT(MyScript)
 */
#pragma once

#include "mtr_native.h"
#include "frame.hpp"
#include "api.hpp"
#include "text.hpp"
#include "util.hpp"
#include "matrices.hpp"

namespace mtr {

/* Fare modes, matching the header values. */
enum class FareType : int32_t { ZONE_BASED = 0, FLAT = 1 };
enum class FareMachineType : uint8_t { BARRIER = 0, TICKET_MACHINE = 1 };

/* One arrival at the nearest platform — the standard JcmArrival pool
   re-read against the fare machine snapshot's own base/time (a plain
   cast to the PIDS wrapper would misread game_time_millis, whose slot
   differs between the two headers). */
class FareArrival {
public:
    FareArrival(const JcmFareMachineSnapshot* snap, const JcmArrival* arr)
        : snap_(snap), arr_(arr) {}

    StrView destination() const { return pool(arr_->destination_offset, arr_->destination_len); }
    StrView route_name() const { return pool(arr_->route_name_offset, arr_->route_name_len); }
    StrView route_number() const { return pool(arr_->route_number_offset, arr_->route_number_len); }
    StrView platform_name() const { return pool(arr_->platform_name_offset, arr_->platform_name_len); }

    int64_t arrival_time() const { return arr_->arrival_epoch_millis; }
    int64_t departure_time() const { return arr_->departure_epoch_millis; }
    int64_t deviation() const { return arr_->deviation_millis; }
    int64_t route_id() const { return arr_->route_id; }
    int64_t platform_id() const { return arr_->platform_id; }
    int32_t route_color() const { return arr_->route_color; }
    int32_t car_count() const { return arr_->car_count; }

    bool realtime() const { return arr_->realtime != 0; }
    bool terminating() const { return arr_->is_terminating != 0; }
    bool arrived() const { return arrival_time() <= snap_->game_time_millis; }

    /* ABI 16: FangSu service flags. */
    bool isExpress() const { return (arr_->flags & MTR_ARRIVAL_FLAG_EXPRESS) != 0; }
    bool isHiddenRoute() const { return (arr_->flags & MTR_ARRIVAL_FLAG_HIDDEN_ROUTE) != 0; }

private:
    StrView pool(int32_t offset, int32_t len) const {
        if (len <= 0 || offset < 0) return StrView{};
        const uint8_t* base = reinterpret_cast<const uint8_t*>(snap_);
        return make_view(reinterpret_cast<const char*>(base + offset), len);
    }
    const JcmFareMachineSnapshot* snap_;
    const JcmArrival* arr_;
};

class FareArrivalList {
public:
    FareArrivalList(const JcmFareMachineSnapshot* snap, int32_t count, int32_t offset)
        : snap_(snap), count_(count), offset_(offset) {}

    int32_t size() const { return count_; }
    bool empty() const { return count_ <= 0; }

    FareArrival at(int32_t i) const {
        if (i < 0 || i >= count_) throw std::out_of_range("FareArrival index");
        const uint8_t* base = reinterpret_cast<const uint8_t*>(snap_);
        const JcmArrival* arr = reinterpret_cast<const JcmArrival*>(base + offset_);
        return FareArrival(snap_, arr + i);
    }

    FareArrival operator[](int32_t i) const { return at(i); }

private:
    const JcmFareMachineSnapshot* snap_;
    int32_t count_, offset_;
};

class FareMachine {
public:
    explicit FareMachine(const JcmFareMachineSnapshot* snap) : snap_(snap) {}

    int64_t game_time_millis() const { return snap_->game_time_millis; }
    int64_t in_game_time() const { return snap_->in_game_time; }
    int64_t pos_x() const { return snap_->block_pos[0]; }
    int64_t pos_y() const { return snap_->block_pos[1]; }
    int64_t pos_z() const { return snap_->block_pos[2]; }

    /* Barrier wing animation value: 0 (open) .. 1 (closed). The host
       derives it with FangSu's timing constants (200 ms open / 500+200 ms
       close) from the open flag's edges; 0 for ticket machines. */
    double door_value() const { return snap_->door_value; }

    /* The barrier's current open state (FangSu's isOpen extraConfig). */
    bool open() const { return snap_->open != 0; }
    /* 1 = exit side (zone-fare barriers). */
    bool exit_side() const { return snap_->exit_side != 0; }

    /* 0..5 MC Direction of the block. */
    uint8_t facing() const { return snap_->facing; }

    FareType fareType() const { return static_cast<FareType>(snap_->fare_type); }
    /* Flat-fare price (fareType() == FLAT); zone fares resolve through
       the snapshot's station name / the JS side's fare tables. */
    int32_t fare_value() const { return snap_->fare_value; }

    /* The client player's mtr_balance scoreboard value (-1 unknown) —
       the number a "balance hint" screen shows. */
    int32_t player_balance() const { return snap_->player_balance; }

    /* The station the machine belongs to ("" when unresolved). */
    StrView station_name() const { return pool(snap_->station_name_offset, snap_->station_name_len); }

    /* Arrivals at the nearest platform — congestion / next-train
       content. */
    FareArrivalList arrivals() const {
        return FareArrivalList(snap_, snap_->arrival_count, snap_->arrival_offset);
    }
    int32_t arrival_count() const { return snap_->arrival_count; }

    FareMachineType machineType() const { return static_cast<FareMachineType>(snap_->machine_type); }

    /* ABI 15 semantics: 0 clear, 1 rain, 2 thunder. */
    Weather weather() const { return static_cast<Weather>(snap_->weather); }
    StrView getWeather() const { return weather_name(snap_->weather); }

    /* 1 = the bound block is a FangSu machine. */
    bool fangsu_present() const { return snap_->fangsu_present != 0; }

    const JcmFareMachineSnapshot& raw() const { return *snap_; }

private:
    StrView pool(int32_t offset, int32_t len) const {
        if (len <= 0 || offset < 0) return StrView{};
        const uint8_t* base = reinterpret_cast<const uint8_t*>(snap_);
        return make_view(reinterpret_cast<const char*>(base + offset), len);
    }
    const JcmFareMachineSnapshot* snap_;
};

/**
 * FareMachineContext — model / sound drawing for fare machine scripts.
 * The replay path is the eyecandy-style render manager; the styled model
 * variant carries the ABI 16 tint / emissive / translucent bits (screen
 * glow: emissive; glass cover: translucent + alpha tint).
 */
class FareMachineContext {
public:
    FareMachineContext(FrameRecorder& frame, const JcmFrameInput& in)
        : frame_(&frame), input_(&in) {}

    void draw_model(int32_t model_handle, const Matrices* matrices) {
        draw_model(model_handle, matrices, 0, false, false);
    }

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
