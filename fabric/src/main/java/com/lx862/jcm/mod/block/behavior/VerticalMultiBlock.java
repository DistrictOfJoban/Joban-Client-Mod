package com.lx862.jcm.mod.block.behavior;

import com.lx862.jcm.mod.util.BlockUtil;
import org.mtr.mapping.holder.*;

public interface VerticalMultiBlock {
    static boolean canBePlaced(World world, BlockPos pos, ItemPlacementContext ctx, int height) {
        return BlockUtil.isReplacable(world, pos, Direction.UP, ctx, height);
    }

    static void placeBlock(World world, BlockPos startPos, BlockState state, Property<Integer> partProperty, int height) {
        for (int i = 0; i < height; i++) {
            if (i == 0) continue;
            world.setBlockState(startPos.up(i), state.with(partProperty, i));
        }
    }

    static boolean blockNotValid(WorldAccess world, BlockPos pos, BlockState state, Property<Integer> partProperty, int height) {
        int thisPart = state.get(partProperty);
        return !BlockUtil.canSurvive(state.getBlock(), world, pos, Direction.UP, thisPart, height);
    }
}
