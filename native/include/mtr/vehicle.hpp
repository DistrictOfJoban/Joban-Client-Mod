/**
 * vehicle.hpp — typed wrapper over JcmVehicleSnapshot.
 *
 * Mirrors the JS objects exposed to vehicle scripts:
 *   com.lx862.mtrscripting.mod.impl.mtr.vehicle.VehicleWrapper
 *   + NTETrainWrapper (legacy getters)
 * Every JS getter has a same-named C++ accessor, reading from the
 * flat snapshot instead of crossing into the JVM per call.
 */
#pragma once

#include "mtr_native.h"
#include <stdexcept>
#include "frame.hpp"
#include "matrices.hpp"
#include "util.hpp"

namespace mtr {

enum class TransportMode : int32_t { UNKNOWN=-1, TRAIN=0, BOAT=1, CABLE_CAR=2, AIRPLANE=3 };

/*
 * Transport type ids (ABI 9) — the same vocabulary the JS wrapper's
 * getTransportType() returns, so scripts can compare identically on both
 * sides. Base modes are the lowercase enum names; the MAGIC MTR Addon
 * contributes additional route types for TRAIN routes.
 */
namespace transport_type {
    constexpr const char* TRAIN = "train";
    constexpr const char* BOAT = "boat";
    constexpr const char* CABLE_CAR = "cable_car";
    constexpr const char* AIRPLANE = "airplane";
    constexpr const char* UNKNOWN = "unknown";
    /* MAGIC MTR Addon (org.justnoone.jme) route type overrides. */
    constexpr const char* TRAIN_NORMAL = "train_normal";
    constexpr const char* TRAIN_LIGHT_RAIL = "train_light_rail";
    constexpr const char* TRAIN_HIGH_SPEED = "train_high_speed";
    constexpr const char* TRAIN_METRO = "train_metro";
    constexpr const char* TRAIN_BUS = "train_bus";
    constexpr const char* TRAIN_TRAM = "train_tram";
    constexpr const char* TRAIN_SBAHN = "train_sbahn";
}

class Stop {
public:
    Stop(const JcmVehicleSnapshot* snap, const JcmStop* stop)
        : snap_(snap), stop_(stop) {}

    StrView name() const { return pool(stop_->name_offset, stop_->name_len); }
    StrView destination_name() const { return pool(stop_->destination_offset, stop_->destination_len); }

    /* customDestination: null when unset (mirrors @ValueNullable). */
    bool has_custom_destination() const { return stop_->custom_destination_offset >= 0; }
    StrView custom_destination() const {
        return pool(stop_->custom_destination_offset, stop_->custom_destination_len);
    }

    double distance() const { return stop_->distance; }
    double dwell_time_millis() const { return stop_->dwell_time_millis; }
    int64_t route_id() const { return stop_->route_id; }
    int64_t station_id() const { return stop_->station_id; }
    int64_t platform_id() const { return stop_->platform_id; }
    bool is_route_switchover() const { return stop_->is_route_switchover != 0; }
    /* v5: this stop's route CircularState (JS: stop.route.getCircularState()).
       0 NONE, 1 CLOCKWISE, 2 ANTICLOCKWISE. */
    uint8_t route_circular_state() const { return stop_->route_circular_state; }

    int32_t interchange_count() const { return stop_->interchange_count; }
    /* i-th interchange: { color, name } (mirrors Stop.RouteInterchange). */
    struct Interchange {
        int32_t color;
        StrView name;
        int32_t transport_mode;
        /* ABI 9: canonical transport type id; empty when unresolved. */
        StrView type;
        TransportMode getTransportMode() const { return static_cast<TransportMode>(transport_mode); }
        /*
         * Canonical type id: a base mode ("train", "boat", "cable_car",
         * "airplane") or a MAGIC MTR Addon route type ("train_metro",
         * "train_high_speed", ...). Same string as the JS side's
         * RouteInterchange.getTransportType(). Empty view when the host
         * could not resolve a type (older server data).
         */
        StrView getTransportType() const { return type; }
    };
    Interchange interchange(int32_t i) const {
        if (i < 0 || i >= stop_->interchange_count) throw std::out_of_range("Stop interchange index");
        /* interchanges are stored in the snapshot blob at interchange_offset. */
        const uint8_t* base = reinterpret_cast<const uint8_t*>(snap_);
        const JcmInterchange* ic = reinterpret_cast<const JcmInterchange*>(base + stop_->interchange_offset) + i;
        Interchange out;
        out.color = ic->color;
        out.transport_mode = ic->transport_mode;
        out.name = pool(ic->route_name_offset, ic->route_name_len);
        out.type = pool(ic->type_offset, ic->type_len);
        return out;
    }

    /* ---- v3: station exits (JS: station.getExits() -> Exit). ---- */
    int32_t exit_count() const { return stop_->exit_count; }
    struct Exit {
        StrView name;                 /* e.g. "A" */
        int32_t destination_count;
        /* j-th destination landmark string (JS: getDestinations().get(j)). */
        StrView destination(int32_t j) const {
            if (j < 0 || j >= destination_count) throw std::out_of_range("Exit destination index");
            const uint8_t* base = reinterpret_cast<const uint8_t*>(snap_);
            const JcmStrRef* refs = reinterpret_cast<const JcmStrRef*>(base + destination_offset);
            return make_view(reinterpret_cast<const char*>(base + refs[j].offset), refs[j].len);
        }
        /* internal: snapshot-relative wiring, set by Stop::exit() */
        const JcmVehicleSnapshot* snap_ = nullptr;
        int32_t destination_offset = 0;
    };
    Exit exit(int32_t i) const {
        if (i < 0 || i >= stop_->exit_count) throw std::out_of_range("Stop exit index");
        const uint8_t* base = reinterpret_cast<const uint8_t*>(snap_);
        const JcmExit* ex = reinterpret_cast<const JcmExit*>(base + stop_->exit_offset) + i;
        Exit out;
        out.name = pool(ex->name_offset, ex->name_len);
        out.destination_count = ex->destination_count;
        out.snap_ = snap_;
        out.destination_offset = ex->destination_offset;
        return out;
    }

private:
    StrView pool(int32_t offset, int32_t len) const {
        const uint8_t* base = reinterpret_cast<const uint8_t*>(snap_);
        return make_view(reinterpret_cast<const char*>(base + offset), len);
    }

    const JcmVehicleSnapshot* snap_;
    const JcmStop* stop_;
};

class StopList {
public:
    StopList(const JcmVehicleSnapshot* snap, int32_t count, int32_t offset)
        : snap_(snap), count_(count), offset_(offset) {}

    int32_t size() const { return count_; }
    bool empty() const { return count_ == 0; }

    Stop at(int32_t i) const {
        if (i < 0 || i >= count_) throw std::out_of_range("StopList index");
        const uint8_t* base = reinterpret_cast<const uint8_t*>(snap_);
        const JcmStop* arr = reinterpret_cast<const JcmStop*>(base + offset_);
        return Stop(snap_, arr + i);
    }

    Stop operator[](int32_t i) const { return at(i); }

    /* Mirrors VehicleWrapper.getNextStopIndex(stops, 0.5) — precomputed
       by the host into snapshot.next_stop_index for this-route stops;
       for other lists we recompute the same loop. */
    int32_t next_stop_index(double rail_progress, double overrun_tolerance = 0.5) const {
        int32_t idx = 0;
        for (int32_t i = 0; i < count_; i++) {
            if (at(i).distance() < 0) return count_; /* distance unavailable */
            if (rail_progress > at(i).distance() + overrun_tolerance) idx = i + 1;
            else break;
        }
        return idx;
    }

private:
    const JcmVehicleSnapshot* snap_;
    int32_t count_, offset_;
};

class Car {
public:
    Car(const JcmVehicleSnapshot* snap, const JcmCar* car)
        : snap_(snap), car_(car) {}

    float length() const { return car_->length; }
    float width() const { return car_->width; }
    bool left_door_open() const { return car_->left_door_open != 0; }
    bool right_door_open() const { return car_->right_door_open != 0; }
    bool rendered() const { return car_->rendered != 0; }
    StrView vehicle_type() const {
        const uint8_t* base = reinterpret_cast<const uint8_t*>(snap_);
        return make_view(reinterpret_cast<const char*>(base + car_->vehicle_type_offset),
                         car_->vehicle_type_len);
    }

private:
    const JcmVehicleSnapshot* snap_;
    const JcmCar* car_;
};

/**
 * Vehicle — mirrors VehicleWrapper + NTETrainWrapper as seen by JS.
 * All accessors are trivial reads into the snapshot (no JNI, no
 * reflection, no boxing — this is the whole performance story).
 *
 * Named Vehicle after MTR 4's scripting terminology (vehicleScripts /
 * VehicleWrapper). The MTR 3-era name `mtr::Train` is kept as an alias
 * below so modules written against older SDK revisions keep compiling.
 */
class Vehicle {
public:
    explicit Vehicle(const JcmVehicleSnapshot* snap) : snap_(snap) {}

    int64_t id() const { return snap_->vehicle_id; }
    int64_t siding_id() const { return snap_->siding_id; }
    int64_t this_route_id() const { return snap_->this_route_id; }
    int64_t departure_index() const { return snap_->departure_index; }
    int32_t car_count() const { return snap_->car_count; }

    double speed_kmh() const { return snap_->speed_kmh; }
    double speed_ms() const { return snap_->speed_ms; }
    double rail_progress() const { return snap_->rail_progress; }
    double rail_progress(int32_t car) const {
        double p = snap_->rail_progress;
        if (car_count() == 0) return p; /* car(-1) used to read off the blob */
        const Car c = this->car(car < 0 ? 0 : (car < car_count() ? car : car_count() - 1));
        return p - static_cast<double>(c.length()) * car;
    }
    double door_value() const { return snap_->door_value; }
    int32_t notch_level() const { return snap_->notch_level; }
    double notch_position() const { return static_cast<double>(snap_->notch_level) / 5.0; }

    bool reversed() const { return snap_->reversed != 0; }
    bool on_route() const { return snap_->on_route != 0; }
    bool door_opening() const { return snap_->door_opening != 0; }
    bool currently_manual() const { return snap_->currently_manual != 0; }
    bool manual_allowed() const { return snap_->manual_allowed != 0; }
    bool client_player_riding() const { return snap_->client_player_riding != 0; }
    bool rendered() const { return snap_->any_car_rendered != 0; }

    double total_dwell_time_millis() const { return snap_->total_dwell_time_millis; }
    double elapsed_dwell_time_millis() const { return snap_->elapsed_dwell_time_millis; }

    /* Timing (mirrors Date.now()/game tick helpers available to JS). */
    int64_t game_time_millis() const { return snap_->game_time_millis; }
    int64_t in_game_time() const { return snap_->in_game_time; }

    Car car(int32_t i) const {
        if (i < 0 || i >= snap_->car_count) throw std::out_of_range("Vehicle car index");
        const uint8_t* base = reinterpret_cast<const uint8_t*>(snap_);
        const JcmCar* arr = reinterpret_cast<const JcmCar*>(base + snap_->car_offset);
        return Car(snap_, arr + i);
    }

    StopList stops() const { return StopList(snap_, snap_->stop_count, snap_->stop_offset); }              /* getStops / getAllPlatforms */
    StopList this_route_stops() const { return StopList(snap_, snap_->this_route_stop_count, snap_->this_route_stop_offset); } /* getThisRouteStops */
    StopList next_route_stops() const { return StopList(snap_, snap_->next_route_stop_count, snap_->next_route_stop_offset); } /* getNextRouteStops */

    /* Precomputed next stop index for this route (host-side, mirrors
       vehicleWrapper.getNextStopIndex(...) in the JS hot path). */
    int32_t next_stop_index() const { return snap_->next_stop_index; }

    /* v2: current-route identity (JS: thisRouteStops.get(0).route.*). */
    StrView route_name() const {
        return pool(snap_->route_name_offset, snap_->route_name_len);
    }
    uint32_t route_color() const {
        return static_cast<uint32_t>(snap_->route_color);
    }
    /* 0 = NONE, 1 = CLOCKWISE, 2 = ANTICLOCKWISE (Route.CircularState). */
    uint8_t circular_state() const { return snap_->circular_state; }
    bool circular() const { return snap_->circular_state != 0; }

    /* v9: the vehicle's transport mode (JS: vehicle.getTransportMode()).
       Covers the four base modes — see the TransportMode enum. */
    TransportMode getTransportMode() const { return static_cast<TransportMode>(snap_->transport_mode); }

    /*
     * v9: the vehicle's canonical transport TYPE id (JS:
     * vehicle.getTransportType()). Returns the base mode as a lowercase id
     * ("train", "boat", "cable_car", "airplane") — or, when the MAGIC MTR
     * Addon assigns the vehicle's current route an extended type, that id
     * instead ("train_metro", "train_light_rail", "train_high_speed",
     * "train_bus", "train_tram", "train_sbahn", "train_normal"). Compare
     * with the mtr::transport_type constants; empty view when unresolved.
     */
    StrView getTransportType() const {
        return pool(snap_->vehicle_type_offset, snap_->vehicle_type_len);
    }

    /* v2: siding name (JS: vehicle.getSiding().getName()). */
    StrView siding_name() const {
        return pool(snap_->siding_name_offset, snap_->siding_name_len);
    }

    const JcmVehicleSnapshot& raw() const { return *snap_; }

private:
    StrView pool(int32_t offset, int32_t len) const {
        const uint8_t* base = reinterpret_cast<const uint8_t*>(snap_);
        return make_view(reinterpret_cast<const char*>(base + offset), len);
    }

    const JcmVehicleSnapshot* snap_;
};

/* MTR 3-era name for the vehicle snapshot wrapper — kept for source
   compatibility with modules written against older SDK revisions. */
using Train = Vehicle;

/**
 * VehicleContext — mirrors VehicleScriptContext as seen by JS.
 * Draw calls append into the frame recorder; the host replays them
 * per car / per bogie exactly like ScriptRenderManager.invoke().
 */
class VehicleContext {
public:
    VehicleContext(FrameRecorder& frame, const JcmFrameInput& in)
        : frame_(&frame), input_(&in) {}

    /* ctx.drawCarModel(model, carIndex, matrices) */
    void draw_car_model(int32_t model_handle, int32_t car, const Matrices* matrices) {
        JcmDrawModel& r = frame_->push_model();
        r.model_handle = model_handle;
        r.car = static_cast<uint8_t>(car);
        r.bogie = 0xFF;
        if (matrices) {
            r.pose_offset = matrices->compile();
            r.pose_count = 1;
        }
    }

    /* Bogie variant: ctx.getCarBogieRenderManager(car, bogie).drawModel(...) */
    void draw_bogie_model(int32_t model_handle, int32_t car, int32_t bogie,
                          const Matrices* matrices) {
        JcmDrawModel& r = frame_->push_model();
        r.model_handle = model_handle;
        r.car = static_cast<uint8_t>(car);
        r.bogie = static_cast<uint8_t>(bogie);
        if (matrices) {
            r.pose_offset = matrices->compile();
            r.pose_count = 1;
        }
    }

    /* ctx.playCarSound(sound, car, x, y, z, volume, pitch) */
    void play_car_sound(const char* sound, int32_t car, float x, float y, float z,
                        float volume, float pitch) {
        JcmDrawSound& r = frame_->push_sound(JCM_DRAW_SOUND);
        r.car = static_cast<uint8_t>(car);
        r.x = x; r.y = y; r.z = z;
        r.volume = volume; r.pitch = pitch;
        r.sound_offset = frame_->intern(sound, -1);
        r.sound_len = static_cast<int32_t>(std::strlen(sound));
    }

    /* ctx.playAnnSound(sound, volume, pitch) — only audible when riding */
    void play_ann_sound(const char* sound, float volume, float pitch) {
        JcmDrawSound& r = frame_->push_sound(JCM_DRAW_LOCAL_SOUND);
        r.car = 0;
        r.volume = volume; r.pitch = pitch;
        r.sound_offset = frame_->intern(sound, -1);
        r.sound_len = static_cast<int32_t>(std::strlen(sound));
    }

    /* Access to host services (model handles, host fonts, logging). */
    const JcmHostServices* host() const { return input_->host; }

    /* Full frame input (ABI envelope), needed by GraphicsTexture etc. */
    const JcmFrameInput& input() const { return *input_; }

    FrameRecorder& frame() { return *frame_; }

private:
    FrameRecorder* frame_;
    const JcmFrameInput* input_;
};

} /* namespace mtr */
