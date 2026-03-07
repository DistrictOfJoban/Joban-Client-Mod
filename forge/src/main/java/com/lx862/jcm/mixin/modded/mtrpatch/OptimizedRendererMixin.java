package com.lx862.jcm.mixin.modded.mtrpatch;

import org.mtr.mapping.mapper.OptimizedRenderer;
import org.mtr.mapping.render.tool.GlStateTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = OptimizedRenderer.class, remap = false)
public class OptimizedRendererMixin {
    @Inject(method = "beginReload", at = @At("HEAD"), cancellable = true)
    private void jsblock$doNotReloadShader(CallbackInfo ci) {
        GlStateTracker.capture();
        ci.cancel();
    }
}
