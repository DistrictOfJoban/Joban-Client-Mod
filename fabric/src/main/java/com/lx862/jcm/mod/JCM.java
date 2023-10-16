package com.lx862.jcm.mod;

import com.lx862.jcm.mod.registry.Registry;
import com.lx862.jcm.mod.util.JCMLogger;

public class JCM {
    public static void initialize() {
        JCMLogger.info("Joban Client Mod v{}", Constants.MOD_VERSION);
        Registry.register();
    }
}