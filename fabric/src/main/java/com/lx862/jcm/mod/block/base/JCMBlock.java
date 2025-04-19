package com.lx862.jcm.mod.block.base;

import com.lx862.jcm.mapping.LoaderImpl;
import com.lx862.jcm.mod.util.BlockUtil;
import org.mtr.mapping.holder.*;
import org.mtr.mapping.mapper.BlockExtension;

import java.util.function.BiConsumer;

public abstract class JCMBlock extends BlockExtension {
    public JCMBlock(BlockSettings settings) {
        super(LoaderImpl.getSolidBlockSettings(settings));
    }

    /**
     * Loop through each block that should be associated with this block (i.e. Multiblock structure)<br>
     * The callback can be called multiple times on, for example a multi-block, and that multiple block entity should be updated to create a consistent state.<br>
     */
    public void loopStructure(BlockState state, World world, BlockPos sourcePos, BiConsumer<BlockState, BlockEntity> callback) {
        for(BlockPos bPos : getAllPos(state, world, sourcePos)) {
            BlockState bs = world.getBlockState(sourcePos);
            BlockEntity be = BlockUtil.getBlockEntityOrNull(world, bPos);
            if(be != null) callback.accept(bs, be);
        }
    }

    /* Get all pos of the entire block structure */
    public BlockPos[] getAllPos(BlockState state, World world, BlockPos sourcePos) {
        return new BlockPos[]{sourcePos};
    }
}
