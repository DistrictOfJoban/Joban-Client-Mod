package com.lx862.jcm.mod.scripting.mtr.vehicle;

import com.lx862.jcm.mod.scripting.mtr.render.ScriptRenderManager;
import com.lx862.jcm.mod.scripting.mtr.sound.ScriptSoundManager;
import com.lx862.mtrscripting.core.ParsedScript;
import com.lx862.mtrscripting.core.ScriptInstance;
import org.mtr.mapping.holder.MinecraftClient;
import org.mtr.mod.client.MinecraftClientData;
import org.mtr.mod.data.VehicleExtension;

import java.util.HashMap;
import java.util.Map;

public class VehicleScriptInstance extends ScriptInstance<VehicleWrapper> {
    private final VehicleExtension vehicleExtension;
    public final Map<Integer, ScriptRenderManager> renderManagers;
    public final Map<Integer, ScriptSoundManager> soundManagers;

    public VehicleScriptInstance(VehicleScriptContext context, VehicleExtension vehicleExtension, ParsedScript script) {
        super(context, script);
        this.vehicleExtension = vehicleExtension;
        this.renderManagers = new HashMap<>();
        this.soundManagers = new HashMap<>();
    }

    public void captureRenderCalls(Map<Integer, ScriptRenderManager> renderManagers) {
        for(Map.Entry<Integer, ScriptRenderManager> renderManager : renderManagers.entrySet()) {
            this.renderManagers.put(renderManager.getKey(), renderManager.getValue().copy());
        }
    }

    public void captureSoundCalls(Map<Integer, ScriptSoundManager> soundManagers) {
        for(Map.Entry<Integer, ScriptSoundManager> soundManager : soundManagers.entrySet()) {
            ScriptSoundManager sm = soundManager.getValue().copy();
            if(!this.soundManagers.containsKey(soundManager.getKey())) {
                this.soundManagers.put(soundManager.getKey(), sm);
            } else {
                // HACK: We use different injection point for commiting and initiating vehicle script execution.
                // Sometimes the script would be executed multiple times without committing, resulting in previous unexecuted sound calls being dropped.
                // Should look into it at some point, though for now let's just keep a list of all historic sound calls.
                this.soundManagers.get(soundManager.getKey()).addCallsFrom(sm);
            }
        }
    }

    public boolean shouldInvalidate() {
        boolean notInGame = MinecraftClient.getInstance().getWorldMapped() == null;
        return notInGame || MinecraftClientData.getInstance().vehicles.stream().noneMatch(v -> v.getId() == vehicleExtension.getId());
    }
}
