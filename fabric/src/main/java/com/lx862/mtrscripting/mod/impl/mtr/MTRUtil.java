package com.lx862.mtrscripting.mod.impl.mtr;

import com.lx862.mtrscripting.core.util.ScriptVector3f;
import com.lx862.mtrscripting.mod.impl.mtr.pids.ArrivalsWrapper;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import org.mtr.core.data.PathData;
import org.mtr.core.data.Rail;
import org.mtr.core.data.SimplifiedRoute;
import org.mtr.core.data.Station;
import org.mtr.libraries.it.unimi.dsi.fastutil.longs.LongImmutableList;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.mtr.mod.InitClient;
import org.mtr.mod.client.MinecraftClientData;
import org.mtr.mod.data.ArrivalsCacheClient;

import java.util.List;

public class MTRUtil {
    public static class Data {

        public static Station getStation(long stationId) {
            return MinecraftClientData.getInstance().stationIdMap.get(stationId);
        }

        public static SimplifiedRoute getClientRoute(long routeId) {
            return MinecraftClientData.getInstance().simplifiedRouteIdMap.get(routeId);
        }

        public static Station getStationAtPos(ScriptVector3f pos) {
            return InitClient.findStation(pos.rawBlockPos());
        }

        public static List<SimplifiedRoute> getRoutesAtPlatform(long platformId) {
            ObjectArrayList<SimplifiedRoute> list = new ObjectArrayList<>();
            for(SimplifiedRoute route : MinecraftClientData.getInstance().simplifiedRoutes) {
                if(route.getPlatformIndex(platformId) >= 0) list.add(route);
            }
            return list;
        }

        public static Rail getRailFromPath(PathData pathData) {
            String hexId = pathData.getRail().getHexId();
            for(MinecraftClientData.RailWrapper railWrapper : MinecraftClientData.getInstance().railWrapperList.values()) {
                if(railWrapper.getRail().getHexId().equals(hexId)) return railWrapper.getRail();
            }
            return pathData.getRail();
        }

        public static ArrivalsWrapper getArrivals(long platformId) {
            return getArrivals(List.of(platformId));
        }

        public static ArrivalsWrapper getArrivals(List<Long> platformId) {
            return new ArrivalsWrapper(ArrivalsCacheClient.INSTANCE.requestArrivals(new LongImmutableList(platformId)));
        }
    }
}
