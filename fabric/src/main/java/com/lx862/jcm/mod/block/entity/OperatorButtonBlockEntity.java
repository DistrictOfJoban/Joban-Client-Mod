package com.lx862.jcm.mod.block.entity;

import com.lx862.jcm.mod.block.OperatorButtonBlock;
import com.lx862.jcm.mod.registry.BlockEntities;
import com.lx862.jcm.mod.util.JCMUtil;
import org.mtr.mapping.holder.*;

import java.util.Objects;

public class OperatorButtonBlockEntity extends JCMBlockEntityBase {
    private boolean[] keyRequirements = new boolean[OperatorButtonBlock.ACCEPTED_KEYS.length];

    public OperatorButtonBlockEntity(BlockPos blockPos, BlockState blockState) {
        super(BlockEntities.OPERATOR_BUTTON.get(), blockPos, blockState);
        for(int i = 0; i < OperatorButtonBlock.ACCEPTED_KEYS.length; i++) {
            this.keyRequirements[i] = true; // Enable all by default
        }
    }

    @Override
    public void readCompoundTag(CompoundTag compoundTag) {
        super.readCompoundTag(compoundTag);
        for(int i = 0; i < OperatorButtonBlock.ACCEPTED_KEYS.length; i++) {
            boolean isEnabled = !compoundTag.contains("required_key" + i) || compoundTag.getBoolean("required_key" + i);
            this.keyRequirements[i] = isEnabled;
        }
    }

    @Override
    public void writeCompoundTag(CompoundTag compoundTag) {
        super.writeCompoundTag(compoundTag);
        for(int i = 0; i < OperatorButtonBlock.ACCEPTED_KEYS.length; i++) {
            compoundTag.putBoolean("required_key" + i, keyRequirements[i]);
        }
    }

    public void setData(boolean[] keyRequirements) {
        this.keyRequirements = keyRequirements;
        this.markDirty2();
    }

    public boolean[] getKeyRequirements() {
        return this.keyRequirements;
    }

    public boolean canOpen(ItemStack itemStack) {
        for(int i = 0; i < OperatorButtonBlock.ACCEPTED_KEYS.length; i++) {
            if(Objects.equals(OperatorButtonBlock.ACCEPTED_KEYS[i].get().data, itemStack.getItem().data)) {
                return keyRequirements[i];
            }
        }
        return false;
    }
}
