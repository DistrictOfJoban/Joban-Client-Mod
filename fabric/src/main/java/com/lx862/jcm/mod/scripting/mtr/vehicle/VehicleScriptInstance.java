package com.lx862.jcm.mod.scripting.mtr.vehicle;

import com.lx862.jcm.mod.scripting.mtr.sound.ScriptSoundManager;
import com.lx862.jcm.mod.scripting.mtr.sound.SoundCall;
import com.lx862.mtrscripting.core.ParsedScript;
import com.lx862.mtrscripting.core.ScriptInstance;
import org.mtr.mod.client.MinecraftClientData;
import org.mtr.mod.data.VehicleExtension;

import java.util.ArrayList;
import java.util.List;

public class VehicleScriptInstance extends ScriptInstance<VehicleWrapper> {
    private final VehicleExtension vehicleExtension;
    public final List<VehicleModelDrawCall> carModelDrawCalls;
    private final ScriptSoundManager scriptSoundManager;

    public VehicleScriptInstance(VehicleScriptContext context, VehicleExtension vehicleExtension, ParsedScript script) {
        super(context, script);
        this.vehicleExtension = vehicleExtension;
        this.carModelDrawCalls = new ArrayList<>();
        this.scriptSoundManager = new ScriptSoundManager();
    }

    public void setCarModelDrawCalls(List<VehicleModelDrawCall> calls) {
        this.carModelDrawCalls.clear();
        this.carModelDrawCalls.addAll(calls);
    }

    public void updateSound(ScriptSoundManager soundManager) {
        scriptSoundManager.updateSoundCalls(soundManager);
    }

    public ScriptSoundManager getSoundManager() {
        return this.scriptSoundManager;
    }

    public boolean shouldInvalidate() {
        return !MinecraftClientData.getInstance().vehicles.contains(vehicleExtension);
    }
}
