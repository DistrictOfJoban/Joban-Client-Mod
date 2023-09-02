package com.lx862.jcm.blocks;

import com.lx862.jcm.blocks.base.WallAttachedBlock;
import com.lx862.jcm.util.VoxelUtil;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;

public class TCLEmergencyButtonBlock extends WallAttachedBlock {
    public TCLEmergencyButtonBlock(Settings settings) {
        super(settings);
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView view, BlockPos pos, ShapeContext context) {
        return VoxelUtil.getDirectionalShape16(state.get(FACING),4.5, 3, 0, 11.5, 13, 6);
    }
}