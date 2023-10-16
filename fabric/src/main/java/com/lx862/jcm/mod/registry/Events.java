package com.lx862.jcm.mod.registry;

import com.lx862.jcm.mod.data.JCMStats;

public class Events {
    public static void register() {
        // Start Tick Event for counting tick
        org.mtr.mapping.registry.EventRegistry.registerStartServerTick(() -> {
            JCMStats.incrementGameTick();
        });
    }
}
