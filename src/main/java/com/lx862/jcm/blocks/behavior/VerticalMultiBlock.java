package com.lx862.jcm.blocks.behavior;

import com.lx862.jcm.util.BlockUtil;
import net.minecraft.world.WorldView;
import org.mtr.mapping.holder.*;

public interface VerticalMultiBlock {
    static boolean canBePlaced(BlockState state, WorldView world, BlockPos pos, int height) {
        return BlockUtil.isReplacable(world, pos, Direction.UP, height);
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
