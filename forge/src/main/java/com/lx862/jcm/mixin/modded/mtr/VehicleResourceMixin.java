package com.lx862.jcm.mixin.modded.mtr;

import com.lx862.jcm.mod.scripting.mtr.MTRScripting;
import com.lx862.jcm.mod.scripting.mtr.render.ScriptRenderManager;
import com.lx862.jcm.mod.scripting.mtr.sound.ScriptSoundManager;
import com.lx862.jcm.mod.scripting.mtr.vehicle.VehicleScriptInstance;
import com.lx862.mtrscripting.core.ScriptInstance;
import com.lx862.mtrscripting.data.UniqueKey;
import com.lx862.mtrscripting.util.ScriptVector3f;
import org.mtr.core.data.VehicleCar;
import org.mtr.core.tool.Vector;
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
        int carIndex = 0;
        for(VehicleCar vehicleCar : vehicle.vehicleExtraData.immutableVehicleCars) {
            ScriptInstance<?> scriptInstance = MTRScripting.getScriptManager().getInstanceManager().getInstance(new UniqueKey("mtr", "vehicle", vehicle.getId(), vehicleCar.getVehicleId()));
            if(!(scriptInstance instanceof VehicleScriptInstance)) return;
            VehicleScriptInstance vehicleScriptInstance = (VehicleScriptInstance)scriptInstance;

            ScriptRenderManager scriptRenderManager = ((VehicleScriptInstance)scriptInstance).renderManagers[carIndex];
            ScriptSoundManager scriptSoundManager = ((VehicleScriptInstance)scriptInstance).soundManagers[carIndex];

            MainRenderer.scheduleRender(QueuedRenderLayer.TEXT, (graphicsHolder, vec) -> {
                World world = World.cast(MinecraftClient.getInstance().getWorldMapped());

                Vector carMidPos = vehicle.getHeadPosition(); // TODO: Get middle pos of each car...
                ScriptVector3f basePos = new ScriptVector3f((float)carMidPos.x, (float)carMidPos.y, (float)carMidPos.z);
                scriptRenderManager.invoke(world, storedMatrixTransformations.copy(), Direction.NORTH, light);
                scriptSoundManager.invoke(world, basePos);
            });
            carIndex++;
        }
    }
}
