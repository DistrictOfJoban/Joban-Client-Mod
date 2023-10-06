package com.lx862.jcm.mod.blocks;

import com.lx862.jcm.mod.blocks.base.WallAttachedBlock;
import com.lx862.jcm.mod.util.BlockUtil;
import com.lx862.jcm.mod.util.VoxelUtil;
import org.mtr.mapping.holder.*;

public class TCLEmergencyButtonBlock extends WallAttachedBlock {
    public TCLEmergencyButtonBlock(BlockSettings settings) {
        super(settings);
    }

    @Override
    public VoxelShape getOutlineShape2(BlockState state, BlockView view, BlockPos pos, ShapeContext context) {
        return VoxelUtil.getDirectionalShape16(BlockUtil.getProperty(state, FACING), 4.5, 3, 0, 11.5, 13, 6);
    }
}