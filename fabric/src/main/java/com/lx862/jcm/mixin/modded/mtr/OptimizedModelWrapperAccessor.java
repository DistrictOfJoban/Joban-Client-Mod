package com.lx862.jcm.mixin.modded.mtr;

import org.mtr.mapping.mapper.OptimizedModel;
import org.mtr.mod.resource.OptimizedModelWrapper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = OptimizedModelWrapper.class, remap = false)
public interface OptimizedModelWrapperAccessor {
    @Accessor("optimizedModel")
    OptimizedModel getOptimizedModel();
}
