package com.lx862.jcm.mod.block.base;

import net.minecraft.world.WorldView;
import org.mtr.mapping.holder.*;

public abstract class PoleBlock extends SlabExtendableBlock {
    public PoleBlock(BlockSettings settings) {
        super(settings);
    }

    @Override
    public boolean canPlaceAt2(BlockState state, WorldView world, BlockPos pos) {
        Block blockBelow = new Block(world.getBlockState(pos.down().data).getBlock());

        return (blockBelow.data instanceof PoleBlock) || blockIsAllowed(blockBelow);
    }

    @Override
    public BlockState getPlacementState2(ItemPlacementContext ctx) {
        BlockState superState = super.getPlacementState2(ctx);
        if(superState == null) return null;

        BlockState blockBelow = ctx.getWorld().getBlockState(ctx.getBlockPos().down());
        if (blockIsAllowed(blockBelow.getBlock())) {
            return superState;
        } else {
            return null;
        }
    }

    public abstract boolean blockIsAllowed(Block block);
}
