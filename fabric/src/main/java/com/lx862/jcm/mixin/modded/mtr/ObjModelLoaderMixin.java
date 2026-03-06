package com.lx862.jcm.mixin.modded.mtr;

import org.mtr.mapping.mapper.OptimizedModel;
import org.mtr.mapping.render.obj.ObjModelLoader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Locale;

/**
 * Same patch as <a href="https://github.com/Minecraft-Transit-Railway/Minecraft-Mappings/pull/18"></a>
 * Provide better compatibility for legacy trains
 */
@Mixin(value = ObjModelLoader.class, remap = false)
public class ObjModelLoaderMixin {
    @Inject(method = "legacyMapping", at = @At("HEAD"), cancellable = true)
    private static void jsblock$patchMTRLegacyMapping(String type, CallbackInfoReturnable<OptimizedModel.ShaderType> cir) {
        switch(type.toLowerCase(Locale.ENGLISH)) {
            case "exteriortranslucent":
                cir.setReturnValue(OptimizedModel.ShaderType.TRANSLUCENT);
            case "lighttranslucent":
                cir.setReturnValue(OptimizedModel.ShaderType.TRANSLUCENT_GLOWING);
            case "interiortranslucent":
                cir.setReturnValue(OptimizedModel.ShaderType.TRANSLUCENT_BRIGHT);
        }
    }
}
