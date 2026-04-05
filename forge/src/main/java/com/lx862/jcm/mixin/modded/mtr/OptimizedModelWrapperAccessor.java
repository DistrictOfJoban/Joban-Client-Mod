package com.lx862.jcm.mixin.modded.mtr;

import org.mtr.mapping.mapper.OptimizedModel;
import org.mtr.mod.resource.OptimizedModelWrapper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(value = OptimizedModelWrapper.class, remap = false)
public interface OptimizedModelWrapperAccessor {
    @Invoker("<init>")
    static OptimizedModelWrapper createNew(OptimizedModel optimizedModel) {
        throw new AssertionError();
    }

    @Accessor("optimizedModel")
    OptimizedModel getOptimizedModel();
}
