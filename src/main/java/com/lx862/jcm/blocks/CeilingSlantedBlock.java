package com.lx862.jcm.blocks;

import com.lx862.jcm.blocks.base.DirectionalBlock;
import com.lx862.jcm.util.BlockUtil;
import com.lx862.jcm.util.VoxelUtil;
import org.mtr.mapping.holder.*;
import org.mtr.mapping.holder.BlockSettings;

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
        return VoxelUtil.getDirectionalShape16(BlockUtil.getStateProperty(state, FACING).data, 7, 0, 0, 9, 16, 16);
    }
}
