package com.lx862.jcm.blocks;

import com.lx862.jcm.blocks.base.WallAttachedBlock;
import com.lx862.jcm.util.BlockUtil;
import com.lx862.jcm.util.VoxelUtil;
import org.mtr.mapping.holder.*;

public class FireAlarmWall extends WallAttachedBlock {
    public FireAlarmWall(BlockSettings settings) {
        super(settings);
    }

    @Override
    public VoxelShape getOutlineShape2(BlockState state, BlockView view, BlockPos pos, ShapeContext context) {
        return VoxelUtil.getDirectionalShape16(BlockUtil.getProperty(state, FACING), 5, 5, 0, 11, 11, 1);
    }
}
