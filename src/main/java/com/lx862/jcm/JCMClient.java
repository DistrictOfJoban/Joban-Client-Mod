package com.lx862.jcm;

import com.lx862.jcm.config.ClientConfig;
import com.lx862.jcm.registry.Registry;

public class JCMClient {
    public static void onInitializeClient() {
        Registry.registerClient();
        ClientConfig.readFile();
    }
}