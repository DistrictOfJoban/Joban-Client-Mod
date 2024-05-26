package com.lx862.jcm.mod.block;

import com.lx862.jcm.mod.block.base.CeilingAttachedDirectional2Block;
import com.lx862.jcm.mod.util.BlockUtil;
import com.lx862.jcm.mod.util.VoxelUtil;
import org.mtr.mapping.holder.*;

public class ExitSignEven extends CeilingAttachedDirectional2Block {

    public ExitSignEven(BlockSettings settings) {
        super(settings, true, false);
    }

    @Override
    public VoxelShape getOutlineShape2(BlockState state, BlockView view, BlockPos pos, ShapeContext context) {
        if(BlockUtil.getProperty(state, IS_LEFT)) {
            return VoxelUtil.getDirectionalShape16(BlockUtil.getProperty(state, FACING), 8, 9, 7.9, 16, 16, 8.1);
        } else {
            return VoxelUtil.getDirectionalShape16(BlockUtil.getProperty(state, FACING), 0, 9, 7.9, 8, 16, 8.1);
        }
    }
}
