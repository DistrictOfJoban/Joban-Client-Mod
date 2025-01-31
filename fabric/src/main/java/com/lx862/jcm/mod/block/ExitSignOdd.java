package com.lx862.jcm.mod.block;

import com.lx862.jcm.mod.block.base.CeilingAttachedDirectionalBlock;
import org.mtr.mod.block.IBlock;
import org.mtr.mapping.holder.*;

public class ExitSignOdd extends CeilingAttachedDirectionalBlock {

    public ExitSignOdd(BlockSettings settings) {
        super(settings, true);
    }

    @Override
    public VoxelShape getOutlineShape2(BlockState state, BlockView view, BlockPos pos, ShapeContext context) {
        return IBlock.getVoxelShapeByDirection(0, 9, 7.9, 16, 16, 8.1, IBlock.getStatePropertySafe(state, FACING));
    }
}
