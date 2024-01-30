package com.lx862.jcm.mod.registry;

import com.lx862.jcm.mod.Constants;
import com.lx862.jcm.mod.JCMClient;
import org.mtr.mapping.holder.*;
import org.mtr.mapping.mapper.BlockEntityExtension;
import org.mtr.mapping.mapper.BlockEntityRenderer;
import org.mtr.mapping.registry.*;

import java.util.function.Function;

public class RegistryHelperClient {
    public static void setupPacketClient() {
        JCMClient.REGISTRY_CLIENT.setupPackets(new Identifier(Constants.MOD_ID, "packet"));
    }

    public static <T extends BlockEntityTypeRegistryObject<U>, U extends BlockEntityExtension> void registerBlockEntityRenderer(T blockEntityType, Function<BlockEntityRenderer.Argument, BlockEntityRenderer<U>> rendererInstance) {
        JCMClient.REGISTRY_CLIENT.registerBlockEntityRenderer(blockEntityType, rendererInstance);
    }

    public static void registerBlockRenderType(RenderLayer renderLayer, BlockRegistryObject block) {
        JCMClient.REGISTRY_CLIENT.registerBlockRenderType(renderLayer, block);
    }

    public static void register() {
        RenderLayers.registerClient();
        BlockEntityRenderers.registerClient();
        Networking.registerClient();
        JCMClient.REGISTRY_CLIENT.init();
    }
}
