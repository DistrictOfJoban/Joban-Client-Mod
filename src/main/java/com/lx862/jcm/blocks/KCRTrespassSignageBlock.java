package com.lx862.jcm.blocks;

import com.lx862.jcm.blocks.base.HorizontalWallAttachedMultiBlock;
import com.lx862.jcm.util.VoxelUtil;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;

public class KCRTrespassSignageBlock extends HorizontalWallAttachedMultiBlock {

    public KCRTrespassSignageBlock(Settings settings) {
        super(settings);
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView view, BlockPos pos, ShapeContext context) {
        switch(state.get(PART)) {
            case 0:
                return VoxelUtil.getDirectionalShape16(state.get(FACING), 4, 2, 0, 16, 14, 0.1);
            case 1:
                return VoxelUtil.getDirectionalShape16(state.get(FACING), 0, 2, 0, 12, 14, 0.1);
            default:
                return VoxelShapes.empty();
        }
    }
}
