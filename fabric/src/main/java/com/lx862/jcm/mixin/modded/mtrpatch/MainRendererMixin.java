package com.lx862.jcm.mixin.modded.mtrpatch;

import com.lx862.jcm.mod.extra.JCMPatchForMTR;
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
}
