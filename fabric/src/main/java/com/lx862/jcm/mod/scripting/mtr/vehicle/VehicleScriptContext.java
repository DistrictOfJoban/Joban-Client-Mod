package com.lx862.jcm.mod.scripting.mtr.vehicle;

import com.lx862.jcm.mod.scripting.mtr.MTRScriptContext;
import com.lx862.jcm.mod.scripting.mtr.render.*;
import com.lx862.jcm.mod.scripting.mtr.sound.ScriptSoundManager;
import com.lx862.mtrscripting.util.Matrices;
import com.lx862.jcm.mod.scripting.mtr.util.ScriptedModel;
import com.lx862.mtrscripting.util.ScriptVector3f;
import org.apache.commons.lang3.NotImplementedException;
import org.mtr.mapping.holder.Identifier;


public class VehicleScriptContext extends MTRScriptContext {
    private final ScriptRenderManager[] carRenderers;
    private final ScriptSoundManager[] carSoundManagers;
    private final int[] myCars;
    private final String vehicleGroupId;

    public VehicleScriptContext(String vehicleGroupId, int[] myCars, int carLength) {
        super(vehicleGroupId);
        this.myCars = myCars;
        this.vehicleGroupId = vehicleGroupId;
        this.carRenderers = new ScriptRenderManager[carLength];
        this.carSoundManagers = new ScriptSoundManager[carLength];

        for(int i : myCars) {
            carRenderers[i] = new ScriptRenderManager();
            carSoundManagers[i] = new ScriptSoundManager();
        }
    }

    public void drawCarModel(DisplayHelperCompat dh, int carIndex, Matrices matrices) {
        if(carRenderers[carIndex] == null) return;

        if(matrices != null) dh.matrices(matrices);
        carRenderers[carIndex].queue(dh);
    }

    public void drawCarModel(ScriptedModel model, int carIndex, Matrices matrices) {
        if(carRenderers[carIndex] == null) return;

        ModelDrawCall modelDrawCall = ModelDrawCall.create()
                .matrices(matrices == null ? null : matrices.copy())
                .modelObject(model);
        carRenderers[carIndex].queue(modelDrawCall);
    }

    public void drawConnModel(ScriptedModel model, int carIndex, Matrices matrices) {
        throw new NotImplementedException("Not implemented");
    }

    public void playCarSound(Identifier sound, int carIndex, float x, float y, float z, float volume, float pitch) {
        if(carSoundManagers[carIndex] == null) return;
        carSoundManagers[carIndex].playSound(sound, new ScriptVector3f(x, y, z), volume, pitch);
    }

    public void playAnnSound(Identifier sound, float volume, float pitch) {
        if(carSoundManagers[0] == null) return;
        carSoundManagers[0].playLocalSound(sound, volume, pitch);
    }

    public String vehicleGroupId() {
        return this.vehicleGroupId;
    }

    public int[] myCars() {
        return this.myCars;
    }

    public ScriptRenderManager[] getCarRenderManager() {
        return this.carRenderers;
    }

    public ScriptSoundManager[] getCarSoundManagers() {
        return this.carSoundManagers;
    }

    @Override
    public void resetForNextRun() {
        for (ScriptRenderManager carRenderer : carRenderers) {
            if (carRenderer != null) {
                carRenderer.reset();
            }
        }
        for(ScriptSoundManager soundManager : carSoundManagers) {
            if (soundManager != null) {
                soundManager.reset();
            }
        }
    }
}
