package com.lx862.jcm.mixin.modded.mtr;

import com.lx862.jcm.mod.config.JCMClientConfig;
import com.lx862.mtrscripting.mod.impl.mtr.vehicle.NTETrainWrapper;
import com.lx862.mtrscripting.mod.impl.mtr.vehicle.VehicleScriptContext;
import com.lx862.mtrscripting.mod.impl.mtr.vehicle.VehicleScriptInstance;
import com.lx862.mtrscripting.mod.impl.mtr.vehicle.VehicleWrapper;
import com.lx862.mtrscripting.mod.resource.MTRContentResourceManager;
import com.lx862.mtrscripting.mod.impl.mtr.MTRContentScripting;
import com.lx862.mtrscripting.core.primitive.ParsedScript;
import com.lx862.mtrscripting.core.primitive.UniqueKey;
import org.mtr.core.data.VehicleCar;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.ObjectImmutableList;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.ObjectObjectImmutablePair;
import org.mtr.mapping.holder.*;
import org.mtr.mod.client.MinecraftClientData;
import org.mtr.mod.client.VehicleRidingMovement;
import org.mtr.mod.data.VehicleExtension;
import org.mtr.mod.render.*;
import org.mtr.mod.resource.VehicleResource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Mixin(value = RenderVehicles.class, remap = false)
public class RenderVehiclesMixin {
    @Inject(method = "render(JLorg/mtr/mapping/holder/Vector3d;)V", at = @At(value = "INVOKE", target = "Lorg/mtr/libraries/it/unimi/dsi/fastutil/objects/ObjectArraySet;forEach(Ljava/util/function/Consumer;)V"))
    private static void jsblock$executeScript(long millisElapsed, Vector3d cameraShakeOffset, CallbackInfo ci) {
        for(VehicleExtension vehicle : MinecraftClientData.getInstance().vehicles) {
            ObjectImmutableList<VehicleCar> cars = vehicle.vehicleExtraData.immutableVehicleCars;
            Object2ObjectOpenHashMap<String, ParsedScript> scriptsInVehicle = new Object2ObjectOpenHashMap<>();

            for (VehicleCar vehicleCar : cars) {
                String scriptEntryId = MTRContentResourceManager.getVehicleScriptEntryId(vehicleCar.getVehicleId());
                ParsedScript script = MTRContentResourceManager.getVehicleScript(scriptEntryId);
                if (script == null || scriptsInVehicle.containsKey(scriptEntryId)) continue;
                scriptsInVehicle.put(scriptEntryId, script);
            }

            for(Map.Entry<String, ParsedScript> scriptEntry : scriptsInVehicle.entrySet()) {
                String scriptEntryId = scriptEntry.getKey();
                List<Integer> carsForScripts = new ArrayList<>();
                for(int i = 0; i < cars.size(); i++) {
                    if(MTRContentResourceManager.getVehicleScriptEntryId(cars.get(i).getVehicleId()).equals(scriptEntryId)) carsForScripts.add(i);
                }
                int[] carsArray = carsForScripts.stream().mapToInt(i->i).toArray();
                boolean requireDataPrefetching = MTRContentResourceManager.shouldPrefetchVehicleData(scriptEntryId);

                VehicleScriptInstance scriptInstance = (VehicleScriptInstance) MTRContentScripting.getScriptManager().getInstanceManager().getInstance(new UniqueKey("vehicle", vehicle.getHexId(), scriptEntryId), () -> new VehicleScriptInstance(new VehicleScriptContext(vehicle, scriptEntryId, carsArray, requireDataPrefetching), vehicle, scriptEntry.getValue()));
                if(scriptInstance == null) continue;

                VehicleScriptContext.DataFetchMode dataFetchMode = ((VehicleScriptContext)scriptInstance.getContextObject()).getDataFetchMode();
                VehicleWrapper wrapperObject = new NTETrainWrapper(dataFetchMode, vehicle);
                scriptInstance.setWrapperObject(wrapperObject);

                if(dataFetchMode == VehicleScriptContext.DataFetchMode.MANDATORY && !wrapperObject.isStopsDataFullyFetched()) {
                    continue;
                }

                scriptInstance.getScript().invokeRenderFunctions(scriptInstance, () -> {
                    VehicleScriptContext ctx = (VehicleScriptContext) scriptInstance.getContextObject();
                    scriptInstance.captureRenderCalls(ctx.getCarRenderManagers());
                    scriptInstance.captureSoundCalls(ctx.getCarSoundManagers());
                    ctx.resetForNextRun();
                });
            }
        }
    }

    @Inject(method = "lambda$render$5", at = @At("HEAD"), cancellable = true)
    private static void jsblock$hideRidingTrainBogie(Vector3d offsetVector, Double offsetRotation, PositionAndRotation ridingCarPositionAndRotation, Vector3d cameraShakeOffset, VehicleResource vehicleResource, VehicleExtension vehicle, PositionAndRotation absoluteVehicleCarPositionAndRotation, int carNumber, int[] scrollingDisplayIndexTracker, boolean fromResourcePackCreator, int bogieIndex, PositionAndRotation absoluteBogiePositionAndRotation, CallbackInfo ci) {
        if(JCMClientConfig.INSTANCE.hideRidingVehicle.value() && VehicleRidingMovement.isRiding(vehicle.getId())) ci.cancel();
    }

    @Inject(method = "lambda$render$12", at = @At("HEAD"), cancellable = true)
    private static void jsblock$hideRidingTrainModel(StoredMatrixTransformations storedMatrixTransformations, VehicleExtension vehicle, int carNumber, int[] scrollingDisplayIndexTracker, PositionAndRotation absoluteVehicleCarPositionAndRotation, ObjectArrayList openDoorways, boolean fromResourcePackCreator, ObjectArrayList previousGangwayPositionsList, ObjectArrayList previousBarrierPositionsList, VehicleResource vehicleResource, PositionAndRotation vehicleCarRenderingPositionAndRotation, Vector3d offsetVector, ObjectObjectImmutablePair vehicleCarDetails, double oscillationAmount, int modelIndex, DynamicVehicleModel model, CallbackInfo ci) {
        if(JCMClientConfig.INSTANCE.hideRidingVehicle.value() && VehicleRidingMovement.isRiding(vehicle.getId())) ci.cancel();
    }
}
