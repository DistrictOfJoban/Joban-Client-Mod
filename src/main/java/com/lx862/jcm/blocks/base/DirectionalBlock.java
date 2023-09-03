package com.lx862.jcm.blocks.base;

import net.minecraft.state.property.Properties;
import org.mtr.mapping.holder.*;
import org.mtr.mapping.tool.HolderBase;

import java.util.List;

public class DirectionalBlock extends WaterloggableBlock {
    public static final DirectionProperty FACING = new DirectionProperty(Properties.HORIZONTAL_FACING);
    public DirectionalBlock(BlockSettings settings) {
        super(settings);
        setDefaultState2(getDefaultState2().with(new Property<>(FACING.data), Direction.NORTH.data));
    }

    @Override
    public void addBlockProperties(List<HolderBase<?>> properties) {
        super.addBlockProperties(properties);
        properties.add(FACING);
    }

    @Override
    public BlockState getPlacementState2(ItemPlacementContext ctx) {
        return super.getPlacementState2(ctx).with(new Property<>(FACING.data), ctx.getPlayerFacing().data);
    }
}
