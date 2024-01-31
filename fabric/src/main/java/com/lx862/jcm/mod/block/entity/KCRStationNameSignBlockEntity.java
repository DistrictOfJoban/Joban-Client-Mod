package com.lx862.jcm.mod.block.entity;

import com.lx862.jcm.mod.registry.BlockEntities;
import org.mtr.mapping.holder.BlockPos;
import org.mtr.mapping.holder.BlockState;
import org.mtr.mapping.mapper.BlockEntityExtension;

public class KCRStationNameSignBlockEntity extends BlockEntityExtension {
    public KCRStationNameSignBlockEntity(BlockPos blockPos, BlockState blockState, boolean stationColored) {
        super(stationColored ? BlockEntities.KCR_STATION_NAME_SIGN_STATION_COLOR.get() : BlockEntities.KCR_STATION_NAME_SIGN.get(), blockPos, blockState);
    }
}
