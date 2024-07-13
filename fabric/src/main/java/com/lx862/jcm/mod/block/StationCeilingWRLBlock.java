package com.lx862.jcm.mod.block;

import com.lx862.jcm.mod.block.base.CeilingAttachedDirectionalBlock;
import com.lx862.jcm.mod.util.BlockUtil;
import org.mtr.mapping.holder.*;
import org.mtr.mod.block.IBlock;

public class StationCeilingWRLBlock extends CeilingAttachedDirectionalBlock {

    public StationCeilingWRLBlock(BlockSettings settings) {
        super(settings, false);
    }

    @Override
    public VoxelShape getOutlineShape2(BlockState state, BlockView view, BlockPos pos, ShapeContext context) {
        VoxelShape ceiling = IBlock.getVoxelShapeByDirection(0.5, 8, 1, 15.5, 9, 15, IBlock.getStatePropertySafe(state, FACING));
        VoxelShape pole = IBlock.getVoxelShapeByDirection(7.5, 9, 7.5, 8.5, 16, 8.5, IBlock.getStatePropertySafe(state, FACING));
        return VoxelShapes.union(ceiling, pole);
    }
}
