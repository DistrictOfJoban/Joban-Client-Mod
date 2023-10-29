package com.lx862.jcm.mod.block.entity;

import com.lx862.jcm.mod.registry.BlockEntities;
import org.mtr.mapping.holder.BlockPos;
import org.mtr.mapping.holder.BlockState;
import org.mtr.mapping.holder.CompoundTag;

public class PIDSBlockEntity extends JCMBlockEntityBase {
    public static final int ROW_AMOUNT = 4;
    private final String[] customMessages;
    private final Boolean[] rowHidden;
    private String pidsPresetId;
    public PIDSBlockEntity(BlockPos blockPos, BlockState blockState) {
        super(BlockEntities.RV_PIDS.get(), blockPos, blockState);
        this.customMessages = new String[ROW_AMOUNT];
        this.rowHidden = new Boolean[ROW_AMOUNT];
        this.pidsPresetId = "";
    }

    @Override
    public void readCompoundTag(CompoundTag compoundTag) {
        super.readCompoundTag(compoundTag);
        for(int i = 0; i < ROW_AMOUNT; i++) {
            this.customMessages[i] = compoundTag.getString("message" + i);
            this.rowHidden[i] = compoundTag.getBoolean("hide_arrival" + i);
            this.pidsPresetId = compoundTag.getString("preset_id");
        }
    }

    @Override
    public void writeCompoundTag(CompoundTag compoundTag) {
        super.writeCompoundTag(compoundTag);
        for(int i = 0; i < ROW_AMOUNT; i++) {
            String customMessage = this.customMessages[i] == null ? "" : this.customMessages[i];
            boolean rowHidden = this.rowHidden[i] != null && this.rowHidden[i];
            compoundTag.putString(("message" + i), customMessage);
            compoundTag.putBoolean(("hide_arrival" + i), rowHidden);
        }
        compoundTag.putString("preset_id", this.pidsPresetId);
    }

    public void setData() {
        this.markDirty2();
    }
}
