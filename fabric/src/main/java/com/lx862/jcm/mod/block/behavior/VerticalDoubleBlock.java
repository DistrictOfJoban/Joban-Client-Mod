package com.lx862.jcm.mod.block.behavior;

import com.lx862.jcm.mod.data.BlockProperties;
import com.lx862.jcm.mod.util.BlockUtil;
import org.mtr.mapping.holder.*;
import org.mtr.mod.block.IBlock;

public interface VerticalDoubleBlock {
    EnumProperty<IBlock.DoubleBlockHalf> HALF = BlockProperties.VERTICAL_2;
    static boolean canBePlaced(World world, BlockPos pos, ItemPlacementContext ctx) {
        return BlockUtil.isReplacable(world, pos, Direction.UP, ctx, 2);
    }

    static void placeBlock(World world, BlockPos startPos, BlockState state) {
        world.setBlockState(startPos.up(), state.with(new Property<>(HALF.data), IBlock.DoubleBlockHalf.UPPER));
    }

    static boolean blockNotValid(Block blockInstance, WorldAccess world, BlockPos pos, BlockState state) {
        IBlock.DoubleBlockHalf thisPart = IBlock.getStatePropertySafe(state, new Property<>(HALF.data));
        int offset = thisPart == IBlock.DoubleBlockHalf.UPPER ? -1 : 1;
        return !world.getBlockState(pos.offset(Axis.Y, offset)).isOf(blockInstance);
    }
}
