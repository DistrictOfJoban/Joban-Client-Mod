package com.lx862.jcm.mod.scripting.mtr.vehicle;

import org.apache.commons.lang3.NotImplementedException;
import org.mtr.core.data.PathData;
import org.mtr.core.data.TransportMode;
import org.mtr.mod.data.VehicleExtension;

import java.util.List;

public class VehicleWrapper {
    private final VehicleExtension vehicleExtension;
    private final boolean[] doorLeftOpen;
    private final boolean[] doorRightOpen;

    public VehicleWrapper(VehicleExtension vehicleExtension) {
        this.vehicleExtension = vehicleExtension;
        this.doorLeftOpen = new boolean[trainCars()];
        this.doorRightOpen = new boolean[trainCars()];
        for(int i = 0; i < trainCars(); i++) {
            this.doorLeftOpen[i] = doorValue() > 0; // TODO
            this.doorRightOpen[i] = doorValue() > 0; // TODO
        }
    }

    public boolean shouldRender() {
        return true;
    }

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

    public int manualToAutomaticTime() {
        throw new NotImplementedException("Not implemented yet.");
    }

    public double railProgress() {
        throw new NotImplementedException("Not implemented yet.");
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
}
