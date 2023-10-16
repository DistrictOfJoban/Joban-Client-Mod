package com.lx862.jcm.mod.block;

import com.lx862.jcm.mod.block.base.Vertical2Block;
import com.lx862.jcm.mod.util.BlockUtil;
import com.lx862.jcm.mod.util.VoxelUtil;
import org.mtr.mapping.holder.*;

public class LRTTrespassSignageBlock extends Vertical2Block {

    public LRTTrespassSignageBlock(BlockSettings settings) {
        super(settings);
    }

    @Override
    public VoxelShape getOutlineShape2(BlockState state, BlockView view, BlockPos pos, ShapeContext context) {
        switch (BlockUtil.getProperty(state, PART)) {
            case 0:
                return VoxelUtil.getDirectionalShape16(BlockUtil.getProperty(state, FACING), 7.5, 0, 7, 8.5, 16, 8);
            case 1:
                VoxelShape vx1R = VoxelUtil.getDirectionalShape16(BlockUtil.getProperty(state, FACING), 7.5, 0, 7, 8.5, 11, 8);
                VoxelShape vx2R = VoxelUtil.getDirectionalShape16(BlockUtil.getProperty(state, FACING), 5.5, 2, 7, 10.5, 10, 8);
                return VoxelShapes.union(vx1R, vx2R);
            default:
                return VoxelShapes.empty();
        }
    }
}