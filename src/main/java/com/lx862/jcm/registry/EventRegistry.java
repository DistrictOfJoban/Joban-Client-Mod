package com.lx862.jcm.registry;

import com.lx862.jcm.data.JCMStats;

public class EventRegistry {
    public static void register() {
        // Start Tick Event for counting tick
        org.mtr.mapping.registry.EventRegistry.registerStartServerTick(() -> {
            JCMStats.incrementGameTick();
        });
    }
}
