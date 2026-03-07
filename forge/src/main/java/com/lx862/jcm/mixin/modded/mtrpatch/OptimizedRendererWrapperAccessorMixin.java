package com.lx862.jcm.mixin.modded.mtrpatch;

import org.mtr.mapping.mapper.OptimizedRenderer;
import org.mtr.mod.resource.OptimizedRendererWrapper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = OptimizedRendererWrapper.class, remap = false)
public interface OptimizedRendererWrapperAccessorMixin {
    @Accessor
    OptimizedRenderer getOptimizedRenderer();
}
