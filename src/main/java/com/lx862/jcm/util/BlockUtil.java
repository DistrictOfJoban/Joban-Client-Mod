package com.lx862.jcm.util;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.WorldView;

public class BlockUtil {

    public static boolean blockConsideredSolid(BlockState state) {
        return !state.isAir() && !state.isLiquid();
    }

    public static boolean canSurvive(Block instance, WorldAccess world, BlockPos pos, Direction facing, int part, int totalWidthHeight) {
        boolean checkLeftOrBottom = part != 0;
        boolean checkRightOrTop = part != totalWidthHeight - 1;
        boolean canSurvive = true;

        if(checkLeftOrBottom) {
            BlockPos posLeftOrDown = pos.offset(facing.getOpposite());
            BlockState blockLeftOrDown = world.getBlockState(posLeftOrDown);
            if(blockLeftOrDown.getBlock() != instance) {
                return false;
            }
        }

        if(checkRightOrTop) {
            BlockPos posRightOrUp = pos.offset(facing);
            BlockState blockRightOrUp = world.getBlockState(posRightOrUp);
            if(blockRightOrUp.getBlock() != instance) {
                return false;
            }
        }
        return canSurvive;
    }

    public static boolean isReplacable(WorldView world, BlockPos startPos, Direction direction, int distance) {
        for(int i = 0; i < distance; i++) {
            BlockPos posUp = startPos.offset(direction, i);
            BlockState blockState = world.getBlockState(posUp);
            if(!blockState.isReplaceable()) {
                return false;
            }
        }
        return true;
    }
}
