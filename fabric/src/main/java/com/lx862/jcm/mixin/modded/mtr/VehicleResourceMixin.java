package com.lx862.jcm.mixin.modded.mtr;

import com.lx862.jcm.mod.resources.MTRContentResourceManager;
import com.lx862.jcm.mod.scripting.mtr.MTRScripting;
import com.lx862.jcm.mod.scripting.mtr.render.ScriptRenderManager;
import com.lx862.jcm.mod.scripting.mtr.sound.ScriptSoundManager;
import com.lx862.jcm.mod.scripting.mtr.vehicle.VehicleScriptInstance;
import com.lx862.mtrscripting.core.ScriptInstance;
import com.lx862.mtrscripting.data.UniqueKey;
import com.lx862.mtrscripting.util.ScriptVector3f;
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
    private void renderCarScript(StoredMatrixTransformations storedMatrixTransformations, VehicleExtension vehicle, int carNumber, int totalCars, int light, boolean noOpenDoorways, CallbackInfo ci) {
        VehicleCar vehicleCar = vehicle.vehicleExtraData.immutableVehicleCars.get(carNumber);
        String scriptGroupId = MTRContentResourceManager.getVehicleScriptGroupId(vehicleCar.getVehicleId());
        if(scriptGroupId == null) return;

        ScriptInstance<?> scriptInstance = MTRScripting.getScriptManager().getInstanceManager().getInstance(new UniqueKey("vehicle", vehicle.getHexId(), scriptGroupId));
        if(!(scriptInstance instanceof VehicleScriptInstance)) return;

        ScriptRenderManager carRenderManager = ((VehicleScriptInstance)scriptInstance).renderManagers[carNumber];
        ScriptSoundManager carSoundManager = ((VehicleScriptInstance)scriptInstance).soundManagers[carNumber];

        StoredMatrixTransformations newTransform = storedMatrixTransformations.copy();
        newTransform.add(gh -> gh.translate(0, -1, 0)); // Replicate behaviour from MTR 3. Not sure if we should do it here tho?

        World world = World.cast(MinecraftClient.getInstance().getWorldMapped());
        Vector carMidPos = vehicle.getHeadPosition(); // TODO: Get middle pos of each car...
        ScriptVector3f basePos = new ScriptVector3f((float)carMidPos.x, (float)carMidPos.y, (float)carMidPos.z);

        if(carRenderManager != null) {
            carRenderManager.invoke(world, newTransform, Direction.NORTH, light);
        }

        if(carSoundManager != null) {
            carSoundManager.invoke(world, basePos);
        }
    }
}
