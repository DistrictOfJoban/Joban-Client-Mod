package com.lx862.jcm.mod.registry;

import com.lx862.jcm.mod.JCMClient;
import com.lx862.jcm.mod.data.JCMServerStats;
import com.lx862.jcm.mod.resources.JCMResourceManager;
import com.lx862.jcm.mod.resources.MTRContentResourceManager;
import com.lx862.jcm.mod.scripting.jcm.JCMScripting;
import com.lx862.jcm.mod.scripting.mtr.MTRScripting;

public class Events {
    public static void register() {
        // Start Tick Event for counting tick
        JCMRegistry.REGISTRY.eventRegistry.registerStartServerTick(JCMServerStats::incrementGameTick);
    }

    public static void registerClient() {
        JCMRegistryClient.REGISTRY_CLIENT.eventRegistryClient.registerResourceReloadEvent(() -> {
            JCMScripting.reset();
            MTRScripting.reset();
            JCMResourceManager.reload();
            MTRContentResourceManager.reload();
        });

        JCMRegistryClient.REGISTRY_CLIENT.eventRegistryClient.registerStartClientTick(() -> {
            JCMClient.getMcMetaManager().tick();
            JCMScripting.tick();
            MTRScripting.tick();
        });
    }
}
