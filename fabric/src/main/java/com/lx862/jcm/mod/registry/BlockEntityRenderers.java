package com.lx862.jcm.mod.registry;

import com.lx862.jcm.mod.render.block.*;
import com.lx862.jcm.mod.util.JCMLogger;
import org.mtr.mod.render.RenderPIDS;

public final class BlockEntityRenderers {
    public static void registerClient() {
        JCMLogger.debug("Registering Block Entity Renderer...");
        RegistryHelperClient.registerBlockEntityRenderer(BlockEntities.APG_DOOR_DRL, (dispatcher) -> new RenderDRLAPGDoor<>(dispatcher, 2));
        RegistryHelperClient.registerBlockEntityRenderer(BlockEntities.BUTTERFLY_LIGHT, ButterflyLightRenderer::new);
        RegistryHelperClient.registerBlockEntityRenderer(BlockEntities.DEPARTURE_TIMER, DepartureTimerRenderer::new);
        RegistryHelperClient.registerBlockEntityRenderer(BlockEntities.FARE_SAVER, FareSaverRenderer::new);
        RegistryHelperClient.registerBlockEntityRenderer(BlockEntities.KCR_STATION_NAME_SIGN, KCRStationNameSignRenderer::new);
        RegistryHelperClient.registerBlockEntityRenderer(BlockEntities.KCR_STATION_NAME_SIGN_STATION_COLOR, KCRStationNameSignRenderer::new);
        RegistryHelperClient.registerBlockEntityRenderer(BlockEntities.SIGNAL_LIGHT_INVERTED_RED_ABOVE, (dispatcher) -> new SignalBlockInvertedRenderer<>(dispatcher, 0xFFFF0000, true, true));
        RegistryHelperClient.registerBlockEntityRenderer(BlockEntities.SIGNAL_LIGHT_INVERTED_RED_BELOW, (dispatcher) -> new SignalBlockInvertedRenderer<>(dispatcher, 0xFFFF0000, true, false));
        RegistryHelperClient.registerBlockEntityRenderer(BlockEntities.SIGNAL_LIGHT_RED_BELOW, (dispatcher) -> new StaticSignalLightRenderer(dispatcher, 0xFFFF0000, false));
        RegistryHelperClient.registerBlockEntityRenderer(BlockEntities.SIGNAL_LIGHT_RED_TOP, (dispatcher) -> new StaticSignalLightRenderer(dispatcher, 0xFFFF0000, true));
        RegistryHelperClient.registerBlockEntityRenderer(BlockEntities.SIGNAL_LIGHT_BLUE, (dispatcher) -> new StaticSignalLightRenderer(dispatcher, 0xFF0000FF, true));
        RegistryHelperClient.registerBlockEntityRenderer(BlockEntities.SIGNAL_LIGHT_GREEN, (dispatcher) -> new StaticSignalLightRenderer(dispatcher, 0xFF00FF00, false));
        RegistryHelperClient.registerBlockEntityRenderer(BlockEntities.PIDS_1A, (dispatcher) -> new RenderPIDS<>(dispatcher, 1, 9.75F, 6, 8.85F, 30, true, 1));
        RegistryHelperClient.registerBlockEntityRenderer(BlockEntities.RV_PIDS, RVPIDSRenderer::new);
        RegistryHelperClient.registerBlockEntityRenderer(BlockEntities.LCD_PIDS, LCDPIDSRenderer::new);
        RegistryHelperClient.registerBlockEntityRenderer(BlockEntities.STATION_NAME_STANDING, StationNameStandingRenderer::new);
    }
}
