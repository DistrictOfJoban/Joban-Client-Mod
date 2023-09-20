package com.lx862.jcm.blocks.entity;

import com.lx862.jcm.registry.BlockEntities;
import org.mtr.mapping.holder.BlockPos;
import org.mtr.mapping.holder.BlockState;
import org.mtr.mapping.holder.CompoundTag;

public class SubsidyMachineBlockEntity extends JCMBlockEntity {
    private int subsidyAmount = 10;
    private int cooldown = 0;
    public SubsidyMachineBlockEntity(BlockPos blockPos, BlockState blockState) {
        super(BlockEntities.SUBSIDY_MACHINE.get(), blockPos, blockState);
    }

    @Override
    public void readCompoundTag(CompoundTag compoundTag) {
        super.readCompoundTag(compoundTag);
        this.subsidyAmount = compoundTag.getInt("price_per_click");
        this.cooldown = compoundTag.getInt("timeout");
    }

    @Override
    public void writeCompoundTag(CompoundTag compoundTag) {
        super.writeCompoundTag(compoundTag);
        compoundTag.putInt("price_per_click", subsidyAmount);
        compoundTag.putInt("timeout", cooldown);
    }

    public void setData(int pricePerUse, int timeout) {
        this.subsidyAmount = pricePerUse;
        this.cooldown = timeout;
        this.markDirty2();
    }

    public int getSubsidyAmount() {
        return subsidyAmount;
    }

    public int getCooldown() {
        return cooldown;
    }
}
