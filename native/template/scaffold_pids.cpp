/** scaffold_pids.cpp — a minimal PIDS (platform information display)
 *  native script. Declared in mtr_custom_resources.json as:
 *
 *    "pidsScripts": [{
 *        "id": "scaffold:pids",
 *        "scriptLocations": ["scaffold:natives/pids"]
 *    }]
 */
#include <mtr/script.hpp>
#include <cstdio>

struct ScaffoldState {
    double blink = 0;
};

struct ScaffoldPids : mtr::PidsScript<ScaffoldState> {
    static constexpr auto ID = "scaffold:pids";

    void create(mtr::PidsContext& ctx, ScaffoldState& state, const mtr::Pids& pids) override {
        (void)ctx;
        std::printf("[scaffold] create on a %dx%d display\n", pids.width(), pids.height());
        state.blink = 0;
    }

    void render(mtr::PidsContext& ctx, ScaffoldState& state, const mtr::Pids& pids) override {
        state.blink += 0.02;
        const bool on = state.blink - static_cast<long>(state.blink) < 0.5;
        if (on) {
            mtr::Text::create()
                    .text("Hello native PIDS")
                    .pos(0, 0)
                    .size(pids.width(), pids.height() / 3.0)
                    .center_align()
                    .draw(ctx);
        }
        /* The next arrival's destination, always visible. */
        auto arrivals = pids.arrivals();
        if (arrivals.size() > 0) {
            const mtr::StrView destination = arrivals.at(0).destination();
            mtr::Text::create()
                    .text(destination.data, destination.len)
                    .pos(0, pids.height() * 2.0 / 3.0)
                    .size(pids.width(), pids.height() / 3.0)
                    .center_align()
                    .draw(ctx);
        }
    }
};

MTR_REGISTER_PIDS_SCRIPT(ScaffoldPids)
