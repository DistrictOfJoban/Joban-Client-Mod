package com.lx862.mtrscripting.mod.impl.mtr.vehicle;

import com.lx862.mtrscripting.core.annotation.ApiInternal;
import com.lx862.mtrscripting.core.util.ScriptVector3f;
import org.mtr.core.data.PathData;
import org.mtr.core.data.Siding;
import org.mtr.core.data.TransportMode;
import org.mtr.mod.data.VehicleExtension;
import org.mtr.mod.render.PositionAndRotation;

import java.util.List;

public class NTETrainWrapper extends VehicleWrapper {
    public final ScriptVector3f[] lastCarPosition;
    public final ScriptVector3f[] lastCarRotation;

    @ApiInternal
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
        return getNextStopIndex(getStops(), 0);
    }

    public int getThisRoutePlatformsNextIndex() {
        return getNextStopIndex(getThisRouteStops(), 0);
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
