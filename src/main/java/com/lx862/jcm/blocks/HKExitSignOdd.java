package com.lx862.jcm.blocks;

import com.lx862.jcm.blocks.base.CeilingGroundAttachedDirectionalBlock;
import com.lx862.jcm.util.VoxelUtil;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;

public class HKExitSignOdd extends CeilingGroundAttachedDirectionalBlock {

    public HKExitSignOdd(Settings settings) {
        super(settings, true, false);
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView view, BlockPos pos, ShapeContext context) {
        return VoxelUtil.getDirectionalShape16(state.get(FACING), 0, 9, 7.9, 16, 16, 8.1);
    }

    @Override
    protected boolean shouldBreakOnBlockUpdate() {
        return true;
    }
}
