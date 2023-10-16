package com.lx862.jcm.mod.block;

import com.lx862.jcm.mod.block.base.Vertical3Block;
import com.lx862.jcm.mod.util.BlockUtil;
import com.lx862.jcm.mod.util.VoxelUtil;
import org.mtr.mapping.holder.*;

public class TMLEmergencyButtonBlock extends Vertical3Block {
    public TMLEmergencyButtonBlock(BlockSettings settings) {
        super(settings);
    }

    @Override
    public VoxelShape getOutlineShape2(BlockState state, BlockView view, BlockPos pos, ShapeContext context) {
        switch (BlockUtil.getProperty(state, PART)) {
            case 0:
            case 1:
                return VoxelUtil.getDirectionalShape16(BlockUtil.getProperty(state, FACING), 4, 0, 7.5, 12, 16, 8.5);
            case 2:
                return VoxelUtil.getDirectionalShape16(BlockUtil.getProperty(state, FACING), 4, 0, 7.5, 12, 12, 8.5);
            default:
                return VoxelShapes.empty();
        }
    }
}