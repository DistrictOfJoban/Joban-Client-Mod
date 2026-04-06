package com.lx862.jcm.mod.scripting.mtr.vehicle;

import com.lx862.jcm.mixin.modded.mtr.VehicleSchemaMixin;
import com.lx862.jcm.mixin.modded.tsc.VehicleAccessorMixin;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
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
    private static final int FULL_NOTCH_LEVEL = 5;
    protected final VehicleExtension vehicleExtension;
    protected final StopsData stopsData;
    protected final ObjectArrayList<PositionAndRotation> posAndRotations;
    public final boolean[] doorLeftOpen;
    public final boolean[] doorRightOpen;
    private final boolean anyVehicleCarVisible;

    public VehicleWrapper(VehicleScriptContext.DataFetchMode dataFetchMode, VehicleExtension vehicleExtension) {
        this.vehicleExtension = vehicleExtension;
        this.doorLeftOpen = new boolean[getCarCount()];
        this.doorRightOpen = new boolean[getCarCount()];
        boolean anyVehicleCarVisible = false;
        for(boolean rayTracingVisible : vehicleExtension.persistentVehicleData.rayTracing) {
            if(rayTracingVisible) {
                anyVehicleCarVisible = true;
                break;
            }
        }
        this.anyVehicleCarVisible = anyVehicleCarVisible;
        posAndRotations = vehicleExtension.getSmoothedVehicleCarsAndPositions(0).stream()
                .map(vehicleCarAndPosition -> {
                    final ObjectArrayList<PositionAndRotation> bogiePositions = vehicleCarAndPosition.right()
                            .stream()
                            .map(bogiePositionPair -> new PositionAndRotation(bogiePositionPair.left(), bogiePositionPair.right(), true))
                            .collect(Collectors.toCollection(ObjectArrayList::new));
                    return new PositionAndRotation(bogiePositions, vehicleCarAndPosition.left(), vehicleExtension.getTransportMode().hasPitchAscending || vehicleExtension.getTransportMode().hasPitchDescending);
                })
                .collect(Collectors.toCollection(ObjectArrayList::new));

        for(int i = 0; i < getCarCount(); i++) {
            PositionAndRotation posAndRotation = posAndRotations.get(i);
            double halfWidth = getWidth(i) / 2;
            double halfLength = getLength(i) / 2;

            this.doorLeftOpen[i] = getDoorValue() > 0 && RenderVehicleHelper.canOpenDoors(new Box(-halfWidth - 0.1, -1, -halfLength, -halfWidth, 1, halfLength), posAndRotation, getDoorValue());
            this.doorRightOpen[i] = getDoorValue() > 0 && RenderVehicleHelper.canOpenDoors(new Box(halfWidth, -1, -halfLength, halfWidth + 0.1, 1, halfLength), posAndRotation, getDoorValue());
        }

        this.stopsData = StopsData.constructData(dataFetchMode, vehicleExtension);
    }

    public List<Stop> getStops() {
        return stopsData.allStops;
    }

    public List<Stop> getThisRouteStops() {
        long routeId = getThisRouteId();
        return getRouteStops(routeId);
    }

    public List<Stop> getNextRouteStops() {
        long thisRouteId = getThisRouteId();
        int routeIndex = stopsData.routeToRun.indexOf(thisRouteId);
        int nextRouteIndex = routeIndex+1;
        if(nextRouteIndex >= stopsData.routeToRun.size()) return List.of();
        return getRouteStops(stopsData.routeToRun.getLong(nextRouteIndex));
    }

    public int getNextStopIndex(List<Stop> stops) {
        return getNextStopIndex(stops, 0.5);
    }

    public int getNextStopIndex(List<Stop> stops, double overrunTolerance) {
        return findNextStopIndex(overrunTolerance, getRailProgress(), stops);
    }

    public boolean isStopsDataFetched() {
        return stopsData.isFullData;
    }

    /** Whether stops data (Including MTR data) are fully fetched.
    HACK for existing MTR 3 scripts, not for use by scripts. */
    public boolean isStopsDataFullyFetched() {
        if(!isStopsDataFetched()) return false;

        for(Stop stop : stopsData.allStops) {
            if(stop.platform == null) return false; // Use platform as a measure, since that will never be null if fetched.
        }
        return true;
    }

    protected long getThisRouteId() {
        long routeId = vehicleExtension.vehicleExtraData.getThisRouteId();
        if(isStopsDataFetched()) {
            int nextStopIndex = getNextStopIndex(getStops(), 0);
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
        return stops == null ? List.of() : stops;
    }

    protected int findNextStopIndex(double overrunTolerance, double currentRailProgress, List<Stop> stops) {
        boolean distanceAvailable = !stops.isEmpty() && stops.get(0).distance >= 0;

        if(distanceAvailable) {
            int stopIdx = 0;

            for(Stop stop : stops) {
                if(currentRailProgress > stop.distance + overrunTolerance) stopIdx++;
                else break;
            }
            return stopIdx;
        } else {
            long nextPlatId = vehicleExtension.vehicleExtraData.getThisPlatformId();
            Stop nextStop = stops.stream().filter(e -> e.platform != null && e.platform.getId() == nextPlatId).findFirst().orElse(null);
            if(nextStop != null) {
                int idx = stops.indexOf(nextStop);
                if(idx != -1) return idx;
            }
            return stops.size();
        }
    }

    public static class StopsData {
        public final ObjectArrayList<Stop> allStops;
        public final ObjectArrayList<Stop> allStopsNextRoute;
        public final Long2ObjectOpenHashMap<List<Stop>> routeStops;
        public final LongArrayList routeToRun;
        public final Siding siding;
        public final boolean isFullData;

        public StopsData(Siding siding, boolean isFullData) {
            this.isFullData = isFullData;
            this.allStops = new ObjectArrayList<>();
            this.allStopsNextRoute = new ObjectArrayList<>();
            this.routeToRun = new LongArrayList();
            this.routeStops = new Long2ObjectOpenHashMap<>();
            this.siding = siding;
        }

        public static StopsData constructData(VehicleScriptContext.DataFetchMode dataFetchMode, VehicleExtension vehicle) {
            if(dataFetchMode != VehicleScriptContext.DataFetchMode.SKIP) {
                VehicleDataCache.requestVehicleStopsData(vehicle.getId(), vehicle.vehicleExtraData.getSidingId());
            }

            StopsData fullStopsData = VehicleDataCache.buildVehicleStopsData(vehicle);
            if(dataFetchMode != VehicleScriptContext.DataFetchMode.SKIP && fullStopsData != null) {
                return fullStopsData;
            }

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

                List<SimplifiedRoutePlatform> routePlatforms = route.getPlatforms();
                Station destinationStation = MinecraftClientData.getInstance().stationIdMap.get(routePlatforms.get(routePlatforms.size()-1).getStationId());

                for(SimplifiedRoutePlatform routePlatform : routePlatforms) {
                    String destinationName = routePlatform.getDestination();
                    Station station = MinecraftClientData.getInstance().stationIdMap.get(routePlatform.getStationId());
                    Platform platform = MinecraftClientData.getInstance().platformIdMap.get(routePlatform.getPlatformId());

                    Stop thisStop = new Stop(route, station, platform, routePlatform.getStationName(), destinationName, -1);
                    thisStop.destinationStation = destinationStation;
                    // In-station interchange
                    routePlatform.forEach((color, routes) -> {
                        routes.forEach(routeName -> {
                            thisStop.routeInterchanges.add(new Stop.RouteInterchange(color, routeName));
                        });
                    });
                    // Connecting station interchange
                    if(station != null) {
                        station.getInterchangeStationNameToColorToRouteNamesMap(true).forEach((stationName, interchanges) -> {
                            if(!stationName.equals(station.getName())) {
                                interchanges.forEach((color, routeNames) -> {
                                    routeNames.forEach(routeName -> {
                                        thisStop.connectingInterchanges.computeIfAbsent(stationName, k -> new ObjectArrayList<>()).add(new Stop.RouteInterchange(color, routeName));
                                    });
                                });
                            }
                        });
                    }

                    if(routePlatform.getPlatformId() == lastPlatformId) { // Duplicated platform, likely double-added stop from route changeover.
                        Stop prevStop = stopsData.allStops.get(stopsData.allStops.size()-1);
                        prevStop.roundUpRoute = thisStop;
                        prevStop.reverseAtPlatform = true;
                        prevStop.isRouteSwitchoverStop = true;
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
        public Map<String, List<RouteInterchange>> connectingInterchanges;
        public long dwellTimeMillis;
        public double distance;
        public Stop roundUpRoute;
        public boolean isRouteSwitchoverStop;
        @Deprecated
        public Station destinationStation; // Manually obtain the Station of the last stop instead
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
            this.dwellTimeMillis = platform == null ? -1 : platform.getDwellTime();
            this.routeInterchanges = new ArrayList<>();
            this.connectingInterchanges = new HashMap<>();
            this.name = name;
            this.destinationName = destinationName;
            this.distance = distance;
            this.roundUpRoute = this;
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
    public int getCarCount() {
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
    public int getNotchLevel() {
        return vehicleExtension.vehicleExtraData.getPowerLevel();
    }
    public double getNotchPosition() {
        return (double)getNotchLevel() / FULL_NOTCH_LEVEL;
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
    /** NOTE: Elapsed Dwell Time is not counted on the client-side, it purely relies on the synced value from the server, which isn't real-time. */
    public long getElapsedDwellTime() {
        return ((VehicleAccessorMixin)vehicleExtension).getElapsedDwellTime();
    }
}
