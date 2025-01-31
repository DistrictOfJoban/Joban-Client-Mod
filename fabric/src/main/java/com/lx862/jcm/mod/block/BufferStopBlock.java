package com.lx862.jcm.mod.block;

import com.lx862.jcm.mod.block.base.DirectionalBlock;
import org.mtr.mapping.holder.*;
import org.mtr.mod.block.IBlock;

public class BufferStopBlock extends DirectionalBlock {

    public BufferStopBlock(BlockSettings settings) {
        super(settings);
    }

    @Override
    public VoxelShape getOutlineShape2(BlockState state, BlockView view, BlockPos pos, ShapeContext context) {
        return IBlock.getVoxelShapeByDirection(-4, 0, 0, 20, 16, 32, IBlock.getStatePropertySafe(state, FACING));
    }

    @Override
    public VoxelShape getCollisionShape2(BlockState state, BlockView view, BlockPos pos, ShapeContext context) {
        return IBlock.getVoxelShapeByDirection(0, 0, 0, 16, 16, 16, IBlock.getStatePropertySafe(state, FACING));
    }
}
