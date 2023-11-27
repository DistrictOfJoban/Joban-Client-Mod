package com.lx862.jcm.mod.registry;

import com.lx862.jcm.mod.render.block.*;
import com.lx862.jcm.mod.util.JCMLogger;
import org.mtr.mapping.registry.RegistryClient;

public final class BlockEntityRenderers {
    public static void registerClient() {
        JCMLogger.debug("Registering Block Entity Renderer...");
        RegistryClient.registerBlockEntityRenderer(BlockEntities.DEPARTURE_TIMER, DepartureTimerRenderer::new);
        RegistryClient.registerBlockEntityRenderer(BlockEntities.FARE_SAVER, FareSaverRenderer::new);
        RegistryClient.registerBlockEntityRenderer(BlockEntities.KCR_STATION_NAME_SIGN, KCRStationNameSignRenderer::new);
        RegistryClient.registerBlockEntityRenderer(BlockEntities.RV_PIDS, RVPIDSRenderer::new);
        RegistryClient.registerBlockEntityRenderer(BlockEntities.LCD_PIDS, LCDPIDSRenderer::new);
        RegistryClient.registerBlockEntityRenderer(BlockEntities.STATION_NAME_STANDING, StationNameStandingRenderer::new);
    }
}
