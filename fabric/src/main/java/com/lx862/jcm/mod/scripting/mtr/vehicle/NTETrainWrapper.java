package com.lx862.jcm.mod.scripting.mtr.vehicle;

import org.mtr.core.data.SimplifiedRoute;
import org.mtr.core.serializer.JsonReader;
import org.mtr.mod.data.VehicleExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class NTETrainWrapper extends VehicleWrapper {

    public NTETrainWrapper(VehicleExtension vehicleExtension) {
        super(vehicleExtension);
    }

    public List<Stop> getAllPlatforms() {
        return stops();
    }

    public int getAllPlatformsNextIndex() {
        if(stopsData.isFullData) {
            return findNextStopIndex(railProgress(), stops());
        } else {
            int idx = stops().indexOf(stops().stream().filter(e -> e.platform != null && e.platform.getId() == vehicleExtension.vehicleExtraData.getThisPlatformId()).findFirst().orElse(null));
            if(idx != -1) return idx;
            return stops().size();
        }
    }

    private int findNextStopIndex(double currentRailProgress, List<Stop> stops) {
        int stopIdx = 0;

        for(Stop stop : stops) {
            if(currentRailProgress > stop.distance) stopIdx++;
            else break;
        }
        return stopIdx;
    }

    public List<Stop> getThisRoutePlatforms() {
        return stops().stream().filter(stop ->
                stop.route != null && stop.route.getId() == vehicleExtension.vehicleExtraData.getThisRouteId() ||
                        stop.nextRoute != null && stop.nextRoute.getId() == vehicleExtension.vehicleExtraData.getThisRouteId()
        ).collect(Collectors.toList());
    }

    public List<Stop> getNextRoutePlatforms() {
        return stops().stream().filter(stop ->
                stop.route != null && stop.route.getId() == vehicleExtension.vehicleExtraData.getNextRouteId() ||
                        stop.nextRoute != null && stop.nextRoute.getId() == vehicleExtension.vehicleExtraData.getNextRouteId()
        ).collect(Collectors.toList());
    }

    public int getThisRoutePlatformsNextIndex() {
        List<Stop> thisRoutePlatforms = getThisRoutePlatforms();
        if(stopsData.isFullData) {
            return findNextStopIndex(railProgress(), thisRoutePlatforms);
        } else {
            int idx = thisRoutePlatforms.indexOf(thisRoutePlatforms.stream().filter(stop -> stop.platform != null && stop.platform.getId() == vehicleExtension.vehicleExtraData.getThisPlatformId()).findFirst().orElse(null));
            if(idx != -1) return idx;

            return thisRoutePlatforms.size();
        }
    }

    public List<Stop> getDebugThisRoutePlatforms(int count) {
        // TODO WIP
        org.mtr.libraries.com.google.gson.JsonObject routeObject = new org.mtr.libraries.com.google.gson.JsonObject();
        routeObject.addProperty("id", 10000001);
        routeObject.addProperty("name", "调试线路|Debug Route||DRL");
        routeObject.addProperty("color", 0xFF0000);
        routeObject.add("routePlatformData", new org.mtr.libraries.com.google.gson.JsonArray());
        SimplifiedRoute debugRoute = new SimplifiedRoute(new JsonReader(routeObject));

        String destinationName = String.format("车站 %d|Station %d||S%02d", count, count, count);
        List<Stop> stops = new ArrayList<>();
        for(int i = 0; i < count; i++) {
            String name = String.format("车站 %d|Station %d||S%02d", i + 1, i + 1, i + 1);
            stops.add(new Stop(debugRoute, null, null, name, destinationName, i * 1000));
        }

        return stops;
    }
}
