package com.lx862.jcm.mod.block.base;

import com.lx862.jcm.mod.util.BlockUtil;
import org.mtr.mapping.holder.*;
import org.mtr.mapping.mapper.BlockExtension;

import java.util.function.Consumer;

public abstract class JCMBlock extends BlockExtension {
    public JCMBlock(BlockSettings settings) {
        super(settings);
    }

    protected BlockEntity[] getBlockEntity(BlockState state, World world, BlockPos pos) {
        return new BlockEntity[]{ BlockUtil.getBlockEntityOrNull(world, pos) };
    }

    /**
     * Loop through each block entity that should be associated with this block via a callback.<br>
     * The callback can be called multiple times on, for example a multi-block, and that multiple block entity should be updated to create a consistent state.<br>
     * There are no guarantee the BlockEntity type is the one you would like, make sure to do type checking!
     */
    public void forEachBlockEntity(BlockState state, World world, BlockPos pos, Consumer<BlockEntity> callback) {
        for(BlockEntity be : getBlockEntity(state, world, pos)) {
            if(be == null) continue;
            callback.accept(be);
        }
    }
}
