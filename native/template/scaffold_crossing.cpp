/** scaffold_crossing.cpp — a minimal ABI 16 level crossing script.
 *  Bind it to a FangSu level_crossing block with the PSD Script Editor
 *  item after declaring it:
 *
 *    "levelCrossingScripts": [{
 *        "id": "scaffold:crossing",
 *        "scriptLocations": ["scaffold:natives/crossing"]
 *    }]
 */
#include <mtr/script.hpp>
#include <cmath>

struct ScaffoldState {
    double barrier = 0;   /* 0 raised .. 1 lowered */
};

struct ScaffoldCrossing : mtr::LevelCrossingScript<ScaffoldState> {
    static constexpr auto ID = "scaffold:crossing";

    void render(mtr::LevelCrossingContext& ctx, ScaffoldState& state, const mtr::LevelCrossing& lc) override {
        /* Animate toward the host's barrier target (0.35s travel). */
        const double target = lc.barrier_closed() ? 1.0 : 0.0;
        state.barrier += (target - state.barrier) * 0.2;
        if (std::abs(target - state.barrier) < 0.01) state.barrier = target;

        const int32_t arms = ctx.host() ? ctx.host()->acquire_model(ctx.host()->user, "scaffold:models/barrier_arm.json") : -1;
        if (arms >= 0) {
            /* The wing lamp glows (emissive) while warning. */
            ctx.draw_model(arms, nullptr, 0, lc.isWarning(), false);
        }
        auto approaches = lc.approaches();
        if (!approaches.empty()) {
            ctx.play_sound("minecraft:block.note_block.bell", 0.6f, 1.0f);
        }
    }
};

MTR_REGISTER_LEVEL_CROSSING_SCRIPT(ScaffoldCrossing)
