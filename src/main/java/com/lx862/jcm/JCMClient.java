package com.lx862.jcm;

import com.lx862.jcm.config.ClientConfig;
import com.lx862.jcm.registry.Registry;
import net.fabricmc.api.ClientModInitializer;

public class JCMClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        Registry.registerClient();
        ClientConfig.readFile();
    }
}