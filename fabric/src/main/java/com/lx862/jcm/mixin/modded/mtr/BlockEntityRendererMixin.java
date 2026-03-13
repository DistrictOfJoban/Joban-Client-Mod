package com.lx862.jcm.mixin.modded.mtr;

import org.mtr.mapping.mapper.BlockEntityExtension;
import org.mtr.mapping.mapper.BlockEntityRenderer;
import org.mtr.mod.block.BlockEyeCandy;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Allow eyecandy block to render even if off-screen, so scripts can run consistently
 */
@Mixin(value = BlockEntityRenderer.class, remap = false)
public class BlockEntityRendererMixin<T extends BlockEntityExtension> {
    @Inject(method = "rendersOutsideBoundingBox2", at = @At("HEAD"), cancellable = true)
    private void jsblock$doNotCullEyecandy(T blockEntity, CallbackInfoReturnable<Boolean> cir) {
        if(blockEntity instanceof BlockEyeCandy.BlockEntity) {
            cir.setReturnValue(true);
        }
    }
}
