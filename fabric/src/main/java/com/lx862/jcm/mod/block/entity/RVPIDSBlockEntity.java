package com.lx862.jcm.mod.block.entity;

import com.lx862.jcm.mod.registry.BlockEntities;
import org.mtr.mapping.holder.BlockPos;
import org.mtr.mapping.holder.BlockState;
import org.mtr.mapping.holder.CompoundTag;

public class RVPIDSBlockEntity extends PIDSBlockEntity {
    private boolean hidePlatformNumber;
    public RVPIDSBlockEntity(BlockPos blockPos, BlockState blockState) {
        super(BlockEntities.RV_PIDS.get(), blockPos, blockState);
    }

    @Override
    public void readCompoundTag(CompoundTag compoundTag) {
        super.readCompoundTag(compoundTag);
        this.hidePlatformNumber = compoundTag.getBoolean("hide_platform_number");
    }

    @Override
    public void writeCompoundTag(CompoundTag compoundTag) {
        super.writeCompoundTag(compoundTag);
        compoundTag.putBoolean("hide_platform_number", hidePlatformNumber);
    }

    @Override
    public int getRowAmount() {
        return 4;
    }
}
