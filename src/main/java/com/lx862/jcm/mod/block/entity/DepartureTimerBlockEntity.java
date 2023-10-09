package com.lx862.jcm.mod.block.entity;

import com.lx862.jcm.mod.registry.BlockEntities;
import org.mtr.mapping.holder.BlockPos;
import org.mtr.mapping.holder.BlockState;

public class DepartureTimerBlockEntity extends FontBlockEntityBase {
    public DepartureTimerBlockEntity(BlockPos blockPos, BlockState blockState) {
        super("mtr:mtr", BlockEntities.DEPARTURE_TIMER.get(), blockPos, blockState);
    }
}
