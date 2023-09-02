package com.lx862.jcm.blocks;

import com.lx862.jcm.blocks.base.VerticalMultiBlock;
import com.lx862.jcm.util.VoxelUtil;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;

public class SILEmergencyButtonBlock extends VerticalMultiBlock {
    public SILEmergencyButtonBlock(Settings settings) {
        super(settings, 3);
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView view, BlockPos pos, ShapeContext context) {
        switch(state.get(PART)) {
            case 0:
            case 1:
                return VoxelUtil.getDirectionalShape16(state.get(FACING),4, 0, 7.5, 12, 16, 8.5);
            case 2:
                return VoxelUtil.getDirectionalShape16(state.get(FACING),4, 0, 7.5, 12, 12, 8.5);
            default:
                return VoxelShapes.empty();
        }
    }
}
