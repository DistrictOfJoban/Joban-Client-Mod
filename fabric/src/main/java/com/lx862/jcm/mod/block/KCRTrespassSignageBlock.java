package com.lx862.jcm.mod.block;

import com.lx862.jcm.mod.block.base.HorizontalWallAttached2Block;
import com.lx862.jcm.mod.util.BlockUtil;
import com.lx862.jcm.mod.util.VoxelUtil;
import org.mtr.mapping.holder.*;

public class KCRTrespassSignageBlock extends HorizontalWallAttached2Block {

    public KCRTrespassSignageBlock(BlockSettings settings) {
        super(settings);
    }

    @Override
    public VoxelShape getOutlineShape2(BlockState state, BlockView view, BlockPos pos, ShapeContext context) {
        if(BlockUtil.getProperty(state, IS_LEFT)) {
            return VoxelUtil.getDirectionalShape16(BlockUtil.getProperty(state, FACING), 4, 2, 0, 16, 14, 0.1);
        } else {
            return VoxelUtil.getDirectionalShape16(BlockUtil.getProperty(state, FACING), 0, 2, 0, 12, 14, 0.1);
        }
    }
}
