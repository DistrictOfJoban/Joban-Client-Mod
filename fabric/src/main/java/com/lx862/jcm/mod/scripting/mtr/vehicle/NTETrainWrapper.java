package com.lx862.jcm.mod.scripting.mtr.vehicle;

import com.lx862.jcm.mixin.modded.mtr.VehicleSchemaMixin;
import org.mtr.core.data.PathData;
import org.mtr.core.data.Siding;
import org.mtr.core.data.SimplifiedRoute;
import org.mtr.core.data.TransportMode;
import org.mtr.core.serializer.JsonReader;
import org.mtr.mod.data.VehicleExtension;

import java.util.ArrayList;
import java.util.List;

public class NTETrainWrapper extends VehicleWrapper {

    public NTETrainWrapper(VehicleScriptContext.DataFetchMode dataFetchMode, VehicleExtension vehicleExtension) {
        super(dataFetchMode, vehicleExtension);
    }

    public List<Stop> getAllPlatforms() {
        return getStops();
    }

    public List<Stop> getThisRoutePlatforms() {
        return getThisRouteStops();
    }

    public List<Stop> getNextRoutePlatforms() {
        return getNextRouteStops();
    }

    public int getAllPlatformsNextIndex() {
        return getNextStopIndex(0);
    }

    public int getThisRoutePlatformsNextIndex() {
        return getThisRouteNextStopIndex(0);
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

    /* Start getters */
    @Deprecated
    public long id() {
        return this.vehicleExtension.getId();
    }
    @Deprecated
    public Siding siding() {
        return this.stopsData.siding;
    }
    @Deprecated
    public TransportMode transportMode() {
        return getTransportMode();
    }
    @Deprecated
    public int trainCars() {
        return getCars();
    }
    @Deprecated
    public boolean shouldRender() {
        return true;
    }
    @Deprecated
    public boolean shouldRenderDetail() {
        return true;
    }
    @Deprecated
    public String trainTypeId() {
        return getVehicleId(0);
    }
    @Deprecated
    public VehicleExtension mtrTrain() {
        return getMtrVehicle();
    }
    @Deprecated
    public double speed() {
        return getSpeedMs() * (1/20d);
    }
    @Deprecated
    public double spacing() {
        return getLength(0);
    }
    @Deprecated
    public double width() {
        return getWidth(0);
    }
    @Deprecated
    public double accelerationConstant() {
        return (getServiceAcceleration() * 1000 * 1000) / (1/400d);
    }
    @Deprecated
    public double railProgress() {
        return getRailProgress();
    }
    @Deprecated
    public List<PathData> path() {
        return getPathData();
    }
    @Deprecated
    public boolean manualAllowed() {
        return isManualAllowed();
    }
    @Deprecated
    public double doorValue() {
        return getDoorValue();
    }
    @Deprecated
    public int manualToAutomaticTime() {
        return getManualToAutomaticTime();
    }
}
