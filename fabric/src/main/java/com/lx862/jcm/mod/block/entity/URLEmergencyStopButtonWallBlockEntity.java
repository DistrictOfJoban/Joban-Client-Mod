package com.lx862.jcm.mod.block.entity;

import com.lx862.jcm.mod.registry.BlockEntities;
import org.mtr.mapping.holder.BlockPos;
import org.mtr.mapping.holder.BlockState;

public class URLEmergencyStopButtonWallBlockEntity extends JCMBlockEntityBase {
    public URLEmergencyStopButtonWallBlockEntity(BlockPos blockPos, BlockState blockState) {
        super(BlockEntities.URL_EMG_STOP_BUTTON_WALL.get(), blockPos, blockState);
    }
}
