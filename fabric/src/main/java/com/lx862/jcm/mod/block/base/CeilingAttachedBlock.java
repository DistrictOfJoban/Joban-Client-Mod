package com.lx862.jcm.mod.block.base;

import org.mtr.mapping.holder.*;

public abstract class CeilingAttachedBlock extends JCMBlock {

    public CeilingAttachedBlock(BlockSettings settings) {
        super(settings);
    }

    @Override
    public BlockState getPlacementState2(ItemPlacementContext ctx) {
        BlockState superState = super.getPlacementState2(ctx);
        BlockState blockAbove = ctx.getWorld().getBlockState(ctx.getBlockPos().up());
        if(superState == null) return null;
        if(!com.lx862.jcm.mod.block.behavior.VerticallyAttachedBlock.canPlace(true, false, ctx)) return null;
        return superState;
    }

    @Override
    public BlockState getStateForNeighborUpdate2(BlockState state, Direction direction, BlockState neighborState, WorldAccess world, BlockPos pos, BlockPos neighborPos) {
        BlockState attachedBlock = world.getBlockState(pos.up());

        if (shouldBreakOnBlockUpdate() && attachedBlock.isAir()) {
            return Blocks.getAirMapped().getDefaultState();
        }

        return super.getStateForNeighborUpdate2(state, direction, neighborState, world, pos, neighborPos);
    }

    protected boolean shouldBreakOnBlockUpdate() {
        return true;
    }
}
