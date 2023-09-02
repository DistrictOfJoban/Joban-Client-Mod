package com.lx862.jcm.blocks;

import com.lx862.jcm.blocks.base.CeilingGroundAttachedDirectionalMultiBlock;
import com.lx862.jcm.util.VoxelUtil;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;

public class WRLStationCeilingBlock extends CeilingGroundAttachedDirectionalMultiBlock {

    public WRLStationCeilingBlock(Settings settings) {
        super(settings, true, false);
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView view, BlockPos pos, ShapeContext context) {
        switch(state.get(PART)) {
            case 0:
                VoxelShape vx1 = VoxelUtil.getDirectionalShape16(state.get(FACING), 0.5, 8, 1, 16, 9, 15);
                VoxelShape vx2 = VoxelUtil.getDirectionalShape16(state.get(FACING), 5.5, 9, 7.5, 6.5, 16, 8.5);
                return VoxelShapes.union(vx1, vx2);
            default:
                VoxelShape vx1R = VoxelUtil.getDirectionalShape16(state.get(FACING), 0, 8, 1, 15.5, 9, 15);
                VoxelShape vx2R = VoxelUtil.getDirectionalShape16(state.get(FACING), 10.5, 9, 7.5, 11.5, 16, 8.5);
                return VoxelShapes.union(vx1R, vx2R);
        }
    }

    @Override
    protected boolean shouldBreakOnBlockUpdate() {
        return false;
    }
}
