#include <mtr/script.hpp>
struct SignalState { bool powered = false; mtr::Api::Object quad; };
struct Signal : mtr::EyecandyScript<SignalState> {
    static constexpr auto ID="example:redstone_signal";
    void create(mtr::EyeCandyContext& ctx,SignalState& state,const mtr::EyeCandy&) override {
        mtr::Api api(ctx.input());
        auto shape=api.call("VoxelShape","create",{0.0,0.0,0.0,1.0,0.4,1.0});
        api.context().call("setOutlineShape",{shape});
        api.context().call("setCollisionShape",{shape});
        state.quad=api.call("QuadDrawCall","create");
        auto point=[&](double x,double y,double z){return api.create("Vector3f",{x,y,z});};
        state.quad.call("quad",{point(-0.5,0.5,0.0),point(-0.5,1.0,0.0),point(0.5,1.0,0.0),point(0.5,0.5,0.0)});
        state.quad.call("texture",{api.call("Resources","id",{"minecraft:textures/block/white_concrete.png"})});
    }
    void render(mtr::EyeCandyContext& ctx,SignalState& state,const mtr::EyeCandy& block) override {
        mtr::Api api(ctx.input());
        bool powered=block.redstone_level()>0;
        state.quad.call("color",{powered?0x00ff00:0xff0000});
        api.context().call("getRenderManager").call("draw",{state.quad});
        if(powered&&!state.powered) ctx.play_sound("minecraft:block.note_block.bell",0.5f,1.0f);
        state.powered=powered;
    }
};
MTR_REGISTER_EYECANDY_SCRIPT(Signal)
