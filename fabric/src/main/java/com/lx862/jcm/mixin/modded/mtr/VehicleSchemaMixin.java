package com.lx862.jcm.mixin.modded.mtr;

import org.mtr.core.generated.data.VehicleSchema;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = VehicleSchema.class, remap = false)
public interface VehicleSchemaMixin {
    @Accessor("railProgress")
    double getRailProgress();
}
