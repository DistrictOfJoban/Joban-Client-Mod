package com.lx862.jcm.mod.block;

import com.lx862.jcm.mod.block.base.DirectionalBlock;
import org.mtr.mod.block.IBlock;
import org.mtr.mapping.holder.*;

public class LRTInterCarBarrierBlock extends DirectionalBlock {

    public LRTInterCarBarrierBlock(BlockSettings settings) {
        super(settings);
    }

    @Override
    public VoxelShape getOutlineShape2(BlockState state, BlockView view, BlockPos pos, ShapeContext context) {
        return IBlock.getVoxelShapeByDirection(0, 0, 0, 16, 16, 16, Direction.NORTH);
    }

    @Override
    public VoxelShape getCollisionShape2(BlockState state, BlockView view, BlockPos pos, ShapeContext context) {
        return IBlock.getVoxelShapeByDirection(0, 0, 0, 16, 24, 16, IBlock.getStatePropertySafe(state, FACING));
    }

    @Override
    public VoxelShape getCullingShape2(BlockState state, BlockView view, BlockPos pos) {
        return VoxelShapes.empty();
    }
}
