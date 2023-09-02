package com.lx862.jcm.blocks.base;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.Direction;

public abstract class CeilingGroundAttachedDirectionalBlock extends CeilingGroundAttachedBlock {
    public static final DirectionProperty FACING = Properties.HORIZONTAL_FACING;

    public CeilingGroundAttachedDirectionalBlock(Settings settings, boolean canAttachTop, boolean canAttachBottom) {
        super(settings, canAttachTop, canAttachBottom);
        setDefaultState(getDefaultState().with(FACING, Direction.NORTH));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        super.appendProperties(builder);
        builder.add(FACING);
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        return super.getPlacementState(ctx).with(FACING, ctx.getHorizontalPlayerFacing());
    }
}
