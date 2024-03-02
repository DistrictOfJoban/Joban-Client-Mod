package com.lx862.jcm.mod.registry;

import com.lx862.jcm.mod.data.JCMClientStats;
import com.lx862.jcm.mod.data.JCMServerStats;
import com.lx862.jcm.mod.resources.JCMResourceManager;
import com.lx862.jcm.mod.resources.mcmeta.McMetaManager;
import org.mtr.mapping.registry.EventRegistryClient;

public class Events {
    public static void register() {
        // Start Tick Event for counting tick
        org.mtr.mapping.registry.EventRegistry.registerStartServerTick(() -> {
            JCMServerStats.incrementGameTick();
        });
    }

    public static void registerClient() {
        EventRegistryClient.registerResourceReloadEvent(JCMResourceManager::reload);

        EventRegistryClient.registerStartClientTick(() -> {
            JCMClientStats.incrementGameTick();
            McMetaManager.tick();
        });
    }
}
