package com.lx862.jcm.mod.block.base;

import com.lx862.jcm.mod.block.behavior.HorizontalMultiBlock;
import com.lx862.jcm.mod.data.BlockProperties;
import com.lx862.jcm.mod.util.BlockUtil;
import org.mtr.mapping.holder.*;
import org.mtr.mapping.tool.HolderBase;

import java.util.List;

public abstract class Horizontal2Block extends DirectionalBlock implements HorizontalMultiBlock {
    public static final int width = 2;
    public static final IntegerProperty PART = BlockProperties.HORIZONTAL_PART;

    public Horizontal2Block(BlockSettings settings) {
        super(settings);
    }

    @Override
    public BlockState getPlacementState2(ItemPlacementContext ctx) {
        return HorizontalMultiBlock.canBePlaced(super.getPlacementState2(ctx), ctx, width) ? super.getPlacementState2(ctx) : null;
    }

    @Override
    public void onPlaced2(World world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack itemStack) {
        HorizontalMultiBlock.placeBlock(world, pos, state, new Property<>(PART.data), BlockUtil.getProperty(state, FACING).rotateYClockwise(), width);
    }

    @Override
    public BlockState getStateForNeighborUpdate2(BlockState state, Direction direction, BlockState neighborState, WorldAccess world, BlockPos pos, BlockPos neighborPos) {
        if (!HorizontalMultiBlock.blockNotValid(pos, state, world, new Property<>(PART.data), width)) {
            return Blocks.getAirMapped().getDefaultState();
        }

        return super.getStateForNeighborUpdate2(state, direction, neighborState, world, pos, neighborPos);
    }

    @Override
    public void addBlockProperties(List<HolderBase<?>> properties) {
        super.addBlockProperties(properties);
        properties.add(PART);
    }
}
