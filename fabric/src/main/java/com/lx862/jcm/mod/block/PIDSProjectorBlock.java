package com.lx862.jcm.mod.block;

import com.lx862.jcm.mod.block.entity.PIDSProjectorBlockEntity;
import org.mtr.mapping.holder.*;
import org.mtr.mapping.mapper.BlockEntityExtension;
import org.mtr.mod.block.IBlock;

public class PIDSProjectorBlock extends JCMProjectorBlock {

    public PIDSProjectorBlock(BlockSettings settings) {
        super(settings);
    }

    @Override
    public VoxelShape getOutlineShape2(BlockState state, BlockView view, BlockPos pos, ShapeContext context) {
        return VoxelShapes.cuboid(0, 0, 0, 1, 1, 1);
    }

    @Override
    public VoxelShape getCollisionShape2(BlockState state, BlockView view, BlockPos pos, ShapeContext context) {
        return VoxelShapes.empty();
    }


    @Override
    public BlockEntityExtension createBlockEntity(BlockPos blockPos, BlockState blockState) {
        return new PIDSProjectorBlockEntity(blockPos, blockState);
    }
}