package com.lx862.jcm.mod.block.entity;

import com.lx862.jcm.mod.registry.BlockEntities;
import org.mtr.mapping.holder.BlockPos;
import org.mtr.mapping.holder.BlockState;
import org.mtr.mapping.holder.Direction;
import org.mtr.mod.block.IBlock;

import static com.lx862.jcm.mod.block.base.DirectionalBlock.FACING;

public class RVPIDSSIL2BlockEntity extends PIDSBlockEntity {

    public RVPIDSSIL2BlockEntity(BlockPos blockPos, BlockState blockState) {
        super(BlockEntities.RV_PIDS_SIL_2.get(), blockPos, blockState);
    }

    @Override
    public String getPIDSType() {
        return "rv_pids_sil_2";
    }

    @Override
    public String getDefaultPresetId() {
        return "rv_pids";
    }

    @Override
    public int getRowAmount() {
        return 4;
    }

    @Override
    public boolean isKeyBlock() {
        Direction dir = IBlock.getStatePropertySafe(getCachedState2(), FACING);
        return dir.equals(Direction.NORTH) || dir.equals(Direction.EAST);
    }
}
