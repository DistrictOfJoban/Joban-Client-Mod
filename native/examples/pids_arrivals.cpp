#include <mtr/script.hpp>
struct ArrivalState {};
struct ArrivalDisplay : mtr::PidsScript<ArrivalState> {
    static constexpr auto ID="example:arrivals";
    void render(mtr::PidsContext& ctx,ArrivalState&,const mtr::Pids& pids) override {
        /* Persistent per-script store (the JCM "数据存储" API): every
           context exposes getDataStore() — one JSON-backed key-value file
           per script id, shared by every PIDS running this script and kept
           across game restarts. Here it remembers the next train's
           destination and counts the DISTINCT trains announced, writing
           only when the destination actually changes (good practice: the
           store dirties on write, and flushes are debounced anyway).
           C++ reaches the very same store object JS scripts read through
           ctx.getDataStore(), so both runtimes share the data. */
        mtr::Api api(ctx.input());
        mtr::Api::Object store=api.context().call("getDataStore");
        if(pids.arrivals().size()>0) {
            const std::string dest=pids.arrivals().at(0).destination().str();
            const std::string last=store.call("getString",{"last.destination",""}).as<std::string>();
            if(dest!=last) {
                store.call("set",{"last.destination",dest});
                store.call("set",{"trains.counted",store.call("getNumber",{"trains.counted",0.0}).as<double>()+1});
            }
        }
        for(int row=0;row<pids.rows();row++) {
            if(pids.is_row_hidden(row)) continue;
            std::string text=pids.get_custom_message(row).str();
            if(text.empty() && row<pids.arrivals().size()) text=pids.arrivals().at(row).destination().str();
            if(!text.empty()) mtr::Text::create().text(text.c_str()).pos(2,row*10.0+2).size(pids.width()-4,9).draw(ctx);
        }
        /* ABI 15 (weather): swap the last row for a service notice while
           it rains — the same value the JS pids.getWeather() returns, so
           JS and C++ scripts can share the layout logic. */
        if(pids.weather()!=mtr::Weather::CLEAR && pids.arrivals().size()>0) {
            const char* notice=pids.getWeather()==mtr::weather::THUNDER
                ? "雷暴天气 请注意安全 Thunderstorm — take care"
                : "雨天路滑 请小心慢行 Rain — mind your step";
            mtr::Text::create().text(notice).pos(2,(pids.rows()-1)*10.0+2).size(pids.width()-4,9).color(0x64B5F6).draw(ctx);
        }
    }
};
MTR_REGISTER_PIDS_SCRIPT(ArrivalDisplay)
