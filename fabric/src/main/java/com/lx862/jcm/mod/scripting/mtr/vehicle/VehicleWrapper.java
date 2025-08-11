package com.lx862.jcm.mod.scripting.mtr.vehicle;

import com.lx862.jcm.mixin.modded.mtr.VehicleSchemaMixin;
import com.lx862.jcm.mixin.modded.tsc.MixinVehiclePlatformRouteInfo;
import org.apache.commons.lang3.NotImplementedException;
import org.mtr.core.data.*;
import org.mtr.mod.client.MinecraftClientData;
import org.mtr.mod.client.VehicleRidingMovement;
import org.mtr.mod.data.VehicleExtension;

import java.util.ArrayList;
import java.util.List;

public class VehicleWrapper {
    private final VehicleExtension vehicleExtension;
    private final boolean[] doorLeftOpen;
    private final boolean[] doorRightOpen;
    private final List<PlatformInfo> allRoutePlatforms;
    private final List<PlatformInfo> thisRoutePlatforms;

    public VehicleWrapper(VehicleExtension vehicleExtension) {
        this.vehicleExtension = vehicleExtension;
        this.doorLeftOpen = new boolean[trainCars()];
        this.doorRightOpen = new boolean[trainCars()];
        for(int i = 0; i < trainCars(); i++) {
            this.doorLeftOpen[i] = doorValue() > 0; // TODO
            this.doorRightOpen[i] = doorValue() > 0; // TODO
        }
        this.allRoutePlatforms = new ArrayList<>();
        this.thisRoutePlatforms = new ArrayList<>();

        Depot depot = MinecraftClientData.getInstance().depotIdMap.get(vehicleExtension.vehicleExtraData.getDepotId());

        int stopIdx = 0;
        for(PathData path : path()) {
            if(path.getDwellTime() <= 0 || !path.getRail().isPlatform()) continue;
            VehicleExtraData.VehiclePlatformRouteInfo info = depot.getVehiclePlatformRouteInfo(stopIdx);
            Route route = ((MixinVehiclePlatformRouteInfo)info).getThisRoute();
            String destinationName = route.getDestination(stopIdx);
            Platform platform = ((MixinVehiclePlatformRouteInfo)info).getThisPlatform();
            Station station = platform.area;
            double startDistance = path.getStartDistance();
            double endDistance = path.getEndDistance();
            PlatformInfo pi = new PlatformInfo(route, station, platform, null, destinationName, startDistance, endDistance, false /* TEMP*/);
            allRoutePlatforms.add(pi);
            if(route.getId() == vehicleExtension.vehicleExtraData.getThisRouteId()) {
                thisRoutePlatforms.add(pi);
            }

            stopIdx++;
        }
    }

    public boolean shouldRender(int carIndex) {
        return vehicleExtension.persistentVehicleData.rayTracing[carIndex];
    }

    @Deprecated
    public boolean shouldRenderDetail() {
        return true;
    }

    public String trainTypeId(int carIndex) {
        if(carIndex >= vehicleExtension.vehicleExtraData.immutableVehicleCars.size()) return null;
        return vehicleExtension.vehicleExtraData.immutableVehicleCars.get(carIndex).getVehicleId();
    }

    public String baseTrainType(int carIndex) {
        throw new NotImplementedException("Not implemented yet.");
    }

    public long id() {
        return this.vehicleExtension.getId();
    }

    public TransportMode transportMode() {
        return vehicleExtension.getTransportMode();
    }

    public double spacing(int carIndex) {
        return vehicleExtension.vehicleExtraData.immutableVehicleCars.get(carIndex).getLength();
    }

    public double width(int carIndex) {
        return vehicleExtension.vehicleExtraData.immutableVehicleCars.get(carIndex).getWidth();
    }

    public int trainCars() {
        return vehicleExtension.vehicleExtraData.immutableVehicleCars.size();
    }

    public double accelerationConstant() {
        return vehicleExtension.vehicleExtraData.getAcceleration();
    }

    public boolean manualAllowed() {
        return vehicleExtension.vehicleExtraData.getIsManualAllowed();
    }

    public double maxManualSpeed() {
        return vehicleExtension.vehicleExtraData.getMaxManualSpeed();
    }

    public double railProgress() {
        return ((VehicleSchemaMixin)vehicleExtension).getRailProgress();
    }

    public double getRailSpeed(int pathIndex) {
        return vehicleExtension.vehicleExtraData.immutablePath.get(pathIndex).getSpeedLimitMetersPerMillisecond();
    }

    public double speed() {
        return vehicleExtension.getSpeed();
    }

    public double doorValue() {
        return vehicleExtension.persistentVehicleData.getDoorValue();
    }

    public List<PathData> path() {
        return vehicleExtension.vehicleExtraData.immutablePath;
    }

    public boolean isDoorOpening() {
        return vehicleExtension.persistentVehicleData.getAdjustedDoorMultiplier(vehicleExtension.vehicleExtraData) > 0;
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

    public boolean isClientPlayerRiding() {
        return VehicleRidingMovement.isRiding(vehicleExtension.getId());
    }

    public List<PlatformInfo> getThisRoutePlatforms() {
        return thisRoutePlatforms;
    }

    public List<PlatformInfo> getAllRoutePlatforms() {
        return allRoutePlatforms;
    }

    public static class PlatformInfo {
        public Route route;
        public Station station;
        public Platform platform;
        public Station destinationStation;
        public String destinationName;
        public double startDistance;
        public double endDistance;
        public boolean reverseAtPlatform;

        public PlatformInfo(Route route, Station station, Platform platform,
                            Station destinationStation, String destinationName, double startDistance, double endDistance,
                            boolean reverseAtPlatform) {
            this.route = route;
            this.station = station;
            this.platform = platform;
            this.destinationStation = destinationStation;
            this.destinationName = destinationName;
            this.startDistance = startDistance;
            this.endDistance = endDistance;
            this.reverseAtPlatform = reverseAtPlatform;
        }
    }
}
