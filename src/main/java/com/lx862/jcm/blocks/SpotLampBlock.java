package com.lx862.jcm.blocks;

import com.lx862.jcm.blocks.base.CeilingGroundAttachedBlock;
import com.lx862.jcm.util.BlockUtil;
import com.lx862.jcm.util.VoxelUtil;
import org.mtr.mapping.holder.*;

public class SpotLampBlock extends CeilingGroundAttachedBlock {

    public SpotLampBlock(BlockSettings settings) {
        super(settings, true, true);
    }

    @Override
    public VoxelShape getOutlineShape2(BlockState state, BlockView view, BlockPos pos, ShapeContext context) {
        if(BlockUtil.getStateProperty(state, TOP)) {
            return VoxelUtil.getShape16(4, 15.75, 4, 12, 16, 12);
        } else {
            return VoxelUtil.getShape16(4, 0, 4, 12, 0.25, 12);
        }
    }

    @Override
    protected boolean shouldBreakOnBlockUpdate() {
        return true;
    }
}
