package com.lx862.jcm.blocks.blockentity;

import com.lx862.jcm.registry.BlockEntityRegistry;
import org.mtr.mapping.holder.BlockPos;
import org.mtr.mapping.holder.BlockState;
import org.mtr.mapping.holder.CompoundTag;

public class SubsidyMachineBlockEntity extends JCMBlockEntity {
    private int subsidyAmount = 10;
    private int timeout = 0;
    public SubsidyMachineBlockEntity(BlockPos blockPos, BlockState blockState) {
        super(BlockEntityRegistry.SUBSIDY_MACHINE.get(), blockPos, blockState);
    }

    @Override
    public void readCompoundTag(CompoundTag compoundTag) {
        super.readCompoundTag(compoundTag);
        this.subsidyAmount = compoundTag.getInt("price_per_click");
        this.timeout = compoundTag.getInt("timeout");
    }

    @Override
    public void writeCompoundTag(CompoundTag compoundTag) {
        super.writeCompoundTag(compoundTag);
        compoundTag.putInt("price_per_click", subsidyAmount);
        compoundTag.putInt("timeout", timeout);
    }

    public void setData(int pricePerUse, int timeout) {
        this.subsidyAmount = pricePerUse;
        this.timeout = timeout;
        this.markDirty2();
    }

    public int getSubsidyAmount() {
        return subsidyAmount;
    }

    public int getTimeout() {
        return timeout;
    }
}
