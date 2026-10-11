#include <mtr/script.hpp>
struct LiftState { int lastFloor=-1; };
struct LiftIndicator : mtr::LiftScript<LiftState> {
    static constexpr auto ID="demo:lift_indicator";
    void render(mtr::LiftContext& ctx,LiftState& state,const mtr::Lift& lift) override {
        mtr::Api api(ctx.input());
        auto floors=api.wrapper().call("getFloors");
        auto text=api.call("TextUtil","cycleString",{"Lift"});
        api.context().call("setDebugInfo",{"floors",floors.call("size")});
        for(int i=0;i<lift.floor_count();i++) if(lift.floor(i).current_floor) state.lastFloor=i;
    }
};
MTR_REGISTER_LIFT_SCRIPT(LiftIndicator)
