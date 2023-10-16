package com.lx862.jcm.mod.block.behavior;

import com.lx862.jcm.mod.util.BlockUtil;
import net.minecraft.world.WorldView;
import org.mtr.mapping.holder.*;

import static com.lx862.jcm.mod.block.base.DirectionalBlock.FACING;

public interface HorizontalMultiBlock {
    static boolean canBePlaced(BlockState state, WorldView world, BlockPos pos, int width) {
        return BlockUtil.isReplacable(world, pos, BlockUtil.getProperty(state, FACING).rotateYClockwise(), width);
    }

    static void placeBlock(World world, BlockPos pos, BlockState state, Property<Integer> partProperty, Direction directionToPlace, int length) {
        for (int i = 0; i < length; i++) {
            if (i == 0) continue;
            world.setBlockState(pos.offset(directionToPlace, i), state.with(partProperty, i));
        }
    }

    static boolean blockNotValid(BlockPos pos, BlockState state, WorldAccess world, Property<Integer> partProperty, int totalWidthHeight) {
        int thisPart = state.get(partProperty);
        return BlockUtil.canSurvive(state.getBlock(), world, pos, BlockUtil.getProperty(state, FACING).rotateYClockwise(), thisPart, totalWidthHeight);
    }
}
