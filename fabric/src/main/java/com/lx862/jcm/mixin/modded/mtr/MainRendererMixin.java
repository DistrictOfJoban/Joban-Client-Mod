package com.lx862.jcm.mixin.modded.mtr;

import com.lx862.mtrscripting.util.TimingUtil;
import org.mtr.mod.render.MainRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = MainRenderer.class, remap = false)
public class MainRendererMixin {
    @Inject(method = "getMillisElapsed", at = @At("RETURN"))
    private static void incrementTimer(CallbackInfoReturnable<?> ci) {
        long elapsedTime = ci.getReturnValueJ();
        if(elapsedTime > 0) { // This can be negative after rejoining world
            TimingUtil.update(ci.getReturnValueJ());
        }
    }
}
