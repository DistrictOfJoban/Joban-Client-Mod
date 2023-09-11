package com.lx862.jcm.blocks;

import com.lx862.jcm.blocks.base.VerticalDoubleBlock;
import com.lx862.jcm.util.BlockUtil;
import com.lx862.jcm.util.VoxelUtil;
import org.mtr.mapping.holder.*;

public class HelpLineStandingEALBlock extends VerticalDoubleBlock {
    public HelpLineStandingEALBlock(BlockSettings settings) {
        super(settings);
    }

    @Override
    public VoxelShape getOutlineShape2(BlockState state, BlockView view, BlockPos pos, ShapeContext context) {
        switch (BlockUtil.getProperty(state, PART)) {
            case 0:
                return VoxelUtil.getDirectionalShape16(BlockUtil.getProperty(state, FACING), 0, 0, 5.5, 16, 16, 10.5);
            case 1:
                return VoxelUtil.getDirectionalShape16(BlockUtil.getProperty(state, FACING), 0, 0, 5.5, 16, 24, 10.5);
            default:
                return VoxelShapes.empty();
        }
    }
}