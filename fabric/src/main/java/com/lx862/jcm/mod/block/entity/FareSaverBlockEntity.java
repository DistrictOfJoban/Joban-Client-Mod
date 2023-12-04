package com.lx862.jcm.mod.block.entity;

import com.lx862.jcm.mod.registry.BlockEntities;
import com.lx862.jcm.mod.util.BlockUtil;
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

    public void setBlockData(int discount) {
        for(int i = 1; i < 3; i++) {
            BlockEntity topBE = BlockUtil.getBlockEntityOrNull(getWorld2(), getPos2().offset(Axis.Y, i));
            BlockEntity bottomBE = BlockUtil.getBlockEntityOrNull(getWorld2(), getPos2().offset(Axis.Y, -i));

            if(topBE != null && topBE.data instanceof FareSaverBlockEntity) {
                ((FareSaverBlockEntity)topBE.data).setData(discount);
            }

            if(bottomBE != null && bottomBE.data instanceof FareSaverBlockEntity) {
                ((FareSaverBlockEntity)bottomBE.data).setData(discount);
            }
        }

        setData(discount);
    }

    public void setData(int discount) {
        this.discount = discount;
        this.markDirty2();
    }

    public int getDiscount() {
        return discount;
    }
}
