package com.lx862.jcm.mod.block;

import com.lx862.jcm.mod.block.base.VerticallyAttachedBlock;
import com.lx862.jcm.mod.util.BlockUtil;
import com.lx862.jcm.mod.util.VoxelUtil;
import org.mtr.mapping.holder.*;

public class SpotLampBlock extends VerticallyAttachedBlock {

    public SpotLampBlock(BlockSettings settings) {
        super(settings, true, true);
    }

    @Override
    public VoxelShape getOutlineShape2(BlockState state, BlockView view, BlockPos pos, ShapeContext context) {
        if (BlockUtil.getProperty(state, TOP)) {
            return VoxelUtil.getShape16(4, 15.75, 4, 12, 16, 12);
        } else {
            return VoxelUtil.getShape16(4, 0, 4, 12, 0.25, 12);
        }
    }
}
