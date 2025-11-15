package com.lx862.jcm.mapping;

import org.mtr.mapping.holder.*;

import java.util.Optional;

/**
 * Fabric implementation via Yarn mapping
 */
public class LoaderImpl {
    public static boolean isRainingAt(World world, BlockPos pos) {
        return world.data.hasRain(pos.data);
    }

    /** Get a block settings forcing it to be solid, as we don't want water to break our block. */
    public static BlockSettings getSolidBlockSettings(BlockSettings settings) {
        #if MC_VERSION >= "12001"
            return new BlockSettings(settings.data.solid());
        #else
        return settings;
        #endif
    }

    public static Item getItemFromId(Identifier id) {
        final Optional<net.minecraft.item.Item> itm;
        #if MC_VERSION < "11903"
            itm = net.minecraft.util.registry.Registry.ITEM.getOrEmpty(id.data);
        #else
            itm = net.minecraft.registry.Registries.ITEM.getOrEmpty(id.data);
        #endif
        return itm.map(Item::new).orElse(null);
    }

    public static Identifier getIdFromItem(Item itm) {
        #if MC_VERSION < "11903"
            return new Identifier(net.minecraft.util.registry.Registry.ITEM.getId(itm.data));
        #else
            return new Identifier(net.minecraft.registry.Registries.ITEM.getId(itm.data));
        #endif
    }
}
