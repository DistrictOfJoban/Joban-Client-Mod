package com.lx862.jcm.mod.block;

import com.lx862.jcm.mod.block.entity.LCDPIDSBlockEntity;
import com.lx862.jcm.mod.util.BlockUtil;
import com.lx862.jcm.mod.util.VoxelUtil;
import org.mtr.mapping.holder.*;
import org.mtr.mapping.mapper.BlockEntityExtension;

public class LCDPIDSBlock extends JCMPIDSBlock {

    public LCDPIDSBlock(BlockSettings settings) {
        super(settings);
    }

    @Override
    public VoxelShape getOutlineShape2(BlockState state, BlockView view, BlockPos pos, ShapeContext context) {
        VoxelShape vx1 = VoxelUtil.getDirectionalShape16(BlockUtil.getProperty(state, FACING), 6, -3, 0, 10, 11, 12);
        VoxelShape vx2 = VoxelUtil.getDirectionalShape16(BlockUtil.getProperty(state, FACING), 7.5, 0, 8.5, 8.5, 16, 9.5);
        return VoxelShapes.union(vx1, vx2);
    }

    @Override
    public BlockEntityExtension createBlockEntity(BlockPos blockPos, BlockState blockState) {
        return new LCDPIDSBlockEntity(blockPos, blockState);
    }
}