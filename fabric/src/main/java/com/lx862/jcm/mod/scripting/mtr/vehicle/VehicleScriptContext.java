package com.lx862.jcm.mod.scripting.mtr.vehicle;

import com.lx862.jcm.mod.scripting.mtr.MTRScriptContext;
import com.lx862.jcm.mod.scripting.mtr.render.ScriptRenderManager;
import com.lx862.jcm.mod.scripting.mtr.sound.ScriptSoundManager;
import com.lx862.mtrscripting.util.Matrices;
import com.lx862.jcm.mod.scripting.mtr.util.ScriptedModel;
import com.lx862.mtrscripting.util.Vector3dWrapper;
import org.apache.commons.lang3.NotImplementedException;
import org.mtr.mapping.holder.Identifier;

import java.util.Objects;

public class VehicleScriptContext extends MTRScriptContext {
    private final ScriptRenderManager[] carRenderers;
    private final ScriptSoundManager[] carSoundManagers;
    private final String vehicleId;
    private final int carLength;

    @Override
    public void resetForNextRun() {
        for(ScriptRenderManager rm : carRenderers) {
            rm.reset();
        }
        for(ScriptSoundManager sm : carSoundManagers) {
            sm.reset();
        }
    }

    public VehicleScriptContext(String vehicleId, int carLength) {
        super(vehicleId);
        this.vehicleId = vehicleId;
        this.carLength = carLength;
        this.carRenderers = new ScriptRenderManager[carLength];
        this.carSoundManagers = new ScriptSoundManager[carLength];
        for(int i = 0; i < carLength; i++) {
            carRenderers[i] = new ScriptRenderManager();
            carSoundManagers[i] = new ScriptSoundManager();
        }
    }

    public void drawCarModel(ScriptedModel model, int carIndex, Matrices matrices) {
        carRenderers[carIndex].drawCalls.add(new VehicleModelDrawCall(model, carIndex, matrices == null ? null : matrices.getStoredMatrixTransformations().copy()));
    }

    public void drawConnModel(ScriptedModel model, int carIndex, Matrices matrices) {
        throw new NotImplementedException("Not implemented");
    }

    public void playCarSound(Identifier sound, int carIndex, float x, float y, float z, float volume, float pitch) {
        carSoundManagers[carIndex].playSound(sound, new Vector3dWrapper(x, y, z), volume, pitch);
    }

    public void playAnnSound(Identifier sound, float volume, float pitch) {
        carSoundManagers[0].playLocalSound(sound, volume, pitch);
    }

    public boolean isMyVehicle(VehicleWrapper vehicleWrapper, int carIndex) {
        String theirVehicleId = vehicleWrapper.trainTypeId(carIndex);
        return Objects.equals(vehicleId, theirVehicleId);
    }

    public ScriptRenderManager[] getCarRenderManager() {
        return this.carRenderers;
    }

    public ScriptSoundManager[] getCarSoundManagers() {
        return this.carSoundManagers;
    }
}
