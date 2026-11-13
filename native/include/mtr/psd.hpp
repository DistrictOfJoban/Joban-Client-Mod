/**
 * psd.hpp — typed wrapper over JcmPsdSnapshot.
 *
 * Mirrors the JS objects exposed to PSD (platform screen door) scripts:
 *   com.lx862.jcm.mod.scripting.psd.PSDWrapper
 *   + PSDArrivalsWrapper / PSDArrivalWrapper / PSDRouteWrapper /
 *     PSDStopWrapper / PSDDoorWrapper.
 *
 * The stops of a route reuse the vehicle JcmStop layout; this header keeps
 * its own PsdStop/PsdStopList (instead of reusing mtr::Stop) so the wrapper
 * is typed against the PSD snapshot blob.
 */
#pragma once

#include "mtr_native.h"
#include "frame.hpp"
#include "api.hpp"
#include "text.hpp"
#include "util.hpp"
#include <cstring>

namespace mtr {

/* ------------------------------------------------------------------ */
/* Stop (route station with interchanges / exits)                      */
/* ------------------------------------------------------------------ */

class PsdStop {
public:
    PsdStop(const void* blob, const JcmStop* stop)
        : blob_(blob), stop_(stop) {}

    StrView name() const { return pool(stop_->name_offset, stop_->name_len); }
    StrView destination_name() const { return pool(stop_->destination_offset, stop_->destination_len); }

    /* customDestination: unset when custom_destination_offset < 0. */
    bool has_custom_destination() const { return stop_->custom_destination_offset >= 0; }
    StrView custom_destination() const {
        return pool(stop_->custom_destination_offset, stop_->custom_destination_len);
    }

    double dwell_time_millis() const { return stop_->dwell_time_millis; }
    int64_t route_id() const { return stop_->route_id; }
    int64_t station_id() const { return stop_->station_id; }
    int64_t platform_id() const { return stop_->platform_id; }

    /* 0 NONE, 1 CLOCKWISE, 2 ANTICLOCKWISE. */
    uint8_t route_circular_state() const { return stop_->route_circular_state; }

    int32_t interchange_count() const { return stop_->interchange_count; }
    struct Interchange {
        int32_t color;
        StrView name;
        int32_t transport_mode;   /* -1 when older server data omits it */
        StrView type;             /* canonical transport type id, "" = unresolved */
        TransportMode getTransportMode() const { return static_cast<TransportMode>(transport_mode); }
        StrView getTransportType() const { return type; }
    };
    Interchange interchange(int32_t i) const {
        if (i < 0 || i >= stop_->interchange_count) throw std::out_of_range("PsdStop interchange index");
        const uint8_t* base = reinterpret_cast<const uint8_t*>(blob_);
        const JcmInterchange* ic = reinterpret_cast<const JcmInterchange*>(base + stop_->interchange_offset) + i;
        Interchange out;
        out.color = ic->color;
        out.name = pool(ic->route_name_offset, ic->route_name_len);
        out.transport_mode = ic->transport_mode;
        out.type = pool(ic->type_offset, ic->type_len);
        return out;
    }

    int32_t exit_count() const { return stop_->exit_count; }
    struct Exit {
        StrView name;
        int32_t destination_count;
        StrView destination(int32_t i) const;  /* defined below (needs blob) */
        int32_t destination_count_value() const { return destination_count; }
    private:
        friend class PsdStop;
        Exit(StrView name, int32_t count, const void* blob, int32_t offset)
            : name(name), destination_count(count), blob_(blob), offset_(offset) {}
        const void* blob_;
        int32_t offset_;
    };
    Exit exit(int32_t i) const {
        if (i < 0 || i >= stop_->exit_count) throw std::out_of_range("PsdStop exit index");
        const uint8_t* base = reinterpret_cast<const uint8_t*>(blob_);
        const JcmExit* ex = reinterpret_cast<const JcmExit*>(base + stop_->exit_offset) + i;
        return Exit(pool(ex->name_offset, ex->name_len), ex->destination_count, blob_, ex->destination_offset);
    }

private:
    StrView pool(int32_t offset, int32_t len) const {
        if (len <= 0 || offset < 0) return StrView{};
        const uint8_t* base = reinterpret_cast<const uint8_t*>(blob_);
        return make_view(reinterpret_cast<const char*>(base + offset), len);
    }
    const void* blob_;
    const JcmStop* stop_;
};

inline StrView PsdStop::Exit::destination(int32_t i) const {
    if (i < 0 || i >= destination_count) throw std::out_of_range("Exit destination index");
    const uint8_t* base = reinterpret_cast<const uint8_t*>(blob_);
    const JcmStrRef* ref = reinterpret_cast<const JcmStrRef*>(base + offset_) + i;
    if (ref->len <= 0 || ref->offset < 0) return StrView{};
    return make_view(reinterpret_cast<const char*>(base + ref->offset), ref->len);
}

class PsdStopList {
public:
    PsdStopList(const void* blob, int32_t count, int32_t offset)
        : blob_(blob), count_(count), offset_(offset) {}

    int32_t size() const { return count_; }
    bool empty() const { return count_ == 0; }

    PsdStop at(int32_t i) const {
        if (i < 0 || i >= count_) throw std::out_of_range("PsdStopList index");
        const uint8_t* base = reinterpret_cast<const uint8_t*>(blob_);
        const JcmStop* arr = reinterpret_cast<const JcmStop*>(base + offset_);
        return PsdStop(blob_, arr + i);
    }

    PsdStop operator[](int32_t i) const { return at(i); }

private:
    const void* blob_;
    int32_t count_, offset_;
};

/* ------------------------------------------------------------------ */
/* Route                                                               */
/* ------------------------------------------------------------------ */

class PsdRoute {
public:
    PsdRoute(const void* blob, const JcmPsdRoute* route)
        : blob_(blob), route_(route) {}

    int64_t id() const { return route_->route_id; }
    int32_t color() const { return route_->route_color; }
    /* 0 NONE, 1 CLOCKWISE, 2 ANTICLOCKWISE. */
    uint8_t circular_state() const { return route_->circular_state; }

    StrView name() const { return pool(route_->name_offset, route_->name_len); }
    StrView route_number() const { return pool(route_->number_offset, route_->number_len); }
    /* Canonical transport type id ("train", "train_metro", ...). */
    StrView getTransportType() const { return pool(route_->type_offset, route_->type_len); }

    /* The route's stops in order. Empty unless the entry declared
       "dataFetchMode": "ALL" (or MANDATORY). */
    PsdStopList stops() const { return PsdStopList(blob_, route_->stop_count, route_->stop_offset); }

private:
    StrView pool(int32_t offset, int32_t len) const {
        if (len <= 0 || offset < 0) return StrView{};
        const uint8_t* base = reinterpret_cast<const uint8_t*>(blob_);
        return make_view(reinterpret_cast<const char*>(base + offset), len);
    }
    const void* blob_;
    const JcmPsdRoute* route_;
};

class PsdRouteList {
public:
    PsdRouteList(const void* blob, int32_t count, int32_t offset)
        : blob_(blob), count_(count), offset_(offset) {}

    int32_t size() const { return count_; }
    bool empty() const { return count_ == 0; }

    PsdRoute at(int32_t i) const {
        if (i < 0 || i >= count_) throw std::out_of_range("PsdRouteList index");
        const uint8_t* base = reinterpret_cast<const uint8_t*>(blob_);
        const JcmPsdRoute* arr = reinterpret_cast<const JcmPsdRoute*>(base + offset_);
        return PsdRoute(blob_, arr + i);
    }

    PsdRoute operator[](int32_t i) const { return at(i); }

private:
    const void* blob_;
    int32_t count_, offset_;
};

/* ------------------------------------------------------------------ */
/* Arrival (JcmArrival + the door units that will open)                */
/* ------------------------------------------------------------------ */

class PsdArrival {
public:
    PsdArrival(const void* blob, const JcmPsdArrival* arr)
        : blob_(blob), arr_(arr) {}

    StrView destination() const { return pool(arr_->arrival.destination_offset, arr_->arrival.destination_len); }
    StrView route_name() const { return pool(arr_->arrival.route_name_offset, arr_->arrival.route_name_len); }
    StrView route_number() const { return pool(arr_->arrival.route_number_offset, arr_->arrival.route_number_len); }
    StrView platform_name() const { return pool(arr_->arrival.platform_name_offset, arr_->arrival.platform_name_len); }

    int64_t arrival_time() const { return arr_->arrival.arrival_epoch_millis; }
    int64_t departure_time() const { return arr_->arrival.departure_epoch_millis; }
    int64_t deviation() const { return arr_->arrival.deviation_millis; }
    int64_t departure_index() const { return arr_->arrival.departure_index; }
    int64_t route_id() const { return arr_->arrival.route_id; }
    int64_t platform_id() const { return arr_->arrival.platform_id; }
    int32_t route_color() const { return arr_->arrival.route_color; }
    int32_t car_count() const { return arr_->arrival.car_count; }

    bool realtime() const { return arr_->arrival.realtime != 0; }
    bool terminating() const { return arr_->arrival.is_terminating != 0; }
    /* 0 NONE, 1 CLOCKWISE, 2 ANTICLOCKWISE. */
    uint8_t circular_state() const { return arr_->arrival.circular_state; }

    /* The door unit numbers (1-based, see Psd::doors()) that will open for
       this train — the actual open units once the train dwells, an
       approximation (train centered on the platform midpoint) before. */
    int32_t door_count() const { return arr_->door_count; }
    int32_t door_number(int32_t i) const {
        if (i < 0 || i >= arr_->door_count) throw std::out_of_range("PsdArrival door index");
        const uint8_t* base = reinterpret_cast<const uint8_t*>(blob_);
        const int32_t* numbers = reinterpret_cast<const int32_t*>(base + arr_->door_offset);
        return numbers[i];
    }

private:
    StrView pool(int32_t offset, int32_t len) const {
        if (len <= 0 || offset < 0) return StrView{};
        const uint8_t* base = reinterpret_cast<const uint8_t*>(blob_);
        return make_view(reinterpret_cast<const char*>(base + offset), len);
    }
    const void* blob_;
    const JcmPsdArrival* arr_;
};

class PsdArrivalList {
public:
    PsdArrivalList(const void* blob, int32_t count, int32_t offset)
        : blob_(blob), count_(count), offset_(offset) {}

    int32_t size() const { return count_; }
    bool empty() const { return count_ == 0; }

    PsdArrival at(int32_t i) const {
        if (i < 0 || i >= count_) throw std::out_of_range("PsdArrivalList index");
        const uint8_t* base = reinterpret_cast<const uint8_t*>(blob_);
        const JcmPsdArrival* arr = reinterpret_cast<const JcmPsdArrival*>(base + offset_);
        return PsdArrival(blob_, arr + i);
    }

    PsdArrival operator[](int32_t i) const { return at(i); }

    bool mixed_car_length() const {
        if (count_ <= 0) return false;
        const uint8_t* base = reinterpret_cast<const uint8_t*>(blob_);
        const JcmPsdArrival* arr = reinterpret_cast<const JcmPsdArrival*>(base + offset_);
        const int32_t first = arr[0].arrival.car_count;
        for (int32_t i = 1; i < count_; i++) {
            if (arr[i].arrival.car_count != first) return true;
        }
        return false;
    }

private:
    const void* blob_;
    int32_t count_, offset_;
};

/* ------------------------------------------------------------------ */
/* Door unit                                                           */
/* ------------------------------------------------------------------ */

class PsdDoor {
public:
    PsdDoor(const void* blob, const JcmPsdDoor* door)
        : blob_(blob), door_(door) {}

    /** 1-based number along the wall run. */
    int32_t number() const { return door_->number; }
    bool is_open() const { return door_->open != 0; }
    bool is_locked() const { return door_->locked != 0; }

    int64_t x() const { return door_->pos[0]; }
    int64_t y() const { return door_->pos[1]; }
    int64_t z() const { return door_->pos[2]; }

private:
    const void* blob_;
    const JcmPsdDoor* door_;
};

class PsdDoorList {
public:
    PsdDoorList(const void* blob, int32_t count, int32_t offset)
        : blob_(blob), count_(count), offset_(offset) {}

    int32_t size() const { return count_; }
    bool empty() const { return count_ == 0; }

    PsdDoor at(int32_t i) const {
        if (i < 0 || i >= count_) throw std::out_of_range("PsdDoorList index");
        const uint8_t* base = reinterpret_cast<const uint8_t*>(blob_);
        const JcmPsdDoor* arr = reinterpret_cast<const JcmPsdDoor*>(base + offset_);
        return PsdDoor(blob_, arr + i);
    }

    PsdDoor operator[](int32_t i) const { return at(i); }

private:
    const void* blob_;
    int32_t count_, offset_;
};

/* ------------------------------------------------------------------ */
/* Station (ABI 13) — the complete info of the PSD's station           */
/* ------------------------------------------------------------------ */

namespace detail {

/* The bridge validates the advertised pool end against the actual buffer
   length before invoking a PSD module. Standalone SDK callers with partial
   data can supply the real byte length to Psd(snapshot, bytes). The legacy
   single-pointer constructor requires at least a readable snapshot header. */
class PsdSnapshotView {
public:
    PsdSnapshotView(const void* blob = nullptr, size_t bytes = static_cast<size_t>(-1))
        : blob_(static_cast<const uint8_t*>(blob)), bytes_(0) {
        if (!blob_ || bytes < sizeof(JcmPsdSnapshot)
                || reinterpret_cast<uintptr_t>(blob_) % alignof(JcmPsdSnapshot) != 0) return;
        JcmPsdSnapshot header;
        std::memcpy(&header, blob_, sizeof(header));
        if (header.string_pool_offset < static_cast<int32_t>(sizeof(header))
                || header.string_pool_len < 0) return;
        const uint64_t end = static_cast<uint64_t>(header.string_pool_offset)
                + static_cast<uint64_t>(header.string_pool_len);
        if (end <= bytes) bytes_ = static_cast<size_t>(end);
    }

    bool valid() const { return bytes_ >= sizeof(JcmPsdSnapshot); }
    const void* blob() const { return blob_; }
    size_t bytes() const { return bytes_; }

    template <typename T>
    const T* at(int32_t offset, int32_t count = 1) const {
        if (offset < static_cast<int32_t>(sizeof(JcmPsdSnapshot)) || count <= 0
                || static_cast<size_t>(offset) > bytes_
                || static_cast<size_t>(count) > (bytes_ - static_cast<size_t>(offset)) / sizeof(T)) return nullptr;
        const auto* ptr = blob_ + offset;
        if (reinterpret_cast<uintptr_t>(ptr) % alignof(T) != 0) return nullptr;
        return reinterpret_cast<const T*>(ptr);
    }

    template <typename T>
    const T* checked(const T* ptr) const {
        const uintptr_t base = reinterpret_cast<uintptr_t>(blob_);
        const uintptr_t address = reinterpret_cast<uintptr_t>(ptr);
        if (!ptr || address < base || address - base > INT32_MAX) return nullptr;
        return at<T>(static_cast<int32_t>(address - base));
    }

    StrView string(int32_t offset, int32_t len) const {
        const char* ptr = at<char>(offset, len);
        return ptr ? make_view(ptr, len) : StrView{};
    }

private:
    const uint8_t* blob_;
    size_t bytes_;
};

} /* namespace detail */

/** Station accessors return safe defaults for absent or invalid ranges. */
class PsdStationInterchange {
public:
    PsdStationInterchange() : ic_(nullptr) {}
    PsdStationInterchange(const void* blob, const JcmPsdStationInterchange* ic,
                          size_t bytes = static_cast<size_t>(-1))
        : view_(blob, bytes), ic_(view_.checked(ic)) {}

    int64_t id() const { return ic_ ? ic_->route_id : 0; }
    int32_t color() const { return ic_ ? ic_->color : 0; }
    StrView name() const { return ic_ ? view_.string(ic_->name_offset, ic_->name_len) : StrView{}; }
    int32_t transport_mode() const { return ic_ ? ic_->transport_mode : -1; }
    StrView get_transport_type() const { return ic_ ? view_.string(ic_->type_offset, ic_->type_len) : StrView{}; }

private:
    detail::PsdSnapshotView view_;
    const JcmPsdStationInterchange* ic_;
};

/** Routes of one walkable connecting station, grouped under its name. */
class PsdStationConnectingGroup {
public:
    PsdStationConnectingGroup() : group_(nullptr) {}
    PsdStationConnectingGroup(const void* blob, const JcmPsdStationConnectingGroup* group,
                              size_t bytes = static_cast<size_t>(-1))
        : view_(blob, bytes), group_(view_.checked(group)) {}

    StrView name() const { return group_ ? view_.string(group_->station_name_offset, group_->station_name_len) : StrView{}; }
    int32_t route_count() const {
        return group_ && view_.at<JcmPsdStationInterchange>(group_->route_offset, group_->route_count)
                ? group_->route_count : 0;
    }
    PsdStationInterchange route(int32_t i) const {
        if (i < 0 || i >= route_count()) return PsdStationInterchange{};
        return PsdStationInterchange(view_.blob(), view_.at<JcmPsdStationInterchange>(group_->route_offset) + i, view_.bytes());
    }

private:
    detail::PsdSnapshotView view_;
    const JcmPsdStationConnectingGroup* group_;
};

/** A station exit and its destination landmarks. */
class PsdStationExit {
public:
    PsdStationExit() : exit_(nullptr) {}
    PsdStationExit(const void* blob, const JcmExit* exit, size_t bytes = static_cast<size_t>(-1))
        : view_(blob, bytes), exit_(view_.checked(exit)) {}

    StrView name() const { return exit_ ? view_.string(exit_->name_offset, exit_->name_len) : StrView{}; }
    int32_t destination_count() const {
        return exit_ && view_.at<JcmStrRef>(exit_->destination_offset, exit_->destination_count)
                ? exit_->destination_count : 0;
    }
    StrView destination(int32_t i) const {
        if (i < 0 || i >= destination_count()) return StrView{};
        const auto* ref = view_.at<JcmStrRef>(exit_->destination_offset) + i;
        return view_.string(ref->offset, ref->len);
    }

private:
    detail::PsdSnapshotView view_;
    const JcmExit* exit_;
};

/** The station this PSD's platform belongs to (JS: psd.stop()). */
class PsdStation {
public:
    PsdStation() : station_(nullptr) {}
    PsdStation(const void* blob, const JcmPsdStation* station, size_t bytes = static_cast<size_t>(-1))
        : view_(blob, bytes), station_(view_.checked(station)) {}

    bool ok() const { return station_ != nullptr; }
    int64_t id() const { return station_ ? station_->station_id : 0; }
    StrView name() const { return station_ ? view_.string(station_->name_offset, station_->name_len) : StrView{}; }
    int32_t color() const { return station_ ? station_->color : 0; }
    StrView color_hex() const { return station_ ? view_.string(station_->color_hex_offset, station_->color_hex_len) : StrView{}; }
    int64_t zone1() const { return station_ ? station_->zone1 : 0; }
    int64_t zone2() const { return station_ ? station_->zone2 : 0; }
    int64_t zone3() const { return station_ ? station_->zone3 : 0; }

    int32_t interchange_count() const {
        return station_ && view_.at<JcmPsdStationInterchange>(station_->interchange_offset, station_->interchange_count)
                ? station_->interchange_count : 0;
    }
    PsdStationInterchange interchange(int32_t i) const {
        if (i < 0 || i >= interchange_count()) return PsdStationInterchange{};
        return PsdStationInterchange(view_.blob(), view_.at<JcmPsdStationInterchange>(station_->interchange_offset) + i, view_.bytes());
    }
    int32_t connecting_count() const {
        return station_ && view_.at<JcmPsdStationConnectingGroup>(station_->connecting_offset, station_->connecting_count)
                ? station_->connecting_count : 0;
    }
    PsdStationConnectingGroup connecting(int32_t i) const {
        if (i < 0 || i >= connecting_count()) return PsdStationConnectingGroup{};
        return PsdStationConnectingGroup(view_.blob(), view_.at<JcmPsdStationConnectingGroup>(station_->connecting_offset) + i, view_.bytes());
    }
    int32_t connected_station_count() const {
        return station_ && view_.at<JcmStrRef>(station_->connected_station_offset, station_->connected_station_count)
                ? station_->connected_station_count : 0;
    }
    StrView connected_station(int32_t i) const {
        if (i < 0 || i >= connected_station_count()) return StrView{};
        const auto* ref = view_.at<JcmStrRef>(station_->connected_station_offset) + i;
        return view_.string(ref->offset, ref->len);
    }
    int32_t exit_count() const {
        return station_ && view_.at<JcmExit>(station_->exit_offset, station_->exit_count)
                ? station_->exit_count : 0;
    }
    PsdStationExit exit(int32_t i) const {
        if (i < 0 || i >= exit_count()) return PsdStationExit{};
        return PsdStationExit(view_.blob(), view_.at<JcmExit>(station_->exit_offset) + i, view_.bytes());
    }
    int32_t platform_count() const {
        return station_ && view_.at<JcmStrRef>(station_->platform_offset, station_->platform_count)
                ? station_->platform_count : 0;
    }
    StrView platform(int32_t i) const {
        if (i < 0 || i >= platform_count()) return StrView{};
        const auto* ref = view_.at<JcmStrRef>(station_->platform_offset) + i;
        return view_.string(ref->offset, ref->len);
    }

private:
    detail::PsdSnapshotView view_;
    const JcmPsdStation* station_;
};

/* ------------------------------------------------------------------ */
/* Psd — the top-level wrapper                                         */
/* ------------------------------------------------------------------ */

/**
 * Psd — mirrors PSDWrapper (psd.width / psd.height / psd.type() /
 * psd.platform() / psd.stop() / psd.routes() / psd.arrivals() /
 * psd.doors() / psd.doorValue()).
 */
class Psd {
public:
    enum Type : int32_t {
        GLASS = 0,
        TOP   = 1
    };

    explicit Psd(const JcmPsdSnapshot* snap) : Psd(snap, static_cast<size_t>(-1)) {}
    Psd(const JcmPsdSnapshot* snap, size_t bytes)
        : view_(snap, bytes), snap_(view_.valid() ? snap : &empty_snapshot()) {}

    /** The binding surface — Psd::GLASS or Psd::TOP. */
    Type type() const { return static_cast<Type>(snap_->psd_type); }
    bool is_glass() const { return snap_->psd_type == 0; }
    bool is_top() const { return snap_->psd_type == 1; }

    int32_t width() const { return snap_->width; }
    int32_t height() const { return snap_->height; }

    int64_t platform_id() const { return snap_->platform_id; }
    int64_t station_id() const { return snap_->station_id; }
    bool has_platform() const { return snap_->platform_id != 0; }

    /** The platform number/name (e.g. "1"); empty without a platform. */
    StrView platform_number() const { return pool(snap_->platform_number_offset, snap_->platform_number_len); }
    /* ABI 13 rename: this field always carried the STATION's name (the
       writer stores platform.getStationName()); it was mislabeled
       "platform_name" until the honest rename in ABI 13. */
    StrView station_name() const { return pool(snap_->station_name_offset, snap_->station_name_len); }

    int64_t game_time_millis() const { return snap_->game_time_millis; }
    int64_t in_game_time() const { return snap_->in_game_time; }

    /** Current door open amount of the wall, 0..1. */
    double door_value() const { return snap_->door_value; }
    bool is_door_open() const { return snap_->door_value > 0; }

    PsdDoorList doors() const { return PsdDoorList(snap_, snap_->door_count, snap_->door_offset); }
    PsdArrivalList arrivals() const { return PsdArrivalList(snap_, snap_->arrival_count, snap_->arrival_offset); }
    PsdRouteList routes() const { return PsdRouteList(snap_, snap_->route_count, snap_->route_offset); }

    /* ABI 13: the complete station info (JS: psd.stop()). */
    bool has_station() const { return station().ok(); }
    PsdStation station() const {
        const auto* value = view_.at<JcmPsdStation>(snap_->station_offset);
        return value ? PsdStation(snap_, value, view_.bytes()) : PsdStation{};
    }
    /** Alias of station() — parity with the JS psd.stop(). */
    PsdStation stop() const { return station(); }

    const JcmPsdSnapshot& raw() const { return *snap_; }

private:
    static const JcmPsdSnapshot& empty_snapshot() {
        static const JcmPsdSnapshot empty = [] {
            JcmPsdSnapshot value{};
            value.station_offset = -1;
            value.string_pool_offset = sizeof(JcmPsdSnapshot);
            return value;
        }();
        return empty;
    }
    StrView pool(int32_t offset, int32_t len) const { return view_.string(offset, len); }
    detail::PsdSnapshotView view_;
    const JcmPsdSnapshot* snap_;
};

/**
 * PsdContext — mirrors PSDScriptContext (a PIDS-style draw context):
 *   ctx.draw(Text/Texture), ctx.setAutoZOrdering, ctx.setZOrderStep.
 */
class PsdContext {
public:
    PsdContext(FrameRecorder& frame, const JcmFrameInput& in)
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

/* ---- Text::draw / Texture::draw / Rect::draw on the PSD surface
       (same record shapes as the PIDS path — see pids.hpp) ---- */

inline void Text::draw(PsdContext& ctx) {
    FrameRecorder& f = ctx.frame();
    rec_.text_offset = f.intern(text_, text_len_);
    if (!text_) { rec_.text_len = 0; } else {
        rec_.text_len = text_len_ < 0 ? static_cast<int32_t>(std::strlen(text_)) : text_len_;
    }
    if (font_) {
        rec_.font_id_offset = f.intern(font_, -1);
        rec_.font_id_len = static_cast<int32_t>(std::strlen(font_));
    }
    JcmDrawText& committed = f.push_text();
    const JcmRecordHeader hdr = committed.header;
    committed = rec_;
    committed.header = hdr;
    if (natural_light_) committed.header.flags |= MTR_RECORD_FLAG_NATURAL_LIGHT;
}

inline void Texture::draw(PsdContext& ctx) {
    if (!tex_) return;
    FrameRecorder& f = ctx.frame();
    JcmDrawTexture& committed = f.push_texture();
    const JcmRecordHeader hdr = committed.header;
    committed = rec_;
    committed.header = hdr;
    if (natural_light_) committed.header.flags |= MTR_RECORD_FLAG_NATURAL_LIGHT;
    committed.texture_offset = f.intern(tex_, -1);
    committed.texture_len = static_cast<int32_t>(std::strlen(tex_));
}

inline void Rect::draw(PsdContext& ctx) {
    FrameRecorder& f = ctx.frame();
    JcmDrawTexture& committed = f.push_texture();
    committed.layer = static_cast<int32_t>(layer_);
    committed.color = static_cast<int32_t>(0xFF000000u) + color_;
    committed.x = x_; committed.y = y_;
    committed.w = w_; committed.h = h_;
    committed.u1 = 0; committed.v1 = 0; committed.u2 = 1; committed.v2 = 1;
    committed.z_order = z_;
    if (natural_light_) committed.header.flags |= MTR_RECORD_FLAG_NATURAL_LIGHT;
    static const char WHITE_TEXTURE[] = "mtr:textures/block/white.png";
    committed.texture_offset = f.intern_const(WHITE_TEXTURE, static_cast<int32_t>(sizeof(WHITE_TEXTURE) - 1));
    committed.texture_len = static_cast<int32_t>(sizeof(WHITE_TEXTURE) - 1);
}

} /* namespace mtr */
