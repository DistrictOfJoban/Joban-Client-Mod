package com.lx862.jcm.mod.registry;

import com.lx862.jcm.mod.JCMClient;
import com.lx862.jcm.mod.data.JCMClientStats;
import com.lx862.jcm.mod.data.JCMServerStats;
import com.lx862.jcm.mod.resources.JCMResourceManager;
import com.lx862.jcm.mod.resources.mcmeta.McMetaManager;

public class Events {
    public static void register() {
        // Start Tick Event for counting tick
        JCMRegistry.REGISTRY.eventRegistry.registerStartServerTick(() -> {
            JCMServerStats.incrementGameTick();
        });
    }

    public static void registerClient() {
        JCMRegistryClient.REGISTRY_CLIENT.eventRegistryClient.registerResourceReloadEvent(JCMResourceManager::reload);

        JCMRegistryClient.REGISTRY_CLIENT.eventRegistryClient.registerStartClientTick(() -> {
            JCMClientStats.incrementGameTick();
            McMetaManager.tick();
            JCMClient.scriptManager.instanceManager.clearDeadInstance();
        });
    }
}
