package com.lx862.jcm.mod.block;

import net.minecraft.block.StairsBlock;
import org.mtr.mapping.holder.BlockSettings;
import org.mtr.mapping.holder.Blocks;
//import org.mtr.mapping.mapper.StairsBlockExtension;
/*
public class MTRStairsBlock extends StairsBlockExtension {
    public MTRStairsBlock(BlockSettings settings) {
        super(Blocks.getSmoothStoneMapped().getDefaultState(), settings);
    }
}*/

// TODO: Replace it with StairsBlockExtension

public class MTRStairsBlock extends StairsBlock {
    public MTRStairsBlock(BlockSettings settings) {
        super(Blocks.getSmoothStoneMapped().getDefaultState().data, settings.data);
    }
}