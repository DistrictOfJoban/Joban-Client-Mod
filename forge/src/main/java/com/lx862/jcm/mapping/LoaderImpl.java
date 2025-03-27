package com.lx862.jcm.mapping;

import org.mtr.mapping.holder.BlockPos;
import org.mtr.mapping.holder.World;

/**
 * Forge implementation via Mojang mapping
 */
public class LoaderImpl {
    public static boolean isRainingAt(World world, BlockPos pos) {
        return world.data.isRainingAt(pos.data);
    }
}
