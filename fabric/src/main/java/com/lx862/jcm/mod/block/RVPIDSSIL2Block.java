package com.lx862.jcm.mod.block;

import com.lx862.jcm.mod.block.entity.RVPIDSSIL2BlockEntity;
import org.mtr.mapping.holder.*;
import org.mtr.mapping.mapper.BlockEntityExtension;
import org.mtr.mod.block.IBlock;

public class RVPIDSSIL2Block extends JCMPIDSBlock {

    public RVPIDSSIL2Block(BlockSettings settings) {
        super(settings);
    }

    @Override
    public VoxelShape getOutlineShape2(BlockState state, BlockView view, BlockPos pos, ShapeContext context) {
        VoxelShape vx1 = IBlock.getVoxelShapeByDirection(0, -2, 0, 16, 9, 16, IBlock.getStatePropertySafe(state, FACING));
        VoxelShape vx2 = IBlock.getVoxelShapeByDirection(7.5, 9, 8.5, 8.5, 16, 9.5, IBlock.getStatePropertySafe(state, FACING));
        return VoxelShapes.union(vx1, vx2);
    }

    @Override
    public BlockEntityExtension createBlockEntity(BlockPos blockPos, BlockState blockState) {
        return new RVPIDSSIL2BlockEntity(blockPos, blockState);
    }
}