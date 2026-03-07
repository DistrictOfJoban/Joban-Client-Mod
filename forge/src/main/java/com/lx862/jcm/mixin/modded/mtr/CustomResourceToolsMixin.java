package com.lx862.jcm.mixin.modded.mtr;

import org.mtr.mapping.holder.Identifier;
import org.mtr.mod.resource.CustomResourceTools;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.nio.file.FileSystems;
import java.util.Locale;

@Mixin(value = CustomResourceTools.class, remap = false)
public interface CustomResourceToolsMixin {
    @Inject(method = "getResourceFromSamePath", at = @At("HEAD"), cancellable = true)
    private static void jsblock$relativePathWithPathTraversal(String basePath, String resource, String extension, CallbackInfoReturnable<Identifier> cir) {
        if (resource.contains(":")) { // Assume it is already an identifier
            cir.setReturnValue(CustomResourceTools.formatIdentifierWithDefault(resource, extension));
        } else {
            Identifier basePathId = new Identifier(basePath);
            String relative = resource.toLowerCase(Locale.ROOT).replace('\\', '/');

            String resolvedPath = FileSystems.getDefault().getPath(basePathId.getPath()).getParent().resolve(relative)
                    .normalize().toString().replace('\\', '/');
            cir.setReturnValue(CustomResourceTools.formatIdentifier(basePathId.getNamespace() + ":" + resolvedPath, extension));
        }
    }
}
