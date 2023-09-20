package com.lx862.jcm.blocks;

import com.lx862.jcm.blocks.base.Horizontal2Block;
import com.lx862.jcm.util.BlockUtil;
import com.lx862.jcm.util.VoxelUtil;
import org.mtr.mapping.holder.*;

public class MTRTrainModelBlock extends Horizontal2Block {
    public MTRTrainModelBlock(BlockSettings settings) {
        super(settings);
    }

    @Override
    public VoxelShape getOutlineShape2(BlockState state, BlockView view, BlockPos pos, ShapeContext context) {
        return VoxelUtil.getDirectionalShape16(BlockUtil.getProperty(state, FACING), 0, 0, 3.5, 16, 9, 12.5);
    }
}
