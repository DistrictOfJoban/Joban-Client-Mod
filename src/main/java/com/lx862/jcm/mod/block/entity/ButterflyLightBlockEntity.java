package com.lx862.jcm.mod.block.entity;

import com.lx862.jcm.mod.registry.BlockEntities;
import org.mtr.mapping.holder.BlockPos;
import org.mtr.mapping.holder.BlockState;
import org.mtr.mapping.holder.CompoundTag;

public class ButterflyLightBlockEntity extends JCMBlockEntityBase {
    private int secondsToBlink = 10;
    public ButterflyLightBlockEntity(BlockPos blockPos, BlockState blockState) {
        super(BlockEntities.BUTTERFLY_LIGHT.get(), blockPos, blockState);
    }

    @Override
    public void readCompoundTag(CompoundTag compoundTag) {
        super.readCompoundTag(compoundTag);
        this.secondsToBlink = compoundTag.getInt("seconds_to_blink");
    }

    @Override
    public void writeCompoundTag(CompoundTag compoundTag) {
        super.writeCompoundTag(compoundTag);
        compoundTag.putInt("seconds_to_blink", secondsToBlink);
    }

    @Override
    public void blockEntityTick() {
        //TODO: Implement Butterfly Light checking and blockstate change
    }

    public void setData(int secondsToBlink) {
        this.secondsToBlink = secondsToBlink;
        this.markDirty2();
    }

    public int getSecondsToBlink() {
        return secondsToBlink;
    }
}
