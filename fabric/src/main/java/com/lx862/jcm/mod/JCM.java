package com.lx862.jcm.mod;

import com.lx862.jcm.mod.registry.RegistryHelper;
import com.lx862.jcm.mod.util.JCMLogger;
import org.mtr.mod.Keys;

public class JCM {
    public static void initialize() {
        JCMLogger.info("Joban Client Mod v{}", Constants.MOD_VERSION);
        JCMLogger.info("Hello MTR v{}", Keys.MOD_VERSION);
        RegistryHelper.register();
    }
}