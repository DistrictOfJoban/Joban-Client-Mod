package com.lx862.jcm.mixin.modded.mtrpatch;

import com.lx862.jcm.mod.extra.JCMPatchForMTR;
import com.lx862.jcm.mod.registry.Events;
import org.mtr.mod.client.CustomResourceLoader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = CustomResourceLoader.class, remap = false)
public class CustomResourceLoaderMixin {
    @Inject(method = "reload", at = @At("HEAD"))
    private static void jsblock$onMTRReload(CallbackInfo ci) {
        JCMPatchForMTR.reloadOptimizedRenderingShader();
    }

    @Inject(method = "reload", at = @At("TAIL"))
    private static void jsblock$notifyCustomReload(CallbackInfo ci) {
        Events.onClientReloadResource();
    }
}
