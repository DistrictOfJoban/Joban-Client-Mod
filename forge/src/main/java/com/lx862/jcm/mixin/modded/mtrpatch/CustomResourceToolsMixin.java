package com.lx862.jcm.mixin.modded.mtrpatch;

import com.lx862.jcm.mod.extra.JCMPatchForMTR;
import org.mtr.mapping.holder.Identifier;
import org.mtr.mod.resource.CustomResourceTools;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = CustomResourceTools.class, remap = false)
public interface CustomResourceToolsMixin {
    @Inject(method = "getResourceFromSamePath", at = @At("HEAD"), cancellable = true)
    private static void jsblock$relativePathWithPathTraversal(String basePath, String resource, String extension, CallbackInfoReturnable<Identifier> cir) {
        cir.setReturnValue(JCMPatchForMTR.resolveRelativePath(basePath, resource, extension));
    }
}
