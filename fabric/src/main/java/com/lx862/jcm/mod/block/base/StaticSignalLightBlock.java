package com.lx862.jcm.mod.block.base;

import com.lx862.jcm.mod.block.behavior.WaterloggableBehavior;
import com.lx862.jcm.mod.block.entity.StaticSignalLightBlockEntity;
import com.lx862.jcm.mod.util.BlockUtil;
import com.lx862.jcm.mod.util.VoxelUtil;
import org.mtr.mapping.holder.*;
import org.mtr.mapping.mapper.BlockEntityExtension;
import org.mtr.mapping.mapper.BlockWithEntity;

public abstract class StaticSignalLightBlock extends DirectionalBlock implements BlockWithEntity, WaterloggableBehavior {
    public StaticSignalLightBlock(BlockSettings settings) {
        super(settings);
        setDefaultState2(getDefaultState2().with(new Property<>(WATERLOGGED.data), false));
    }

    @Override
    public VoxelShape getOutlineShape2(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return VoxelUtil.getDirectionalShape16(BlockUtil.getProperty(state, FACING), 2, 0, 5, 14, 14, 11);
    }

    @Override
    public FluidState getFluidState2(BlockState state) {
        return getFluidState(state);
    }

    @Override
    public BlockState getPlacementState2(ItemPlacementContext ctx) {
        return getWaterloggedState(super.getPlacementState2(ctx), ctx.getWorld(), ctx.getBlockPos());
    }

    @Override
    public BlockState getStateForNeighborUpdate2(BlockState state, Direction direction, BlockState neighborState, WorldAccess world, BlockPos pos, BlockPos neighborPos) {
        updateWaterState(state, world, pos);
        return super.getStateForNeighborUpdate2(state, direction, neighborState, world, pos, neighborPos);
    }

    public abstract StaticSignalLightBlockEntity.SignalType getSignalType();

    @Override
    public BlockEntityExtension createBlockEntity(BlockPos blockPos, BlockState blockState) {
        return new StaticSignalLightBlockEntity(getSignalType(), blockPos, blockState);
    }
}
