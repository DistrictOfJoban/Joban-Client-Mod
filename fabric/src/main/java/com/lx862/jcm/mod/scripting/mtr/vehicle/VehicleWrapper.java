package com.lx862.jcm.mod.scripting.mtr.vehicle;

import com.lx862.jcm.mixin.modded.mtr.VehicleSchemaMixin;
import com.lx862.jcm.mixin.modded.tsc.VehicleAccessorMixin;
import org.mtr.core.data.*;
import org.mtr.core.tool.Utilities;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.mtr.mapping.holder.Box;
import org.mtr.mod.client.MinecraftClientData;
import org.mtr.mod.client.VehicleRidingMovement;
import org.mtr.mod.data.VehicleExtension;
import org.mtr.mod.render.PositionAndRotation;
import org.mtr.mod.render.RenderVehicleHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class VehicleWrapper {
    protected final VehicleExtension vehicleExtension;
    protected final StopsData stopsData;
    public final boolean[] doorLeftOpen;
    public final boolean[] doorRightOpen;
    private final boolean anyVehicleCarVisible;

    public VehicleWrapper(VehicleScriptContext.DataFetchMode dataFetchMode, VehicleExtension vehicleExtension) {
        this.vehicleExtension = vehicleExtension;
        this.doorLeftOpen = new boolean[getCars()];
        this.doorRightOpen = new boolean[getCars()];
        boolean anyVehicleCarVisible = false;
        for(boolean rayTracingVisible : vehicleExtension.persistentVehicleData.rayTracing) {
            if(rayTracingVisible) {
                anyVehicleCarVisible = true;
                break;
            }
        }
        this.anyVehicleCarVisible = anyVehicleCarVisible;
        final ObjectArrayList<PositionAndRotation> posAndRotations = vehicleExtension.getSmoothedVehicleCarsAndPositions(0).stream()
                .map(vehicleCarAndPosition -> {
                    final ObjectArrayList<PositionAndRotation> bogiePositions = vehicleCarAndPosition.right()
                            .stream()
                            .map(bogiePositionPair -> new PositionAndRotation(bogiePositionPair.left(), bogiePositionPair.right(), true))
                            .collect(Collectors.toCollection(ObjectArrayList::new));
                    return new PositionAndRotation(bogiePositions, vehicleCarAndPosition.left(), vehicleExtension.getTransportMode().hasPitchAscending || vehicleExtension.getTransportMode().hasPitchDescending);
                })
                .collect(Collectors.toCollection(ObjectArrayList::new));

        for(int i = 0; i < getCars(); i++) {
            PositionAndRotation posAndRotation = posAndRotations.get(i);

            this.doorLeftOpen[i] = vehicleExtension.persistentVehicleData.getDoorValue() > 0 && RenderVehicleHelper.canOpenDoors(new Box(-1.1, 0, 0, -1, 2, 1), posAndRotation, getDoorValue());
            this.doorRightOpen[i] = vehicleExtension.persistentVehicleData.getDoorValue() > 0 && RenderVehicleHelper.canOpenDoors(new Box(1, 0, 0, 1.1, 2, 1), posAndRotation, getDoorValue());
        }

        this.stopsData = StopsData.constructData(dataFetchMode, vehicleExtension);
    }

    public List<Stop> getStops() {
        return getStops(false);
    }

    public List<Stop> getStops(boolean preferNextRouteStop) {
        return mapStops(stopsData.allStops, preferNextRouteStop);
    }

    public List<Stop> getThisRouteStops() {
        return getThisRouteStops(false);
    }

    public List<Stop> getThisRouteStops(boolean preferNextRouteStop) {
        long routeId = getThisRouteId();
        return mapStops(getRouteStops(routeId), preferNextRouteStop);
    }

    public List<Stop> getNextRouteStops() {
        return getNextRouteStops(true);
    }

    public List<Stop> getNextRouteStops(boolean preferNextRouteStop) {
        long thisRouteId = getThisRouteId();
        int routeIndex = stopsData.routeToRun.indexOf(thisRouteId);
        int nextRouteIndex = routeIndex+1;
        if(nextRouteIndex >= stopsData.routeToRun.size()) return new ArrayList<>();
        return mapStops(getRouteStops(stopsData.routeToRun.get(nextRouteIndex)), preferNextRouteStop);
    }

    public int getNextStopIndex() {
        return getNextStopIndex(0.5);
    }

    public int getNextStopIndex(double overrunTolerance) {
        return findNextStopIndex(overrunTolerance, getRailProgress(), getStops());
    }

    public int getThisRouteNextStopIndex() {
        return getThisRouteNextStopIndex(0.5);
    }

    public int getThisRouteNextStopIndex(double overrunTolerance) {
        return findNextStopIndex(overrunTolerance, getRailProgress(), getThisRouteStops());
    }

    public boolean isFullStopData() {
        return stopsData.isFullData;
    }

    /* Private API */
    protected List<Stop> mapStops(List<Stop> stops, boolean preferNextRouteStop) {
        if(!preferNextRouteStop) return stops;
        return stops.stream().map(e -> e.asNextRoute != null ? e.asNextRoute : e).collect(Collectors.toList());
    }

    protected long getThisRouteId() {
        long routeId = vehicleExtension.vehicleExtraData.getThisRouteId();
        if(isFullStopData()) {
            int nextStopIndex = getNextStopIndex(0);
            if(nextStopIndex < getStops().size()) {
                SimplifiedRoute route = getStops().get(nextStopIndex).route;
                if(route != null) {
                    routeId = route.getId();
                }
            }
        }
        return routeId;
    }

    protected List<Stop> getRouteStops(long routeId) {
        List<Stop> stops = stopsData.routeStops.get(routeId);
        return stops == null ? new ArrayList<>(0) : stops;
    }

    protected int findNextStopIndex(double overrunTolerance, double currentRailProgress, List<Stop> stops) {
        if(stopsData.isFullData) {
            int stopIdx = 0;

            for(Stop stop : stops) {
                if(currentRailProgress > stop.distance + overrunTolerance) stopIdx++;
                else break;
            }
            return stopIdx;
        } else {
            int idx = stops.indexOf(getStops().stream().filter(e -> e.platform != null && e.platform.getId() == vehicleExtension.vehicleExtraData.getThisPlatformId()).findFirst().orElse(null));
            if(idx != -1) return idx;
            return getStops().size();
        }
    }

    public static class StopsData {
        public final List<Stop> allStops;
        public final List<Stop> allStopsNextRoute;
        public final Map<Long, List<Stop>> routeStops;
        public final List<Long> routeToRun;
        public final Siding siding;
        public final boolean isFullData;

        public StopsData(Siding siding, boolean isFullData) {
            this.isFullData = isFullData;
            this.allStops = new ArrayList<>();
            this.allStopsNextRoute = new ArrayList<>();
            this.routeToRun = new ArrayList<>();
            this.routeStops = new HashMap<>();
            this.siding = siding;
        }

        public static StopsData constructData(VehicleScriptContext.DataFetchMode dataFetchMode, VehicleExtension vehicle) {
            if(dataFetchMode == VehicleScriptContext.DataFetchMode.ALL) {
                VehicleDataCache.requestVehicleStopsData(vehicle.getId(), vehicle.vehicleExtraData.getSidingId());
            }

            StopsData fullStopsData = VehicleDataCache.getVehicleStopsData(vehicle);
            if(dataFetchMode == VehicleScriptContext.DataFetchMode.ALL && fullStopsData != null) return fullStopsData;

            long sidingId = vehicle.vehicleExtraData.getSidingId();
            Siding siding = MinecraftClientData.getInstance().sidingIdMap.get(sidingId);
            StopsData limitedStopsData = new StopsData(siding, false);
            return buildLimitedStopsData(limitedStopsData, vehicle);
        }

        private static StopsData buildLimitedStopsData(StopsData stopsData, VehicleExtension vehicleExtension) {
            List<SimplifiedRoute> allRoutes = new ArrayList<>();
            SimplifiedRoute lastRoute = MinecraftClientData.getInstance().simplifiedRouteIdMap.get(vehicleExtension.vehicleExtraData.getPreviousRouteId());
            if (lastRoute != null) allRoutes.add(lastRoute);

            SimplifiedRoute thisRoute = MinecraftClientData.getInstance().simplifiedRouteIdMap.get(vehicleExtension.vehicleExtraData.getThisRouteId());
            if (thisRoute != null) allRoutes.add(thisRoute);

            SimplifiedRoute nextRoute = MinecraftClientData.getInstance().simplifiedRouteIdMap.get(vehicleExtension.vehicleExtraData.getNextRouteId());
            if (nextRoute != null) allRoutes.add(nextRoute);

            long lastPlatformId = 0;
            for(SimplifiedRoute route : allRoutes) {
                stopsData.routeToRun.add(route.getId());
                for(SimplifiedRoutePlatform routePlatform : route.getPlatforms()) {
                    String destinationName = routePlatform.getDestination();
                    Station station = MinecraftClientData.getInstance().stationIdMap.get(routePlatform.getStationId());
                    Platform platform = MinecraftClientData.getInstance().platformIdMap.get(routePlatform.getPlatformId());

                    Stop thisStop = new Stop(route, station, platform, routePlatform.getStationName(), destinationName, -1);
                    routePlatform.forEach((color, routes) -> {
                        routes.forEach(routeName -> {
                            thisStop.routeInterchanges.add(new Stop.RouteInterchange(color, routeName));
                        });
                    });

                    if(routePlatform.getPlatformId() == lastPlatformId) { // Duplicated platform, likely double-added stop from route changeover.
                        Stop prevStop = stopsData.allStops.get(stopsData.allStops.size()-1);
                        prevStop.reverseAtPlatform = true;
                        prevStop.asNextRoute = thisStop;
                    } else {
                        stopsData.allStops.add(thisStop);
                    }
                    stopsData.routeStops.computeIfAbsent(route.getId(), (k) -> new ArrayList<>()).add(thisStop);
                    lastPlatformId = routePlatform.getPlatformId();
                }
            }
            return stopsData;
        }
    }

    public static class Stop {
        public SimplifiedRoute route;
        public Station station;
        public String name;
        public Platform platform;
        public String destinationName;
        public List<RouteInterchange> routeInterchanges;
        public long dwellTimeMs;
        public double distance;
        public Stop asNextRoute;
        @Deprecated
        public long dwellTime; // Use dwellTimeMs instead
        @Deprecated
        public boolean reverseAtPlatform; // Identical to routeSwitchover

        public Stop(SimplifiedRoute route, Station station, Platform platform,
                    String name, String destinationName, double distance) {
            this.route = route;
            this.station = station;
            this.platform = platform;
            this.dwellTime = platform == null ? -1 : platform.getDwellTime() / 500;
            this.dwellTimeMs = platform == null ? -1 : platform.getDwellTime();
            this.routeInterchanges = new ArrayList<>();
            this.name = name;
            this.destinationName = destinationName;
            this.distance = distance;
        }

        public static class RouteInterchange {
            public final int color;
            public final String name;

            public RouteInterchange(int color, String name) {
                this.color = color;
                this.name = name;
            }
        }
    }

    /* Start getters */
    public boolean isRendered() {
        return anyVehicleCarVisible;
    }
    public boolean isCarRendered(int... cars) {
        for(int car : cars) {
            if(vehicleExtension.persistentVehicleData.rayTracing[car]) return true;
        }
        return false;
    }
    public boolean isClientPlayerRiding() {
        return VehicleRidingMovement.isRiding(vehicleExtension.getId());
    }
    public VehicleExtension getMtrVehicle() {
        return this.vehicleExtension;
    }
    public long getId() {
        return this.vehicleExtension.getId();
    }
    public Siding getSiding() {
        return this.stopsData.siding;
    }
    public String getVehicleId(int carIndex) {
        if(carIndex >= vehicleExtension.vehicleExtraData.immutableVehicleCars.size()) return null;
        return vehicleExtension.vehicleExtraData.immutableVehicleCars.get(carIndex).getVehicleId();
    }
    public double getLength(int carIndex) {
        return vehicleExtension.vehicleExtraData.immutableVehicleCars.get(carIndex).getLength();
    }
    public double getWidth(int carIndex) {
        return vehicleExtension.vehicleExtraData.immutableVehicleCars.get(carIndex).getWidth();
    }
    public TransportMode getTransportMode() {
        return vehicleExtension.getTransportMode();
    }
    public int getCars() {
        return vehicleExtension.vehicleExtraData.immutableVehicleCars.size();
    }
    public double getServiceAcceleration() {
        return vehicleExtension.vehicleExtraData.getAcceleration();
    }
    public double getServiceDeceleration() {
        return vehicleExtension.vehicleExtraData.getDeceleration();
    }
    public double getMaxManualSpeed() {
        return vehicleExtension.vehicleExtraData.getMaxManualSpeed();
    }
    public boolean isManualAllowed() {
        return vehicleExtension.vehicleExtraData.getIsManualAllowed();
    }
    public int getManualToAutomaticTime() {
        Siding siding = getSiding();
        return siding == null ? -1 : siding.getManualToAutomaticTime();
    }
    public List<PathData> getPathData() {
        return vehicleExtension.vehicleExtraData.immutablePath;
    }
    public double getRailProgress() {
        return ((VehicleSchemaMixin)vehicleExtension).getRailProgress();
    }
    public double getRailProgress(int car) {
        double progress = getRailProgress();
        for(int i = 0; i < Math.min(vehicleExtension.vehicleExtraData.immutableVehicleCars.size(), car); i++) {
            progress -= vehicleExtension.vehicleExtraData.immutableVehicleCars.get(i).getLength();
        }
        return progress;
    }
    public int getRailIndex(double railProgress, boolean roundDown) {
        return Utilities.getIndexFromConditionalList(vehicleExtension.vehicleExtraData.immutablePath, railProgress - (roundDown ? 1.0F : 0));
    }
    public double getRailSpeed(int pathIndex) {
        return vehicleExtension.vehicleExtraData.immutablePath.get(pathIndex).getSpeedLimitKilometersPerHour() / 3.6 / 20;
    }
    public double getSpeedMs() {
        return vehicleExtension.getSpeed() * 1000;
    }
    public double getSpeedKmh() {
        return getSpeedMs() * 3.6;
    }
    public double getDoorValue() {
        return vehicleExtension.persistentVehicleData.getDoorValue();
    }
    public boolean isCurrentlyManual() {
        return vehicleExtension.vehicleExtraData.getIsCurrentlyManual();
    }
    public boolean isReversed() {
        return vehicleExtension.getReversed();
    }
    public boolean isOnRoute() {
        return vehicleExtension.getIsOnRoute();
    }
    public boolean isDoorOpening() {
        return vehicleExtension.persistentVehicleData.getAdjustedDoorMultiplier(vehicleExtension.vehicleExtraData) > 0;
    }
    public long getElapsedDwellTime() {
        return ((VehicleAccessorMixin)vehicleExtension).getElapsedDwellTime();
    }
    public long getTotalDwellTime() {
        int railIndex = getRailIndex(getRailProgress(), true);
        if(railIndex < vehicleExtension.vehicleExtraData.immutablePath.size()) {
            PathData path = vehicleExtension.vehicleExtraData.immutablePath.get(railIndex);
            if(path != null) {
                return path.getDwellTime();
            }
        }
        return -1;
    }
}
