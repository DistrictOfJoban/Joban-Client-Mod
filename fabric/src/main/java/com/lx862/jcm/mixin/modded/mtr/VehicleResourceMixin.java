package com.lx862.jcm.mixin.modded.mtr;

import com.lx862.jcm.mod.scripting.mtr.MTRScripting;
import com.lx862.jcm.mod.scripting.mtr.render.ScriptRenderManager;
import com.lx862.jcm.mod.scripting.mtr.sound.ScriptSoundManager;
import com.lx862.jcm.mod.scripting.mtr.vehicle.VehicleScriptInstance;
import com.lx862.mtrscripting.core.ScriptInstance;
import com.lx862.mtrscripting.data.UniqueKey;
import com.lx862.mtrscripting.util.Vector3dWrapper;
import org.mtr.core.data.VehicleCar;
import org.mtr.core.tool.Vector;
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
    @Inject(method = "queue(Lorg/mtr/mod/render/StoredMatrixTransformations;Lorg/mtr/mod/data/VehicleExtension;IIIZ)V", at = @At("HEAD"))
    private void renderScript(StoredMatrixTransformations storedMatrixTransformations, VehicleExtension vehicle, int carNumber, int totalCars, int light, boolean noOpenDoorways, CallbackInfo ci) {
        int carIndex = 0;
        for(VehicleCar vehicleCar : vehicle.vehicleExtraData.immutableVehicleCars) {
            ScriptInstance<?> scriptInstance = MTRScripting.getScriptManager().getInstanceManager().getInstance(new UniqueKey("mtr", "vehicle", vehicle.getHexId(), vehicleCar.getVehicleId()));
            if(!(scriptInstance instanceof VehicleScriptInstance)) return;

            ScriptRenderManager carRenderManager = ((VehicleScriptInstance)scriptInstance).renderManagers[carIndex];
            ScriptSoundManager carSoundManager = ((VehicleScriptInstance)scriptInstance).soundManagers[carIndex];

            MainRenderer.scheduleRender(QueuedRenderLayer.TEXT, (graphicsHolder, vec) -> {
                World world = World.cast(MinecraftClient.getInstance().getWorldMapped());
                Vector carMidPos = vehicle.getHeadPosition(); // TODO: Get middle pos of each car...
                Vector3dWrapper basePos = new Vector3dWrapper((float)carMidPos.x, (float)carMidPos.y, (float)carMidPos.z);

                carRenderManager.invoke(world, Vector3dWrapper.ZERO, graphicsHolder, storedMatrixTransformations.copy(), Direction.NORTH, light);
                carSoundManager.invoke(world, basePos, graphicsHolder, storedMatrixTransformations.copy(), Direction.NORTH, light);
            });
            carIndex++;
        }
    }
}
