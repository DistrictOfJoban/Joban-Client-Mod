package com.lx862.jcm.mixin.modded.mtr;

import com.lx862.mtrscripting.core.primitive.ScriptInstance;
import com.lx862.mtrscripting.core.util.render.ScriptRenderManager;
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
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.ObjectImmutableList;
import org.mtr.mapping.holder.*;
import org.mtr.mod.client.MinecraftClientData;
import org.mtr.mod.data.VehicleExtension;
import org.mtr.mod.render.*;
import org.mtr.mod.resource.VehicleResource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

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
                    scriptInstance.capturedScriptCalls.capture(ctx.getScriptCallsHolder());
                    ctx.resetForNextRun();
                });
            }
        }
    }

    @Inject(method = "lambda$render$5", at = @At("TAIL"), locals = LocalCapture.CAPTURE_FAILSOFT)
    private static void jsblock$drawBogieScript(Vector3d offsetVector, Double offsetRotation, PositionAndRotation ridingCarPositionAndRotation, Vector3d cameraShakeOffset, VehicleResource vehicleResource, VehicleExtension vehicle, PositionAndRotation absoluteVehicleCarPositionAndRotation, int carNumber, int[] scrollingDisplayIndexTracker, boolean fromResourcePackCreator, int bogieIndex, PositionAndRotation absoluteBogiePositionAndRotation, CallbackInfo ci, PositionAndRotation bogieRenderingPositionAndRotation, StoredMatrixTransformations storedMatrixTransformations) {
        VehicleCar vehicleCar = vehicle.vehicleExtraData.immutableVehicleCars.get(carNumber);
        String scriptGroupId = MTRContentResourceManager.getVehicleScriptEntryId(vehicleCar.getVehicleId());
        if(scriptGroupId == null) return;

        ScriptInstance<?> scriptInstance = MTRContentScripting.getScriptManager().getInstanceManager().getInstance(new UniqueKey("vehicle", vehicle.getHexId(), scriptGroupId));
        if(!(scriptInstance instanceof VehicleScriptInstance)) return;

        World world = World.cast(MinecraftClient.getInstance().getWorldMapped());
        List<ScriptRenderManager> bogieRenderManagers = ((VehicleScriptInstance)scriptInstance).capturedScriptCalls.carBogieRenderers.get(carNumber);
        if(bogieRenderManagers != null && bogieIndex < bogieRenderManagers.size()) {
            bogieRenderManagers.get(bogieIndex).invoke(world, storedMatrixTransformations.copy(), Direction.NORTH, absoluteVehicleCarPositionAndRotation.light);
        }
    }
}
