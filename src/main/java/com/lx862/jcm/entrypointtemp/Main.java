package com.lx862.jcm.entrypointtemp;

import com.lx862.jcm.JCM;
import net.fabricmc.api.ModInitializer;

/**
 * TODO: Move this to Fabric and Forge subproject
 */
public class Main implements ModInitializer {
    @Override
    public void onInitialize() {
        // This code runs as soon as Minecraft is in a mod-load-ready state.
        // However, some things (like resources) may still be uninitialized.
        // Proceed with mild caution.

        JCM.onInitialize();
    }
}