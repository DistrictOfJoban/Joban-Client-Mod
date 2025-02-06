package com.lx862.jcm.mod.block;

import com.lx862.jcm.mod.block.base.PoleBlock;
import org.mtr.mapping.holder.*;
import org.mtr.mod.block.IBlock;

public class RVPIDSPole extends PoleBlock {
    public RVPIDSPole(BlockSettings settings) {
        super(settings);
    }

    @Override
    public VoxelShape getOutlineShape2(BlockState state, BlockView view, BlockPos pos, ShapeContext context) {
        return IBlock.getVoxelShapeByDirection(7.5, 0, 8.5, 8.5, 16, 9.5, IBlock.getStatePropertySafe(state, FACING));
    }

    @Override
    public BlockState getPlacementState2(ItemPlacementContext ctx) {
        BlockState superState = super.getPlacementState2(ctx);
        if(superState == null) return null;

        BlockState blockBelow = ctx.getWorld().getBlockState(ctx.getBlockPos().down());
        return superState
                .with(new Property<>(FACING.data), IBlock.getStatePropertySafe(blockBelow, FACING).data);
    }

    @Override
    public boolean blockIsAllowed(Block block) {
        return block.data instanceof JCMPIDSBlock || block.data instanceof RVPIDSPole;
    }
}
