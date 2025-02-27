package com.lx862.jcm.mod.block;

import com.lx862.jcm.mod.block.base.DirectionalBlock;
import org.mtr.mapping.holder.*;
import org.mtr.mod.block.IBlock;

public class DeparturePoleBlock extends DirectionalBlock {

    public DeparturePoleBlock(BlockSettings settings) {
        super(settings);
    }

    @Override
    public VoxelShape getOutlineShape2(BlockState state, BlockView view, BlockPos pos, ShapeContext context) {
        return IBlock.getVoxelShapeByDirection(6.5, 0, 13, 9.5, 16, 16, IBlock.getStatePropertySafe(state, FACING));
    }
}
