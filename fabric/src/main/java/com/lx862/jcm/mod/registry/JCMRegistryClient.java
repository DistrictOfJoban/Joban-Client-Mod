package com.lx862.jcm.mod.registry;

import com.lx862.jcm.mod.Constants;
import com.lx862.jcm.mod.data.pids.PIDSManager;
import org.mtr.mapping.holder.RenderLayer;
import org.mtr.mapping.mapper.BlockEntityExtension;
import org.mtr.mapping.mapper.BlockEntityRenderer;
import org.mtr.mapping.registry.BlockEntityTypeRegistryObject;
import org.mtr.mapping.registry.BlockRegistryObject;
import org.mtr.mapping.registry.RegistryClient;
import org.mtr.mod.InitClient;

import java.util.function.Function;

public class JCMRegistryClient {
    public static final RegistryClient REGISTRY_CLIENT = new RegistryClient(JCMRegistry.REGISTRY);

    public static void setupPacketClient() {
        REGISTRY_CLIENT.setupPackets(Constants.id("packet"));
    }

    public static <T extends BlockEntityTypeRegistryObject<U>, U extends BlockEntityExtension> void registerBlockEntityRenderer(T blockEntityType, Function<BlockEntityRenderer.Argument, BlockEntityRenderer<U>> rendererInstance) {
        REGISTRY_CLIENT.registerBlockEntityRenderer(blockEntityType, rendererInstance);
    }

    public static void registerBlockRenderType(RenderLayer renderLayer, BlockRegistryObject... blocks) {
        for(BlockRegistryObject block : blocks) {
            REGISTRY_CLIENT.registerBlockRenderType(renderLayer, block);
        }
    }

    public static void registerStationColoredBlock(BlockRegistryObject... blocks) {
        REGISTRY_CLIENT.registerBlockColors((blockState, blockRenderView, blockPos, i) -> InitClient.getStationColor(blockPos), blocks);
    }

    public static void register() {
        Events.registerClient();
        Blocks.registerClient();
        BlockEntityRenderers.registerClient();
        Networking.registerClient();
        PIDSManager.registerScripting();
        REGISTRY_CLIENT.init();
    }
}
