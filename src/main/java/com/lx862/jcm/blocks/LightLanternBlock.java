package com.lx862.jcm.blocks;

import com.lx862.jcm.blocks.base.DirectionalBlock;
import com.lx862.jcm.util.VoxelUtil;
import org.mtr.mapping.holder.*;

public class LightLanternBlock extends DirectionalBlock {

    public LightLanternBlock(BlockSettings settings) {
        super(settings);
    }

    @Override
    public VoxelShape getOutlineShape2(BlockState state, BlockView view, BlockPos pos, ShapeContext context) {
        return VoxelUtil.getShape16(0, 0, 0, 16, 8, 16);
    }
}
