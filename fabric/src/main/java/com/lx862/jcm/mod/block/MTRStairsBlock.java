package com.lx862.jcm.mod.block;

import net.minecraft.block.StairsBlock;
import org.mtr.mapping.holder.Blocks;

public class MTRStairsBlock extends StairsBlock {
    public MTRStairsBlock(Settings settings) {
        super(Blocks.getSmoothStoneMapped().getDefaultState().data, settings);
    }
}
