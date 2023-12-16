package com.lx862.jcm.mod.block.entity;

import org.mtr.mapping.holder.BlockEntityType;
import org.mtr.mapping.holder.BlockPos;
import org.mtr.mapping.holder.BlockState;
import org.mtr.mapping.holder.CompoundTag;

public abstract class PIDSBlockEntity extends JCMBlockEntityBase {
    private final String[] customMessages;
    private final boolean[] rowHidden;
    private String pidsPresetId;
    public PIDSBlockEntity(BlockEntityType<?> blockEntityType, BlockPos blockPos, BlockState blockState) {
        super(blockEntityType, blockPos, blockState);
        this.customMessages = new String[getRowAmount()];
        this.rowHidden = new boolean[getRowAmount()];
        this.pidsPresetId = getDefaultPresetId();
    }

    @Override
    public void readCompoundTag(CompoundTag compoundTag) {
        super.readCompoundTag(compoundTag);
        for(int i = 0; i < getRowAmount(); i++) {
            this.customMessages[i] = compoundTag.getString("message" + i);
            this.rowHidden[i] = compoundTag.getBoolean("hide_arrival" + i);
            this.pidsPresetId = compoundTag.getString("preset_id");
            if(this.pidsPresetId.isEmpty()) this.pidsPresetId = getDefaultPresetId();
        }
    }

    @Override
    public void writeCompoundTag(CompoundTag compoundTag) {
        super.writeCompoundTag(compoundTag);
        for(int i = 0; i < getRowAmount(); i++) {
            String customMessage = this.customMessages[i] == null ? "" : this.customMessages[i];
            boolean rowHidden = this.rowHidden[i];
            compoundTag.putString(("message" + i), customMessage);
            compoundTag.putBoolean(("hide_arrival" + i), rowHidden);
        }
        compoundTag.putString("preset_id", this.pidsPresetId);
    }

    public String[] getCustomMessages() {
        return this.customMessages;
    }

    public boolean[] getHidePlatforms() {
        return this.rowHidden;
    }

    public void setData(String[] customMessages, boolean[] rowHidden, String pidsPresetId) {
        System.arraycopy(customMessages, 0, this.customMessages, 0, customMessages.length);
        System.arraycopy(rowHidden, 0, this.rowHidden, 0, rowHidden.length);
        this.pidsPresetId = pidsPresetId;
        this.markDirty2();
    }

    public String getPresetId() {
        return pidsPresetId;
    }

    public abstract String getDefaultPresetId();

    public abstract int getRowAmount();
}
