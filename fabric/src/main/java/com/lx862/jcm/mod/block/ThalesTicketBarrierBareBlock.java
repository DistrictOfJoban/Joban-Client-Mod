package com.lx862.jcm.mod.block;

import com.lx862.jcm.mod.block.base.DirectionalBlock;
import com.lx862.jcm.mod.data.BlockProperties;
import com.lx862.jcm.mod.util.BlockUtil;
import com.lx862.jcm.mod.util.VoxelUtil;
import org.mtr.mapping.holder.*;
import org.mtr.mapping.tool.HolderBase;

import java.util.List;

public class ThalesTicketBarrierBareBlock extends DirectionalBlock {
    public static final IntegerProperty FENCE_TYPE = BlockProperties.BARRIER_FENCE_TYPE;
    public static final BooleanProperty FLIPPED = BlockProperties.BARRIER_FLIPPED;

    public ThalesTicketBarrierBareBlock(BlockSettings settings) {
        super(settings);
    }

    @Override
    public VoxelShape getOutlineShape2(BlockState state, BlockView view, BlockPos pos, ShapeContext context) {
        boolean hasFence = BlockUtil.getProperty(state, FENCE_TYPE) != 0;
        boolean flipped = BlockUtil.getProperty(state, FLIPPED);
        VoxelShape mainBarrierShape = VoxelUtil.getDirectionalShape16(BlockUtil.getProperty(state, FACING), 12, 0, 0, 16, 16, 16);

        VoxelShape vx1;
        if(hasFence) {
            if(flipped) {
                vx1 = VoxelUtil.getDirectionalShape16(BlockUtil.getProperty(state, FACING), 0, 0, 13, 12, 19, 16);
            } else {
                vx1 = VoxelUtil.getDirectionalShape16(BlockUtil.getProperty(state, FACING), 0, 0, 0, 12, 19, 3);
            }
        } else {
            vx1 = VoxelShapes.empty();
        }
        return VoxelShapes.union(mainBarrierShape, vx1);
    }

    @Override
    public VoxelShape getCollisionShape2(BlockState state, BlockView view, BlockPos pos, ShapeContext context) {
        boolean hasFence = BlockUtil.getProperty(state, FENCE_TYPE) != 0;
        boolean flipped = BlockUtil.getProperty(state, FLIPPED);

        VoxelShape mainBarrierShape = VoxelUtil.getDirectionalShape16(BlockUtil.getProperty(state, FACING), 12, 0, 0, 16, 24, 16);

        VoxelShape vx1;
        if(hasFence) {
            if(flipped) {
                vx1 = VoxelUtil.getDirectionalShape16(BlockUtil.getProperty(state, FACING), 0, 0, 13, 12, 24, 16);
            } else {
                vx1 = VoxelUtil.getDirectionalShape16(BlockUtil.getProperty(state, FACING), 0, 0, 0, 12, 24, 3);
            }
        } else {
            vx1 = VoxelShapes.empty();
        }
        return VoxelShapes.union(mainBarrierShape, vx1);
    }

    @Override
    public void addBlockProperties(List<HolderBase<?>> properties) {
        super.addBlockProperties(properties);
        properties.add(FENCE_TYPE);
        properties.add(FLIPPED);
    }
}
