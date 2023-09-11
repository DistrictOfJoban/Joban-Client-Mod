package com.lx862.jcm.blocks.base;

import com.lx862.jcm.util.BlockUtil;
import net.minecraft.world.WorldView;
import org.mtr.mapping.holder.*;

public abstract class VerticalMultiBlockBase extends DirectionalBlock {
    public VerticalMultiBlockBase(BlockSettings settings) {
        super(settings);
    }

    public static boolean canBePlaced(BlockState state, WorldView world, BlockPos pos, int height) {
        return BlockUtil.isReplacable(world, pos, Direction.UP, height);
    }

    public static void placeAllBlock(World world, BlockPos startPos, BlockState state, Property<Integer> partProperty, int height) {
        for (int i = 0; i < height; i++) {
            if (i == 0) continue;
            world.setBlockState(startPos.up(i), state.with(partProperty, i));
        }
    }

    public static boolean shouldRemove(WorldAccess world, BlockPos pos, BlockState state, net.minecraft.block.Block instance, Property<Integer> partProperty, int height) {
        int thisPart = state.get(partProperty);

        if (!BlockUtil.canSurvive(instance, world, pos, Direction.UP, thisPart, height)) {
            return true;
        }

        return false;
    }
}
