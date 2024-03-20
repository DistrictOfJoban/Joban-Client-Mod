package com.lx862.jcm.mod.block.entity;

import com.lx862.jcm.mod.registry.BlockEntities;
import org.mtr.mapping.holder.*;

public class FareSaverBlockEntity extends JCMBlockEntityBase {
    private String currency = "$";
    private int discount = 2;
    public FareSaverBlockEntity(BlockPos blockPos, BlockState blockState) {
        super(BlockEntities.FARE_SAVER.get(), blockPos, blockState);
    }

    @Override
    public void readCompoundTag(CompoundTag compoundTag) {
        super.readCompoundTag(compoundTag);
        this.discount = compoundTag.getInt("discount");
        this.currency = compoundTag.contains("currency") ? compoundTag.getString("currency") : "$";
    }

    @Override
    public void writeCompoundTag(CompoundTag compoundTag) {
        super.writeCompoundTag(compoundTag);
        compoundTag.putInt("discount", discount);
        compoundTag.putString("currency", currency);
    }

    public void setData(String currency, int discount) {
        this.currency = currency;
        this.discount = discount;
        this.markDirty2();
    }

    public int getDiscount() {
        return discount;
    }

    public String getCurrency() {
        return currency;
    }
}
