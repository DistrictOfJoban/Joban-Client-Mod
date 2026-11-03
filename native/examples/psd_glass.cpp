/**
 * psd_glass.cpp — example PSD (platform screen door) glass script.
 *
 * Declare it in mtr_custom_resources.json:
 *
 *   "psdScripts": [
 *       {
 *           "id": "example:psd_glass",
 *           "type": "glass",
 *           "scriptLocations": ["mtr:my_pack/natives/psd_glass"],
 *           "dataFetchMode": "ALL"
 *       }
 *   ]
 *
 * Then bind it to a PSD glass block in-game with the PSD Script Editor
 * (屏蔽门脚本编辑器). The mod draws the opaque white base; this script draws
 * the next arrival on top of it.
 */
#include <mtr/script.hpp>
#include <cstdio>

struct GlassState {};

struct NextArrivalGlass : mtr::PsdScript<GlassState> {
    static constexpr auto ID = "example:psd_glass";

    void render(mtr::PsdContext& ctx, GlassState&, const mtr::Psd& psd) override {
        char buffer[192];
        int len;

        /* Platform header, e.g. "站台 Platform 1". */
        if (psd.has_platform()) {
            std::snprintf(buffer, sizeof(buffer), "站台 / Platform %.*s",
                          static_cast<int>(psd.platform_number().len),
                          psd.platform_number().data);
            mtr::Text::create()
                    .text(buffer)
                    .pos(3, 3)
                    .size(psd.width() - 6, 10)
                    .stretch_xy()
                    .color(0x1E64C8)
                    .draw(ctx);
        }

        const mtr::PsdArrivalList arrivals = psd.arrivals();
        if (arrivals.size() == 0) return;
        const mtr::PsdArrival arrival = arrivals.at(0);

        /* Destination. */
        std::snprintf(buffer, sizeof(buffer), "%.*s %.*s",
                      static_cast<int>(arrival.route_number().len), arrival.route_number().data,
                      static_cast<int>(arrival.destination().len), arrival.destination().data);
        mtr::Text::create()
                .text(buffer)
                .pos(3, 20)
                .size(psd.width() - 6, 14)
                .stretch_xy()
                .color(0xFC9700)
                .draw(ctx);

        /* Countdown in seconds. */
        const double seconds = static_cast<double>(arrival.arrival_time() - psd.game_time_millis()) / 1000.0;
        std::snprintf(buffer, sizeof(buffer), "%.0f s / %d 卡", seconds < 0 ? 0.0 : seconds, arrival.car_count());
        mtr::Text::create()
                .text(buffer)
                .pos(3, 38)
                .size(psd.width() - 6, 12)
                .stretch_xy()
                .color(0xFFFFFF)
                .draw(ctx);

        /* The door units that will open, e.g. "开门 Doors: 1 2 3 4". */
        len = std::snprintf(buffer, sizeof(buffer), "开门 Doors:");
        for (int32_t i = 0; i < arrival.door_count() && len > 0; i++) {
            len += std::snprintf(buffer + len, sizeof(buffer) - static_cast<size_t>(len), " %d", arrival.door_number(i));
        }
        mtr::Text::create()
                .text(buffer)
                .pos(3, 54)
                .size(psd.width() - 6, 10)
                .stretch_xy()
                .color(arrival.door_count() > 0 ? 0x00C853 : 0x888888)
                .draw(ctx);

        /* Route strip: every station of the first route, interchange-colored. */
        const mtr::PsdRouteList routes = psd.routes();
        if (routes.size() > 0 && !routes.at(0).stops().empty()) {
            const mtr::PsdStopList stops = routes.at(0).stops();
            const double cell = (psd.width() - 6.0) / stops.size();
            for (int32_t i = 0; i < stops.size(); i++) {
                const mtr::PsdStop stop = stops.at(i);
                mtr::Rect::create()
                        .pos(3 + cell * i, psd.height() - 6)
                        .size(cell * 0.7, 4)
                        .color(stop.interchange_count() > 0 ? 0xFC9700 : 0x1E64C8)
                        .draw(ctx);
            }
        }
    }
};

MTR_REGISTER_PSD_SCRIPT(NextArrivalGlass)
