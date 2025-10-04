package com.lx862.jcm.mixin.modded.mtr;

import com.lx862.jcm.mod.block.PIDS1ABlock;
import org.mtr.mapping.holder.Block;
import org.mtr.mod.block.BlockPIDSPole;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = BlockPIDSPole.class, remap = false)
public class BlockPIDSPoleMixin {
    @Inject(method = "isBlock", at = @At("HEAD"), cancellable = true)
    public void checkForPIDS1A(Block block, CallbackInfoReturnable<Boolean> cir) {
        if(block.data instanceof PIDS1ABlock) cir.setReturnValue(true);
    }
}
