#include <mtr/script.hpp>
#if CONTENT_KIND == 1
using Adapter = mtr::detail::PidsAdapter;
#elif CONTENT_KIND == 2
using Adapter = mtr::detail::EyecandyAdapter;
#elif CONTENT_KIND == 4
using Adapter = mtr::detail::PsdAdapter;
#else
using Adapter = mtr::detail::LiftAdapter;
#endif
struct ContentState { int frames=0; };
struct ContentProbe : mtr::ScriptBase<ContentState, Adapter> {
    static constexpr auto ID="test:content_probe";
    void create(Adapter::Context& context,ContentState&,const Adapter::Wrapper&) override {
        mtr::Api(context.input()).context().call("mark",{"create"});
#if CONTENT_KIND == 2
        static float boxes[] = {0,0,0,1,1,1};
        context.set_outline_shape(boxes,1);
        context.set_collision_shape(boxes,1);
#endif
    }
    void render(Adapter::Context& context,ContentState& state,const Adapter::Wrapper& wrapper) override {
        mtr::Api api(context.input());
        if(api.context().call("largeId").as<int64_t>()!=9223372036854775000LL) throw std::runtime_error("content context");
#if CONTENT_KIND == 1
        if(wrapper.width()!=42 || wrapper.arrivals().size()!=1 || wrapper.arrivals().at(0).destination()!="测试终点") throw std::runtime_error("PIDS snapshot");
        if(wrapper.get_custom_message(0)!="自定义") throw std::runtime_error("PIDS custom message");
        context.set_auto_z_ordering(false); context.set_z_order_step(0.001);
        mtr::Text::create().text("原生 PIDS").draw(context);
#elif CONTENT_KIND == 2
        if(wrapper.model_id()!="test:decoration" || wrapper.redstone_level()!=13 || !wrapper.consume_block_use() || wrapper.consume_block_use()) throw std::runtime_error("Eyecandy snapshot/event");
        context.play_sound("test:click",1,1);
#elif CONTENT_KIND == 4
        if(wrapper.width()!=96 || wrapper.height()!=150 || !wrapper.is_glass()) throw std::runtime_error("PSD identity");
        if(wrapper.platform_number()!="1" || wrapper.platform_name()!="测试车站") throw std::runtime_error("PSD platform");
        if(wrapper.door_value()!=0.5 || wrapper.doors().size()!=2) throw std::runtime_error("PSD doors");
        if(!wrapper.doors().at(0).is_open() || !wrapper.doors().at(1).is_locked()) throw std::runtime_error("PSD door states");
        if(wrapper.arrivals().size()!=1) throw std::runtime_error("PSD arrivals");
        const auto& arrival = wrapper.arrivals().at(0);
        if(arrival.destination()!="测试终点" || arrival.car_count()!=8 || arrival.door_count()!=2 || arrival.door_number(1)!=3) throw std::runtime_error("PSD arrival snapshot");
        if(wrapper.routes().size()!=1) throw std::runtime_error("PSD routes");
        const mtr::PsdRouteList routes = wrapper.routes();
        if(routes.at(0).stops().size()!=2 || routes.at(0).stops().at(0).name()!="车站甲" || routes.at(0).stops().at(0).interchange_count()!=1) throw std::runtime_error("PSD stops");
        if(routes.at(0).stops().at(1).exit_count()!=1 || routes.at(0).stops().at(1).exit(0).destination(0)!="出口目的地") throw std::runtime_error("PSD exits");
        mtr::Text::create().text("原生 PSD").draw(context);
#else
        if(wrapper.width()!=3.5 || wrapper.floor_count()!=2 || wrapper.direction()!=-1 || wrapper.string(wrapper.floor(1).number_offset,wrapper.floor(1).number_len)!="一层") throw std::runtime_error("Lift snapshot");
        context.play_sound("test:floor",1,1);
#endif
        api.context().call("mark",{"render"}); state.frames++;
    }
    void dispose(Adapter::Context& context,ContentState&,const Adapter::Wrapper&) override {
        mtr::Api(context.input()).context().call("mark",{"dispose"});
    }
};
#if CONTENT_KIND == 1
MTR_REGISTER_PIDS_SCRIPT(ContentProbe)
#elif CONTENT_KIND == 2
MTR_REGISTER_EYECANDY_SCRIPT(ContentProbe)
#elif CONTENT_KIND == 4
MTR_REGISTER_PSD_SCRIPT(ContentProbe)
#else
MTR_REGISTER_LIFT_SCRIPT(ContentProbe)
#endif
