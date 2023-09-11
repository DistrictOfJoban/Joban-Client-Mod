package com.lx862.jcm.registry;

import com.lx862.jcm.Constants;
import org.mtr.mapping.holder.*;
import org.mtr.mapping.mapper.BlockEntityExtension;
import org.mtr.mapping.registry.BlockEntityTypeRegistryObject;
import org.mtr.mapping.registry.BlockRegistryObject;
import org.mtr.mapping.registry.CreativeModeTabHolder;
import org.mtr.mapping.registry.ItemRegistryObject;

import java.util.function.BiFunction;

public class Registry {
    public static BlockRegistryObject registerBlockItem(String id, Block block, CreativeModeTabHolder itemGroup) {
        /* Fabric Routine:
            registerBlock(id, block);
            registerItem(id, new BlockItem(Item.Settings()), itemGroup)
        */
        return org.mtr.mapping.registry.Registry.registerBlockWithBlockItem(new Identifier(Constants.MOD_ID, id), () -> block, itemGroup);
    }

    public static BlockRegistryObject registerBlockItem(String id, net.minecraft.block.Block block, CreativeModeTabHolder itemGroup) {
        return registerBlockItem(id, new Block(block), itemGroup);
    }

    public static BlockRegistryObject registerBlock(String id, Block block) {
        // Registry.register(Registries.BLOCK, new Identifier(Constants.MOD_ID, id), block);
        return org.mtr.mapping.registry.Registry.registerBlock(new Identifier(Constants.MOD_ID, id), () -> block);
    }

    public static ItemRegistryObject registerItem(String id, Item item, CreativeModeTabHolder itemGroup) {
        /* Registry.register(Registries.ITEM, new Identifier(Constants.MOD_ID, id), item);
        if(itemGroup != null) {
            ItemGroupRegistry.addItem(item, itemGroup);
        }
        */
        return org.mtr.mapping.registry.Registry.registerItem(new Identifier(Constants.MOD_ID, id), (itemSettings) -> item, itemGroup);
    }

    public static <T extends BlockEntityExtension> BlockEntityTypeRegistryObject<T> registerBlockEntity(String id, BiFunction<BlockPos, BlockState, T> constructor, BlockRegistryObject associatedBlock) {
        return org.mtr.mapping.registry.Registry.registerBlockEntityType(new Identifier(Constants.MOD_ID, id), constructor, associatedBlock.get());
    }

    public static void register() {
        BlockRegistry.register();
        BlockEntityRegistry.register();
        ItemRegistry.register();
        org.mtr.mapping.registry.Registry.init();
    }

    public static void registerClient() {
        BlockRenderTypeRegistry.registerClient();
        org.mtr.mapping.registry.RegistryClient.init();
    }
}
