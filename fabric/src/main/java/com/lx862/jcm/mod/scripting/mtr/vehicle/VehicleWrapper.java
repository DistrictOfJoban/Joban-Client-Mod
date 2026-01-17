package com.lx862.jcm.mod.scripting.mtr.vehicle;

import com.lx862.jcm.mixin.modded.mtr.VehicleSchemaMixin;
import com.lx862.mtrscripting.exceptions.ScriptNotImplementedException;
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
    private StopsData stopsData;

    public VehicleWrapper(VehicleExtension vehicleExtension) {
        this.vehicleExtension = vehicleExtension;
        this.doorLeftOpen = new boolean[trainCars()];
        this.doorRightOpen = new boolean[trainCars()];
        for(int i = 0; i < trainCars(); i++) {
            this.doorLeftOpen[i] = doorValue() > 0;
            this.doorRightOpen[i] = doorValue() > 0;
        }

        this.stopsData = new StopsData(vehicleExtension);
    }

    public List<Stop> getAllPlatforms() {
        return stopsData.allStops;
    }

    public int getAllPlatformsNextIndex() {
        int headIndex = getRailIndex(getRailProgress(), false);
        Map.Entry<Integer, Integer> entry = stopsData.pathToStopIndex.ceilingEntry(headIndex);
        if(entry == null) return stopsData.allStops.size();
        return entry.getValue();
    }

    public List<Stop> getThisRoutePlatforms() {
        return getAllPlatforms().stream().filter(stop -> stop.route.getId() == vehicleExtension.vehicleExtraData.getThisRouteId()).toList();
    }

    public List<Stop> getNextRoutePlatforms() {
        return getAllPlatforms().stream().filter(stop -> stop.route.getId() == vehicleExtension.vehicleExtraData.getNextRouteId()).toList();
    }

    public int getThisRoutePlatformsNextIndex() {
        List<Stop> thisRoutePlatforms = getThisRoutePlatforms();
        int idx = thisRoutePlatforms.indexOf(thisRoutePlatforms.stream().filter(stop -> stop.distance >= railProgress()).findFirst().orElse(null));
        if(idx != -1) return idx;

        return thisRoutePlatforms.size();
    }

    public List<Stop> getDebugThisRoutePlatforms(int count) {
        throw new ScriptNotImplementedException();
    }

    public static class StopsData {
        public final List<Stop> allStops;
        public final TreeMap<Integer, Integer> pathToStopIndex;
        public final Siding siding;

        public StopsData(VehicleExtension vehicleExtension) {
            this.allStops = new ArrayList<>();
            this.pathToStopIndex = new TreeMap<>();
            this.siding = MinecraftClientData.getInstance().sidingIdMap.get(vehicleExtension.vehicleExtraData.getSidingId());

            List<SimplifiedRoute> allRoutes = new ArrayList<>();
            SimplifiedRoute lastRoute = MinecraftClientData.getInstance().simplifiedRouteIdMap.get(vehicleExtension.vehicleExtraData.getPreviousRouteId());
            if (lastRoute != null) allRoutes.add(lastRoute);

            SimplifiedRoute thisRoute = MinecraftClientData.getInstance().simplifiedRouteIdMap.get(vehicleExtension.vehicleExtraData.getThisRouteId());
            if (thisRoute != null) allRoutes.add(thisRoute);

            SimplifiedRoute nextRoute = MinecraftClientData.getInstance().simplifiedRouteIdMap.get(vehicleExtension.vehicleExtraData.getNextRouteId());
            if (nextRoute != null) allRoutes.add(nextRoute);

            List<PathData> vehiclePaths = vehicleExtension.vehicleExtraData.immutablePath;

            int totalStopIdx = 0;
            for (int i = 0; i < vehiclePaths.size(); i++) {
                PathData path = vehiclePaths.get(i);
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

                double stopDistance = path.getEndDistance();
                PathData nextPath = i+1 >= vehiclePaths.size() ? null : vehiclePaths.get(i+1);
                boolean reverseAtPlatform = nextPath != null && nextPath.isOppositeRail(path);

                Stop stop = new Stop(pathRoute, station, platform, null, destinationName, stopDistance, reverseAtPlatform);

                this.pathToStopIndex.put(i, this.allStops.size());
                this.allStops.add(stop);

                totalStopIdx++;
            }
        }
    }

    public static class Stop {
        public SimplifiedRoute route;
        public Station station;
        public String name;
        @Deprecated
        public Platform platform;
        @Deprecated
        public Station destinationStation;
        public String destinationName;

        public List<SimplifiedRoute> interchangeRoutes;
        public long dwellTime; // in millisecond

        public double distance;
        public boolean reverseAtPlatform;

        public Stop(SimplifiedRoute route, Station station, Platform platform,
                    Station destinationStation, String destinationName, double distance,
                    boolean reverseAtPlatform) {
            this.route = route;
            this.station = station;
            this.platform = platform;
            this.interchangeRoutes = new ArrayList<>();
            this.dwellTime = platform.getDwellTime();
            this.name = this.station == null ? platform.getName() : station.getName();
            this.destinationStation = destinationStation;
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
    public int manualToAutomaticTime() { throw new ScriptNotImplementedException(); }
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

    public boolean justOpening() { throw new ScriptNotImplementedException(); }
    public boolean justClosing(float doorCloseTime) { throw new ScriptNotImplementedException(); }
    public boolean isDoorOpening() { return vehicleExtension.persistentVehicleData.getAdjustedDoorMultiplier(vehicleExtension.vehicleExtraData) > 0; }
}
