package com.lx862.jcm.mixin.mtrpatch;

import com.lx862.jcm.mod.config.JCMClientConfig;
import com.lx862.jcm.mod.util.JCMLogger;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.mtr.mod.resource.CachedResource;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

/**
 * dataSupplier holds onto local variables outside the supplier. Not removing reference to CachedResource would cause memory leak.
 */
@Mixin(value = CachedResource.class, remap = false)
public class CachedResourceMixin {
    @Shadow
    @Final
    private static ObjectArrayList<CachedResource<?>> CACHED_RESOURCES;

    @Shadow
    private static boolean canFetchCache;

    @Inject(method = "getData", at = @At("HEAD"))
    private void jsblock$ensureDataTracked(boolean force, CallbackInfoReturnable<?> cir) {
        if(force || canFetchCache) {
            CachedResource<?> thisObj = (CachedResource<?>)(Object)this;
            if(!CACHED_RESOURCES.contains(thisObj)) {
                if(JCMClientConfig.INSTANCE.debugMode.value()) JCMLogger.info("Re-tracked CachedResource: " + thisObj.toString());
                CACHED_RESOURCES.add(thisObj);
            }
        }
    }

    @Inject(method = "tick", at = @At("TAIL"), locals = LocalCapture.CAPTURE_FAILSOFT)
    private static void jsblock$clearUnusedResource(CallbackInfo ci, long currentMillis) {
        CACHED_RESOURCES.removeIf(e -> {
            long expiry = ((CachedResourceAccessorMixin)(Object)e).jsblock$getExpiry();
            boolean expired = currentMillis > expiry;
            if(expired && JCMClientConfig.INSTANCE.debugMode.value()) {
                JCMLogger.info("Untracked CachedResource: " + e.toString());
            }
            return expired;
        });
    }
}
