package com.lx862.jcm.blocks;

import com.lx862.jcm.blocks.base.VerticallyAttachedDirectionalBlock;
import com.lx862.jcm.util.BlockUtil;
import com.lx862.jcm.util.VoxelUtil;
import org.mtr.mapping.holder.*;

public class ExitSignOdd extends VerticallyAttachedDirectionalBlock {

    public ExitSignOdd(BlockSettings settings) {
        super(settings, true, false);
    }

    @Override
    public VoxelShape getOutlineShape2(BlockState state, BlockView view, BlockPos pos, ShapeContext context) {
        return VoxelUtil.getDirectionalShape16(BlockUtil.getProperty(state, FACING), 0, 9, 7.9, 16, 16, 8.1);
    }

    @Override
    protected boolean shouldBreakOnBlockUpdate() {
        return true;
    }
}
