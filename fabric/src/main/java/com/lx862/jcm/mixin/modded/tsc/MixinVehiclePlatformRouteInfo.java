package com.lx862.jcm.mixin.modded.tsc;

import org.mtr.core.data.Platform;
import org.mtr.core.data.Route;
import org.mtr.core.data.VehicleExtraData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = VehicleExtraData.VehiclePlatformRouteInfo.class, remap = false)
public interface MixinVehiclePlatformRouteInfo {
    @Accessor
    Platform getThisPlatform();

    @Accessor
    Route getThisRoute();

    @Accessor
    int getPlatformIndexInRoute();
}
