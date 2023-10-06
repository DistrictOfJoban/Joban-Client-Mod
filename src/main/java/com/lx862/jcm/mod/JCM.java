package com.lx862.jcm.mod;

import com.lx862.jcm.mod.registry.Registry;
import com.lx862.jcm.mod.util.Logger;

public class JCM {
    public static void initialize() {
        Logger.info("Joban Client Mod v{}", Constants.MOD_VERSION);
        Registry.register();
    }
}