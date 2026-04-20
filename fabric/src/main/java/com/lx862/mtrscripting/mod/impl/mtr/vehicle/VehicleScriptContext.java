package com.lx862.mtrscripting.mod.impl.mtr.vehicle;

import com.lx862.mtrscripting.core.annotation.ValueNullable;
import com.lx862.mtrscripting.mod.impl.mtr.MTRScriptContext;
import com.lx862.mtrscripting.core.util.sound.ScriptSoundManager;
import com.lx862.mtrscripting.core.util.model.ModelJS;
import com.lx862.mtrscripting.core.util.Matrices;
import com.lx862.mtrscripting.core.util.ScriptVector3f;
import com.lx862.mtrscripting.core.util.render.ScriptRenderManager;
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
    private final String scriptEntryId;
    private DataFetchMode dataFetchMode;

    public VehicleScriptContext(VehicleExtension vehicleExtension, String scriptEntryId, int[] myCars, boolean fetchDataBeforeExecute) {
        super(scriptEntryId);
        this.vehicleExtension = vehicleExtension;
        this.myCars = myCars;
        this.scriptEntryId = scriptEntryId;
        this.carRenderers = new HashMap<>();
        this.carSoundManagers = new HashMap<>();

        for(int i : myCars) {
            carRenderers.put(i, new ScriptRenderManager());
            carSoundManagers.put(i, new ScriptSoundManager());
        }

        if(fetchDataBeforeExecute) {
            dataFetchMode = DataFetchMode.MANDATORY;
        }
    }

    public void drawCarModel(ModelJS model, int carIndex, @ValueNullable Matrices matrices) {
        ScriptRenderManager renderManager = getCarRenderManager(carIndex);
        if(renderManager == null) return;
        renderManager.drawModel(model, matrices);
    }

    public void drawConnModel(ModelJS model, int carIndex, @ValueNullable Matrices matrices) {
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

    public String getScriptEntryId() {
        return this.scriptEntryId;
    }

    public int[] getMyCars() {
        return this.myCars;
    }

    public ScriptRenderManager getCarRenderManager(int car) {
        return this.carRenderers.get(car);
    }

    public ScriptSoundManager getCarSoundManager(int car) {
        return this.carSoundManagers.get(car);
    }

    public Map<Integer, ScriptRenderManager> getCarRenderManagers() {
        return this.carRenderers;
    }

    public Map<Integer, ScriptSoundManager> getCarSoundManagers() {
        return this.carSoundManagers;
    }

    public DataFetchMode getDataFetchMode() {
        return dataFetchMode;
    }

    public void setDataFetchMode(String enumName) {
        this.dataFetchMode = DataFetchMode.valueOf(enumName);
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

    public enum DataFetchMode {
        SKIP,
        MANDATORY,
        ALL
    }
}
