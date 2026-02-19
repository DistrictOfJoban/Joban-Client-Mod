package com.lx862.jcm.mod.scripting.mtr.vehicle;

import com.lx862.jcm.mixin.modded.mtr.VehicleSchemaMixin;
import com.lx862.jcm.mixin.modded.tsc.VehicleAccessorMixin;
import com.lx862.mtrscripting.exceptions.ScriptNotImplementedException;
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
import java.util.List;
import java.util.stream.Collectors;

public class VehicleWrapper {
    protected final VehicleExtension vehicleExtension;
    protected final StopsData stopsData;
    public final boolean[] doorLeftOpen;
    public final boolean[] doorRightOpen;

    public VehicleWrapper(VehicleScriptContext.DataFetchMode dataFetchMode, VehicleExtension vehicleExtension) {
        this.vehicleExtension = vehicleExtension;
        this.doorLeftOpen = new boolean[trainCars()];
        this.doorRightOpen = new boolean[trainCars()];
        final ObjectArrayList<PositionAndRotation> posAndRotations = vehicleExtension.getSmoothedVehicleCarsAndPositions(0).stream()
                .map(vehicleCarAndPosition -> {
                    final ObjectArrayList<PositionAndRotation> bogiePositions = vehicleCarAndPosition.right()
                            .stream()
                            .map(bogiePositionPair -> new PositionAndRotation(bogiePositionPair.left(), bogiePositionPair.right(), true))
                            .collect(Collectors.toCollection(ObjectArrayList::new));
                    return new PositionAndRotation(bogiePositions, vehicleCarAndPosition.left(), vehicleExtension.getTransportMode().hasPitchAscending || vehicleExtension.getTransportMode().hasPitchDescending);
                })
                .collect(Collectors.toCollection(ObjectArrayList::new));

        for(int i = 0; i < trainCars(); i++) {
            PositionAndRotation posAndRotation = posAndRotations.get(i);

            this.doorLeftOpen[i] = vehicleExtension.persistentVehicleData.getDoorValue() > 0 && RenderVehicleHelper.canOpenDoors(new Box(-1.1, 0, 0, -1, 2, 1), posAndRotation, doorValue());
            this.doorRightOpen[i] = vehicleExtension.persistentVehicleData.getDoorValue() > 0 && RenderVehicleHelper.canOpenDoors(new Box(1, 0, 0, 1.1, 2, 1), posAndRotation, doorValue());
        }

        this.stopsData = StopsData.constructData(dataFetchMode, vehicleExtension);
    }

    public List<Stop> stops() {
        return stopsData.allStops;
    }

    public List<Stop> thisRouteStops() {
        long routeId = vehicleExtension.vehicleExtraData.getThisRouteId();
        if(fullStopData()) {
            int nextStopIndex = nextStopIndex();
            if(nextStopIndex >= stops().size()) return new ArrayList<>(0);
            SimplifiedRoute route = stops().get(nextStopIndex).route;
            if(route != null) {
                routeId = route.getId();
            }
        }

        return filterStopsForRoute(routeId);
    }

    public int nextStopIndex() {
        return nextStopIndex(0.5);
    }

    public int nextStopIndex(double tolerance) {
        return findNextStopIndex(tolerance, railProgress(), stops());
    }

    public int thisRouteNextStopIndex() {
        return thisRouteNextStopIndex(0.5);
    }

    public int thisRouteNextStopIndex(double tolerance) {
        return findNextStopIndex(tolerance, railProgress(), thisRouteStops());
    }

    public boolean fullStopData() {
        return stopsData.isFullData;
    }

    /* Private API */
    protected List<Stop> filterStopsForRoute(long routeId) {
        return stopsData.allStops.stream().filter(e ->
                (e.route != null && e.route.getId() == routeId) ||
                (e.nextRoute != null && e.nextRoute.getId() == routeId)
        ).collect(Collectors.toList());
    }

    protected int findNextStopIndex(double tolerance, double currentRailProgress, List<Stop> stops) {
        if(stopsData.isFullData) {
            int stopIdx = 0;

            for(Stop stop : stops) {
                if(currentRailProgress > stop.distance + tolerance) stopIdx++;
                else break;
            }
            return stopIdx;
        } else {
            int idx = stops.indexOf(stops().stream().filter(e -> e.platform != null && e.platform.getId() == vehicleExtension.vehicleExtraData.getThisPlatformId()).findFirst().orElse(null));
            if(idx != -1) return idx;
            return stops().size();
        }
    }

    public static class StopsData {
        public final List<Stop> allStops;
        public final Siding siding;
        public final boolean isFullData;

        public StopsData(Siding siding, boolean isFullData) {
            this.isFullData = isFullData;
            this.allStops = new ArrayList<>();
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
                for(SimplifiedRoutePlatform routePlatform : route.getPlatforms()) {
                    if(routePlatform.getPlatformId() == lastPlatformId) { // Duplicated platform, likely double-added stop from route changeover.
                        Stop prevStop = stopsData.allStops.get(stopsData.allStops.size()-1);
                        prevStop.nextRoute = route;
                        prevStop.nextDestinationName = routePlatform.getDestination();
                        prevStop.routeSwitchover = true;
                        prevStop.reverseAtPlatform = true;
                    } else {
                        String destinationName = routePlatform.getDestination();
                        Station station = MinecraftClientData.getInstance().stationIdMap.get(routePlatform.getStationId());
                        Platform platform = MinecraftClientData.getInstance().platformIdMap.get(routePlatform.getPlatformId());

                        Stop stop = new Stop(route, station, platform, routePlatform.getStationName(), destinationName, -1);
                        stopsData.allStops.add(stop);

                        lastPlatformId = routePlatform.getPlatformId();
                    }
                }
            }
            return stopsData;
        }
    }

    public static class Stop {
        public SimplifiedRoute route;
        public SimplifiedRoute nextRoute;
        public Station station;
        public String name;
        public Platform platform;
        public String destinationName;
        public String nextDestinationName;
        public List<SimplifiedRoute> interchangeRoutes;
        public long dwellTimeMs;
        public double distance;
        public boolean routeSwitchover;
        @Deprecated
        public long dwellTime;
        @Deprecated
        public boolean reverseAtPlatform;

        public Stop(SimplifiedRoute route, Station station, Platform platform,
                    String name, String destinationName, double distance) {
            this.route = route;
            this.nextRoute = null;
            this.nextDestinationName = null;
            this.station = station;
            this.platform = platform;
            this.interchangeRoutes = new ArrayList<>();
            this.dwellTime = platform == null ? -1 : platform.getDwellTime() / 500;
            this.dwellTimeMs = platform == null ? -1 : platform.getDwellTime();
            this.name = name;
            this.destinationName = destinationName;
            this.distance = distance;
        }
    }

    @Deprecated
    public boolean shouldRender() {
        return true;
    }

    @Deprecated
    public boolean shouldRenderDetail() {
        return true;
    }

    public boolean isClientPlayerRiding() {
        return VehicleRidingMovement.isRiding(vehicleExtension.getId());
    }

    public VehicleExtension vehicle() { return this.vehicleExtension; }
    public VehicleExtension mtrTrain() { return vehicle(); }
    public long id() {
        return this.vehicleExtension.getId();
    }
    public Siding siding() { return this.stopsData.siding; }
    public String trainTypeId(int carIndex) {
        if(carIndex >= vehicleExtension.vehicleExtraData.immutableVehicleCars.size()) return null;
        return vehicleExtension.vehicleExtraData.immutableVehicleCars.get(carIndex).getVehicleId();
    }
    public String baseTrainType(int carIndex) { throw new ScriptNotImplementedException(); }
    public TransportMode transportMode() { return vehicleExtension.getTransportMode(); }
    public double spacing(int carIndex) { return vehicleExtension.vehicleExtraData.immutableVehicleCars.get(carIndex).getLength(); }
    public double width(int carIndex) { return vehicleExtension.vehicleExtraData.immutableVehicleCars.get(carIndex).getWidth(); }
    public int trainCars() {
        return vehicleExtension.vehicleExtraData.immutableVehicleCars.size();
    }
    public double accelerationConstant() { return vehicleExtension.vehicleExtraData.getAcceleration(); }
    public boolean manualAllowed() { return vehicleExtension.vehicleExtraData.getIsManualAllowed(); }
    public double maxManualSpeed() { return vehicleExtension.vehicleExtraData.getMaxManualSpeed(); }
    public int manualToAutomaticTime() { Siding siding = siding(); return siding == null ? -1 : siding.getManualToAutomaticTime(); }
    public List<PathData> path() { return vehicleExtension.vehicleExtraData.immutablePath; }
    public double railProgress() { return ((VehicleSchemaMixin)vehicleExtension).getRailProgress(); }
    public double getRailProgress(int car) {
        double progress = railProgress();
        for(int i = 0; i < Math.min(vehicleExtension.vehicleExtraData.immutableVehicleCars.size(), car); i++) {
            progress -= vehicleExtension.vehicleExtraData.immutableVehicleCars.get(i).getLength();
        }
        return progress;
    }
    public int getRailIndex(double railProgress, boolean roundDown) { return Utilities.getIndexFromConditionalList(vehicleExtension.vehicleExtraData.immutablePath, railProgress - 1.0F); }
    public double getRailSpeed(int pathIndex) { return vehicleExtension.vehicleExtraData.immutablePath.get(pathIndex).getSpeedLimitMetersPerMillisecond(); }
    public double speed() { return vehicleExtension.getSpeed(); }
    public double doorValue() { return vehicleExtension.persistentVehicleData.getDoorValue(); }
    public boolean isCurrentlyManual() { return vehicleExtension.vehicleExtraData.getIsCurrentlyManual(); }
    public boolean isReversed() {
        return vehicleExtension.getReversed();
    }
    public boolean isOnRoute() { return vehicleExtension.getIsOnRoute(); }

    public boolean justOpening() { throw new ScriptNotImplementedException(); }
    public boolean justClosing(float doorCloseTime) { throw new ScriptNotImplementedException(); }
    public boolean isDoorOpening() { return vehicleExtension.persistentVehicleData.getAdjustedDoorMultiplier(vehicleExtension.vehicleExtraData) > 0; }

    public long elapsedDwellTime() { return ((VehicleAccessorMixin)vehicleExtension).getElapsedDwellTime(); }
    public long totalDwellTime() {
        int railIndex = getRailIndex(railProgress(), false);
        if(railIndex < vehicleExtension.vehicleExtraData.immutablePath.size()) {
            PathData pd = vehicleExtension.vehicleExtraData.immutablePath.get(railIndex);
            if(pd != null) {
                return pd.getDwellTime();
            }
        }
        return -1;
    }
}
