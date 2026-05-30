package com.lx862.jcm.mixin.modded.mtrpatch;

import org.mtr.core.data.RailMath;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * NOTE: Only apply to client!
 * Calculating rail bound in 0.1m increment is rather expensive and may cause lag spikes. Follow that of built-in rail model (0.5m)
 */
@Mixin(value = RailMath.class, remap = false)
public class RailMathMixin {
    @ModifyArg(method = "<init>", at = @At(value = "INVOKE", target = "Lorg/mtr/core/data/RailMath;render(Lorg/mtr/core/data/RailMath$RenderRail;DFF)V"), index = 1)
    private double jsblock$lowerCullingBoundPrecision(double v) {
        return 0.5;
    }
}
