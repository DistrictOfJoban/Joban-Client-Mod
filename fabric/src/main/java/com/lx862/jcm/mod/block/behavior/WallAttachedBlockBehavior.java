package com.lx862.jcm.mod.block.behavior;

import org.mtr.mapping.holder.*;

public interface WallAttachedBlockBehavior {
    static boolean canBePlaced(BlockPos pos, World world, Direction directionTowardsWall) {
        BlockPos wallBlockPos = pos.offset(directionTowardsWall);
        BlockState wallBlock = world.getBlockState(wallBlockPos);

        return wallBlock.isSideSolidFullSquare(BlockView.cast(world), pos, directionTowardsWall);
    }

    default Direction getWallDirection(Direction defaultDirection) {
        return defaultDirection;
    }
}
