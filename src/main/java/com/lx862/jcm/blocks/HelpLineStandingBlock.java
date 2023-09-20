package com.lx862.jcm.blocks;

import com.lx862.jcm.blocks.base.Vertical3Block;
import com.lx862.jcm.util.BlockUtil;
import com.lx862.jcm.util.VoxelUtil;
import org.mtr.mapping.holder.*;

public class HelpLineStandingBlock extends Vertical3Block {
    public HelpLineStandingBlock(BlockSettings settings) {
        super(settings);
    }

    @Override
    public VoxelShape getOutlineShape2(BlockState state, BlockView view, BlockPos pos, ShapeContext context) {
        switch (BlockUtil.getProperty(state, PART)) {
            case 0:
            case 2:
                return VoxelUtil.getDirectionalShape16(BlockUtil.getProperty(state, FACING), 4, 0, 7.5, 12, 16, 8.5);
            case 1:
                VoxelShape vx1 = VoxelUtil.getDirectionalShape16(BlockUtil.getProperty(state, FACING), 4, 0, 7.5, 12, 16, 8.5);
                VoxelShape vx2 = VoxelUtil.getDirectionalShape16(BlockUtil.getProperty(state, FACING), 5.5, 0, 8.5, 10.5, 16, 10.5);
                return VoxelShapes.union(vx1, vx2);
            default:
                return VoxelShapes.empty();
        }
    }
}