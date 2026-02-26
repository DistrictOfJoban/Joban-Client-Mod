package com.lx862.jcm.mod.scripting.mtr.vehicle;

import com.lx862.mtrscripting.util.ScriptVector3f;
import org.mtr.core.data.PathData;
import org.mtr.core.data.Siding;
import org.mtr.core.data.SimplifiedRoute;
import org.mtr.core.data.TransportMode;
import org.mtr.core.serializer.JsonReader;
import org.mtr.mod.data.VehicleExtension;
import org.mtr.mod.render.PositionAndRotation;

import java.util.ArrayList;
import java.util.List;

public class NTETrainWrapper extends VehicleWrapper {
    public final ScriptVector3f[] lastCarPosition;
    public final ScriptVector3f[] lastCarRotation;

    public NTETrainWrapper(VehicleScriptContext.DataFetchMode dataFetchMode, VehicleExtension vehicleExtension) {
        super(dataFetchMode, vehicleExtension);
        int carCount = getCarCount();
        this.lastCarPosition = new ScriptVector3f[carCount];
        this.lastCarRotation = new ScriptVector3f[carCount];
        for(int i = 0; i < carCount; i++) {
            PositionAndRotation posAndRotation = posAndRotations.get(i);
            lastCarPosition[i] = new ScriptVector3f(posAndRotation.position);
            lastCarRotation[i] = new ScriptVector3f((float)posAndRotation.pitch, (float)(Math.PI + posAndRotation.yaw), 0);
        }
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
        return getId();
    }
    @Deprecated
    public Siding siding() {
        return getSiding();
    }
    @Deprecated
    public TransportMode transportMode() {
        return getTransportMode();
    }
    @Deprecated
    public int trainCars() {
        return getCarCount();
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
