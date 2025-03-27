package com.lx862.jcm.mapping;

import org.mtr.mapping.holder.BlockPos;
import org.mtr.mapping.holder.World;

/**
 * Fabric implementation via Yarn mapping
 */
public class LoaderImpl {
    public static boolean isRainingAt(World world, BlockPos pos) {
        return world.data.hasRain(pos.data);
    }
}
