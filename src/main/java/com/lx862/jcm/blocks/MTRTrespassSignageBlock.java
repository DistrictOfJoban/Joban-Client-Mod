package com.lx862.jcm.blocks;

import com.lx862.jcm.blocks.base.HorizontalMultiBlock;
import com.lx862.jcm.util.VoxelUtil;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;

public class MTRTrespassSignageBlock extends HorizontalMultiBlock {

    public MTRTrespassSignageBlock(Settings settings) {
        super(settings);
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView view, BlockPos pos, ShapeContext context) {
        switch(state.get(PART)) {
            case 0:
                VoxelShape vx1 = VoxelUtil.getDirectionalShape16(state.get(FACING), 2, 4, 7.5, 16, 16, 8.5);
                VoxelShape vx2 = VoxelUtil.getDirectionalShape16(state.get(FACING), 7, 0, 7.5, 8, 16, 8.5);
                return VoxelShapes.union(vx1, vx2);
            case 1:
                VoxelShape vx1R = VoxelUtil.getDirectionalShape16(state.get(FACING), 0, 4, 7.5, 14, 16, 8.5);
                VoxelShape vx2R = VoxelUtil.getDirectionalShape16(state.get(FACING), 8, 0, 7.5, 9, 16, 8.5);
                return VoxelShapes.union(vx1R, vx2R);
            default:
                return VoxelShapes.empty();
        }
    }
}
