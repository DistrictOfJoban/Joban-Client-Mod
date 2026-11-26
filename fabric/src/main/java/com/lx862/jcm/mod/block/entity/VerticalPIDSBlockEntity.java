package com.lx862.jcm.mod.block.entity;

import com.lx862.jcm.mod.registry.BlockEntities;
import org.mtr.mapping.holder.BlockPos;
import org.mtr.mapping.holder.BlockState;
import org.mtr.mod.block.IBlock;

import static com.lx862.jcm.mod.block.base.Vertical2Block.HALF;

public class VerticalPIDSBlockEntity extends PIDSBlockEntity {

    public VerticalPIDSBlockEntity(BlockPos blockPos, BlockState blockState) {
        super(BlockEntities.VERTICAL_PIDS.get(), blockPos, blockState);
    }

    @Override
    public String getPIDSType() {
        return "vertical_pids";
    }

    @Override
    public String getDefaultPresetId() {
        return "vertical_pids_8";
    }

    @Override
    public int getRowAmount() {
        return 16;
    }

    @Override
    public boolean isKeyBlock() {
        return true; // We only have a single block
    }
}
