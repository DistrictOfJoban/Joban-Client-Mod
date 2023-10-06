package com.lx862.jcm.mod.blocks.entity;

import com.lx862.jcm.mod.data.JCMStats;
import com.lx862.jcm.mod.registry.BlockEntities;
import com.lx862.jcm.mod.util.Logger;
import org.mtr.mapping.holder.BlockPos;
import org.mtr.mapping.holder.BlockState;
import org.mtr.mapping.holder.CompoundTag;

public class ButterflyLightBlockEntity extends JCMBlockEntity {
    private int secondsLeftUntilBlink = 10;
    public ButterflyLightBlockEntity(BlockPos blockPos, BlockState blockState) {
        super(BlockEntities.BUTTERFLY_LIGHT.get(), blockPos, blockState);
    }

    @Override
    public void readCompoundTag(CompoundTag compoundTag) {
        super.readCompoundTag(compoundTag);
        this.secondsLeftUntilBlink = compoundTag.getInt("seconds_to_blink");
    }

    @Override
    public void writeCompoundTag(CompoundTag compoundTag) {
        super.writeCompoundTag(compoundTag);
        compoundTag.putInt("seconds_to_blink", secondsLeftUntilBlink);
    }

    @Override
    public void blockEntityTick() {
        if(JCMStats.getGameTick() % 100 == 0) {
            Logger.info("Butterfly light block entity ticking");
        }
    }

    public void setData(int secondsLeftUntilBlink) {
        this.secondsLeftUntilBlink = secondsLeftUntilBlink;
        this.markDirty2();
    }

    public int getSecondsLeftUntilBlink() {
        return secondsLeftUntilBlink;
    }
}
