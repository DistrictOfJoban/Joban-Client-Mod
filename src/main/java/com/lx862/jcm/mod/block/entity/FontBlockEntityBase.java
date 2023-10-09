package com.lx862.jcm.mod.block.entity;

import org.mtr.mapping.holder.BlockEntityType;
import org.mtr.mapping.holder.BlockPos;
import org.mtr.mapping.holder.BlockState;
import org.mtr.mapping.holder.CompoundTag;
import org.mtr.mapping.mapper.BlockEntityExtension;

public abstract class FontBlockEntityBase extends BlockEntityExtension {
    private String font;
    public FontBlockEntityBase(String defaultFont, BlockEntityType<?> type, BlockPos blockPos, BlockState blockState) {
        super(type, blockPos, blockState);
        this.font = defaultFont;
    }

    @Override
    public void readCompoundTag(CompoundTag compoundTag) {
        this.font = compoundTag.getString("font");
        super.readCompoundTag(compoundTag);
    }

    @Override
    public void writeCompoundTag(CompoundTag compoundTag) {
        compoundTag.putString("font", this.font);
        super.writeCompoundTag(compoundTag);
    }
}
