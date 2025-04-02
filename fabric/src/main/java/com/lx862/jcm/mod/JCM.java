package com.lx862.jcm.mod;

import com.lx862.jcm.mod.registry.JCMRegistry;
import com.lx862.jcm.mod.util.JCMLogger;
import org.mtr.mod.Keys;

public class JCM {
    public static void initialize() {
        try {
            JCMLogger.info("Joban Client Mod v{} @ MTR {}", Constants.MOD_VERSION, Keys.class.getField("MOD_VERSION").get(null));
        } catch (Exception e) {
            JCMLogger.warn("Cannot obtain MTR Version, countdown to disaster...");
        }
        JCMRegistry.register();
    }
}