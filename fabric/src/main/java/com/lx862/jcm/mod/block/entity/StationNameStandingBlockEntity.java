package com.lx862.jcm.mod.block.entity;

import com.lx862.jcm.mod.registry.BlockEntities;
import org.mtr.mapping.holder.BlockPos;
import org.mtr.mapping.holder.BlockState;
import org.mtr.mapping.mapper.BlockEntityExtension;

public class StationNameStandingBlockEntity extends BlockEntityExtension {
    public StationNameStandingBlockEntity(BlockPos blockPos, BlockState blockState) {
        super(BlockEntities.STATION_NAME_STANDING.get(), blockPos, blockState);
    }
}
