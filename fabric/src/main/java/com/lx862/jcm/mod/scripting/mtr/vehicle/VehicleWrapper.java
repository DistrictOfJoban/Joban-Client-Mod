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
    private final VehicleExtension vehicleExtension;
    private final StopsData stopsData;
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

        this.stopsData = new StopsData(vehicleExtension);
    }

    public List<Stop> getAllPlatforms() {
        return stopsData.allStops;
    }

    public int getAllPlatformsNextIndex() {
        int idx = stopsData.allStops.indexOf(stopsData.allStops.stream().filter(e -> e.platform != null && e.platform.getId() == vehicleExtension.vehicleExtraData.getThisPlatformId()).findFirst().orElse(null));
        if(idx != -1) return idx;
        return stopsData.allStops.size();
    }

    public List<Stop> getThisRoutePlatforms() {
        return getAllPlatforms().stream().filter(stop -> stop.route.getId() == vehicleExtension.vehicleExtraData.getThisRouteId()).collect(Collectors.toList());
    }

    public List<Stop> getNextRoutePlatforms() {
        return getAllPlatforms().stream().filter(stop -> stop.route.getId() == vehicleExtension.vehicleExtraData.getNextRouteId()).collect(Collectors.toList());
    }

    public int getThisRoutePlatformsNextIndex() {
        List<Stop> thisRoutePlatforms = getThisRoutePlatforms();
        int idx = thisRoutePlatforms.indexOf(thisRoutePlatforms.stream().filter(stop -> stop.platform != null && stop.platform.getId() == vehicleExtension.vehicleExtraData.getThisPlatformId()).findFirst().orElse(null));
        if(idx != -1) return idx;

        return thisRoutePlatforms.size();
    }

    public List<Stop> getDebugThisRoutePlatforms(int count) {
        throw new ScriptNotImplementedException();
    }

    public static class StopsData {
        public final List<Stop> allStops;
        public final Siding siding;

        public StopsData(VehicleExtension vehicleExtension) {
            this.allStops = new ArrayList<>();
            this.siding = MinecraftClientData.getInstance().sidingIdMap.get(vehicleExtension.vehicleExtraData.getSidingId());

            List<SimplifiedRoute> allRoutes = new ArrayList<>();
            SimplifiedRoute lastRoute = MinecraftClientData.getInstance().simplifiedRouteIdMap.get(vehicleExtension.vehicleExtraData.getPreviousRouteId());
            if (lastRoute != null) allRoutes.add(lastRoute);

            SimplifiedRoute thisRoute = MinecraftClientData.getInstance().simplifiedRouteIdMap.get(vehicleExtension.vehicleExtraData.getThisRouteId());
            if (thisRoute != null) allRoutes.add(thisRoute);

            SimplifiedRoute nextRoute = MinecraftClientData.getInstance().simplifiedRouteIdMap.get(vehicleExtension.vehicleExtraData.getNextRouteId());
            if (nextRoute != null) allRoutes.add(nextRoute);

            for(SimplifiedRoute route : allRoutes) {
                for(SimplifiedRoutePlatform routePlatform : route.getPlatforms()) {
                    String destinationName = routePlatform.getDestination();
                    Station station = MinecraftClientData.getInstance().stationIdMap.get(routePlatform.getStationId());
                    Platform platform = MinecraftClientData.getInstance().platformIdMap.get(routePlatform.getPlatformId());

                    double stopDistance = -1; //path.getEndDistance();
                    boolean reverseAtPlatform = false;

                    Stop stop = new Stop(route, station, platform, destinationName, stopDistance, reverseAtPlatform);
                    this.allStops.add(stop);
                }
            }
        }
    }

    public static class Stop {
        public SimplifiedRoute route;
        public Station station;
        public String name;
        public Platform platform;
        public String destinationName;

        public List<SimplifiedRoute> interchangeRoutes;
        public long dwellTime; // in millisecond

        public double distance;
        public boolean reverseAtPlatform;

        public Stop(SimplifiedRoute route, Station station, Platform platform,
                    String destinationName, double distance,
                    boolean reverseAtPlatform) {
            this.route = route;
            this.station = station;
            this.platform = platform;
            this.interchangeRoutes = new ArrayList<>();
            this.dwellTime = platform == null ? -1 : platform.getDwellTime();
            this.name = station == null ? platform == null ? "" : platform.getName() : station.getName();
            this.destinationName = destinationName;
            this.distance = distance;
            this.reverseAtPlatform = reverseAtPlatform;
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
    public int manualToAutomaticTime() { return siding().getManualToAutomaticTime(); }
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
