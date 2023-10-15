package com.lx862.jcm.mod.registry;

import com.lx862.jcm.mod.render.block.DepartureTimerRenderer;
import com.lx862.jcm.mod.render.block.FareSaverRenderer;
import com.lx862.jcm.mod.render.block.KCRStationNameSignRenderer;
import com.lx862.jcm.mod.util.JCMLogger;
import org.mtr.mapping.registry.RegistryClient;

public final class BlockEntityRenderers {
    public static void registerClient() {
        JCMLogger.debug("Registering block entity renderer...");
        RegistryClient.registerBlockEntityRenderer(BlockEntities.DEPARTURE_TIMER, DepartureTimerRenderer::new);
        RegistryClient.registerBlockEntityRenderer(BlockEntities.FARE_SAVER, FareSaverRenderer::new);
        RegistryClient.registerBlockEntityRenderer(BlockEntities.KCR_STATION_NAME_SIGN, KCRStationNameSignRenderer::new);
    }
}
