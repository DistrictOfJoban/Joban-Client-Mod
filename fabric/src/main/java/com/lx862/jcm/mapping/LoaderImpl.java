package com.lx862.jcm.mapping;

import org.mtr.mapping.holder.*;

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
}
