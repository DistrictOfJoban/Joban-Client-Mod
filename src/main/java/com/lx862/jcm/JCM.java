package com.lx862.jcm;

import com.lx862.jcm.registry.Registry;
import com.lx862.jcm.util.Logger;

public class JCM {
    public static void initialize() {
        Logger.info("Joban Client Mod v{}", Constants.MOD_VERSION);
        Registry.register();
    }
}