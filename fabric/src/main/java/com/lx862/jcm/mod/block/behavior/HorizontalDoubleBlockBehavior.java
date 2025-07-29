package com.lx862.jcm.mod.block.behavior;

import com.lx862.jcm.mod.data.BlockProperties;
import com.lx862.jcm.mod.util.BlockUtil;
import org.mtr.mapping.holder.*;
import org.mtr.mapping.tool.HolderBase;
import org.mtr.mod.block.IBlock;

import java.util.List;

import static com.lx862.jcm.mod.block.base.DirectionalBlock.FACING;

public interface HorizontalDoubleBlockBehavior {
    BooleanProperty IS_LEFT = BlockProperties.HORIZONTAL_IS_LEFT;
    static boolean canBePlaced(ItemPlacementContext ctx) {
        return BlockUtil.isReplacable(ctx.getWorld(), ctx.getBlockPos(), ctx.getPlayerFacing().rotateYClockwise(), ctx, 2);
    }

    static void placeBlock(World world, BlockPos pos, BlockState state, BooleanProperty isLeft, Direction directionToPlace, int length) {
        world.setBlockState(pos.offset(directionToPlace), state.with(new Property<>(isLeft.data), false));
    }

    static boolean blockIsValid(BlockPos pos, BlockState state, Direction updateFrom, WorldAccess world, boolean thisBlockIsLeftPart) {
        Direction dir = IBlock.getStatePropertySafe(state, FACING);
        Direction checkDir = thisBlockIsLeftPart ? dir.rotateYClockwise() : dir.rotateYCounterclockwise();
        BlockPos neighbourPos = pos.offset(checkDir);
        BlockState neighbourState = world.getBlockState(neighbourPos);
        return !checkDir.equals(updateFrom) || neighbourState.isOf(state.getBlock());
    }

    static void onPlaced(World world, BlockState state, BlockPos placedPos) {
        world.setBlockState(placedPos.offset(IBlock.getStatePropertySafe(state, FACING).rotateYClockwise()), state.with(new Property<>(IS_LEFT.data), false));
    }

    static BlockPos getLootDropPos(BlockState state, BlockPos pos) {
        Direction facing = IBlock.getStatePropertySafe(state, FACING);
        boolean isLeft = IBlock.getStatePropertySafe(state, IS_LEFT);
        return isLeft ? pos : pos.offset(facing.rotateYCounterclockwise());
    }

    static void addProperties(List<HolderBase<?>> properties) {
        properties.add(IS_LEFT);
    }
}
