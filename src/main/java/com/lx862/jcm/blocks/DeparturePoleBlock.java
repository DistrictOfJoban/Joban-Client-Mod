package com.lx862.jcm.blocks;

import com.lx862.jcm.blocks.base.DirectionalBlock;
import com.lx862.jcm.util.BlockUtil;
import com.lx862.jcm.util.VoxelUtil;
import org.mtr.mapping.holder.*;

public class DeparturePoleBlock extends DirectionalBlock {

    public DeparturePoleBlock(BlockSettings settings) {
        super(settings);
    }

    @Override
    public VoxelShape getOutlineShape2(BlockState state, BlockView view, BlockPos pos, ShapeContext context) {
        return VoxelUtil.getDirectionalShape16(BlockUtil.getProperty(state, FACING), 6.5, 0, 13, 9.5, 16, 16);
    }
}
