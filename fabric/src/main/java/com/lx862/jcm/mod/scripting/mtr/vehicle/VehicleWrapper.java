package com.lx862.jcm.mod.scripting.mtr.vehicle;

import com.lx862.jcm.mixin.modded.mtr.VehicleSchemaMixin;
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

    public VehicleWrapper(VehicleExtension vehicleExtension) {
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
            PositionAndRotation par = posAndRotations.get(i);

            this.doorLeftOpen[i] = RenderVehicleHelper.canOpenDoors(new Box(-1.1, 0, 0, -1, 2, 1), par, doorValue());
            this.doorRightOpen[i] = RenderVehicleHelper.canOpenDoors(new Box(1, 0, 0, 1.1, 2, 1), par, doorValue());
        }

        this.stopsData = StopsData.constructData(vehicleExtension);
    }

    public List<Stop> stops() {
        return stopsData.allStops;
    }

    public static class StopsData {
        public final List<Stop> allStops;
        public final Siding siding;
        public final boolean isFullData;

        public StopsData(VehicleExtension vehicleExtension, boolean isFullData) {
            this.isFullData = isFullData;
            this.allStops = new ArrayList<>();
            this.siding = MinecraftClientData.getInstance().sidingIdMap.get(vehicleExtension.vehicleExtraData.getSidingId());
        }

        public static StopsData constructData(VehicleExtension vehicleExtension) {
            StopsData fullStopsData = VehicleStopsDataCache.getStopData(vehicleExtension);
            if(fullStopsData != null) return fullStopsData;

            StopsData limitedStopsData = new StopsData(vehicleExtension, false);
            return buildLimitedStopsData(limitedStopsData, vehicleExtension);
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
        public long dwellTime; // in millisecond
        public double distance;
        public boolean routeSwitchover;

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
            this.dwellTime = platform == null ? -1 : platform.getDwellTime();
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
}
