package com.lx862.jcm.mod.registry;

import com.lx862.jcm.mod.data.JCMStats;
import com.lx862.jcm.mod.resources.mcmeta.McMetaManager;

public class Events {
    public static void register() {
        // Start Tick Event for counting tick
        org.mtr.mapping.registry.EventRegistry.registerStartServerTick(() -> {
            JCMStats.incrementGameTick();
            // TODO: Does this work on client in multiplayer?
            McMetaManager.tick();
        });
    }
}
