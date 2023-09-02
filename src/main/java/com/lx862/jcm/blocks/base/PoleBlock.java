package com.lx862.jcm.blocks.base;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldView;

public abstract class PoleBlock extends SlabExtendibleBlock {
    public PoleBlock(Settings settings) {
        super(settings);
    }

    @Override
    public boolean canPlaceAt(BlockState state, WorldView world, BlockPos pos) {
        Block blockBelow = world.getBlockState(pos.down()).getBlock();

        return (blockBelow instanceof PoleBlock) || blockIsAllowed(blockBelow);
    }

    public abstract boolean blockIsAllowed(Block block);
}
