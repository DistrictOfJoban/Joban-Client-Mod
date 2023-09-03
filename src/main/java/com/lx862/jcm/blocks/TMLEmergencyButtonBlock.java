package com.lx862.jcm.blocks;

import com.lx862.jcm.blocks.base.VerticalMultiBlock;
import com.lx862.jcm.util.BlockUtil;
import com.lx862.jcm.util.VoxelUtil;
import org.mtr.mapping.holder.*;

public class TMLEmergencyButtonBlock extends VerticalMultiBlock {
    public TMLEmergencyButtonBlock(BlockSettings settings) {
        super(settings, 3);
    }

    @Override
    public VoxelShape getOutlineShape2(BlockState state, BlockView view, BlockPos pos, ShapeContext context) {
        switch(BlockUtil.getStateProperty(state, PART)) {
            case 0:
            case 1:
                return VoxelUtil.getDirectionalShape16(BlockUtil.getStateProperty(state, FACING).data,4, 0, 7.5, 12, 16, 8.5);
            case 2:
                return VoxelUtil.getDirectionalShape16(BlockUtil.getStateProperty(state, FACING).data,4, 0, 7.5, 12, 12, 8.5);
            default:
                return VoxelShapes.empty();
        }
    }
}