package com.lx862.jcm.mod.block.entity;

import com.lx862.jcm.mod.registry.BlockEntities;
import org.mtr.mapping.holder.BlockPos;
import org.mtr.mapping.holder.BlockState;
import org.mtr.mod.block.BlockPSDAPGDoorBase;

public class APGDoorDRLBlockEntity extends BlockPSDAPGDoorBase.BlockEntityBase {
    public APGDoorDRLBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntities.APG_DOOR_DRL.get(), pos, state);
    }
}