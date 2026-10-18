package com.lx862.mtrscripting.mod.impl.mtr.vehicle;

import com.lx862.mtrscripting.core.annotation.ApiInternal;
import com.lx862.mtrscripting.core.util.render.ScriptRenderManager;
import com.lx862.mtrscripting.core.util.sound.ScriptSoundManager;

import java.util.ArrayList;
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

    private static void resetAll(Map<Integer, ScriptRenderManager> managers) {
        /* entrySet loop: one iterator (no lambda/view allocations, no key
           array snapshot — toArray would allocate per call). */
        for (Map.Entry<Integer, ScriptRenderManager> entry : managers.entrySet()) {
            entry.getValue().reset();
        }
    }

    /**
     * An instance of VehicleScriptCallsHolder for scripts to commit calls on. (Directly or indirectly)
     */
    public static class Committer extends VehicleScriptCallsHolder {
        public void reset() {
            resetAll(carRenderManagers);
            for (Map.Entry<Integer, List<ScriptRenderManager>> entry : carBogieRenderers.entrySet()) {
                final List<ScriptRenderManager> bogies = entry.getValue();
                for (int i = 0; i < bogies.size(); i++) {
                    bogies.get(i).reset();
                }
            }
            for (Map.Entry<Integer, ScriptSoundManager> entry : carSoundManagers.entrySet()) {
                entry.getValue().reset();
            }
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
            /* Zero-copy capture: render managers swap their backing lists
               (takeFrom) instead of being copy()-d per car per frame — the
               old path allocated a new ScriptRenderManager plus a full list
               copy for every car of every scripted vehicle every frame, and
               the copy was discarded right after the replay. Stale cars
               (context rebuilt with fewer cars) are released and dropped;
               new cars appear as swap targets created exactly once. */
            for (Map.Entry<Integer, ScriptRenderManager> entry : committer.carRenderManagers.entrySet()) {
                final int car = entry.getKey();
                final ScriptRenderManager committerManager = entry.getValue();
                ScriptRenderManager capturedManager = this.carRenderManagers.get(car);
                if (capturedManager == null) {
                    capturedManager = new ScriptRenderManager();
                    this.carRenderManagers.put(car, capturedManager);
                }
                capturedManager.takeFrom(committerManager);
            }
            this.carRenderManagers.keySet().retainAll(committer.carRenderManagers.keySet());

            for (Map.Entry<Integer, List<ScriptRenderManager>> entry : committer.carBogieRenderers.entrySet()) {
                final int car = entry.getKey();
                final List<ScriptRenderManager> committerBogies = entry.getValue();
                List<ScriptRenderManager> capturedBogies = this.carBogieRenderers.get(car);
                if (capturedBogies == null) {
                    capturedBogies = new ArrayList<>(committerBogies.size());
                    this.carBogieRenderers.put(car, capturedBogies);
                }
                if (capturedBogies.size() < committerBogies.size()) {
                    for (int i = capturedBogies.size(); i < committerBogies.size(); i++) {
                        capturedBogies.add(new ScriptRenderManager());
                    }
                } else {
                    for (int i = capturedBogies.size() - 1; i >= committerBogies.size(); i--) {
                        capturedBogies.remove(i).reset();
                    }
                }
                for (int i = 0; i < committerBogies.size(); i++) {
                    capturedBogies.get(i).takeFrom(committerBogies.get(i));
                }
            }
            this.carBogieRenderers.keySet().retainAll(committer.carBogieRenderers.keySet());

            /* Sound managers stay one-shot/accumulating (upstream semantics:
               unexecuted sound calls are kept). The empty frame — the common
               case — no longer allocates a copy per car. */
            for (Map.Entry<Integer, ScriptSoundManager> entry : committer.carSoundManagers.entrySet()) {
                final ScriptSoundManager sm = entry.getValue();
                if (sm.isEmpty()) continue;
                ScriptSoundManager captured = this.carSoundManagers.get(entry.getKey());
                if (captured == null) {
                    captured = new ScriptSoundManager();
                    this.carSoundManagers.put(entry.getKey(), captured);
                }
                captured.addCallsFrom(sm.copy());
            }
        }
    }
}
