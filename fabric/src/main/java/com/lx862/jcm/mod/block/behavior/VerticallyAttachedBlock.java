package com.lx862.jcm.mod.block.behavior;

import com.lx862.jcm.mod.util.BlockUtil;
import org.mtr.mapping.holder.BlockPos;
import org.mtr.mapping.holder.ItemPlacementContext;
import org.mtr.mapping.holder.World;

public interface VerticallyAttachedBlock {
    static boolean canPlace(boolean canAttachTop, boolean canAttachBottom, BlockPos pos, ItemPlacementContext ctx) {
        World world = ctx.getWorld();

        boolean blockAboveSolid = !world.getBlockState(pos.up()).isAir();
        boolean blockBelowSolid = !world.getBlockState(pos.down()).isAir();

        if (!blockAboveSolid && !blockBelowSolid) return false;
        if (!canAttachTop && !blockBelowSolid) return false;
        return canAttachBottom || blockAboveSolid;
    }

    static boolean canPlace(boolean canAttachTop, boolean canAttachBottom, ItemPlacementContext ctx) {
        BlockPos pos = ctx.getBlockPos();
        return canPlace(canAttachTop, canAttachBottom, pos, ctx);
    }
}
