package com.lx862.jcm.mod.block.entity;

import com.lx862.jcm.mod.block.PIDS1ABlock;
import com.lx862.jcm.mod.registry.BlockEntities;
import org.mtr.mapping.holder.BlockPos;
import org.mtr.mapping.holder.BlockState;
import org.mtr.mod.block.BlockPIDSHorizontalBase;

public class PIDS1ABlockEntity extends BlockPIDSHorizontalBase.BlockEntityHorizontalBase {
    public PIDS1ABlockEntity(BlockPos blockPos, BlockState blockState) {
        super(PIDS1ABlock.MAX_ARRIVALS, BlockEntities.PIDS_1A.get(), blockPos, blockState);
    }

    @Override
    public String defaultFormat(int i) {
        return "@0-60L@$#FF9900$%destination*%@60-100R@%RAH*%:%RA0m*%:%RA0s*%";
    }
}
