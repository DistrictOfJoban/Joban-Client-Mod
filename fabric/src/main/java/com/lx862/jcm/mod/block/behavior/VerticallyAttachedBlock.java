package com.lx862.jcm.mod.block.behavior;

import com.lx862.jcm.mod.util.BlockUtil;
import org.mtr.mapping.holder.BlockPos;
import org.mtr.mapping.holder.ItemPlacementContext;
import org.mtr.mapping.holder.World;

public interface VerticallyAttachedBlock {
    static boolean canPlace(boolean canAttachTop, boolean canAttachBottom, BlockPos pos, ItemPlacementContext ctx) {
        World world = ctx.getWorld();

        boolean blockAboveSolid = BlockUtil.blockConsideredSolid(world.getBlockState(pos.up()));
        boolean blockBelowSolid = BlockUtil.blockConsideredSolid(world.getBlockState(pos.down()));

        if (!blockAboveSolid && !blockBelowSolid) return false;
        if (!canAttachTop && !blockBelowSolid) return false;
        return canAttachBottom || blockAboveSolid;
    }

    static boolean canPlace(boolean canAttachTop, boolean canAttachBottom, ItemPlacementContext ctx) {
        BlockPos pos = ctx.getBlockPos();
        return canPlace(canAttachTop, canAttachBottom, pos, ctx);
    }
}
