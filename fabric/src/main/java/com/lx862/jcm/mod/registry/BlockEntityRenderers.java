package com.lx862.jcm.mod.registry;

import com.lx862.jcm.mod.render.block.*;
import com.lx862.jcm.mod.util.JCMLogger;

public final class BlockEntityRenderers {
    public static void registerClient() {
        JCMLogger.debug("Registering Block Entity Renderer...");
        RegistryHelperClient.registerBlockEntityRenderer(BlockEntities.DEPARTURE_TIMER, DepartureTimerRenderer::new);
        RegistryHelperClient.registerBlockEntityRenderer(BlockEntities.FARE_SAVER, FareSaverRenderer::new);
        RegistryHelperClient.registerBlockEntityRenderer(BlockEntities.KCR_STATION_NAME_SIGN, KCRStationNameSignRenderer::new);
        RegistryHelperClient.registerBlockEntityRenderer(BlockEntities.RV_PIDS, RVPIDSRenderer::new);
        RegistryHelperClient.registerBlockEntityRenderer(BlockEntities.LCD_PIDS, LCDPIDSRenderer::new);
        RegistryHelperClient.registerBlockEntityRenderer(BlockEntities.STATION_NAME_STANDING, StationNameStandingRenderer::new);
    }
}
