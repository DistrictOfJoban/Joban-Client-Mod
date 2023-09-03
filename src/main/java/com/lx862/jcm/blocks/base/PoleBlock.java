package com.lx862.jcm.blocks.base;

import net.minecraft.world.WorldView;
import org.mtr.mapping.holder.*;

public abstract class PoleBlock extends SlabExtendibleBlock {
    public PoleBlock(BlockSettings settings) {
        super(settings);
    }

    @Override
    public boolean canPlaceAt2(BlockState state, WorldView world, BlockPos pos) {
        Block blockBelow = new Block(world.getBlockState(pos.down().data).getBlock());

        return (blockBelow.data instanceof PoleBlock) || blockIsAllowed(blockBelow);
    }

    public abstract boolean blockIsAllowed(Block block);
}
