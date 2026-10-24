package com.lx862.jcm.mixin.mtrscripting;

import com.lx862.mtrscripting.mod.impl.mtr.vehicle.VehicleDataCache;
import com.lx862.mtrscripting.mod.impl.mtr.vehicle.VehicleWrapper;
import org.mtr.core.data.Vehicle;
import org.mtr.mod.data.VehicleExtension;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = VehicleExtension.class, remap = false)
public class VehicleExtensionMixin {
    @Inject(method = "dispose", at = @At("HEAD"))
    public void clearScriptedVehicleStopsCache(CallbackInfo ci) {
        final long vehicleId = ((Vehicle)(Object)this).getId();
        VehicleDataCache.clearStopsDataCache(vehicleId);
        VehicleWrapper.clearDoorDirectionCache(vehicleId);
    }
}
