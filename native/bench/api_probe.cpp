#include <mtr/script.hpp>
struct ProbeState {
    std::unique_ptr<mtr::Api::Callback> consumer;
    std::unique_ptr<mtr::Api::Callback> task;
    long calls=0;
};
struct ApiProbe : mtr::VehicleScript<ProbeState> {
    static constexpr auto ID="test:api_probe";
    void create(mtr::VehicleContext& ctx,ProbeState& state,const mtr::Train&) override {
        mtr::Api api(ctx.input());
        auto wrapper=api.wrapper();
        if(wrapper.call("largeId").as<int64_t>()!=9223372036854775000LL) throw std::runtime_error("int64 roundtrip");
        if(api.input().at("message")!="原生 API") throw std::runtime_error("Script input");
        if(api.get("TransportMode","BOAT").enum_name()!="BOAT") throw std::runtime_error("Transport mode");
        auto text=api.create("java.lang.StringBuilder",{"中文"});
        text.call("append",{" Test"});
        if(text.call("toString").as<std::string>()!="中文 Test") throw std::runtime_error("UTF-8");
        auto array=wrapper.call("array");
        if(array.call("length").as<int>()!=3||array.call("get",{2}).as<int>()!=30) throw std::runtime_error("array");
        if(wrapper.call("join",{"first","second"}).as<std::string>()!="first/second") throw std::runtime_error("varargs");
        if(wrapper.call("join",{mtr::Json::array({"first","second"})}).as<std::string>()!="first/second") throw std::runtime_error("packed varargs");
        if(!wrapper.call("config",{{{"image",wrapper.call("image")},{"nested",{{"label","配置"}}}}}).as<bool>()) throw std::runtime_error("nested request object");
        api.root("console").call("log",{"native",42});
        auto drawing=wrapper.call("graphics");
        auto color=api.create("java.awt.Color",{255,0,0,255});
        drawing.call("setColor",{color}); drawing.call("fillRect",{0,0,4,4});
        state.consumer=std::make_unique<mtr::Api::Callback>([api,&state](mtr::Json args){
            auto arg=api.wrap(args.at(0));
            state.calls+=arg.as<int>();
            return mtr::Json();
        });
        wrapper.call("consume",{*state.consumer});
        state.task=std::make_unique<mtr::Api::Callback>([api](mtr::Json){
            api.wrapper().call("backgroundDone",{"异步"}); return mtr::Json();
        });
        api.root("BackgroundWorker").call("submit",{*state.task});
        wrapper.call("mark",{"create"});
    }
    void render(mtr::VehicleContext& ctx,ProbeState& state,const mtr::Train&) override {
        mtr::Api api(ctx.input());
        if(state.calls!=7) throw std::runtime_error("Consumer callback");
        api.wrapper().call("mark",{"render"});
    }
    void dispose(mtr::VehicleContext& ctx,ProbeState&,const mtr::Train&) override {
        mtr::Api api(ctx.input()); api.wrapper().call("mark",{"dispose"});
    }
};
MTR_REGISTER_VEHICLE_SCRIPT(ApiProbe)
