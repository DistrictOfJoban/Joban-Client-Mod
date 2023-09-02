package com.lx862.jcm.registry;

import com.lx862.jcm.Constants;
import net.minecraft.block.Block;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

public class RegistryHelper {
    public static void registerBlockItem(String id, Block block, ItemGroupRegistry.TYPE itemGroup) {
        registerBlock(id, block);
        registerItem(id, new BlockItem(block, new Item.Settings()), itemGroup);
    }

    public static void registerBlock(String id, Block block) {
        Registry.register(Registries.BLOCK, new Identifier(Constants.MOD_ID, id), block);
    }

    public static void registerItem(String id, Item item, @Nullable ItemGroupRegistry.TYPE itemGroup) {
        Registry.register(Registries.ITEM, new Identifier(Constants.MOD_ID, id), item);
        if(itemGroup != null) {
            ItemGroupRegistry.addItem(item, itemGroup);
        }
    }
}
