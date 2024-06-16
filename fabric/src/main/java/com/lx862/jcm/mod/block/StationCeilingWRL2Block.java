package com.lx862.jcm.mod.block;

import com.lx862.jcm.mod.block.base.CeilingAttachedDirectional2Block;
import com.lx862.jcm.mod.util.BlockUtil;
import com.lx862.jcm.mod.util.VoxelUtil;
import org.mtr.mapping.holder.*;

public class StationCeilingWRL2Block extends CeilingAttachedDirectional2Block {

    public StationCeilingWRL2Block(BlockSettings settings) {
        super(settings, true, false, false);
    }

    @Override
    public VoxelShape getOutlineShape2(BlockState state, BlockView view, BlockPos pos, ShapeContext context) {
        if(BlockUtil.getProperty(state, IS_LEFT)) {
            VoxelShape ceiling = VoxelUtil.getDirectionalShape16(BlockUtil.getProperty(state, FACING), 0.5, 8, 1, 16, 9, 15);
            VoxelShape pole = VoxelUtil.getDirectionalShape16(BlockUtil.getProperty(state, FACING), 5.5, 9, 7.5, 6.5, 16, 8.5);
            return VoxelShapes.union(ceiling, pole);
        } else {
            VoxelShape ceilingR = VoxelUtil.getDirectionalShape16(BlockUtil.getProperty(state, FACING), 0, 8, 1, 15.5, 9, 15);
            VoxelShape poleR = VoxelUtil.getDirectionalShape16(BlockUtil.getProperty(state, FACING), 10.5, 9, 7.5, 11.5, 16, 8.5);
            return VoxelShapes.union(ceilingR, poleR);
        }
    }
}
