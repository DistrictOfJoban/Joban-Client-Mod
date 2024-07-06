package com.lx862.jcm.mod.block.entity;

import com.lx862.jcm.mod.registry.BlockEntities;
import org.mtr.mapping.holder.BlockPos;
import org.mtr.mapping.holder.BlockState;

public class RVPIDSSIL2BlockEntity extends PIDSBlockEntity {

    public RVPIDSSIL2BlockEntity(BlockPos blockPos, BlockState blockState) {
        super(BlockEntities.RV_PIDS_SIL_2.get(), blockPos, blockState);
    }

    @Override
    public String getDefaultPresetId() {
        return "rv_pids";
    }

    @Override
    public int getRowAmount() {
        return 4;
    }
}
