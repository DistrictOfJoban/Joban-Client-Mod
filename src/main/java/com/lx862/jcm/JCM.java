package com.lx862.jcm;

import com.lx862.jcm.registry.BlockRegistry;
import com.lx862.jcm.registry.ItemRegistry;
import com.lx862.jcm.util.Logger;
import net.fabricmc.api.ModInitializer;

public class JCM implements ModInitializer {
	@Override
	public void onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.

		Logger.info("Hello Fabric world!");
		BlockRegistry.register();
		ItemRegistry.register();
	}
}