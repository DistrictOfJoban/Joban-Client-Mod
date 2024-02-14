package com.lx862.jcm.mod.block;

import com.lx862.jcm.mod.block.base.Vertical2Block;
import com.lx862.jcm.mod.util.BlockUtil;
import com.lx862.jcm.mod.util.VoxelUtil;
import org.mtr.mapping.holder.*;

public class HelpLineStandingEALBlock extends Vertical2Block {
    public HelpLineStandingEALBlock(BlockSettings settings) {
        super(settings);
    }

    @Override
    public VoxelShape getOutlineShape2(BlockState state, BlockView view, BlockPos pos, ShapeContext context) {
        switch (BlockUtil.getProperty(state, new Property<>(HALF.data))) {
            case LOWER:
                return VoxelUtil.getDirectionalShape16(BlockUtil.getProperty(state, FACING), 0, 0, 5.5, 16, 16, 10.5);
            case UPPER:
                return VoxelUtil.getDirectionalShape16(BlockUtil.getProperty(state, FACING), 0, 0, 5.5, 16, 24, 10.5);
            default:
                return VoxelShapes.empty();
        }
    }
}