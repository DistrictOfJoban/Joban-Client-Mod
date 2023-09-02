package com.lx862.jcm.blocks;

import com.lx862.jcm.blocks.base.CeilingGroundAttachedBlock;
import com.lx862.jcm.util.VoxelUtil;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;

public class SpotLampBlock extends CeilingGroundAttachedBlock {

    public SpotLampBlock(Settings settings) {
        super(settings, true, true);
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView view, BlockPos pos, ShapeContext context) {
        if(state.get(TOP)) {
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
