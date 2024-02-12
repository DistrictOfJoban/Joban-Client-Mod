package com.lx862.jcm.mod.block;

import com.lx862.jcm.mod.block.entity.SignalBlockInvertedEntityRedAbove;
import org.mtr.mapping.holder.BlockPos;
import org.mtr.mapping.holder.BlockSettings;
import org.mtr.mapping.holder.BlockState;
import org.mtr.mapping.mapper.BlockEntityExtension;
import org.mtr.mod.block.BlockSignalLightBase;

public class InvertedSignalBlockRedAbove extends BlockSignalLightBase {
    public InvertedSignalBlockRedAbove(BlockSettings settings) {
        super(settings, 2, 14);
    }

    @Override
    public BlockEntityExtension createBlockEntity(BlockPos blockPos, BlockState blockState) {
        return new SignalBlockInvertedEntityRedAbove(blockPos, blockState);
    }
}
