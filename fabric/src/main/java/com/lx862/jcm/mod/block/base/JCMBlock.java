package com.lx862.jcm.mod.block.base;

import com.lx862.jcm.mod.util.Utils;
import org.mtr.mapping.holder.*;
import org.mtr.mapping.mapper.BlockExtension;

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

    public ActionResult getBrushActionResult(PlayerEntity player) {
        return Utils.playerHoldingBrush(player) ? ActionResult.SUCCESS : ActionResult.PASS;
    }
}
