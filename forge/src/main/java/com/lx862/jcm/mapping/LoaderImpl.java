package com.lx862.jcm.mapping;

import org.mtr.mapping.holder.*;

import java.util.Optional;

/**
 * Forge implementation via Mojang mapping
 */
public class LoaderImpl {
    public static boolean isRainingAt(World world, BlockPos pos) {
        return world.data.isRainingAt(pos.data);
    }

    /** Get a block settings forcing it to be solid, as we don't want water to break our block. */
    public static BlockSettings getSolidBlockSettings(BlockSettings settings) {
        #if MC_VERSION >= "12001"
            return new BlockSettings(settings.data.forceSolidOn());
        #else
            return settings;
        #endif
    }

    public static Item getItemFromId(Identifier id) {
        final Optional<net.minecraft.world.item.Item> itm;
        #if MC_VERSION < "11903"
            itm = net.minecraft.core.Registry.ITEM.getOrEmpty(id.data);
        #else
            itm = net.minecraft.core.registries.BuiltInRegistries.ITEM.getOptional(id.data);
        #endif
        return itm.map(Item::new).orElse(null);
    }
}
