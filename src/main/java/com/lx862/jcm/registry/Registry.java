package com.lx862.jcm.registry;

import com.lx862.jcm.Constants;
import org.jetbrains.annotations.Nullable;
import org.mtr.mapping.holder.*;
import org.mtr.mapping.registry.BlockRegistryObject;
import org.mtr.mapping.registry.CreativeModeTabHolder;
import org.mtr.mapping.registry.ItemRegistryObject;

public class Registry {
    public static BlockRegistryObject registerBlockItem(String id, Block block, CreativeModeTabHolder itemGroup) {
        return org.mtr.mapping.registry.Registry.registerBlockWithBlockItem(new Identifier(Constants.MOD_ID, id), () -> block, itemGroup);
    }

    public static BlockRegistryObject registerBlock(String id, Block block) {
        return org.mtr.mapping.registry.Registry.registerBlock(new Identifier(Constants.MOD_ID, id), () -> block);
    }

    public static ItemRegistryObject registerItem(String id, Item item, CreativeModeTabHolder itemGroup) {
        return org.mtr.mapping.registry.Registry.registerItem(new Identifier(Constants.MOD_ID, id), (itemSettings) -> item, itemGroup);
    }

    public static void register() {
        BlockRegistry.register();
        ItemRegistry.register();
        org.mtr.mapping.registry.Registry.init();
    }

    public static void registerClient() {
        BlockRenderTypeRegistry.registerClient();
        org.mtr.mapping.registry.RegistryClient.init();
    }
}
