package com.lx862.jcm.mod.block;

import com.lx862.jcm.mod.block.base.Vertical3Block;
import com.lx862.jcm.mod.block.entity.StationNameStandingBlockEntity;
import org.mtr.mapping.holder.*;
import org.mtr.mapping.mapper.BlockEntityExtension;
import org.mtr.mapping.mapper.BlockWithEntity;
import org.mtr.mod.block.IBlock;

public class StationNameStandingBlock extends Vertical3Block implements BlockWithEntity {
    public StationNameStandingBlock(BlockSettings settings) {
        super(settings);
    }

    @Override
    public VoxelShape getOutlineShape2(BlockState state, BlockView view, BlockPos pos, ShapeContext context) {
        switch (IBlock.getStatePropertySafe(state, new Property<>(THIRD.data))) {
            case LOWER:
                VoxelShape vx1 = IBlock.getVoxelShapeByDirection(1, 0, 0, 2, 16, 1, IBlock.getStatePropertySafe(state, FACING));
                VoxelShape vx2 = IBlock.getVoxelShapeByDirection(14, 0, 0, 15, 16, 1, IBlock.getStatePropertySafe(state, FACING));
                return VoxelShapes.union(vx1, vx2);
            case MIDDLE:
                return IBlock.getVoxelShapeByDirection(1, 0, 0, 15, 16, 1, IBlock.getStatePropertySafe(state, FACING));
            case UPPER:
                return IBlock.getVoxelShapeByDirection(1, 0, 0, 15, 6, 1, IBlock.getStatePropertySafe(state, FACING));
            default:
                return VoxelShapes.empty();
        }
    }

    @Override
    public BlockEntityExtension createBlockEntity(BlockPos blockPos, BlockState blockState) {
        return new StationNameStandingBlockEntity(blockPos, blockState);
    }
}
