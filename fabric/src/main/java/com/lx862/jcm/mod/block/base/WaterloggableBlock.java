package com.lx862.jcm.mod.block.base;

import com.lx862.jcm.mod.block.behavior.WaterloggableBehavior;
import org.mtr.mapping.holder.*;
import org.mtr.mapping.tool.HolderBase;

import java.util.List;

/**
 * A waterlogged block
 * Cannot find ways to migrate non-waterlogged block yet, so this is not in use.
 * (WATERLOGGED is a BooleanProperty, by default Minecraft will use the first value as it's default value, and somehow "true" comes first)
 */
public abstract class WaterloggableBlock extends JCMBlock implements WaterloggableBehavior {
    public WaterloggableBlock(BlockSettings settings) {
        super(settings);
        setDefaultState2(getDefaultState2().with(new Property<>(WATERLOGGED.data), false));
    }

    @Override
    public void addBlockProperties(List<HolderBase<?>> properties) {
        super.addBlockProperties(properties);
        properties.add(WATERLOGGED);
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
}
