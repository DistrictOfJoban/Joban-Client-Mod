package com.lx862.jcm.mod.block.entity;

import it.unimi.dsi.fastutil.longs.LongAVLTreeSet;
import org.mtr.mapping.holder.BlockEntityType;
import org.mtr.mapping.holder.BlockPos;
import org.mtr.mapping.holder.BlockState;
import org.mtr.mapping.holder.CompoundTag;

import java.util.ArrayList;

public abstract class PIDSBlockEntity extends JCMBlockEntityBase {
    private final String[] customMessages;
    private final boolean[] rowHidden;
    private boolean hidePlatformNumber;
    private String pidsPresetId;
    private final LongAVLTreeSet platformIds = new LongAVLTreeSet();
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
        }

        platformIds.clear();
        final long[] platformIdsArray = compoundTag.getLongArray("platform_ids");
        for(final long platformId : platformIdsArray) {
            platformIds.add(platformId);
        }

        this.hidePlatformNumber = compoundTag.getBoolean("hide_platform_number");
        this.pidsPresetId = compoundTag.getString("preset_id");
        if(this.pidsPresetId.isEmpty()) this.pidsPresetId = getDefaultPresetId();
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

        compoundTag.putLongArray("platform_ids", new ArrayList<>(platformIds));
        compoundTag.putBoolean("hide_platform_number", hidePlatformNumber);
        compoundTag.putString("preset_id", this.pidsPresetId);
    }

    public String[] getCustomMessages() {
        return this.customMessages;
    }

    public LongAVLTreeSet getPlatformIds() {
        return platformIds;
    }

    public boolean[] getRowHidden() {
        return this.rowHidden;
    }

    public void setData(String[] customMessages, boolean[] rowHidden, boolean hidePlatformNumber, String pidsPresetId) {
        System.arraycopy(customMessages, 0, this.customMessages, 0, customMessages.length);
        System.arraycopy(rowHidden, 0, this.rowHidden, 0, rowHidden.length);
        this.hidePlatformNumber = hidePlatformNumber;
        this.pidsPresetId = pidsPresetId;
        this.markDirty2();
    }

    public boolean platformNumberHidden() {
        return hidePlatformNumber;
    }

    public String getPresetId() {
        return pidsPresetId;
    }

    public abstract String getDefaultPresetId();

    public abstract int getRowAmount();
}
