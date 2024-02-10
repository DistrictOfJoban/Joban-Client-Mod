package com.lx862.jcm.mod.block.behavior;

import com.lx862.jcm.mod.util.BlockUtil;
import org.mtr.mapping.holder.BlockPos;
import org.mtr.mapping.holder.BlockState;
import org.mtr.mapping.holder.Direction;
import org.mtr.mapping.holder.World;

public interface WallAttachedBlockBehavior {
    static boolean canBePlaced(BlockPos pos, World world, Direction directionTowardsWall) {
        BlockPos wallBlockPos = pos.offset(directionTowardsWall);
        BlockState wallBlock = world.getBlockState(wallBlockPos);

        return BlockUtil.blockConsideredSolid(wallBlock);
    }

    default Direction getWallDirection(Direction defaultDirection) {
        return defaultDirection;
    }
}
