package com.lx862.jcm.mod.block.behavior;

import com.lx862.jcm.mod.util.BlockUtil;
import org.mtr.mapping.holder.*;

import static com.lx862.jcm.mod.block.base.DirectionalBlock.FACING;

public interface HorizontalMultiBlock {
    static boolean canBePlaced(BlockState state, ItemPlacementContext ctx, int width) {
        if(state == null) return false;
        return BlockUtil.isReplacable(ctx.getWorld(), ctx.getBlockPos(), BlockUtil.getProperty(state, FACING).rotateYClockwise(), ctx, width);
    }

    static void placeBlock(World world, BlockPos pos, BlockState state, BooleanProperty isLeft, Direction directionToPlace, int length) {
        world.setBlockState(pos.offset(directionToPlace), state.with(new Property<>(isLeft.data), false));
    }

    static boolean blockIsValid(BlockPos pos, BlockState state, WorldAccess world, boolean checkLeft, boolean checkRight) {
        boolean sameBlock = false;
        Direction dir = BlockUtil.getProperty(state, FACING);

        if(checkLeft) {
            BlockState bs = world.getBlockState(pos.offset(dir.rotateYCounterclockwise()));
            sameBlock = bs.isOf(state.getBlock());
        }

        if(checkRight && !sameBlock) {
            BlockState bs = world.getBlockState(pos.offset(dir.rotateYClockwise()));
            sameBlock = bs.isOf(state.getBlock());
        }

        return sameBlock;
    }
}
