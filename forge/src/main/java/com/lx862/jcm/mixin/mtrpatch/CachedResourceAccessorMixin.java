package com.lx862.jcm.mixin.mtrpatch;

import org.mtr.mod.resource.CachedResource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = CachedResource.class, remap = false)
public interface CachedResourceAccessorMixin {
    @Accessor("expiry")
    long jsblock$getExpiry();
}
