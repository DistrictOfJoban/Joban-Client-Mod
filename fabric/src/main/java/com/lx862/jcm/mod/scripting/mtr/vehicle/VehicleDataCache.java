package com.lx862.jcm.mod.scripting.mtr.vehicle;

import com.lx862.jcm.mod.network.scripting.RequestStopsDataC2SPacket;
import com.lx862.jcm.mod.registry.Networking;
import com.lx862.jcm.mod.scripting.MTRDatasetHolder;
import org.mtr.core.data.*;
import org.mtr.mod.client.MinecraftClientData;
import org.mtr.mod.data.VehicleExtension;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * A custom place to store MTR data requested by scripts
 * Required as {@link MinecraftClientData} will strip out non-nearby data.
 */
public class VehicleDataCache {
    public static final MTRDatasetHolder mtrData = new MTRDatasetHolder();
    private static final Map<Long, SimplifiedStopsData> vehicleStopsCache = new HashMap<>();

    /* Data transfer counter to diagnose network usage in Debug Overlay */
    public static long stopsDataByteCounter;
    public static long mtrDataByteCounter;

    /**
     * Sends a Minecraft packet to request the full stops data from the specified vehicle.
     * Packet only sent on first invocation of the vehicleId
     * @param vehicleId The numeric id of the vehicle
     * @param sidingId The numeric siding id of the belonging vehicle
     */
    public static void requestVehicleStopsData(long vehicleId, long sidingId) {
        if(!vehicleStopsCache.containsKey(vehicleId)) {
            vehicleStopsCache.put(vehicleId, null);
            Networking.sendPacketToServer(new RequestStopsDataC2SPacket(vehicleId, sidingId));
        }
    }

    public static VehicleWrapper.StopsData getVehicleStopsData(VehicleExtension vehicleExtension) {
        SimplifiedStopsData simplifiedStopsData = vehicleStopsCache.get(vehicleExtension.getId());
        if(simplifiedStopsData == null) return null;

        Siding siding = VehicleDataCache.mtrData.sidingIdMap.get(vehicleExtension.vehicleExtraData.getSidingId());
        VehicleWrapper.StopsData stopsData = new VehicleWrapper.StopsData(siding, true);

        long lastPlatformId = 0;
        for(RouteStopsData routeStopsData : simplifiedStopsData.routeStopsData) {
            long routeId = routeStopsData.routeId;
            SimplifiedRoute route = VehicleDataCache.mtrData.routeIdMap.get(routeId);
            stopsData.routeToRun.add(routeId);

            int stopIdx = 0;
            for(SimplifiedStop simplifiedStop : routeStopsData.stops) {
                Station station = VehicleDataCache.mtrData.stationIdMap.get(simplifiedStop.stationId);
                Platform platform = VehicleDataCache.mtrData.platformIdMap.get(simplifiedStop.platformId);
                VehicleWrapper.Stop thisStop = new VehicleWrapper.Stop(route, station, platform, station == null ? platform == null ? "" : platform.getName() : station.getName(), simplifiedStop.destination, simplifiedStop.distance);

                if(route != null) {
                    List<SimplifiedRoutePlatform> platforms = route.getPlatforms();
                    if(stopIdx < platforms.size()) {
                        SimplifiedRoutePlatform simplifiedRoutePlatform = platforms.get(stopIdx);
                        simplifiedRoutePlatform.forEach((color, name) -> {
                            name.forEach(routeName -> {
                                thisStop.routeInterchanges.add(new VehicleWrapper.Stop.RouteInterchange(color, routeName));
                            });
                        });
                    }
                }

                if(lastPlatformId == simplifiedStop.platformId) {
                    VehicleWrapper.Stop prevStop = stopsData.allStops.get(stopsData.allStops.size()-1);
                    prevStop.asNextRoute = thisStop;
                    prevStop.reverseAtPlatform = true;
                } else {
                    stopsData.allStops.add(thisStop);
                }
                stopsData.routeStops.computeIfAbsent(routeId, (k) -> new ArrayList<>()).add(thisStop);
                lastPlatformId = simplifiedStop.platformId;
                stopIdx++;
            }
        }

        return stopsData;
    }

    public static void clearData() {
        stopsDataByteCounter = 0;
        mtrDataByteCounter = 0;
        vehicleStopsCache.clear();
        mtrData.stations.clear();
        mtrData.platforms.clear();
        mtrData.routes.clear();
        mtrData.sidings.clear();
        mtrData.stationIdMap.clear();
        mtrData.platformIdMap.clear();
        mtrData.routeIdMap.clear();
        mtrData.sidingIdMap.clear();
    }

    public static void putStopsDataCache(long vehicleId, SimplifiedStopsData stopsData) {
        vehicleStopsCache.put(vehicleId, stopsData);
    }

    public static void clearStopsDataCache(long vehicleId) {
        vehicleStopsCache.remove(vehicleId);
    }

    public static void putMTRDataCache(MTRDatasetHolder other) {
        mtrData.addFrom(other);
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
