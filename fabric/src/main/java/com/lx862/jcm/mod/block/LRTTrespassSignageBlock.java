package com.lx862.jcm.mod.block;

import com.lx862.jcm.mod.block.base.Vertical2Block;
import org.mtr.mod.block.IBlock;
import org.mtr.mapping.holder.*;

public class LRTTrespassSignageBlock extends Vertical2Block {

    public LRTTrespassSignageBlock(BlockSettings settings) {
        super(settings);
    }

    @Override
    public VoxelShape getOutlineShape2(BlockState state, BlockView view, BlockPos pos, ShapeContext context) {
        switch (IBlock.getStatePropertySafe(state, new Property<>(HALF.data))) {
            case LOWER:
                return IBlock.getVoxelShapeByDirection(7.5, 0, 7, 8.5, 16, 8, IBlock.getStatePropertySafe(state, FACING));
            case UPPER:
                VoxelShape vx1R = IBlock.getVoxelShapeByDirection(7.5, 0, 7, 8.5, 11, 8, IBlock.getStatePropertySafe(state, FACING));
                VoxelShape vx2R = IBlock.getVoxelShapeByDirection(5.5, 2, 7, 10.5, 10, 8, IBlock.getStatePropertySafe(state, FACING));
                return VoxelShapes.union(vx1R, vx2R);
            default:
                return VoxelShapes.empty();
        }
    }
}