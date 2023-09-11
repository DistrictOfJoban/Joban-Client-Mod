package com.lx862.jcm.blocks.base;

import com.lx862.jcm.data.BlockProperties;
import org.mtr.mapping.holder.*;
import org.mtr.mapping.tool.HolderBase;

import java.util.List;

public abstract class CeilingGroundAttachedDirectionalBlock extends CeilingGroundAttachedBlock {

    public static final DirectionProperty FACING = BlockProperties.FACING;

    public CeilingGroundAttachedDirectionalBlock(BlockSettings settings, boolean canAttachTop, boolean canAttachBottom) {
        super(settings, canAttachTop, canAttachBottom);
    }

    @Override
    public BlockState getPlacementState2(ItemPlacementContext ctx) {
        return super.getPlacementState2(ctx).with(new Property<>(FACING.data), ctx.getPlayerFacing().data);
    }

    @Override
    public void addBlockProperties(List<HolderBase<?>> properties) {
        super.addBlockProperties(properties);
        properties.add(FACING);
    }
}
