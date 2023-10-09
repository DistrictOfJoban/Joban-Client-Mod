package com.lx862.jcm.mod.block;

import com.lx862.jcm.mod.block.base.VerticallyAttachedDirectional2Block;
import com.lx862.jcm.mod.util.BlockUtil;
import com.lx862.jcm.mod.util.VoxelUtil;
import org.mtr.mapping.holder.*;

public class ExitSignEven extends VerticallyAttachedDirectional2Block {

    public ExitSignEven(BlockSettings settings) {
        super(settings, true, false);
    }

    @Override
    public VoxelShape getOutlineShape2(BlockState state, BlockView view, BlockPos pos, ShapeContext context) {
        switch(BlockUtil.getProperty(state, PART)) {
            case 0:
                return VoxelUtil.getDirectionalShape16(BlockUtil.getProperty(state, FACING), 8, 9, 7.9, 16, 16, 8.1);
            case 1:
                return VoxelUtil.getDirectionalShape16(BlockUtil.getProperty(state, FACING), 0, 9, 7.9, 8, 16, 8.1);
            default:
                return VoxelShapes.empty();
        }
    }

    @Override
    protected boolean shouldBreakOnBlockUpdate() {
        return true;
    }
}
