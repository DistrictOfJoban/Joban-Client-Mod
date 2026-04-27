package com.lx862.mtrscripting.mod.impl.mtr.vehicle;

import com.lx862.mtrscripting.core.annotation.ApiInternal;
import com.lx862.mtrscripting.core.annotation.ValueNullable;
import com.lx862.mtrscripting.core.util.model.DynamicModelHolderJS;
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

import java.util.ArrayList;
import java.util.List;

public class VehicleScriptContext extends MTRScriptContext {
    private final VehicleExtension vehicleExtension;
    private final VehicleScriptCallsHolder.Committer scriptCallsHolder;
    private final int[] myCars;
    private final String scriptEntryId;
    private DataFetchMode dataFetchMode;

    @ApiInternal
    public VehicleScriptContext(VehicleExtension vehicleExtension, String scriptEntryId, int[] myCars, boolean fetchDataBeforeExecute) {
        super(scriptEntryId);
        this.vehicleExtension = vehicleExtension;
        this.myCars = myCars;
        this.scriptEntryId = scriptEntryId;
        this.scriptCallsHolder = new VehicleScriptCallsHolder.Committer();

        var vehiclePositions = vehicleExtension.getSmoothedVehicleCarsAndPositions(0);

        for(int i : myCars) {
            scriptCallsHolder.carRenderManagers.put(i, new ScriptRenderManager());
            scriptCallsHolder.carSoundManagers.put(i, new ScriptSoundManager());

            List<ScriptRenderManager> carBogieRenderManagers = new ArrayList<>();
            var bogiePoses = vehiclePositions.get(i).right();
            for(int j = 0; j < bogiePoses.size(); j++) {
                carBogieRenderManagers.add(new ScriptRenderManager());
            }
            scriptCallsHolder.carBogieRenderers.put(i, carBogieRenderManagers);
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

    public void drawCarModel(DynamicModelHolderJS modelHolder, int carIndex, @ValueNullable Matrices matrices) {
        ScriptRenderManager renderManager = getCarRenderManager(carIndex);
        if(renderManager == null) return;
        renderManager.drawModel(modelHolder, matrices);
    }

    @Deprecated
    public void drawConnModel(ModelJS model, int carIndex, @ValueNullable Matrices matrices) {
        throw new NotImplementedException("Not implemented");
    }

    @Deprecated
    public void drawConnModel(DynamicModelHolderJS model, int carIndex, @ValueNullable Matrices matrices) {
        throw new NotImplementedException("Not implemented");
    }

    public void playCarSound(Identifier sound, int carIndex, float x, float y, float z, float volume, float pitch) {
        if(!scriptCallsHolder.carSoundManagers.containsKey(carIndex)) return;
        scriptCallsHolder.carSoundManagers.get(carIndex).playSound(sound, new ScriptVector3f(x, y, z), volume, pitch);
    }

    public void playAnnSound(Identifier sound, float volume, float pitch) {
        if(!scriptCallsHolder.carSoundManagers.containsKey(0)) return;

        if(VehicleRidingMovement.isRiding(vehicleExtension.getId())) {
            scriptCallsHolder.carSoundManagers.get(0).playLocalSound(sound, volume, pitch);
        }
    }

    public String getScriptEntryId() {
        return this.scriptEntryId;
    }

    public int[] getMyCars() {
        return this.myCars;
    }

    public @ValueNullable ScriptRenderManager getCarRenderManager(int car) {
        return scriptCallsHolder.carRenderManagers.get(car);
    }

    public @ValueNullable ScriptSoundManager getCarSoundManager(int car) {
        return scriptCallsHolder.carSoundManagers.get(car);
    }

    public @ValueNullable ScriptRenderManager getCarBogieRenderManager(int car, int bogieIndex) {
        List<ScriptRenderManager> bogieRenderManagers = scriptCallsHolder.carBogieRenderers.get(car);
        return bogieRenderManagers == null ? null : bogieIndex >= bogieRenderManagers.size() ? null :  bogieRenderManagers.get(bogieIndex);
    }

    @ApiInternal
    public VehicleScriptCallsHolder.Committer getScriptCallsHolder() {
        return scriptCallsHolder;
    }

    @ApiInternal
    public DataFetchMode getDataFetchMode() {
        return dataFetchMode;
    }

    public void setDataFetchMode(String enumName) {
        this.dataFetchMode = DataFetchMode.valueOf(enumName);
    }

    @Override
    public void resetForNextRun() {
        scriptCallsHolder.reset();
    }

    public enum DataFetchMode {
        SKIP,
        MANDATORY,
        ALL
    }
}
