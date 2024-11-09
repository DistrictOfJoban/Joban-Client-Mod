package com.lx862.jcm.mod.block.entity;

import com.lx862.jcm.mod.registry.BlockEntities;
import org.mtr.mapping.holder.BlockPos;
import org.mtr.mapping.holder.BlockState;

public class PIDS1ABlockEntity extends PIDSBlockEntity {

    public PIDS1ABlockEntity(BlockPos blockPos, BlockState blockState) {
        super(BlockEntities.PIDS_1A.get(), blockPos, blockState);
    }

    @Override
    public String getPIDSType() {
        return "pids_1a";
    }

    @Override
    public String getDefaultPresetId() {
        return "pids_1a";
    }

    @Override
    public int getRowAmount() {
        return 3;
    }
}
