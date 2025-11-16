package com.lx862.jcm.mod.scripting.mtr.vehicle;

import com.lx862.jcm.mod.scripting.mtr.render.ScriptRenderManager;
import com.lx862.jcm.mod.scripting.mtr.sound.ScriptSoundManager;
import com.lx862.mtrscripting.core.ParsedScript;
import com.lx862.mtrscripting.core.ScriptInstance;
import org.mtr.mapping.holder.MinecraftClient;
import org.mtr.mod.client.MinecraftClientData;
import org.mtr.mod.data.VehicleExtension;

public class VehicleScriptInstance extends ScriptInstance<VehicleWrapper> {
    private final VehicleExtension vehicleExtension;
    public final ScriptRenderManager[] renderManagers;
    public final ScriptSoundManager[] soundManagers;

    public VehicleScriptInstance(VehicleScriptContext context, VehicleExtension vehicleExtension, ParsedScript script) {
        super(context, script);
        this.vehicleExtension = vehicleExtension;
        this.renderManagers = new ScriptRenderManager[vehicleExtension.vehicleExtraData.immutableVehicleCars.size()];
        this.soundManagers = new ScriptSoundManager[vehicleExtension.vehicleExtraData.immutableVehicleCars.size()];
    }

    public void saveRenderCalls(ScriptRenderManager[] renderManagers) {
        for(int i = 0; i < renderManagers.length; i++) {
            this.renderManagers[i] = renderManagers[i].copy();
        }
    }

    public void saveSoundCalls(ScriptSoundManager[] soundManagers) {
        for(int i = 0; i < soundManagers.length; i++) {
            this.soundManagers[i] = soundManagers[i].copy();
        }
    }

    public boolean shouldInvalidate() {
        boolean notInGame = MinecraftClient.getInstance().getWorldMapped() == null;
        return notInGame || !MinecraftClientData.getInstance().vehicles.contains(vehicleExtension);
    }
}
