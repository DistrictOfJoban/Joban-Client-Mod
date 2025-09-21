package com.lx862.jcm.mixin.modded.mtr;

import com.lx862.jcm.mod.scripting.mtr.MTRScripting;
import com.lx862.jcm.mod.scripting.mtr.eyecandy.ModelDrawCall;
import com.lx862.jcm.mod.scripting.mtr.vehicle.VehicleScriptInstance;
import com.lx862.mtrscripting.core.ScriptInstance;
import com.lx862.mtrscripting.data.UniqueKey;
import org.mtr.core.data.VehicleCar;
import org.mtr.mapping.holder.Direction;
import org.mtr.mapping.holder.MinecraftClient;
import org.mtr.mapping.holder.World;
import org.mtr.mod.data.VehicleExtension;
import org.mtr.mod.render.MainRenderer;
import org.mtr.mod.render.QueuedRenderLayer;
import org.mtr.mod.render.StoredMatrixTransformations;
import org.mtr.mod.resource.VehicleResource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = VehicleResource.class, remap = false)
public abstract class VehicleResourceMixin {
    @Inject(method = "queue(Lorg/mtr/mod/render/StoredMatrixTransformations;Lorg/mtr/mod/data/VehicleExtension;IIIZ)V", at = @At("HEAD"))
    private void renderScript(StoredMatrixTransformations storedMatrixTransformations, VehicleExtension vehicle, int carNumber, int totalCars, int light, boolean noOpenDoorways, CallbackInfo ci) {
        for(VehicleCar vehicleCar : vehicle.vehicleExtraData.immutableVehicleCars) {
            ScriptInstance<?> scriptInstance = MTRScripting.getScriptManager().getInstanceManager().getInstance(new UniqueKey("mtr", "vehicle", vehicle.getId(), vehicleCar.getVehicleId()));
            if(!(scriptInstance instanceof VehicleScriptInstance)) return;
            VehicleScriptInstance vehicleScriptInstance = (VehicleScriptInstance)scriptInstance;

            MainRenderer.scheduleRender(QueuedRenderLayer.TEXT, (graphicsHolder, vec) -> {
                World world = World.cast(MinecraftClient.getInstance().getWorldMapped());
                for(ModelDrawCall drawCall : vehicleScriptInstance.carModelDrawCalls) {
                    drawCall.run(world, graphicsHolder, storedMatrixTransformations.copy(), Direction.NORTH, light);
                }
                vehicleScriptInstance.getSoundManager().invoke(world, graphicsHolder, storedMatrixTransformations.copy(), Direction.NORTH, light);
            });
        }
    }
}
