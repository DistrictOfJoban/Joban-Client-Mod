package com.lx862.jcm.mixin.modded.mtrpatch;

import com.lx862.jcm.mod.extra.JCMPatchForMTR;
import org.mtr.mapping.mapper.OptimizedModel;
import org.mtr.mapping.render.obj.ObjModelLoader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Same patch as <a href="https://github.com/Minecraft-Transit-Railway/Minecraft-Mappings/pull/18"></a>
 * Provide better compatibility for legacy trains
 */
@Mixin(value = ObjModelLoader.class, remap = false)
public class ObjModelLoaderMixin {
    @Inject(method = "legacyMapping", at = @At("HEAD"), cancellable = true)
    private static void jsblock$patchMTRLegacyMapping(String type, CallbackInfoReturnable<OptimizedModel.ShaderType> cir) {
        OptimizedModel.ShaderType nteShaderType = JCMPatchForMTR.getNTEShaderTypeMigration(type);
        if(nteShaderType != null) {
            cir.setReturnValue(nteShaderType);
        }
    }
}
