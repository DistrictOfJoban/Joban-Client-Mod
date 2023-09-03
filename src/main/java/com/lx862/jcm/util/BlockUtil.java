package com.lx862.jcm.util;

import net.minecraft.world.WorldView;
import org.mtr.mapping.holder.*;

public class BlockUtil {

    public static boolean blockConsideredSolid(BlockState state) {
        return !state.isAir() && !state.isLiquid();
    }

    public static boolean canSurvive(net.minecraft.block.Block instance, WorldAccess world, BlockPos pos, Direction facing, int part, int totalWidthHeight) {
        boolean checkLeftOrBottom = part != 0;
        boolean checkRightOrTop = part != totalWidthHeight - 1;
        boolean canSurvive = true;

        if (checkLeftOrBottom) {
            BlockPos posLeftOrDown = pos.offset(facing.getOpposite());
            BlockState blockLeftOrDown = world.getBlockState(posLeftOrDown);
            if (blockLeftOrDown.getBlock().data != instance) {
                return false;
            }
        }

        if (checkRightOrTop) {
            BlockPos posRightOrUp = pos.offset(facing);
            BlockState blockRightOrUp = world.getBlockState(posRightOrUp);
            if (blockRightOrUp.getBlock().data != instance) {
                return false;
            }
        }
        return canSurvive;
    }

    public static boolean isReplacable(WorldView world, org.mtr.mapping.holder.BlockPos startPos, org.mtr.mapping.holder.Direction direction, int distance) {
        for (int i = 0; i < distance; i++) {
            org.mtr.mapping.holder.BlockPos posUp = startPos.offset(direction, i);
            net.minecraft.block.BlockState blockState = world.getBlockState(posUp.data);
            if (!blockState.isReplaceable()) {
                return false;
            }
        }
        return true;
    }

    public static boolean getProperty(BlockState state, BooleanProperty property) {
        return getProperty(state, new Property<>(property.data));
    }

    public static Direction getProperty(BlockState state, DirectionProperty property) {
        return Direction.convert(getProperty(state, new Property<>(property.data)));
    }

    public static int getProperty(BlockState state, IntegerProperty property) {
        return getProperty(state, new Property<>(property.data));
    }

    static <T extends Comparable<T>> T getProperty(BlockState state, Property<T> property) {
        return state.get(property);
    }
}
