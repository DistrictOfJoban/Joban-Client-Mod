package com.lx862.jcm.mixin.modded.mtr;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.lx862.mtrscripting.core.util.TimingJS;
import com.lx862.mtrscripting.mod.impl.mtr.vehicle.VehicleRenderDataCache;
import org.mtr.mapping.holder.MinecraftClient;
import org.mtr.mapping.holder.Vector3d;
import org.mtr.mod.render.MainRenderer;
import org.mtr.mod.render.RenderVehicles;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = MainRenderer.class, remap = false)
public class MainRendererMixin {
    @WrapOperation(method = "render(Lorg/mtr/mapping/mapper/GraphicsHolder;Lorg/mtr/mapping/holder/Vector3d;)V", at = @At(value = "INVOKE", target = "Lorg/mtr/mod/render/RenderVehicles;render(JLorg/mtr/mapping/holder/Vector3d;)V"))
    private static void jsblock$cacheVehicleRenderData(long millisElapsed, Vector3d cameraShakeOffset, Operation<Void> original) {
        VehicleRenderDataCache.startRendering();
        try {
            original.call(millisElapsed, cameraShakeOffset);
        } finally {
            VehicleRenderDataCache.finishRendering();
        }
    }

    @Inject(method = "getMillisElapsed", at = @At("RETURN"))
    private static void incrementTimer(CallbackInfoReturnable<?> ci) {
        long elapsedTime = ci.getReturnValueJ();
        if(elapsedTime > 0) { // This can be negative after rejoining world
            TimingJS.update(MinecraftClient.getInstance().isPaused() ? 0 : ci.getReturnValueJ());
        }
    }
}
