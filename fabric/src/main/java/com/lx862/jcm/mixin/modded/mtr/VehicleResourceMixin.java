package com.lx862.jcm.mixin.modded.mtr;

import com.lx862.jcm.mod.scripting.mtr.MTRScripting;
import com.lx862.jcm.mod.scripting.mtr.eyecandy.ModelDrawCall;
import com.lx862.jcm.mod.scripting.mtr.vehicle.VehicleScriptInstance;
import com.lx862.mtrscripting.core.ScriptInstance;
import com.lx862.mtrscripting.data.UniqueKey;
import org.mtr.core.data.VehicleCar;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.mtr.mapping.holder.*;
import org.mtr.mod.data.VehicleExtension;
import org.mtr.mod.render.*;
import org.mtr.mod.resource.VehicleResource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = VehicleResource.class, remap = false)
public abstract class VehicleResourceMixin {
    @Inject(method = "queue(Lorg/mtr/mod/render/StoredMatrixTransformations;Lorg/mtr/mod/data/VehicleExtension;IIILorg/mtr/libraries/it/unimi/dsi/fastutil/objects/ObjectArrayList;)V", at = @At("HEAD"))
    private void renderScript(StoredMatrixTransformations storedMatrixTransformations, VehicleExtension vehicle, int carNumber, int totalCars, int light, ObjectArrayList<Box> openDoorways, CallbackInfo ci) {
        for(VehicleCar vehicleCar : vehicle.vehicleExtraData.immutableVehicleCars) {
            ScriptInstance<?> scriptInstance = MTRScripting.getScriptManager().getInstanceManager().getInstance(new UniqueKey("mtr", "vehicle", vehicle.getId(), vehicleCar.getVehicleId()));
            if(!(scriptInstance instanceof VehicleScriptInstance)) return;

            MainRenderer.scheduleRender(QueuedRenderLayer.TEXT, (graphicsHolder, vec) -> {
                for(ModelDrawCall drawCall : ((VehicleScriptInstance)scriptInstance).carModelDrawCalls) {
                    drawCall.run(World.cast(MinecraftClient.getInstance().getWorldMapped()), graphicsHolder, storedMatrixTransformations.copy(), Direction.NORTH, light);
                }
            });
        }
    }
}
