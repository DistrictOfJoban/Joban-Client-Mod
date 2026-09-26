package com.lx862.jcm.mixin.mtrpatch;

import com.lx862.jcm.mod.extra.SignExtraAccessor;
import org.mtr.core.serializer.ReaderBase;
import org.mtr.core.serializer.WriterBase;
import org.mtr.mod.generated.resource.SignResourceSchema;
import org.mtr.mod.resource.CustomResourceTools;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = SignResourceSchema.class, remap = false)
public class SignResourceSchemaMixin implements SignExtraAccessor {
    @Unique
    private int jsblock$textColor;

    @Unique
    private String jsblock$strTextColor;

    @Inject(method = "<init>(Lorg/mtr/core/serializer/ReaderBase;)V", at = @At("TAIL"))
    private void jsblock$parseExtra(ReaderBase readerBase, CallbackInfo ci) {
        jsblock$strTextColor = readerBase.getString("textColor", "");
        if(jsblock$strTextColor.isEmpty()) {
            jsblock$textColor = -1;
        } else {
            jsblock$textColor = CustomResourceTools.colorStringToInt(jsblock$strTextColor);
        }
    }

    @Inject(method = "serializeData", at = @At("HEAD"))
    private void jsblock$serializeExtra(WriterBase writerBase, CallbackInfo ci) {
        writerBase.writeString("textColor", jsblock$strTextColor);
    }

    @Override
    public int jsblock$getSignTextColor() {
        return jsblock$textColor;
    }
}
