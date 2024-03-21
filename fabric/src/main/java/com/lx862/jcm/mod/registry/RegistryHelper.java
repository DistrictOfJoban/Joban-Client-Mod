package com.lx862.jcm.mod.registry;

import com.lx862.jcm.mod.Constants;
import org.mtr.mapping.holder.*;
import org.mtr.mapping.mapper.BlockEntityExtension;
import org.mtr.mapping.registry.*;
import org.mtr.mapping.tool.PacketBufferReceiver;

import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

public class RegistryHelper {

    public static final Registry REGISTRY = new Registry();

    public static BlockRegistryObject registerBlockItem(String id, Supplier<Block> supplier, CreativeModeTabHolder itemGroup) {
        /* Fabric Routine:
            registerBlock(id, block);
            registerItem(id, new BlockItem(Item.Settings()), itemGroup)
        */
        return REGISTRY.registerBlockWithBlockItem(new Identifier(Constants.MOD_ID, id), supplier, itemGroup);
    }

    public static BlockRegistryObject registerBlock(String id, Supplier<Block> supplier) {
        // Registry.register(Registries.BLOCK, new Identifier(Constants.MOD_ID, id), block);
        return REGISTRY.registerBlock(new Identifier(Constants.MOD_ID, id), supplier);
    }

    public static ItemRegistryObject registerItem(String id, Function<ItemSettings, org.mtr.mapping.holder.Item> callback, CreativeModeTabHolder itemGroup) {
        /* Registry.register(Registries.ITEM, new Identifier(Constants.MOD_ID, id), item);
        if(itemGroup != null) {
            ItemGroupRegistry.addItem(item, itemGroup);
        }
        */
        return REGISTRY.registerItem(new Identifier(Constants.MOD_ID, id), callback, itemGroup);
    }

    public static <T extends BlockEntityExtension> BlockEntityTypeRegistryObject<T> registerBlockEntity(String id, BiFunction<BlockPos, BlockState, T> constructor, BlockRegistryObject associatedBlock) {
        return REGISTRY.registerBlockEntityType(new Identifier(Constants.MOD_ID, id), constructor, associatedBlock::get);
    }

    public static void setupPacket() {
        REGISTRY.setupPackets(new Identifier(Constants.MOD_ID, "packet"));
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
