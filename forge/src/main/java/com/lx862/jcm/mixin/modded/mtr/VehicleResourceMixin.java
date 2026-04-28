package com.lx862.jcm.mixin.modded.mtr;

import com.lx862.jcm.mod.config.JCMClientConfig;
import com.lx862.mtrscripting.mod.resource.MTRContentResourceManager;
import com.lx862.mtrscripting.mod.impl.mtr.MTRContentScripting;
import com.lx862.mtrscripting.mod.impl.mtr.vehicle.VehicleScriptInstance;
import com.lx862.mtrscripting.core.primitive.ScriptInstance;
import com.lx862.mtrscripting.core.primitive.UniqueKey;
import com.lx862.mtrscripting.core.util.ScriptVector3f;
import com.lx862.mtrscripting.core.util.render.ScriptRenderManager;
import com.lx862.mtrscripting.core.util.sound.ScriptSoundManager;
import org.mtr.core.data.VehicleCar;
import org.mtr.core.tool.Vector;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.ObjectObjectImmutablePair;
import org.mtr.mapping.holder.*;
import org.mtr.mod.client.VehicleRidingMovement;
import org.mtr.mod.data.VehicleExtension;
import org.mtr.mod.render.*;
import org.mtr.mod.resource.VehicleResource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = VehicleResource.class, remap = false)
public abstract class VehicleResourceMixin {
    @Inject(method = "queue(Lorg/mtr/mod/render/StoredMatrixTransformations;Lorg/mtr/mod/data/VehicleExtension;IIIZ)V", at = @At("HEAD"), cancellable = true)
    private void jsblock$renderScriptResult(StoredMatrixTransformations storedMatrixTransformations, VehicleExtension vehicle, int carNumber, int totalCars, int light, boolean noOpenDoorways, CallbackInfo ci) {
        // If hide riding vehicle and is current vehicle, both cancel the rendering, and cancel our script result rendering
        if(JCMClientConfig.INSTANCE.mtrPatch.hideRidingVehicle.value() && VehicleRidingMovement.isRiding(vehicle.getId())) {
            ci.cancel();
            return;
        }

        VehicleCar vehicleCar = vehicle.vehicleExtraData.immutableVehicleCars.get(carNumber);
        String scriptGroupId = MTRContentResourceManager.getVehicleScriptEntryId(vehicleCar.getVehicleId());
        if(scriptGroupId == null) return;

        ScriptInstance<?> scriptInstance = MTRContentScripting.getScriptManager().getInstanceManager().getInstance(new UniqueKey("vehicle", vehicle.getHexId(), scriptGroupId));
        if(!(scriptInstance instanceof VehicleScriptInstance)) return;

        ScriptRenderManager carRenderManager = ((VehicleScriptInstance)scriptInstance).capturedScriptCalls.carRenderManagers.get(carNumber);
        ScriptSoundManager carSoundManager = ((VehicleScriptInstance)scriptInstance).capturedScriptCalls.carSoundManagers.get(carNumber);

        StoredMatrixTransformations newTransform = storedMatrixTransformations.copy();
        newTransform.add(gh -> gh.translate(0, -1, 0)); // Replicate behaviour from MTR 3. Not sure if we should do it here tho?

        World world = World.cast(MinecraftClient.getInstance().getWorldMapped());

        double x = 0, y = 0, z = 0;
        int total = 0;
        ObjectArrayList<ObjectObjectImmutablePair<Vector, Vector>> bogiePoses = vehicle.getVehicleCarsAndPositions().get(carNumber).right();
        for(ObjectObjectImmutablePair<Vector, Vector> bogiePos : bogiePoses) {
            x += bogiePos.left().x;
            y += bogiePos.left().y;
            z += bogiePos.left().z;
            x += bogiePos.right().x;
            y += bogiePos.right().y;
            z += bogiePos.right().z;
            total += 2;
        }

        Vector bogieMidPos = new Vector(x / total, y / total, z / total);
        ScriptVector3f soundPos = new ScriptVector3f((float)bogieMidPos.x, (float)bogieMidPos.y, (float)bogieMidPos.z);

        if(carRenderManager != null) {
            carRenderManager.invoke(world, newTransform, Direction.NORTH, light);
        }

        if(carSoundManager != null) {
            carSoundManager.invoke(world, soundPos);
            carSoundManager.reset();
        }
    }
}
