package com.lx862.jcm.mod.scripting.mtr.vehicle;

import com.lx862.jcm.mixin.modded.mtr.VehicleSchemaMixin;
import org.apache.commons.lang3.NotImplementedException;
import org.mtr.core.data.*;
import org.mtr.core.tool.Utilities;
import org.mtr.mod.client.MinecraftClientData;
import org.mtr.mod.client.VehicleRidingMovement;
import org.mtr.mod.data.VehicleExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class VehicleWrapper {
    private final VehicleExtension vehicleExtension;
    @Deprecated public final boolean[] doorLeftOpen;
    @Deprecated public final boolean[] doorRightOpen;
    private OperationData operationData;

    public VehicleWrapper(VehicleExtension vehicleExtension) {
        this.vehicleExtension = vehicleExtension;
        this.doorLeftOpen = new boolean[trainCars()];
        this.doorRightOpen = new boolean[trainCars()];
        for(int i = 0; i < trainCars(); i++) {
            this.doorLeftOpen[i] = doorValue() > 0;
            this.doorRightOpen[i] = doorValue() > 0;
        }

        this.operationData = new OperationData(vehicleExtension);
    }

    public List<PlatformInfo> getAllPlatforms() {
        return operationData.allRoutePlatforms;
    }

    public int getAllPlatformsNextIndex() {
        int headIndex = getRailIndex(getRailProgress(), false);
        Map.Entry<Integer, Integer> entry = operationData.pathToPlatformIndex.ceilingEntry(headIndex);
        if(entry == null) return operationData.allRoutePlatforms.size();
        return entry.getValue();
    }

    public List<PlatformInfo> getThisRoutePlatforms() {
        // TODO: Segment PlatInfo into routes?
        return operationData.thisRoutePlatforms;
    }

    public List<PlatformInfo> getNextRoutePlatforms() {
        throw new NotImplementedException();
    }

    public int getThisRoutePlatformsNextIndex() {
        int headIndex = getRailIndex(getRailProgress(), false);
        Map.Entry<Integer, Integer> entry = operationData.pathToRoutePlatformIndex.ceilingEntry(headIndex);
        if(entry == null) return operationData.thisRoutePlatforms.size();
        return entry.getValue();
    }

    public List<PlatformInfo> getDebugThisRoutePlatforms(int count) {
        throw new NotImplementedException();
    }

    public static class OperationData {
        public final List<PlatformInfo> allRoutePlatforms;
        public final List<PlatformInfo> thisRoutePlatforms;
        public final TreeMap<Integer, Integer> pathToPlatformIndex;
        public final TreeMap<Integer, Integer> pathToRoutePlatformIndex;
        public final Siding siding;

        public OperationData(VehicleExtension vehicleExtension) {
            this.allRoutePlatforms = new ArrayList<>();
            this.thisRoutePlatforms = new ArrayList<>();
            this.pathToPlatformIndex = new TreeMap<>();
            this.pathToRoutePlatformIndex = new TreeMap<>();
            this.siding = MinecraftClientData.getInstance().sidingIdMap.get(vehicleExtension.vehicleExtraData.getSidingId());

            List<SimplifiedRoute> allRoutes = new ArrayList<>();
            SimplifiedRoute lastRoute = MinecraftClientData.getInstance().simplifiedRouteIdMap.get(vehicleExtension.vehicleExtraData.getPreviousRouteId());
            if (lastRoute != null) allRoutes.add(lastRoute);

            SimplifiedRoute thisRoute = MinecraftClientData.getInstance().simplifiedRouteIdMap.get(vehicleExtension.vehicleExtraData.getThisRouteId());
            if (thisRoute != null) allRoutes.add(thisRoute);

            SimplifiedRoute nextRoute = MinecraftClientData.getInstance().simplifiedRouteIdMap.get(vehicleExtension.vehicleExtraData.getNextRouteId());
            if (nextRoute != null) allRoutes.add(nextRoute);

            final SimplifiedRoute runningRoute = thisRoute == null ? nextRoute : thisRoute;

            int totalStopIdx = 0;
            for (int i = 0; i < vehicleExtension.vehicleExtraData.immutablePath.size(); i++) {
                PathData path = vehicleExtension.vehicleExtraData.immutablePath.get(i);
                if (path.getDwellTime() < 500) continue;

                SimplifiedRoute pathRoute = null;
                int pathRouteStopIdx = 0;

                {
                    int routeStopIdx = 0;
                    for (SimplifiedRoute simplifiedRoute : allRoutes) {
                        int routeStartStopIdx = routeStopIdx;
                        int routeEndStopIdx = routeStopIdx + simplifiedRoute.getPlatforms().size();

                        if (totalStopIdx >= routeStartStopIdx && totalStopIdx <= routeEndStopIdx) {
                            pathRoute = simplifiedRoute;
                            pathRouteStopIdx = totalStopIdx - routeStartStopIdx;
                            break;
                        }
                        routeStopIdx = routeEndStopIdx;
                    }
                }

                if (pathRoute == null) { // None when in siding
                    continue;
                }

                List<SimplifiedRoutePlatform> routePlatforms = pathRoute.getPlatforms();

                String destinationName = routePlatforms.get(pathRouteStopIdx).getDestination();
                Station station = MinecraftClientData.getInstance().stationIdMap.get(routePlatforms.get(pathRouteStopIdx).getStationId());
                Platform platform = MinecraftClientData.getInstance().platformIdMap.get(routePlatforms.get(pathRouteStopIdx).getPlatformId());

                double startDistance = path.getStartDistance();
                double endDistance = path.getEndDistance();
                PlatformInfo platInfo = new PlatformInfo(pathRoute, station, platform, null, destinationName, startDistance, endDistance, false /* TEMP*/);

                this.pathToPlatformIndex.put(i, this.allRoutePlatforms.size());
                this.allRoutePlatforms.add(platInfo);
                
                if (runningRoute != null && pathRoute.getId() == runningRoute.getId()) {
                    pathToRoutePlatformIndex.put(i, thisRoutePlatforms.size());
                    thisRoutePlatforms.add(platInfo);
                }

                totalStopIdx++;
            }
        }
    }

    public static class PlatformInfo {
        public SimplifiedRoute route;
        public Station station;
        public Platform platform;
        public Station destinationStation;
        public String destinationName;
        public double startDistance;
        public double endDistance;
        public boolean reverseAtPlatform;

        public PlatformInfo(SimplifiedRoute route, Station station, Platform platform,
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
    public Siding siding() { return this.operationData.siding; }
    public String trainTypeId(int carIndex) {
        if(carIndex >= vehicleExtension.vehicleExtraData.immutableVehicleCars.size()) return null;
        return vehicleExtension.vehicleExtraData.immutableVehicleCars.get(carIndex).getVehicleId();
    }
    public String baseTrainType(int carIndex) { throw new NotImplementedException(); }
    public TransportMode transportMode() { return vehicleExtension.getTransportMode(); }
    public double spacing(int carIndex) { return vehicleExtension.vehicleExtraData.immutableVehicleCars.get(carIndex).getLength(); }
    public double width(int carIndex) { return vehicleExtension.vehicleExtraData.immutableVehicleCars.get(carIndex).getWidth(); }
    public int trainCars() {
        return vehicleExtension.vehicleExtraData.immutableVehicleCars.size();
    }
    public double accelerationConstant() { return vehicleExtension.vehicleExtraData.getAcceleration(); }
    public boolean manualAllowed() { return vehicleExtension.vehicleExtraData.getIsManualAllowed(); }
    public double maxManualSpeed() { return vehicleExtension.vehicleExtraData.getMaxManualSpeed(); }
    public int manualToAutomaticTime() { throw new NotImplementedException(); }
    public List<PathData> path() { return vehicleExtension.vehicleExtraData.immutablePath; }
    public double railProgress() { return this.getRailProgress(); }
    public double getRailProgress() { return ((VehicleSchemaMixin)vehicleExtension).getRailProgress(); }
    public int getRailIndex(double railProgress, boolean roundDown) { return Utilities.getIndexFromConditionalList(vehicleExtension.vehicleExtraData.immutablePath, railProgress - 1.0F); }
    public double getRailSpeed(int pathIndex) { return vehicleExtension.vehicleExtraData.immutablePath.get(pathIndex).getSpeedLimitMetersPerMillisecond(); }
    public double speed() { return vehicleExtension.getSpeed(); }
    public double doorValue() { return vehicleExtension.persistentVehicleData.getDoorValue(); }
    public boolean isCurrentlyManual() { return vehicleExtension.vehicleExtraData.getIsCurrentlyManual(); }
    public boolean isReversed() {
        return vehicleExtension.getReversed();
    }
    public boolean isOnRoute() { return vehicleExtension.getIsOnRoute(); }

    public boolean justOpening() { throw new NotImplementedException(); }
    public boolean justClosing(float doorCloseTime) { throw new NotImplementedException(); }
    public boolean isDoorOpening() { return vehicleExtension.persistentVehicleData.getAdjustedDoorMultiplier(vehicleExtension.vehicleExtraData) > 0; }
}
