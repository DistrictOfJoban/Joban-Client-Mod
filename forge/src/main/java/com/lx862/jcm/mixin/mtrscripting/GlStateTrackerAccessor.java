package com.lx862.jcm.mixin.mtrscripting;

import org.mtr.mapping.render.tool.GlStateTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = GlStateTracker.class, remap = false)
public interface GlStateTrackerAccessor {
    @Accessor("isStateProtected")
    static boolean isStateProtected() {
        throw new AssertionError();
    }
}
