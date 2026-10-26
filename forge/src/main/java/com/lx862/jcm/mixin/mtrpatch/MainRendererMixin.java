package com.lx862.jcm.mixin.mtrpatch;

import com.lx862.jcm.mod.extra.JCMPatchForMTR;
import com.lx862.mtrscripting.core.util.render.TransformCapturePool;
import org.mtr.mapping.holder.Vector3d;
import org.mtr.mapping.mapper.GraphicsHolder;
import org.mtr.mod.render.MainRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = MainRenderer.class, remap = false)
public class MainRendererMixin {
    @Inject(method = "render(Lorg/mtr/mapping/mapper/GraphicsHolder;Lorg/mtr/mapping/holder/Vector3d;)V", at = @At("HEAD"))
    private static void jsblock$updateRenderingShadow(GraphicsHolder graphicsHolder, Vector3d offset, CallbackInfo ci) {
        JCMPatchForMTR.updateRenderingShadow();
    }

    /* TAIL (not RETURN): only reached after this method's queue-flush loops
       have run. The early returns (no world / disabled shadow pass) skip the
       flush and must also skip recycling — their scheduled callbacks are
       still pending and read their transform snapshots on a later frame.
       See TransformCapturePool's recycling invariant. */
    @Inject(method = "render(Lorg/mtr/mapping/mapper/GraphicsHolder;Lorg/mtr/mapping/holder/Vector3d;)V", at = @At("TAIL"))
    private static void jsblock$recycleCapturedTransforms(GraphicsHolder graphicsHolder, Vector3d offset, CallbackInfo ci) {
        TransformCapturePool.recycleAll();
    }
}
