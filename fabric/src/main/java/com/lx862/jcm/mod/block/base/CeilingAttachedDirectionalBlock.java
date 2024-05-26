package com.lx862.jcm.mod.block.base;

import com.lx862.jcm.mod.data.BlockProperties;
import org.mtr.mapping.holder.*;
import org.mtr.mapping.tool.HolderBase;

import java.util.List;

public abstract class CeilingAttachedDirectionalBlock extends CeilingAttachedBlock {

    public static final DirectionProperty FACING = BlockProperties.FACING;

    public CeilingAttachedDirectionalBlock(BlockSettings settings) {
        super(settings);
    }

    @Override
    public BlockState getPlacementState2(ItemPlacementContext ctx) {
        BlockState superState = super.getPlacementState2(ctx);
        return superState == null ? null : superState.with(new Property<>(FACING.data), ctx.getPlayerFacing().data);
    }

    @Override
    public void addBlockProperties(List<HolderBase<?>> properties) {
        super.addBlockProperties(properties);
        properties.add(FACING);
    }
}
