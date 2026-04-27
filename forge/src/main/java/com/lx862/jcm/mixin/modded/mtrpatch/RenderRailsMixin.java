package com.lx862.jcm.mixin.modded.mtrpatch;

import com.lx862.jcm.mod.config.JCMClientConfig;
import org.mtr.core.data.Rail;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.ObjectArraySet;
import org.mtr.mapping.holder.ClientWorld;
import org.mtr.mod.render.RenderRails;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = RenderRails.class, remap = false)
public class RenderRailsMixin {
    @Inject(method = "lambda$render$5", at = @At("HEAD"), cancellable = true)
    private static void jsblock$cancelRailRendering(boolean holdingRailRelated, ObjectArraySet hoverRails, ClientWorld clientWorld, Rail rail, CallbackInfo ci) {
        if(JCMClientConfig.INSTANCE.disableRailRendering.value()) ci.cancel();
    }
}
