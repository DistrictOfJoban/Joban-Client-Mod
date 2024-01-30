package com.lx862.jcm.mod.registry;

import com.lx862.jcm.mod.util.JCMLogger;
import org.mtr.mapping.holder.RenderLayer;
import org.mtr.mapping.registry.BlockRegistryObject;

public final class RenderLayers {
    public static void registerClient() {
        JCMLogger.debug("Registering RenderLayers...");

        registerRenderLayers(RenderLayer.getCutout(),
                Blocks.BUFFER_STOP,
                Blocks.BUTTERFLY_LIGHT,
                Blocks.CIRCLE_WALL_1,
                Blocks.CIRCLE_WALL_2,
                Blocks.CIRCLE_WALL_3,
                Blocks.CIRCLE_WALL_4,
                Blocks.CIRCLE_WALL_5,
                Blocks.CIRCLE_WALL_6,
                Blocks.FIRE_ALARM,
                Blocks.FARE_SAVER,
                Blocks.KCR_STATION_NAME_SIGN,
                Blocks.KCR_EMG_STOP_SIGN,
                Blocks.MTR_ENQUIRY_MACHINE,
                Blocks.RV_ENQUIRY_MACHINE,
                Blocks.SUBSIDY_MACHINE,
                Blocks.SPOT_LAMP,
                Blocks.HELPLINE_1,
                Blocks.HELPLINE_2,
                Blocks.HELPLINE_STANDING,
                Blocks.HELPLINE_STANDING_EAL,
                Blocks.SIL_EMG_STOP_BUTTON,
                Blocks.STATION_NAME_STANDING,
                Blocks.TML_EMG_STOP_BUTTON,
                Blocks.MTR_TRESPASS_SIGN,
                Blocks.WATER_MACHINE
        );
    }

    private static void registerRenderLayers(RenderLayer renderLayer, BlockRegistryObject... blocks) {
        for (BlockRegistryObject blockRegistryObject : blocks) {
            RegistryHelperClient.registerBlockRenderType(renderLayer, blockRegistryObject);
        }
    }
}
