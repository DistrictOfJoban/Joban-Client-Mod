package com.lx862.jcm.mod.block;

import com.lx862.jcm.mod.block.base.DirectionalBlock;
import com.lx862.jcm.mod.util.BlockUtil;
import org.mtr.mod.block.IBlock;
import org.mtr.mapping.holder.*;

public class CeilingSlantedBlock extends DirectionalBlock {

    public CeilingSlantedBlock(BlockSettings settings) {
        super(settings);
    }

    @Override
    public VoxelShape getOutlineShape2(BlockState state, BlockView view, BlockPos pos, ShapeContext context) {
        return VoxelShapes.fullCube();
    }

    @Override
    public VoxelShape getCollisionShape2(BlockState state, BlockView view, BlockPos pos, ShapeContext context) {
        return IBlock.getVoxelShapeByDirection(7, 0, 0, 9, 16, 16, IBlock.getStatePropertySafe(state, FACING));
    }
}
