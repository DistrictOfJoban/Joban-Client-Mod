package com.lx862.jcm.mod.block;

import com.lx862.jcm.mod.block.base.VerticallyAttachedDirectional2Block;
import com.lx862.jcm.mod.util.BlockUtil;
import com.lx862.jcm.mod.util.VoxelUtil;
import org.mtr.mapping.holder.*;

public class StationCeilingWRL2Block extends VerticallyAttachedDirectional2Block {

    public StationCeilingWRL2Block(BlockSettings settings) {
        super(settings, true, false);
    }

    @Override
    public VoxelShape getOutlineShape2(BlockState state, BlockView view, BlockPos pos, ShapeContext context) {
        switch (BlockUtil.getProperty(state, PART)) {
            case 0:
                VoxelShape vx1 = VoxelUtil.getDirectionalShape16(BlockUtil.getProperty(state, FACING), 0.5, 8, 1, 16, 9, 15);
                VoxelShape vx2 = VoxelUtil.getDirectionalShape16(BlockUtil.getProperty(state, FACING), 5.5, 9, 7.5, 6.5, 16, 8.5);
                return VoxelShapes.union(vx1, vx2);
            default:
                VoxelShape vx1R = VoxelUtil.getDirectionalShape16(BlockUtil.getProperty(state, FACING), 0, 8, 1, 15.5, 9, 15);
                VoxelShape vx2R = VoxelUtil.getDirectionalShape16(BlockUtil.getProperty(state, FACING), 10.5, 9, 7.5, 11.5, 16, 8.5);
                return VoxelShapes.union(vx1R, vx2R);
        }
    }

    @Override
    protected boolean shouldBreakOnBlockUpdate() {
        return false;
    }
}
