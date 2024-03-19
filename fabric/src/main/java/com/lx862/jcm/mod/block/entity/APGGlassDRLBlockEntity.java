package com.lx862.jcm.mod.block.entity;

import com.lx862.jcm.mod.registry.BlockEntities;
import org.mtr.mapping.holder.BlockPos;
import org.mtr.mapping.holder.BlockState;
import org.mtr.mod.block.BlockPSDTop;

public class APGGlassDRLBlockEntity extends BlockPSDTop.BlockEntityBase {
    public APGGlassDRLBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntities.APG_GLASS_DRL.get(), pos, state);
    }
}