package com.lx862.jcm.mod.block;

import com.lx862.jcm.mod.block.base.CeilingAttachedDirectional2Block;
import org.mtr.mod.block.IBlock;
import org.mtr.mapping.holder.*;

public class ExitSignEven extends CeilingAttachedDirectional2Block {

    public ExitSignEven(BlockSettings settings) {
        super(settings, true, false, true);
    }

    @Override
    public VoxelShape getOutlineShape2(BlockState state, BlockView view, BlockPos pos, ShapeContext context) {
        if(IBlock.getStatePropertySafe(state, IS_LEFT)) {
            return IBlock.getVoxelShapeByDirection(8, 9, 7.9, 16, 16, 8.1, IBlock.getStatePropertySafe(state, FACING));
        } else {
            return IBlock.getVoxelShapeByDirection(0, 9, 7.9, 8, 16, 8.1, IBlock.getStatePropertySafe(state, FACING));
        }
    }
}
