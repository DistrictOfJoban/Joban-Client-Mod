package com.lx862.jcm.mod.scripting.mtr.vehicle;

import com.lx862.jcm.mod.network.scripting.RequestFullStopsDataC2SPacket;
import com.lx862.jcm.mod.registry.Networking;
import org.mtr.core.data.Platform;
import org.mtr.core.data.SimplifiedRoute;
import org.mtr.core.data.Station;
import org.mtr.mod.client.MinecraftClientData;
import org.mtr.mod.data.VehicleExtension;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class VehicleStopsDataCache {
    private static final Map<Long, SimplifiedStopsData> cache = new HashMap<>();

    /**
     * Sends a Minecraft packet to request the full stops data from the specified vehicle.
     * Packet only sent on first invocation of the vehicleId
     * @param vehicleId The numeric id of the vehicle
     * @param sidingId The numeric siding id of the belonging vehicle
     */
    public static void requestStopData(long vehicleId, long sidingId) {
        if(!cache.containsKey(vehicleId)) {
            cache.put(vehicleId, null);
            Networking.sendPacketToServer(new RequestFullStopsDataC2SPacket(vehicleId, sidingId));
        }
    }

    public static VehicleWrapper.StopsData getStopData(VehicleExtension vehicleExtension) {
        SimplifiedStopsData simplifiedStopsData = cache.get(vehicleExtension.getId());
        if(simplifiedStopsData == null) return null;

        VehicleWrapper.StopsData stopsData = new VehicleWrapper.StopsData(vehicleExtension, true);

        long lastPlatformId = 0;
        for(RouteStopsData routeStopsData : simplifiedStopsData.routeStopsData) {
            long routeId = routeStopsData.routeId;
            SimplifiedRoute route = MinecraftClientData.getInstance().simplifiedRouteIdMap.get(routeId);

            for(SimplifiedStop simplifiedStop : routeStopsData.stops) {
                if(lastPlatformId == simplifiedStop.platformId) {
                    VehicleWrapper.Stop prevStop = stopsData.allStops.get(stopsData.allStops.size()-1);
                    prevStop.nextRoute = route;
                    prevStop.nextDestinationName = simplifiedStop.destination;
                    prevStop.routeSwitchover = true;
                    prevStop.reverseAtPlatform = true;
                } else {
                    Station station = MinecraftClientData.getInstance().stationIdMap.get(simplifiedStop.stationId);
                    Platform platform = MinecraftClientData.getInstance().platformIdMap.get(simplifiedStop.platformId);

                    VehicleWrapper.Stop stop = new VehicleWrapper.Stop(route, station, platform, station == null ? platform == null ? "" : platform.getName() : station.getName(), simplifiedStop.destination, simplifiedStop.distance);
                    stopsData.allStops.add(stop);
                    lastPlatformId = simplifiedStop.platformId;
                }
            }
        }

        return stopsData;
    }

    public static void putCache(long vehicleId, SimplifiedStopsData stopsData) {
        cache.put(vehicleId, stopsData);
    }

    public static class SimplifiedStopsData {
        public final List<RouteStopsData> routeStopsData = new ArrayList<>();

        public void addRouteStopsData(RouteStopsData routeStopsData) {
            this.routeStopsData.add(routeStopsData);
        }

        @Override
        public String toString() {
            return String.format("SimplifiedStopsData[routeStopsData=\n%s\n]", routeStopsData.stream().map(RouteStopsData::toString).collect(Collectors.joining("\n")));
        }
    }

    public static class RouteStopsData {
        public final long routeId;
        public final List<SimplifiedStop> stops;

        public RouteStopsData(long routeId) {
            this.routeId = routeId;
            this.stops = new ArrayList<>();
        }

        public void addStop(SimplifiedStop stop) {
            this.stops.add(stop);
        }

        @Override
        public String toString() {
            return String.format("RouteStopsData[routeId=%d, stopsData=\n%s\n]", routeId, stops.stream().map(SimplifiedStop::toString).collect(Collectors.joining("\n")));
        }
    }

    public static class SimplifiedStop {
        public final long stationId;
        public final long platformId;
        public final double distance;
        public final String destination;

        public SimplifiedStop(String destination, long stationId, long platformId, double distance) {
            this.stationId = stationId;
            this.platformId = platformId;
            this.distance = distance;
            this.destination = destination;
        }

        @Override
        public String toString() {
            return String.format("SimplifiedStop[stationId=%d,platformId=%d,distance=%f,destination=%s]", stationId, platformId, distance, destination);
        }
    }
}
