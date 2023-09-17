package com.lx862.jcm.entrypointtemp;

import com.lx862.jcm.JCMClient;
import com.lx862.jcm.config.ClientConfig;
import com.lx862.jcm.registry.Registry;
import net.fabricmc.api.ClientModInitializer;

/**
 * TODO: Move this to Fabric and Forge subproject
 */
public class MainClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        JCMClient.onInitializeClient();
    }
}