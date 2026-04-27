package com.lx862.mtrscripting.mod.impl.mtr.vehicle;

import com.lx862.mtrscripting.core.annotation.ApiInternal;
import com.lx862.mtrscripting.core.util.render.ScriptRenderManager;
import com.lx862.mtrscripting.core.util.sound.ScriptSoundManager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ApiInternal
public class VehicleScriptCallsHolder {
    public final Map<Integer, ScriptRenderManager> carRenderManagers;
    public final Map<Integer, List<ScriptRenderManager>> carBogieRenderers;
    public final Map<Integer, ScriptSoundManager> carSoundManagers;

    private VehicleScriptCallsHolder() {
        this.carRenderManagers = new HashMap<>();
        this.carBogieRenderers = new HashMap<>();
        this.carSoundManagers = new HashMap<>();
    }

    /**
     * An instance of VehicleScriptCallsHolder for scripts to commit calls on. (Directly or indirectly)
     */
    public static class Committer extends VehicleScriptCallsHolder {
        public void reset() {
            carRenderManagers.values().forEach(ScriptRenderManager::reset);
            carBogieRenderers.values().forEach(e -> e.forEach(ScriptRenderManager::reset));
            carSoundManagers.values().forEach(ScriptSoundManager::reset);
        }
    }

    /**
     * An instance of VehicleScriptCallsHolder representing the captured calls after the script finish execution.
     * Rendering on the main thread should follow the calls in this instance.
     */
    public static class Captured extends VehicleScriptCallsHolder {
        public Captured() {
            super();
        }

        public void capture(Committer committer) {
            committer.carRenderManagers.forEach((carIndex, renderManager) -> {
                Captured.this.carRenderManagers.put(carIndex, renderManager.copy());
            });
            committer.carBogieRenderers.forEach((carIndex, renderManagers) -> {
                Captured.this.carBogieRenderers.put(carIndex, renderManagers.stream().map(ScriptRenderManager::copy).toList());
            });
            committer.carSoundManagers.forEach((carIndex, sm) -> {
                if(!this.carSoundManagers.containsKey(carIndex)) {
                    this.carSoundManagers.put(carIndex, sm);
                } else {
                    // HACK: We use different injection point for commiting and initiating vehicle script execution.
                    // Sometimes the script would be executed multiple times without committing, resulting in previous unexecuted sound calls being dropped.
                    // Should look into it at some point, though for now let's just keep a list of all historic sound calls.
                    this.carSoundManagers.get(carIndex).addCallsFrom(sm);
                }
            });
        }
    }
}
