package com.lx862.mtrscripting.mod.impl.mtr.vehicle;

import com.lx862.mtrscripting.mod.network.packet.data.RequestStopsDataC2SPacket;
import com.lx862.jcm.mod.registry.Networking;
import com.lx862.mtrscripting.mod.impl.mtr.MTRDatasetHolder;
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
     * Built {@link VehicleWrapper.StopsData} per vehicle, reused across frames.
     *
     * <p>Both the JS and the native render pipelines used to rebuild every Stop,
     * RouteInterchange and backing list on EVERY frame even though the underlying
     * SimplifiedStopsData and dataset are stable — several hundred short-lived
     * objects per vehicle per frame, which showed up as a young-gen GC pause at
     * a fixed period (the reported "every 500ms stutter"). The cache keeps the
     * built object graph until either the synced stops payload or the dataset
     * revision changes, then rebuilds once.
     */
    private static final Map<Long, BuiltStops> builtStopsCache = new HashMap<>();

    private static final class BuiltStops {
        VehicleWrapper.StopsData data;
        SimplifiedStopsData source;
        long sourceRevision;
    }

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

    public static VehicleWrapper.StopsData buildVehicleStopsData(VehicleExtension vehicleExtension) {
        SimplifiedStopsData simplifiedStopsData = vehicleStopsCache.get(vehicleExtension.getId());
        if(simplifiedStopsData == null) return null;

        final long revision = mtrData.getRevision();
        BuiltStops cached = builtStopsCache.get(vehicleExtension.getId());
        if(cached != null && cached.data != null && cached.source == simplifiedStopsData
                && cached.sourceRevision == revision) {
            /* Cached graph is still current. Refresh only the siding reference:
            MTR swaps the object in sidingIdMap on sync, and the wrapper reads
            name/manual time from it. */
            cached.data.siding = VehicleDataCache.mtrData.sidingIdMap.get(
                    vehicleExtension.vehicleExtraData.getSidingId());
            return cached.data;
        }

        Siding siding = VehicleDataCache.mtrData.sidingIdMap.get(vehicleExtension.vehicleExtraData.getSidingId());
        VehicleWrapper.StopsData stopsData = new VehicleWrapper.StopsData(siding, true);

        long lastPlatformId = 0;
        int stopIdx = 0;
        for(RouteStopsData routeStopsData : simplifiedStopsData.routeStopsData) {
            long routeId = routeStopsData.routeId;
            SimplifiedRoute route = VehicleDataCache.mtrData.routeIdMap.get(routeId);
            stopsData.routeToRun.add(routeId);

            Station destinationStation = VehicleDataCache.mtrData.stationIdMap.get(routeStopsData.stops.get(routeStopsData.stops.size()-1).stationId);

            int routeStopIdx = 0;
            List<VehicleWrapper.Stop> routeStops = new ArrayList<>();
            for(SimplifiedStop simplifiedStop : routeStopsData.stops) {
                Station station = VehicleDataCache.mtrData.stationIdMap.get(simplifiedStop.stationId);
                Platform platform = VehicleDataCache.mtrData.platformIdMap.get(simplifiedStop.platformId);
                VehicleWrapper.Stop thisStop = new VehicleWrapper.Stop(route, station, platform, station == null ? platform == null ? "" : platform.getName() : station.getName(), simplifiedStop.destination, simplifiedStop.customDestination, simplifiedStop.distance, simplifiedStop.turnbackPlatform);
                thisStop.destinationStation = destinationStation;

                if(route != null) {
                    List<SimplifiedRoutePlatform> platforms = route.getPlatforms();
                    if(routeStopIdx < platforms.size()) {
                        SimplifiedRoutePlatform simplifiedRoutePlatform = platforms.get(routeStopIdx);
                        final InterchangeRouteModes interchangeModes = new InterchangeRouteModes();
                        mtrData.stationInterchangeMap.getOrDefault(simplifiedStop.stationId, java.util.Collections.emptyList())
                                .forEach(e -> interchangeModes.add(e.color, e.name, e.getTransportMode(),
                                        e.getTransportType(), e.getRouteId()));
                        simplifiedRoutePlatform.forEach((color, name) -> {
                            name.forEach(routeName -> {
                                final InterchangeRouteModes.Entry entry = interchangeModes.getEntry(color, routeName);
                                thisStop.routeInterchanges.add(new VehicleWrapper.Stop.RouteInterchange(
                                        entry == null ? 0 : entry.routeId, color, routeName,
                                        entry == null ? null : entry.mode, entry == null ? null : entry.typeId()));
                            });
                        });
                    }
                }
                if(station != null) {
                    thisStop.connectingInterchanges.putAll(mtrData.connectingStationInterchangeMap.getOrDefault(station.getId(), new HashMap<>()));
                }

                boolean shouldAppendStop = false;
                if(lastPlatformId == simplifiedStop.platformId) {
                    VehicleWrapper.Stop prevStop = stopsData.allStops.get(stopsData.allStops.size()-1);
                    prevStop.roundUpRoute = thisStop;
                    prevStop.reverseAtPlatform = true;
                    prevStop.isRouteSwitchoverStop = true;
                } else {
                    stopsData.allStops.add(thisStop);
                    shouldAppendStop = true;
                }
                routeStops.add(thisStop);
                stopsData.stopIdxToRouteStops.put(stopIdx, routeStops);
                lastPlatformId = simplifiedStop.platformId;
                if(shouldAppendStop) stopIdx++;
                routeStopIdx++;
            }
        }

        if(cached == null) {
            cached = new BuiltStops();
            builtStopsCache.put(vehicleExtension.getId(), cached);
        }
        cached.data = stopsData;
        cached.source = simplifiedStopsData;
        cached.sourceRevision = revision;
        return stopsData;
    }

    public static void clearData() {
        stopsDataByteCounter = 0;
        mtrDataByteCounter = 0;
        vehicleStopsCache.clear();
        builtStopsCache.clear();
        mtrData.stations.clear();
        mtrData.platforms.clear();
        mtrData.routes.clear();
        mtrData.sidings.clear();
        mtrData.stationIdMap.clear();
        mtrData.platformIdMap.clear();
        mtrData.routeIdMap.clear();
        mtrData.sidingIdMap.clear();
        mtrData.stationInterchangeMap.clear();
        mtrData.connectingStationInterchangeMap.clear();
    }

    public static void putStopsDataCache(long vehicleId, SimplifiedStopsData stopsData) {
        vehicleStopsCache.put(vehicleId, stopsData);
    }

    public static void clearStopsDataCache(long vehicleId) {
        vehicleStopsCache.remove(vehicleId);
        builtStopsCache.remove(vehicleId);
    }

    public static void putMTRDataCache(MTRDatasetHolder other) {
        mtrData.addFrom(other);
    }

    public static void tick() {
        boolean needRegenerateCache = false;
        for(int i = 0; i < mtrData.stations.size(); i++) {
            Station data = mtrData.stations.get(i);
            Station mtrData = MinecraftClientData.getInstance().stationIdMap.get(data.getId());
            if(mtrData != null && !mtrData.equals(data)) {
                VehicleDataCache.mtrData.stations.set(i, mtrData);
                needRegenerateCache = true;
            }
        }
        for(int i = 0; i < mtrData.routes.size(); i++) {
            SimplifiedRoute data = mtrData.routes.get(i);
            SimplifiedRoute mtrData = MinecraftClientData.getInstance().simplifiedRouteIdMap.get(data.getId());
            if(mtrData != null && !mtrData.equals(data)) {
                VehicleDataCache.mtrData.routes.set(i, mtrData);
                needRegenerateCache = true;
            }
        }
        for(int i = 0; i < mtrData.platforms.size(); i++) {
            Platform data = mtrData.platforms.get(i);
            Platform mtrData = MinecraftClientData.getInstance().platformIdMap.get(data.getId());
            if(mtrData != null && !mtrData.equals(data)) {
                VehicleDataCache.mtrData.platforms.set(i, mtrData);
                needRegenerateCache = true;
            }
        }
        for(int i = 0; i < mtrData.sidings.size(); i++) {
            Siding data = mtrData.sidings.get(i);
            Siding mtrData = MinecraftClientData.getInstance().sidingIdMap.get(data.getId());
            if(mtrData != null && !mtrData.equals(data)) {
                VehicleDataCache.mtrData.sidings.set(i, mtrData);
                needRegenerateCache = true;
            }
        }
        if(needRegenerateCache) {
            /* generateIdMapCache bumps the dataset revision, which invalidates
            every builtStopsCache entry on the next build call. */
            mtrData.generateIdMapCache();
        }
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
        public final String customDestination;
        public final boolean turnbackPlatform;

        public SimplifiedStop(String destination, String customDestination, long stationId, long platformId, double distance, boolean turnbackPlatform) {
            this.stationId = stationId;
            this.platformId = platformId;
            this.distance = distance;
            this.destination = destination;
            this.customDestination = customDestination;
            this.turnbackPlatform = turnbackPlatform;
        }

        @Override
        public String toString() {
            return String.format("SimplifiedStop[stationId=%d,platformId=%d,distance=%f,destination=%s,turnbackPlatform=%s]", stationId, platformId, distance, destination, turnbackPlatform);
        }
    }
}
