package com.lx862.jcm.mixin.modded.mtr;

import com.lx862.jcm.mod.resources.MTRContentResourceManager;
import com.lx862.jcm.mod.scripting.mtr.MTRScripting;
import com.lx862.jcm.mod.scripting.mtr.vehicle.*;
import com.lx862.mtrscripting.core.ParsedScript;
import com.lx862.mtrscripting.data.UniqueKey;
import org.mtr.core.data.VehicleCar;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.ObjectImmutableList;
import org.mtr.mapping.holder.*;
import org.mtr.mod.client.MinecraftClientData;
import org.mtr.mod.data.VehicleExtension;
import org.mtr.mod.render.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Mixin(value = RenderVehicles.class, remap = false)
public abstract class RenderVehiclesMixin {
    @Inject(method = "render(JLorg/mtr/mapping/holder/Vector3d;)V", at = @At(value = "INVOKE", target = "Lorg/mtr/libraries/it/unimi/dsi/fastutil/objects/ObjectArraySet;forEach(Ljava/util/function/Consumer;)V"))
    private static void renderScript(long millisElapsed, Vector3d cameraShakeOffset, CallbackInfo ci) {
        for(VehicleExtension vehicle : MinecraftClientData.getInstance().vehicles) {
            ObjectImmutableList<VehicleCar> cars = vehicle.vehicleExtraData.immutableVehicleCars;
            Object2ObjectOpenHashMap<String, ParsedScript> scriptsInVehicle = new Object2ObjectOpenHashMap<>();

            for (VehicleCar vehicleCar : cars) {
                String vehicleGroupId = MTRContentResourceManager.getVehicleScriptGroupId(vehicleCar.getVehicleId());
                ParsedScript script = MTRContentResourceManager.getVehicleScript(vehicleGroupId);
                if (script == null || scriptsInVehicle.containsKey(vehicleGroupId)) continue;
                scriptsInVehicle.put(vehicleGroupId, script);
            }

            VehicleWrapper wrapperObject = new NTETrainWrapper(vehicle);

            for(Map.Entry<String, ParsedScript> scriptEntry : scriptsInVehicle.entrySet()) {
                String vehicleGroupId = scriptEntry.getKey();
                List<Integer> carsForScripts = new ArrayList<>();
                for(int i = 0; i < cars.size(); i++) {
                    if(MTRContentResourceManager.getVehicleScriptGroupId(cars.get(i).getVehicleId()).equals(vehicleGroupId)) carsForScripts.add(i);
                }
                int[] carsArray = carsForScripts.stream().mapToInt(i->i).toArray();

                VehicleScriptInstance scriptInstance = (VehicleScriptInstance)MTRScripting.getScriptManager().getInstanceManager().getInstance(new UniqueKey("vehicle", vehicle.getHexId(), vehicleGroupId), () -> new VehicleScriptInstance(new VehicleScriptContext(vehicle, vehicleGroupId, carsArray, cars.size()), vehicle, scriptEntry.getValue()));
                if(scriptInstance == null) continue;

                if(((VehicleScriptContext)scriptInstance.getScriptContext()).requireFullStopsData()) {
                    VehicleDataCache.requestVehicleStopsData(vehicle.getId(), vehicle.vehicleExtraData.getSidingId());
                }

                scriptInstance.setWrapperObject(wrapperObject);
                scriptInstance.getScript().invokeRenderFunctions(scriptInstance, () -> {
                    VehicleScriptContext ctx = (VehicleScriptContext) scriptInstance.getScriptContext();
                    scriptInstance.captureRenderCalls(ctx.getCarRenderManagers());
                    scriptInstance.captureSoundCalls(ctx.getCarSoundManagers());
                    ctx.resetForNextRun();
                });
            }
        }
    }
}
