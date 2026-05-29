package com.lx862.jcm.mixin.modded.mtrpatch;

import org.mtr.core.data.RailMath;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(value = RailMath.class, remap = false)
public interface RailMathAccessor {
    @Invoker("getPositionY")
    double invokeGetPositionY(double value);
}
