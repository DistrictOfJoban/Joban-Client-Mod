package com.lx862.jcm.mod.block.base;

import com.lx862.jcm.mod.data.BlockProperties;
import org.mtr.mapping.holder.*;
import org.mtr.mapping.tool.HolderBase;

import java.util.List;

public abstract class VerticallyAttachedDirectionalBlock extends VerticallyAttachedBlock {

    public static final DirectionProperty FACING = BlockProperties.FACING;

    public VerticallyAttachedDirectionalBlock(BlockSettings settings, boolean canAttachTop, boolean canAttachBottom) {
        super(settings, canAttachTop, canAttachBottom);
    }

    @Override
    public void addBlockProperties(List<HolderBase<?>> properties) {
        super.addBlockProperties(properties);
        properties.add(FACING);
    }
}
