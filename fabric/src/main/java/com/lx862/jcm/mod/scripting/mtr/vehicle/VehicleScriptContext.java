package com.lx862.jcm.mod.scripting.mtr.vehicle;

import com.lx862.jcm.mod.scripting.mtr.MTRScriptContext;
import com.lx862.mtrscripting.util.Matrices;
import com.lx862.jcm.mod.scripting.mtr.util.ScriptedModel;
import com.lx862.mtrscripting.util.Vector3dWrapper;
import org.apache.commons.lang3.NotImplementedException;
import org.mtr.mapping.holder.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class VehicleScriptContext extends MTRScriptContext {
    private final List<VehicleModelDrawCall> carModelDrawCalls = new ArrayList<>();
    private final List<VehicleModelDrawCall> conectionModelDrawCalls = new ArrayList<>();
    private final String vehicleId;

    @Override
    public void resetForNextRun() {
        this.carModelDrawCalls.clear();
        this.conectionModelDrawCalls.clear();
    }

    public VehicleScriptContext(String vehicleId) {
        super(vehicleId);
        this.vehicleId = vehicleId;
    }

    public void drawCarModel(ScriptedModel model, int carIndex, Matrices matrices) {
        carModelDrawCalls.add(new VehicleModelDrawCall(model, carIndex, matrices == null ? null :  matrices.getStoredMatrixTransformations().copy()));
    }

    public void drawConnModel(ScriptedModel model, int carIndex, Matrices matrices) {
        this.conectionModelDrawCalls.add(new VehicleModelDrawCall(model, carIndex, matrices == null ? null : matrices.getStoredMatrixTransformations().copy()));
    }

    public void playCarSound(Identifier sound, int carIndex, float x, float y, float z, float volume, float pitch) {
        soundManager().playSound(sound, new Vector3dWrapper(x, y, z), volume, pitch);
        throw new NotImplementedException("Not implemented");
    }

    public void playAnnSound(Identifier sound, float volume, float pitch) {
        soundManager().playLocalSound(sound, volume, pitch);
    }

    public boolean isMyVehicle(VehicleWrapper vehicleWrapper, int carIndex) {
        String theirVehicleId = vehicleWrapper.trainTypeId(carIndex);
        return Objects.equals(vehicleId, theirVehicleId);
    }

    public List<VehicleModelDrawCall> getCarModelDrawCalls() {
        return new ArrayList<>(this.carModelDrawCalls);
    }

    public List<VehicleModelDrawCall> getConnectionModelDrawCalls() {
        return new ArrayList<>(this.conectionModelDrawCalls);
    }
}
