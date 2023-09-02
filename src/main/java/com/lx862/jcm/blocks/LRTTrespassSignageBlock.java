package com.lx862.jcm.blocks;

import com.lx862.jcm.blocks.base.VerticalMultiBlock;
import com.lx862.jcm.util.VoxelUtil;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;

public class LRTTrespassSignageBlock extends VerticalMultiBlock {

    public LRTTrespassSignageBlock(Settings settings) {
        super(settings, 2);
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView view, BlockPos pos, ShapeContext context) {
        switch(state.get(PART)) {
            case 0:
                return VoxelUtil.getDirectionalShape16(state.get(FACING),7.5, 0, 7, 8.5, 16, 8);
            case 1:
                VoxelShape ShapeA = VoxelUtil.getDirectionalShape16(state.get(FACING),7.5, 0, 7, 8.5, 11, 8);
                VoxelShape ShapeB = VoxelUtil.getDirectionalShape16(state.get(FACING),5.5, 2, 7, 10.5, 10, 8);
                return VoxelShapes.union(ShapeA, ShapeB);
            default:
                return VoxelShapes.empty();
        }
    }
}