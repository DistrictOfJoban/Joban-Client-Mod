package com.lx862.jcm.mixin.modded.mtr;

import org.mtr.core.serializer.ReaderBase;
import org.mtr.legacy.generated.resource.VehicleResourceSchema;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = VehicleResourceSchema.class, remap = false)
public class LegacyVehicleResourceSchemaMixin {
    @Shadow
    protected String base_train_type;
    @Shadow
    protected String model_properties;

    @Inject(method = "updateData", at = @At("HEAD"))
    public void handleNTEBaseType(ReaderBase readerBase, CallbackInfo ci) {
        readerBase.unpackString("base_type", (str) -> {
            this.base_train_type = str;
            // MTR requires at least model_properties to not be empty, just put whatever then
            this.model_properties = "jsblock:dummy_script_model.json";
        });
    }
}
