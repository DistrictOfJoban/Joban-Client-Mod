package com.lx862.jcm.mod.block.entity;

import com.lx862.jcm.mod.registry.BlockEntities;
import org.mtr.mapping.holder.BlockPos;
import org.mtr.mapping.holder.BlockState;
import org.mtr.mapping.holder.CompoundTag;

public class FareSaverBlockEntity extends JCMBlockEntityBase {
    private String prefix = "$";
    private int discount = 2;

    private static final String KEY_DISCOUNT = "discount";
    private static final String KEY_PREFIX = "prefix";

    public FareSaverBlockEntity(BlockPos blockPos, BlockState blockState) {
        super(BlockEntities.FARE_SAVER.get(), blockPos, blockState);
    }

    @Override
    public void readCompoundTag(CompoundTag compoundTag) {
        super.readCompoundTag(compoundTag);
        this.discount = compoundTag.getInt(KEY_DISCOUNT);
        this.prefix = compoundTag.contains(KEY_PREFIX) ? compoundTag.getString(KEY_PREFIX) : "$";
    }

    @Override
    public void writeCompoundTag(CompoundTag compoundTag) {
        super.writeCompoundTag(compoundTag);
        compoundTag.putInt(KEY_DISCOUNT, discount);
        compoundTag.putString(KEY_PREFIX, prefix);
    }

    public void setData(String prefix, int discount) {
        this.prefix = prefix;
        this.discount = discount;
        this.markDirty2();
    }

    public int getDiscount() {
        return discount;
    }

    public String getPrefix() {
        return prefix;
    }
}
