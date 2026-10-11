#pragma once
#include "eyecandy.hpp"
#include "api.hpp"

namespace mtr {
class Lift {
    const JcmLiftSnapshot* snapshot_;
public:
    explicit Lift(const JcmLiftSnapshot* snapshot) : snapshot_(snapshot) {}
    int64_t id() const { return snapshot_->id; }
    bool should_render() const { return snapshot_->should_render; }
    bool is_double_sided() const { return snapshot_->double_sided; }
    bool is_client_player_riding() const { return snapshot_->player_riding; }
    double width() const { return snapshot_->width; }
    double height() const { return snapshot_->height; }
    double depth() const { return snapshot_->depth; }
    double door_value() const { return snapshot_->door_value; }
    double angle_degrees() const { return snapshot_->angle_degrees; }
    double angle_radians() const { return snapshot_->angle_radians; }
    int direction() const { return snapshot_->direction; }
    int floor_count() const { return snapshot_->floor_count; }
    const JcmLiftFloor& floor(int index) const {
        if (index < 0 || index >= floor_count()) throw std::out_of_range("Lift floor index");
        return reinterpret_cast<const JcmLiftFloor*>(reinterpret_cast<const uint8_t*>(snapshot_) + snapshot_->floor_offset)[index];
    }
    StrView string(int32_t offset, int32_t length) const {
        return make_view(reinterpret_cast<const char*>(snapshot_) + offset, length);
    }
    const JcmLiftSnapshot& raw() const { return *snapshot_; }
};
using LiftContext = EyeCandyContext;
}
