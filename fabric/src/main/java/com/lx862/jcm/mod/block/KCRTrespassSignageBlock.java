package com.lx862.jcm.mod.block;

import com.lx862.jcm.mod.block.base.HorizontalWallAttached2Block;
import org.mtr.mapping.holder.*;
import org.mtr.mod.block.IBlock;

public class KCRTrespassSignageBlock extends HorizontalWallAttached2Block {

    public KCRTrespassSignageBlock(BlockSettings settings) {
        super(settings);
    }

    @Override
    public VoxelShape getOutlineShape2(BlockState state, BlockView view, BlockPos pos, ShapeContext context) {
        if(IBlock.getStatePropertySafe(state, IS_LEFT)) {
            return IBlock.getVoxelShapeByDirection(4, 2, 0, 16, 14, 0.1, IBlock.getStatePropertySafe(state, FACING));
        } else {
            return IBlock.getVoxelShapeByDirection(0, 2, 0, 12, 14, 0.1, IBlock.getStatePropertySafe(state, FACING));
        }
    }
}
