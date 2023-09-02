package com.lx862.jcm;

import com.lx862.jcm.registry.BlockRegistry;
import com.lx862.jcm.registry.ItemGroupRegistry;
import net.fabricmc.api.ClientModInitializer;

public class JCMClient implements ClientModInitializer {

	@Override
	public void onInitializeClient() {
		BlockRegistry.registerClient();
		ItemGroupRegistry.registerClient();
	}
}