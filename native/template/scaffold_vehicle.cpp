/** scaffold_vehicle.cpp — a minimal vehicle script: an emissive
 *  headlight quad + a per-car status model slot. Declared as:
 *
 *    "vehicleScripts": [{
 *        "id": "scaffold:vehicle",
 *        "scriptLocations": ["scaffold:natives/vehicle"]
 *    }]
 */
#include <mtr/script.hpp>
#include <cstdio>

struct ScaffoldState {
    mtr::CycleTracker cycle{"|"};
};

struct ScaffoldVehicle : mtr::VehicleScript<ScaffoldState> {
    static constexpr auto ID = "scaffold:vehicle";

    void render(mtr::VehicleContext& ctx, ScaffoldState& state, const mtr::Vehicle& vehicle) override {
        (void)state;
        /* ABI 16 styled model draw — emissive + alpha tinted. */
        const int32_t model = ctx.host() ? ctx.host()->acquire_model(ctx.host()->user, "scaffold:models/headlight.json") : -1;
        if (model >= 0) {
            for (int car = 0; car < vehicle.car_count(); car++) {
                if (!vehicle.car(car).rendered()) continue;
                ctx.draw_car_model(model, car, nullptr,
                        mtr::VehicleContext::ModelStyle().tint(0x80FFFFFF).emissive());
            }
        }
        if (vehicle.signalState() != mtr::SignalState::UNKNOWN) {
            /* The ABI 16 signal extension: brake lights on danger. */
            std::printf("[scaffold] signal %s\n", vehicle.getSignalState().str());
        }
    }
};

MTR_REGISTER_VEHICLE_SCRIPT(ScaffoldVehicle)
