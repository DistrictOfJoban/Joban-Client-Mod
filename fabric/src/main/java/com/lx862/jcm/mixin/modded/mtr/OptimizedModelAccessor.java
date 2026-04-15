package com.lx862.jcm.mixin.modded.mtr;


import org.mtr.mapping.mapper.OptimizedModel;
import org.mtr.mapping.render.object.VertexArray;
import org.mtr.mapping.render.vertex.VertexAttributeMapping;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

@Mixin(value = OptimizedModel.class, remap = false)
public interface OptimizedModelAccessor {
    @Accessor("DEFAULT_MAPPING")
    static VertexAttributeMapping getDefaultMapping() {
        throw new AssertionError();
    }

    @Accessor("uploadedParts")
    List<VertexArray> getUploadedParts();
}
