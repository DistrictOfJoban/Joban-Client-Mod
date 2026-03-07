package com.lx862.jcm.mixin.modded.mtrpatch;

import org.mtr.mapping.mapper.OptimizedRenderer;
import org.mtr.mapping.render.shader.ShaderManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = OptimizedRenderer.class, remap = false)
public interface OptimizedRendererAccessorMixin {
    @Accessor
    ShaderManager getShaderManager();
}
