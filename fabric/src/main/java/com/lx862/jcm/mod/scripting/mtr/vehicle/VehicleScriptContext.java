package com.lx862.jcm.mod.scripting.mtr.vehicle;

import com.lx862.jcm.mod.scripting.mtr.MTRScriptContext;
import com.lx862.jcm.mod.scripting.mtr.render.*;
import com.lx862.jcm.mod.scripting.mtr.sound.ScriptSoundManager;
import com.lx862.mtrscripting.util.Matrices;
import com.lx862.jcm.mod.scripting.mtr.util.ScriptedModel;
import com.lx862.mtrscripting.util.ScriptVector3f;
import org.apache.commons.lang3.NotImplementedException;
import org.mtr.mapping.holder.Identifier;
import org.mtr.mod.client.VehicleRidingMovement;
import org.mtr.mod.data.VehicleExtension;

import java.util.HashMap;
import java.util.Map;


public class VehicleScriptContext extends MTRScriptContext {
    private final VehicleExtension vehicleExtension;
    private final Map<Integer, ScriptRenderManager> carRenderers;
    private final Map<Integer, ScriptSoundManager> carSoundManagers;
    private final int[] myCars;
    private final String vehicleGroupId;
    private boolean requireFetchData;

    public VehicleScriptContext(VehicleExtension vehicleExtension, String vehicleGroupId, int[] myCars, int carLength) {
        super(vehicleGroupId);
        this.vehicleExtension = vehicleExtension;
        this.myCars = myCars;
        this.vehicleGroupId = vehicleGroupId;
        this.carRenderers = new HashMap<>();
        this.carSoundManagers = new HashMap<>();

        for(int i : myCars) {
            carRenderers.put(i, new ScriptRenderManager());
            carSoundManagers.put(i, new ScriptSoundManager());
        }
    }

    public void drawCarModel(DisplayHelperCompat dh, int carIndex, Matrices matrices) {
        if(!carRenderers.containsKey(carIndex)) return;

        if(matrices != null) dh.matrices(matrices);
        carRenderers.get(carIndex).queue(dh);
    }

    public void drawCarModel(ScriptedModel model, int carIndex, Matrices matrices) {
        if(!carRenderers.containsKey(carIndex)) return;

        ModelDrawCall modelDrawCall = ModelDrawCall.create()
                .matrices(matrices == null ? null : matrices.copy())
                .modelObject(model);
        carRenderers.get(carIndex).queue(modelDrawCall);
    }

    public void drawConnModel(ScriptedModel model, int carIndex, Matrices matrices) {
        throw new NotImplementedException("Not implemented");
    }

    public void playCarSound(Identifier sound, int carIndex, float x, float y, float z, float volume, float pitch) {
        if(!carSoundManagers.containsKey(carIndex)) return;
        carSoundManagers.get(carIndex).playSound(sound, new ScriptVector3f(x, y, z), volume, pitch);
    }

    public void playAnnSound(Identifier sound, float volume, float pitch) {
        if(!carSoundManagers.containsKey(0)) return;

        if(VehicleRidingMovement.isRiding(vehicleExtension.getId())) {
            carSoundManagers.get(0).playLocalSound(sound, volume, pitch);
        }
    }

    public String vehicleGroupId() {
        return this.vehicleGroupId;
    }

    public int[] myCars() {
        return this.myCars;
    }

    public Map<Integer, ScriptRenderManager> getCarRenderManagers() {
        return this.carRenderers;
    }

    public Map<Integer, ScriptSoundManager> getCarSoundManagers() {
        return this.carSoundManagers;
    }

    public void enableDataRequesting() {
        this.requireFetchData = true;
    }

    public boolean requireFetchData() {
        return requireFetchData;
    }

    @Override
    public void resetForNextRun() {
        for (ScriptRenderManager carRenderer : carRenderers.values()) {
            if (carRenderer != null) {
                carRenderer.reset();
            }
        }
        for(ScriptSoundManager soundManager : carSoundManagers.values()) {
            if (soundManager != null) {
                soundManager.reset();
            }
        }
    }
}
