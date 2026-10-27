/**
 * eyecandy.hpp — typed wrapper over JcmEyecandySnapshot.
 *
 * Mirrors the JS objects exposed to eye_candy scripts:
 *   com.lx862.mtrscripting.mod.impl.mtr.eyecandy.EyeCandyScriptContext
 *   + EyecandyBlockEntityWrapper + EyecandyEvents (onBlockUse).
 */
#pragma once

#include "mtr_native.h"
#include "frame.hpp"
#include "matrices.hpp"
#include "util.hpp"

namespace mtr {

/**
 * EyeCandy — mirrors EyecandyBlockEntityWrapper
 * (modelId / translate / rotate / facing / redstoneLevel ...).
 */
class EyeCandy {
public:
    explicit EyeCandy(const JcmEyecandySnapshot* snap) : snap_(snap) {}

    StrView model_id() const {
        const uint8_t* base = reinterpret_cast<const uint8_t*>(snap_);
        return make_view(reinterpret_cast<const char*>(base + snap_->model_id_offset),
                         snap_->model_id_len);
    }

    float translate_x() const { return snap_->translate[0]; }
    float translate_y() const { return snap_->translate[1]; }
    float translate_z() const { return snap_->translate[2]; }
    float rotate_x() const { return snap_->rotate[0]; }
    float rotate_y() const { return snap_->rotate[1]; }
    float rotate_z() const { return snap_->rotate[2]; }
    bool full_brightness() const { return snap_->full_brightness != 0; }

    /* Direction.ordinal(), matching the Java scripting wrapper. */
    int32_t facing() const { return snap_->facing; }

    /* facingAngle() — port of EyecandyBlockEntityWrapper.facingAngle()
       (upstream v2.3.0-prerelease.2): Direction.asRotation() degrees.
       MC ordinals: 2=NORTH, 3=SOUTH, 4=WEST, 5=EAST; asRotation() is
       horizontalIndex*90 (SOUTH=0, WEST=90, NORTH=180, EAST=270). Vertical
       ordinals never occur for eyecandy FACING; 0 keeps the arithmetic
       total instead of branching on an impossible case. */
    float facing_angle() const {
        switch (snap_->facing) {
            case 2: return 180.0f; /* NORTH */
            case 4: return 90.0f;  /* WEST  */
            case 5: return 270.0f; /* EAST  */
            default: return 0.0f;  /* SOUTH (and verticals) */
        }
    }
    bool is_crosshair_target() const { return snap_->crosshair_target != 0; }

    int32_t redstone_level() const { return snap_->redstone_level; }

    int64_t block_pos(int32_t i) const { return snap_->block_pos[i]; }
    int64_t game_time_millis() const { return snap_->game_time_millis; }
    int64_t in_game_time() const { return snap_->in_game_time; }

    /* Pending onBlockUse events this frame (bitmask; the host coalesces
       multiple clicks per frame — the JS event queue behaves the same). */
    uint32_t block_use_events() const { return snap_->block_use_events; }
    bool consume_block_use() const {
        if (consumed_use_ || snap_->block_use_events == 0) return false;
        consumed_use_ = true;
        return true;
    }

    const JcmEyecandySnapshot& raw() const { return *snap_; }

private:
    const JcmEyecandySnapshot* snap_;
    mutable bool consumed_use_ = false;
};

/**
 * EyeCandyContext — mirrors EyeCandyScriptContext:
 *   ctx.drawModel(model, matrices) / ctx.playSound(id, volume, pitch)
 *   / ctx.setCollisionShape / ctx.setOutlineShape / ctx.events().
 */
class EyeCandyContext {
public:
    EyeCandyContext(FrameRecorder& frame, const JcmFrameInput& in)
        : frame_(&frame), input_(&in) {}

    void draw_model(int32_t model_handle, const Matrices* matrices) {
        JcmDrawModel& r = frame_->push_model();
        r.model_handle = model_handle;
        r.car = 0;
        r.bogie = 0xFF;
        if (matrices) {
            r.pose_offset = matrices->compile();
            r.pose_count = 1;
        }
    }

    void play_sound(const char* sound, float volume, float pitch) {
        JcmDrawSound& r = frame_->push_sound(JCM_DRAW_SOUND);
        r.x = r.y = r.z = 0;
        r.volume = volume;
        r.pitch = pitch;
        r.sound_offset = frame_->intern(sound, -1);
        r.sound_len = static_cast<int32_t>(std::strlen(sound));
    }

    /* ctx.setOutlineShape(VoxelShape) — 6 floats per box: x1,y1,z1,x2,y2,z2. */
    void set_outline_shape(const float* boxes, int32_t box_count) {
        JcmDrawShape& r = frame_->push_shape();
        r.is_collision = 0;
        r.box_count = box_count;
        r.box_offset = frame_->push_floats(boxes, box_count * 6);
    }

    /* ctx.setCollisionShape(...) — host rejects shapes above 1.5 blocks,
       mirroring the IllegalStateException in EyeCandyScriptContext. */
    void set_collision_shape(const float* boxes, int32_t box_count) {
        for (int32_t i = 0; i < box_count; i++) {
            const float max_y = boxes[i * 6 + 4];
            if (max_y > 1.5f) return; /* host validates and logs */
        }
        JcmDrawShape& r = frame_->push_shape();
        r.is_collision = 1;
        r.box_count = box_count;
        r.box_offset = frame_->push_floats(boxes, box_count * 6);
    }

    const JcmHostServices* host() const { return input_->host; }

    /* Full frame input (ABI envelope), needed by GraphicsTexture etc. */
    const JcmFrameInput& input() const { return *input_; }

    FrameRecorder& frame() { return *frame_; }

private:
    FrameRecorder* frame_;
    const JcmFrameInput* input_;
};

} /* namespace mtr */
