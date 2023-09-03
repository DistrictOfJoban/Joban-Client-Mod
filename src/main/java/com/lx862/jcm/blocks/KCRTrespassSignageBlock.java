package com.lx862.jcm.blocks;

import com.lx862.jcm.blocks.base.HorizontalWallAttachedMultiBlock;
import com.lx862.jcm.util.BlockUtil;
import com.lx862.jcm.util.VoxelUtil;
import org.mtr.mapping.holder.*;

public class KCRTrespassSignageBlock extends HorizontalWallAttachedMultiBlock {

    public KCRTrespassSignageBlock(BlockSettings settings) {
        super(settings);
    }

    @Override
    public VoxelShape getOutlineShape2(BlockState state, BlockView view, BlockPos pos, ShapeContext context) {
        switch (BlockUtil.getProperty(state, PART)) {
            case 0:
                return VoxelUtil.getDirectionalShape16(BlockUtil.getProperty(state, FACING), 4, 2, 0, 16, 14, 0.1);
            case 1:
                return VoxelUtil.getDirectionalShape16(BlockUtil.getProperty(state, FACING), 0, 2, 0, 12, 14, 0.1);
            default:
                return VoxelShapes.empty();
        }
    }
}
