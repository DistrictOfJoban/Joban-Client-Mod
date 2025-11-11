package com.lx862.jcm.mod.block.entity;

import com.lx862.jcm.mod.registry.BlockEntities;
import org.mtr.mapping.holder.BlockPos;
import org.mtr.mapping.holder.BlockState;
import org.mtr.mapping.holder.Direction;
import org.mtr.mod.block.IBlock;

import static com.lx862.jcm.mod.block.base.DirectionalBlock.FACING;

public class LCDPIDSBlockEntity extends PIDSBlockEntity {
    public LCDPIDSBlockEntity(BlockPos blockPos, BlockState blockState) {
        super(BlockEntities.LCD_PIDS.get(), blockPos, blockState);
    }

    @Override
    public String getPIDSType() {
        return "lcd_pids";
    }

    @Override
    public String getDefaultPresetId() {
        return "lcd_pids";
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
