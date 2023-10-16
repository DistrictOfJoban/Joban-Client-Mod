package com.lx862.jcm.mod.block.entity;

import org.mtr.mapping.holder.BlockEntityType;
import org.mtr.mapping.holder.BlockPos;
import org.mtr.mapping.holder.BlockState;
import org.mtr.mapping.mapper.BlockEntityExtension;

public abstract class JCMBlockEntityBase extends BlockEntityExtension {

    public JCMBlockEntityBase(BlockEntityType<?> type, BlockPos blockPos, BlockState blockState) {
        super(type, blockPos, blockState);
    }
}
