#include <mtr/script.hpp>
#if CONTENT_KIND == 1
using Adapter = mtr::detail::PidsAdapter;
#elif CONTENT_KIND == 2
using Adapter = mtr::detail::EyecandyAdapter;
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
#else
MTR_REGISTER_LIFT_SCRIPT(ContentProbe)
#endif
