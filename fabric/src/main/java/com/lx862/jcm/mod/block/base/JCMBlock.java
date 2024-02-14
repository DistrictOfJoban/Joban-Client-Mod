package com.lx862.jcm.mod.block.base;

import com.lx862.jcm.mod.util.BlockUtil;
import com.lx862.jcm.mod.util.JCMLogger;
import com.lx862.jcm.mod.util.JCMUtil;
import org.mtr.mapping.holder.*;
import org.mtr.mapping.mapper.BlockExtension;

import java.util.function.Consumer;

public abstract class JCMBlock extends BlockExtension {
    public JCMBlock(BlockSettings settings) {
        super(settings);
    }

    @Override
    public ActionResult onUse2(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        if(!world.isClient()) {
            onServerUse(state, world, pos, player, hand, hit);
        }
        return ActionResult.PASS;
    }

    /**
     * This method will be called only on the server-side when the onUse method is triggered.
     * Override this method to implement server-side only right click logic
     */
    public void onServerUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {

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

    public ActionResult getBrushActionResult(PlayerEntity player) {
        return JCMUtil.playerHoldingBrush(player) ? ActionResult.SUCCESS : ActionResult.PASS;
    }
}
