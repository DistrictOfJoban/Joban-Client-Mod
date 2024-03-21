package com.lx862.jcm.mod.block;

import com.lx862.jcm.mod.block.base.Vertical3Block;
import com.lx862.jcm.mod.block.entity.StationNameStandingBlockEntity;
import com.lx862.jcm.mod.util.BlockUtil;
import com.lx862.jcm.mod.util.VoxelUtil;
import org.mtr.mapping.holder.*;
import org.mtr.mapping.mapper.BlockEntityExtension;
import org.mtr.mapping.mapper.BlockWithEntity;

public class StationNameStandingBlock extends Vertical3Block implements BlockWithEntity {
    public StationNameStandingBlock(BlockSettings settings) {
        super(settings);
    }

    @Override
    public VoxelShape getOutlineShape2(BlockState state, BlockView view, BlockPos pos, ShapeContext context) {
        switch (BlockUtil.getProperty(state, new Property<>(THIRD.data))) {
            case LOWER:
                VoxelShape vx1 = VoxelUtil.getDirectionalShape16(BlockUtil.getProperty(state, FACING), 1, 0, 0, 2, 16, 1);
                VoxelShape vx2 = VoxelUtil.getDirectionalShape16(BlockUtil.getProperty(state, FACING), 14, 0, 0, 15, 16, 1);
                return VoxelShapes.union(vx1, vx2);
            case MIDDLE:
                return VoxelUtil.getDirectionalShape16(BlockUtil.getProperty(state, FACING), 1, 0, 0, 15, 16, 1);
            case UPPER:
                return VoxelUtil.getDirectionalShape16(BlockUtil.getProperty(state, FACING), 1, 0, 0, 15, 6, 1);
            default:
                return VoxelShapes.empty();
        }
    }

    @Override
    public BlockEntityExtension createBlockEntity(BlockPos blockPos, BlockState blockState) {
        return new StationNameStandingBlockEntity(blockPos, blockState);
    }
}
