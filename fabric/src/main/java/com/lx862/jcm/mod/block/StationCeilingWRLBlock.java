package com.lx862.jcm.mod.block;

import com.lx862.jcm.mod.block.base.VerticallyAttachedDirectionalBlock;
import com.lx862.jcm.mod.util.BlockUtil;
import com.lx862.jcm.mod.util.VoxelUtil;
import org.mtr.mapping.holder.*;

public class StationCeilingWRLBlock extends VerticallyAttachedDirectionalBlock {

    public StationCeilingWRLBlock(BlockSettings settings) {
        super(settings, true, false);
    }

    @Override
    public VoxelShape getOutlineShape2(BlockState state, BlockView view, BlockPos pos, ShapeContext context) {
        VoxelShape ceiling = VoxelUtil.getDirectionalShape16(BlockUtil.getProperty(state, FACING), 0.5, 8, 1, 15.5, 9, 15);
        VoxelShape pole = VoxelUtil.getDirectionalShape16(BlockUtil.getProperty(state, FACING), 7.5, 9, 7.5, 8.5, 16, 8.5);
        return VoxelShapes.union(ceiling, pole);
    }

    @Override
    protected boolean shouldBreakOnBlockUpdate() {
        return false;
    }
}
