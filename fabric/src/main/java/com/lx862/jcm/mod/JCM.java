package com.lx862.jcm.mod;

import com.lx862.jcm.mod.registry.RegistryHelper;
import com.lx862.jcm.mod.util.JCMLogger;
import org.mtr.mod.Keys;

public class JCM {
    public static void initialize() {
        JCMLogger.info("Joban Client Mod v{}", Constants.MOD_VERSION);

        try {
            JCMLogger.info("Hello MTR {}", Keys.class.getField("MOD_VERSION").get(null));
        } catch (Exception e) {
            JCMLogger.warn("Cannot obtain MTR Version, countdown to disaster...");
        }
        RegistryHelper.register();
    }
}