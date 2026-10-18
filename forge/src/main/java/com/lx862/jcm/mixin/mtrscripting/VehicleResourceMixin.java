package com.lx862.jcm.mixin.mtrscripting;

import com.lx862.jcm.mod.config.JCMClientConfig;
import com.lx862.jcm.nativeapi.NativeVehicleDriver;
import com.lx862.mtrscripting.mod.resource.MtrScriptingResourceManager;
import org.mtr.mod.client.VehicleRidingMovement;
import com.lx862.mtrscripting.mod.impl.mtr.MTRContentScripting;
import com.lx862.mtrscripting.core.util.render.ScriptRenderManager;
import com.lx862.mtrscripting.core.util.sound.ScriptSoundManager;
import com.lx862.mtrscripting.mod.impl.mtr.vehicle.VehicleScriptInstance;
import com.lx862.mtrscripting.core.primitive.ScriptInstance;
import com.lx862.mtrscripting.core.primitive.UniqueKey;
import com.lx862.mtrscripting.core.util.ScriptVector3f;
import org.mtr.core.data.VehicleCar;
import org.mtr.core.tool.Vector;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.ObjectObjectImmutablePair;
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
    private void jsblock$drawCarScript(StoredMatrixTransformations storedMatrixTransformations, VehicleExtension vehicle, int carNumber, int totalCars, int light, boolean noOpenDoorways, CallbackInfo ci) {
        /* Yanyang: the mtrpatch VehicleResourceMixin (priority 900) cancels the
           original queue() for the ridden car when hideRidingVehicle is on, but
           this handler still ran and replayed the scripted PIDS/LCD content
           floating in front of the camera — exactly what that option promises
           to hide. Each @Inject handler has its own CallbackInfo, so this needs
           its own guard. */
        if(JCMClientConfig.INSTANCE.mtrPatch.hideRidingVehicle.value() && VehicleRidingMovement.isRiding(vehicle.getId())) return;

        VehicleCar vehicleCar = vehicle.vehicleExtraData.immutableVehicleCars.get(carNumber);
        String scriptGroupId = MtrScriptingResourceManager.vehicle.getVehicleScriptEntryId(vehicleCar.getVehicleId());
        if(scriptGroupId == null) return;

        /* Yanyang: native (language=cpp) scripts never create a
           VehicleScriptInstance, so their captured draw calls live in
           NativeDrawRegistry instead. Replaying them HERE puts the LCD quads
           through the exact same StoredMatrixTransformations / light as the JS
           path, which is what makes the panel follow the car body. */
        NativeVehicleDriver.invokeCar(vehicle, scriptGroupId, carNumber,
                storedMatrixTransformations, light);

        ScriptInstance<?> scriptInstance = MTRContentScripting.getScriptManager().getInstanceManager().getInstance(new UniqueKey("vehicle", vehicle.getHexId(), scriptGroupId));
        if(!(scriptInstance instanceof VehicleScriptInstance)) return;

        ScriptRenderManager carRenderManager = ((VehicleScriptInstance)scriptInstance).capturedScriptCalls.carRenderManagers.get(carNumber);
        ScriptSoundManager carSoundManager = ((VehicleScriptInstance)scriptInstance).capturedScriptCalls.carSoundManagers.get(carNumber);

        StoredMatrixTransformations newTransform = storedMatrixTransformations.copy();
        newTransform.add(gh -> gh.translate(0, -1, 0)); // Replicate behaviour from MTR 3. Not sure if we should do it here tho?

        World world = World.cast(MinecraftClient.getInstance().getWorldMapped());

        /* Yanyang: per-car average bogie position, computed once per frame by
           RenderVehiclesMixin (shared with the native path). This used to
           rebuild MTR's whole getVehicleCarsAndPositions() tree per car per
           frame — an N-car train paid 2N+1 full tree builds per frame across
           culling and replay. */
        ScriptVector3f soundPos = NativeVehicleDriver.carSoundPosition(vehicle.getHexId(), carNumber);

        if(carRenderManager != null) {
            carRenderManager.invoke(world, newTransform, Direction.NORTH, light);
        }

        if(carSoundManager != null && soundPos != null) {
            carSoundManager.invoke(world, soundPos);
        }
    }
}
