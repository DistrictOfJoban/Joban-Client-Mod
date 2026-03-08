package com.lx862.jcm.mixin.modded.mtrpatch;

import org.mtr.core.serializer.ReaderBase;
import org.mtr.mapping.holder.Identifier;
import org.mtr.mod.generated.resource.SignResourceSchema;
import org.mtr.mod.resource.CustomResourceTools;
import org.mtr.mod.resource.SignResource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = SignResource.class, remap = false)
public class SignResourceMixin extends SignResourceSchema {
    @Unique private Identifier jsblock$cachedIdentifier;

    protected SignResourceMixin(String id, String textureResource, boolean flipTexture, String customText, boolean flipCustomText, boolean small, String backgroundColor) {
        super(id, textureResource, flipTexture, customText, flipCustomText, small, backgroundColor);
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    private void jsblock$cacheIdentifier(ReaderBase readerBase, CallbackInfo ci) {
        this.jsblock$cachedIdentifier = CustomResourceTools.formatIdentifierWithDefault(textureResource, "png");
    }

    @Inject(method = "getTexture", at = @At("HEAD"), cancellable = true)
    private void jsblock$returnCachedTexture(CallbackInfoReturnable<Identifier> cir) {
        cir.setReturnValue(this.jsblock$cachedIdentifier);
    }
}
