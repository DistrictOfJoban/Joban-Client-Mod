package com.lx862.jcm.mixin.modded.mtr;

import org.mtr.mapping.render.vertex.VertexAttributeState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = VertexAttributeState.class, remap = false)
public interface VertexAttributeStateAccessor {
    @Accessor("color")
    @Mutable
    void setColor(Integer color);
}
