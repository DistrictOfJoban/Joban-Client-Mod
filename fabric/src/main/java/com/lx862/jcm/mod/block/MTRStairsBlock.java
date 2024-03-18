package com.lx862.jcm.mod.block;

import org.mtr.mapping.holder.BlockSettings;
import org.mtr.mapping.holder.Blocks;
import org.mtr.mapping.mapper.StairsBlockExtension;

public class MTRStairsBlock extends StairsBlockExtension {
    public MTRStairsBlock(BlockSettings settings) {
        super(Blocks.getSmoothStoneMapped().getDefaultState(), settings);
    }
}