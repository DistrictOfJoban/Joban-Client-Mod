package com.lx862.jcm.mod.block.entity;

import com.lx862.jcm.mod.registry.BlockEntities;
import org.mtr.mapping.holder.*;

public class FareSaverBlockEntity extends JCMBlockEntityBase {
    private int discount = 2;
    public FareSaverBlockEntity(BlockPos blockPos, BlockState blockState) {
        super(BlockEntities.FARE_SAVER.get(), blockPos, blockState);
    }

    @Override
    public void readCompoundTag(CompoundTag compoundTag) {
        super.readCompoundTag(compoundTag);
        this.discount = compoundTag.getInt("discount");
    }

    @Override
    public void writeCompoundTag(CompoundTag compoundTag) {
        super.writeCompoundTag(compoundTag);
        compoundTag.putInt("discount", discount);
    }

    public void setData(int discount) {
        this.discount = discount;
        this.markDirty2();
    }

    public int getDiscount() {
        return discount;
    }
}
