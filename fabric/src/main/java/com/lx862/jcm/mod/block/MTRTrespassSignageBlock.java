package com.lx862.jcm.mod.block;

import com.lx862.jcm.mod.block.base.Horizontal2Block;
import com.lx862.jcm.mod.util.BlockUtil;
import com.lx862.jcm.mod.util.VoxelUtil;
import org.mtr.mapping.holder.*;

public class MTRTrespassSignageBlock extends Horizontal2Block {

    public MTRTrespassSignageBlock(BlockSettings settings) {
        super(settings);
    }

    @Override
    public org.mtr.mapping.holder.VoxelShape getOutlineShape2(BlockState state, BlockView view, BlockPos pos, ShapeContext context) {
        if(BlockUtil.getProperty(state, IS_LEFT)) {
            VoxelShape vx1 = VoxelUtil.getDirectionalShape16(BlockUtil.getProperty(state, FACING), 2, 4, 7.5, 16, 16, 8.5);
            VoxelShape vx2 = VoxelUtil.getDirectionalShape16(BlockUtil.getProperty(state, FACING), 7, 0, 7.5, 8, 16, 8.5);
            return VoxelShapes.union(vx1, vx2);
        } else {
            VoxelShape vx1R = VoxelUtil.getDirectionalShape16(BlockUtil.getProperty(state, FACING), 0, 4, 7.5, 14, 16, 8.5);
            VoxelShape vx2R = VoxelUtil.getDirectionalShape16(BlockUtil.getProperty(state, FACING), 8, 0, 7.5, 9, 16, 8.5);
            return VoxelShapes.union(vx1R, vx2R);
        }
    }
}
