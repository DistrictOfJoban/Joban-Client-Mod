package com.lx862.jcm.mod.block;

import com.lx862.jcm.mod.block.entity.PIDS1ABlockEntity;
import com.lx862.jcm.mod.util.BlockUtil;
import com.lx862.jcm.mod.util.VoxelUtil;
import org.mtr.mapping.holder.*;
import org.mtr.mapping.mapper.BlockEntityExtension;
import org.mtr.mod.block.BlockPIDSHorizontalBase;

public class PIDS1ABlock extends BlockPIDSHorizontalBase {
    public static final int MAX_ARRIVALS = 3;
    public PIDS1ABlock() {
        super(MAX_ARRIVALS);
    }

    @Override
    public VoxelShape getOutlineShape2(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        VoxelShape shape1 = VoxelUtil.getDirectionalShape16(BlockUtil.getProperty(state, FACING), 6, 0, 0, 10, 11, 16);
        VoxelShape shape2 = VoxelUtil.getDirectionalShape16(BlockUtil.getProperty(state, FACING), 7.5, 11, 12.5, 8.5, 16, 13.5);
        return VoxelShapes.union(shape1, shape2);
    }

    @Override
    public BlockEntityExtension createBlockEntity(BlockPos blockPos, BlockState blockState) {
        return new PIDS1ABlockEntity(blockPos, blockState);
    }
}
