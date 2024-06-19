package com.lx862.jcm.mod.block.base;

import org.mtr.mapping.holder.Block;
import org.mtr.mapping.holder.BlockSettings;
import org.mtr.mapping.holder.BlockState;
import org.mtr.mapping.holder.ItemPlacementContext;

public abstract class PoleBlock extends SlabExtendableBlock {
    public PoleBlock(BlockSettings settings) {
        super(settings);
    }

    public boolean canPlace(Block block) {
        return (block.data instanceof PoleBlock) || blockIsAllowed(block);
    }

    @Override
    public BlockState getPlacementState2(ItemPlacementContext ctx) {
        BlockState superState = super.getPlacementState2(ctx);
        if(superState == null) return null;

        BlockState blockBelow = ctx.getWorld().getBlockState(ctx.getBlockPos().down());
        if (this.canPlace(blockBelow.getBlock()) && blockIsAllowed(blockBelow.getBlock())) {
            return superState;
        } else {
            return null;
        }
    }

    public abstract boolean blockIsAllowed(Block block);
}
