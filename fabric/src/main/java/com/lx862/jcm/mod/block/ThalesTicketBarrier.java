package com.lx862.jcm.mod.block;

import com.lx862.jcm.mod.util.BlockUtil;
import com.lx862.jcm.mod.util.VoxelUtil;
import org.mtr.mapping.holder.*;
import org.mtr.mod.block.BlockTicketBarrier;

public class ThalesTicketBarrier extends BlockTicketBarrier {
    public ThalesTicketBarrier(boolean isEntrance) {
        super(isEntrance);
    }

    @Override
    public VoxelShape getOutlineShape2(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        Direction facing = BlockUtil.getProperty(state, FACING);
        return VoxelUtil.getDirectionalShape16(facing, 12, 0, 0, 16, 16, 16);
    }
}
