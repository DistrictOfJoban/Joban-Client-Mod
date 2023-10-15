package com.lx862.jcm.mod;

import com.lx862.jcm.mod.config.ClientConfig;
import com.lx862.jcm.mod.registry.Registry;

public class JCMClient {
    public static void initializeClient() {
        ClientConfig.readFile();
        Registry.registerClient();
    }
}