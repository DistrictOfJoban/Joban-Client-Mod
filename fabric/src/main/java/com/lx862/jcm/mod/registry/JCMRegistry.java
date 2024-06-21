package com.lx862.jcm.mod.registry;

import com.lx862.jcm.mod.Constants;
import org.mtr.mapping.holder.Block;
import org.mtr.mapping.holder.BlockPos;
import org.mtr.mapping.holder.BlockState;
import org.mtr.mapping.holder.ItemSettings;
import org.mtr.mapping.mapper.BlockEntityExtension;
import org.mtr.mapping.registry.*;
import org.mtr.mapping.tool.PacketBufferReceiver;

import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

public class JCMRegistry {

    public static final Registry REGISTRY = new Registry();

    public static BlockRegistryObject registerBlockItem(String id, Supplier<Block> supplier, CreativeModeTabHolder itemGroup) {
        return REGISTRY.registerBlockWithBlockItem(Constants.id(id), supplier, itemGroup);
    }

    public static BlockRegistryObject registerBlock(String id, Supplier<Block> supplier) {
        return REGISTRY.registerBlock(Constants.id(id), supplier);
    }

    public static ItemRegistryObject registerItem(String id, Function<ItemSettings, org.mtr.mapping.holder.Item> callback, CreativeModeTabHolder itemGroup) {
        return REGISTRY.registerItem(Constants.id(id), callback, itemGroup);
    }

    public static <T extends BlockEntityExtension> BlockEntityTypeRegistryObject<T> registerBlockEntity(String id, BiFunction<BlockPos, BlockState, T> constructor, BlockRegistryObject associatedBlock) {
        return REGISTRY.registerBlockEntityType(Constants.id(id), constructor, associatedBlock::get);
    }

    public static void setupPacket() {
        REGISTRY.setupPackets(Constants.id("packet"));
    }

    public static <T extends PacketHandler> void registerPacket(Class<T> classObject, Function<PacketBufferReceiver, T> getInstance) {
        REGISTRY.registerPacket(classObject, getInstance);
    }

    public static void register() {
        Blocks.register();
        BlockEntities.register();
        Items.register();
        Events.register();
        Networking.register();
        REGISTRY.init();
    }
}
