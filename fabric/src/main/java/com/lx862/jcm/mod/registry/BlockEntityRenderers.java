package com.lx862.jcm.mod.registry;

import com.lx862.jcm.mod.render.block.*;
import com.lx862.jcm.mod.util.JCMLogger;

public final class BlockEntityRenderers {
    public static void registerClient() {
        JCMLogger.debug("Registering Block Entity Renderer...");
        JCMRegistryClient.registerBlockEntityRenderer(BlockEntities.APG_DOOR_DRL, (dispatcher) -> new RenderDRLAPGDoor<>(dispatcher, 2));
        JCMRegistryClient.registerBlockEntityRenderer(BlockEntities.BUTTERFLY_LIGHT, ButterflyLightRenderer::new);
        JCMRegistryClient.registerBlockEntityRenderer(BlockEntities.DEPARTURE_TIMER, DepartureTimerRenderer::new);
        JCMRegistryClient.registerBlockEntityRenderer(BlockEntities.FARE_SAVER, FareSaverRenderer::new);
        JCMRegistryClient.registerBlockEntityRenderer(BlockEntities.KCR_STATION_NAME_SIGN, KCRStationNameSignRenderer::new);
        JCMRegistryClient.registerBlockEntityRenderer(BlockEntities.KCR_STATION_NAME_SIGN_STATION_COLOR, KCRStationNameSignRenderer::new);
        JCMRegistryClient.registerBlockEntityRenderer(BlockEntities.SIGNAL_LIGHT_INVERTED_RED_ABOVE, (dispatcher) -> new SignalBlockInvertedRenderer<>(dispatcher, 0xFF0000FF, true));
        JCMRegistryClient.registerBlockEntityRenderer(BlockEntities.SIGNAL_LIGHT_INVERTED_RED_BELOW, (dispatcher) -> new SignalBlockInvertedRenderer<>(dispatcher, 0xFF00FF00, false));
        JCMRegistryClient.registerBlockEntityRenderer(BlockEntities.SIGNAL_LIGHT_RED_BELOW, (dispatcher) -> new StaticSignalLightRenderer<>(dispatcher, 0xFFFF0000, false));
        JCMRegistryClient.registerBlockEntityRenderer(BlockEntities.SIGNAL_LIGHT_RED_TOP, (dispatcher) -> new StaticSignalLightRenderer<>(dispatcher, 0xFFFF0000, true));
        JCMRegistryClient.registerBlockEntityRenderer(BlockEntities.SIGNAL_LIGHT_BLUE, (dispatcher) -> new StaticSignalLightRenderer<>(dispatcher, 0xFF0000FF, true));
        JCMRegistryClient.registerBlockEntityRenderer(BlockEntities.SIGNAL_LIGHT_GREEN, (dispatcher) -> new StaticSignalLightRenderer<>(dispatcher, 0xFF00FF00, false));
        JCMRegistryClient.registerBlockEntityRenderer(BlockEntities.PIDS_1A, PIDS1ARenderer::new);
        JCMRegistryClient.registerBlockEntityRenderer(BlockEntities.PIDS_PROJECTOR, PIDSProjectorRenderer::new);
        JCMRegistryClient.registerBlockEntityRenderer(BlockEntities.LCD_PIDS, LCDPIDSRenderer::new);
        JCMRegistryClient.registerBlockEntityRenderer(BlockEntities.RV_PIDS, RVPIDSRenderer::new);
        JCMRegistryClient.registerBlockEntityRenderer(BlockEntities.RV_PIDS_SIL_1, RVPIDSSILRenderer::new);
        JCMRegistryClient.registerBlockEntityRenderer(BlockEntities.RV_PIDS_SIL_2, RVPIDSSILRenderer::new);
        JCMRegistryClient.registerBlockEntityRenderer(BlockEntities.STATION_NAME_STANDING, StationNameStandingRenderer::new);
    }
}
