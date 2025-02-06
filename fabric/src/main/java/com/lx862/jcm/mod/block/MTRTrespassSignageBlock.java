package com.lx862.jcm.mod.block;

import com.lx862.jcm.mod.block.base.Horizontal2Block;
import org.mtr.mapping.holder.*;
import org.mtr.mod.block.IBlock;

public class MTRTrespassSignageBlock extends Horizontal2Block {

    public MTRTrespassSignageBlock(BlockSettings settings) {
        super(settings);
    }

    @Override
    public org.mtr.mapping.holder.VoxelShape getOutlineShape2(BlockState state, BlockView view, BlockPos pos, ShapeContext context) {
        if(IBlock.getStatePropertySafe(state, IS_LEFT)) {
            VoxelShape vx1 = IBlock.getVoxelShapeByDirection(2, 4, 7.5, 16, 16, 8.5, IBlock.getStatePropertySafe(state, FACING));
            VoxelShape vx2 = IBlock.getVoxelShapeByDirection(7, 0, 7.5, 8, 16, 8.5, IBlock.getStatePropertySafe(state, FACING));
            return VoxelShapes.union(vx1, vx2);
        } else {
            VoxelShape vx1R = IBlock.getVoxelShapeByDirection(0, 4, 7.5, 14, 16, 8.5, IBlock.getStatePropertySafe(state, FACING));
            VoxelShape vx2R = IBlock.getVoxelShapeByDirection(8, 0, 7.5, 9, 16, 8.5, IBlock.getStatePropertySafe(state, FACING));
            return VoxelShapes.union(vx1R, vx2R);
        }
    }
}
