package com.lx862.jcm.mod.registry;

import com.lx862.jcm.mod.render.block.DepartureTimerRenderer;
import com.lx862.jcm.mod.util.JCMLogger;
import org.mtr.mapping.registry.RegistryClient;

public final class BlockEntityRenderers {
    public static void registerClient() {
        JCMLogger.info("Registering block entity renderer...");
        RegistryClient.registerBlockEntityRenderer(BlockEntities.DEPARTURE_TIMER, DepartureTimerRenderer::new);
    }
}
