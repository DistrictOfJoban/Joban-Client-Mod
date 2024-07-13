package com.lx862.jcm.mod.block;

import com.lx862.jcm.mod.block.base.DirectionalBlock;
import com.lx862.jcm.mod.data.BlockProperties;
import com.lx862.jcm.mod.util.BlockUtil;
import org.mtr.mapping.holder.*;
import org.mtr.mapping.tool.HolderBase;
import org.mtr.mod.block.BlockGlassFence;
import org.mtr.mod.block.IBlock;

import java.util.List;

public class ThalesTicketBarrierBareBlock extends DirectionalBlock {
    public static final IntegerProperty FENCE_TYPE = BlockProperties.BARRIER_FENCE_TYPE;
    public static final BooleanProperty FLIPPED = BlockProperties.BARRIER_FLIPPED;

    public ThalesTicketBarrierBareBlock(BlockSettings settings) {
        super(settings);
    }

    @Override
    public VoxelShape getOutlineShape2(BlockState state, BlockView view, BlockPos pos, ShapeContext context) {
        boolean hasFence = IBlock.getStatePropertySafe(state, FENCE_TYPE) != 0;
        boolean flipped = IBlock.getStatePropertySafe(state, FLIPPED);
        VoxelShape mainBarrierShape = IBlock.getVoxelShapeByDirection(12, 0, 0, 16, 16, 16, IBlock.getStatePropertySafe(state, FACING));

        VoxelShape vx1;
        if(hasFence) {
            if(flipped) {
                vx1 = IBlock.getVoxelShapeByDirection(0, 0, 13, 12, 19, 16, IBlock.getStatePropertySafe(state, FACING));
            } else {
                vx1 = IBlock.getVoxelShapeByDirection(0, 0, 0, 12, 19, 3, IBlock.getStatePropertySafe(state, FACING));
            }
        } else {
            vx1 = VoxelShapes.empty();
        }
        return VoxelShapes.union(mainBarrierShape, vx1);
    }

    @Override
    public VoxelShape getCollisionShape2(BlockState state, BlockView view, BlockPos pos, ShapeContext context) {
        boolean hasFence = IBlock.getStatePropertySafe(state, FENCE_TYPE) != 0;
        boolean flipped = IBlock.getStatePropertySafe(state, FLIPPED);

        VoxelShape mainBarrierShape = IBlock.getVoxelShapeByDirection(12, 0, 0, 16, 24, 16, IBlock.getStatePropertySafe(state, FACING));

        VoxelShape vx1;
        if(hasFence) {
            if(flipped) {
                vx1 = IBlock.getVoxelShapeByDirection(0, 0, 13, 12, 24, 16, IBlock.getStatePropertySafe(state, FACING));
            } else {
                vx1 = IBlock.getVoxelShapeByDirection(0, 0, 0, 12, 24, 3, IBlock.getStatePropertySafe(state, FACING));
            }
        } else {
            vx1 = VoxelShapes.empty();
        }
        return VoxelShapes.union(mainBarrierShape, vx1);
    }

    @Override
    public BlockState getStateForNeighborUpdate2(BlockState state, Direction direction, BlockState neighborState, WorldAccess world, BlockPos pos, BlockPos neighborPos) {
        return getFenceState(neighborState, direction, state, World.cast(world), pos);
    }

    private BlockState getFenceState(BlockState stateNear, Direction direction, BlockState state, World world, BlockPos pos) {
        if(stateNear.getBlock().data instanceof ThalesTicketBarrier || stateNear.getBlock().data instanceof ThalesTicketBarrierBareBlock) {
            return state;
        }

        Direction thisDirection = IBlock.getStatePropertySafe(state, FACING);
        if(stateNear.getBlock().data instanceof BlockGlassFence) {
            Direction nearbyFacing = IBlock.getStatePropertySafe(stateNear, FACING);
            boolean valid = (nearbyFacing == thisDirection) || (nearbyFacing == thisDirection.getOpposite());
            boolean flipped = nearbyFacing != thisDirection;

            /* Don't connect fence that are placed 90deg to the barrier */
            if(direction != thisDirection.rotateYClockwise() && direction != thisDirection.rotateYCounterclockwise()) {
                valid = false;
            }

            if(valid) {
                int fenceType = 0;
                if(stateNear.isOf(org.mtr.mod.Blocks.GLASS_FENCE_CIO.get())) fenceType = 1;
                if(stateNear.isOf(org.mtr.mod.Blocks.GLASS_FENCE_CKT.get())) fenceType = 2;
                if(stateNear.isOf(org.mtr.mod.Blocks.GLASS_FENCE_HEO.get())) fenceType = 3;
                if(stateNear.isOf(org.mtr.mod.Blocks.GLASS_FENCE_MOS.get())) fenceType = 4;
                if(stateNear.isOf(org.mtr.mod.Blocks.GLASS_FENCE_PLAIN.get())) fenceType = 5;
                if(stateNear.isOf(org.mtr.mod.Blocks.GLASS_FENCE_SHM.get())) fenceType = 6;
                if(stateNear.isOf(org.mtr.mod.Blocks.GLASS_FENCE_STAINED.get())) fenceType = 7;
                if(stateNear.isOf(org.mtr.mod.Blocks.GLASS_FENCE_STW.get())) fenceType = 8;
                if(stateNear.isOf(org.mtr.mod.Blocks.GLASS_FENCE_TSH.get())) fenceType = 9;
                if(stateNear.isOf(org.mtr.mod.Blocks.GLASS_FENCE_WKS.get())) fenceType = 10;
                return state.with(new Property<>(FENCE_TYPE.data), fenceType).with(new Property<>(FLIPPED.data), flipped);
            }
        }

        // Some builder prefer the fence to stay connected to a wall instead of getting updated without a fence, leaving a hole
        // In this case we don't touch the state and just leave it as is
        BlockPos nextToPos = pos.offset(thisDirection.rotateYCounterclockwise());
        boolean hasBlockNextToFence = !world.getBlockState(nextToPos).isAir();
        if(hasBlockNextToFence) {
            return state;
        }

        return state.with(new Property<>(FENCE_TYPE.data), 0);
    }

    @Override
    public void addBlockProperties(List<HolderBase<?>> properties) {
        super.addBlockProperties(properties);
        properties.add(FENCE_TYPE);
        properties.add(FLIPPED);
    }
}
