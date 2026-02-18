package com.lx862.jcm.mod.scripting.mtr.vehicle;

import org.mtr.core.data.SimplifiedRoute;
import org.mtr.core.serializer.JsonReader;
import org.mtr.mod.data.VehicleExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class NTETrainWrapper extends VehicleWrapper {

    public NTETrainWrapper(VehicleScriptContext.DataFetchMode dataFetchMode, VehicleExtension vehicleExtension) {
        super(dataFetchMode, vehicleExtension);
    }

    public List<Stop> getAllPlatforms() {
        return stops();
    }

    public List<Stop> getThisRoutePlatforms() {
        return thisRouteStops();
    }

    public List<Stop> getNextRoutePlatforms() {
        return stops().stream().filter(stop ->
                stop.route != null && stop.route.getId() == vehicleExtension.vehicleExtraData.getNextRouteId() ||
                        stop.nextRoute != null && stop.nextRoute.getId() == vehicleExtension.vehicleExtraData.getNextRouteId()
        ).collect(Collectors.toList());
    }

    public int getAllPlatformsNextIndex() {
        return nextStopIndex(0);
    }

    public int getThisRoutePlatformsNextIndex() {
        return thisRouteNextStopIndex(0);
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
