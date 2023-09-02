package com.lx862.jcm.registry;

import com.lx862.jcm.Constants;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ItemGroupRegistry {
    private static final HashMap<TYPE, List<ItemStack>> deferredItemGroupRegistry = new HashMap<>();
    public static final RegistryKey<ItemGroup> JCM_MAIN = RegistryKey.of(RegistryKeys.ITEM_GROUP, new Identifier(Constants.MOD_ID, "jcm_main"));

    public static void registerClient() {
        Registry.register(Registries.ITEM_GROUP, JCM_MAIN, FabricItemGroup.builder()
                .icon(() -> new ItemStack(BlockRegistry.MTR_STAIRS.asItem()))
                .displayName(Text.translatable("itemGroup.jsblock.core"))
                .build()
        );

        registerItemModifyEntryEvent();
    }

    public static void addItem(Item item, TYPE itemGroup) {
        List<ItemStack> itemList = deferredItemGroupRegistry.getOrDefault(itemGroup, new ArrayList<>());
        itemList.add(new ItemStack(item));
        deferredItemGroupRegistry.put(itemGroup, itemList);
    }

    private static void registerItemModifyEntryEvent() {
        if(deferredItemGroupRegistry.containsKey(TYPE.MAIN)) {
            ItemGroupEvents.modifyEntriesEvent(JCM_MAIN).register(content -> {
                for(ItemStack stack : deferredItemGroupRegistry.get(TYPE.MAIN)) {
                    content.add(stack);
                }
            });
        }
    }

    public enum TYPE {
        MAIN,
        PIDS,
        CEILING
    }
}
