package com.lx862.jcm.mod.block;

import com.lx862.jcm.mod.block.base.CeilingAttachedDirectional2Block;
import org.mtr.mapping.holder.*;
import org.mtr.mod.block.IBlock;

public class StationCeilingWRL2Block extends CeilingAttachedDirectional2Block {

    public StationCeilingWRL2Block(BlockSettings settings) {
        super(settings, true, false, false);
    }

    @Override
    public VoxelShape getOutlineShape2(BlockState state, BlockView view, BlockPos pos, ShapeContext context) {
        if(IBlock.getStatePropertySafe(state, IS_LEFT)) {
            VoxelShape ceiling = IBlock.getVoxelShapeByDirection(0.5, 8, 1, 16, 9, 15, IBlock.getStatePropertySafe(state, FACING));
            VoxelShape pole = IBlock.getVoxelShapeByDirection(5.5, 9, 7.5, 6.5, 16, 8.5, IBlock.getStatePropertySafe(state, FACING));
            return VoxelShapes.union(ceiling, pole);
        } else {
            VoxelShape ceilingR = IBlock.getVoxelShapeByDirection(0, 8, 1, 15.5, 9, 15, IBlock.getStatePropertySafe(state, FACING));
            VoxelShape poleR = IBlock.getVoxelShapeByDirection(10.5, 9, 7.5, 11.5, 16, 8.5, IBlock.getStatePropertySafe(state, FACING));
            return VoxelShapes.union(ceilingR, poleR);
        }
    }
}
