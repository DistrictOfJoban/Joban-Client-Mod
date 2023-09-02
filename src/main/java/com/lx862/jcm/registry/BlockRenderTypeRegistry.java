package com.lx862.jcm.registry;

import com.lx862.jcm.util.Logger;
import org.mtr.mapping.holder.RenderLayer;
import org.mtr.mapping.registry.BlockRegistryObject;
import org.mtr.mapping.registry.RegistryClient;

public final class BlockRenderTypeRegistry {

    public static void registerClient() {
        Logger.info("Registering RenderType...");
        registerBlockRenderType(RenderLayer.getCutout(),
                BlockRegistry.SPOT_LAMP,
                BlockRegistry.SIL_EMG_STOP_BUTTON,
                BlockRegistry.TML_EMG_STOP_BUTTON,
                BlockRegistry.MTR_TRESPASS_SIGN,
                BlockRegistry.WATER_MACHINE
        );
    }

    private static void registerBlockRenderType(RenderLayer renderLayer, BlockRegistryObject... blocks) {
        for(BlockRegistryObject blockRegistryObject : blocks) {
            RegistryClient.registerBlockRenderType(renderLayer, blockRegistryObject);
        }
    }
}
