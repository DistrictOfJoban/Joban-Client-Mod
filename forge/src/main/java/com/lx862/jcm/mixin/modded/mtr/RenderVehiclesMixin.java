package com.lx862.jcm.mixin.modded.mtr;

import com.lx862.jcm.mod.resources.MTRContentResourceManager;
import com.lx862.jcm.mod.scripting.mtr.MTRScripting;
import com.lx862.jcm.mod.scripting.mtr.vehicle.VehicleScriptContext;
import com.lx862.jcm.mod.scripting.mtr.vehicle.VehicleScriptInstance;
import com.lx862.jcm.mod.scripting.mtr.vehicle.VehicleWrapper;
import com.lx862.mtrscripting.core.ParsedScript;
import com.lx862.mtrscripting.data.UniqueKey;
import org.mtr.core.data.VehicleCar;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.ObjectImmutableList;
import org.mtr.mapping.holder.Vector3d;
import org.mtr.mod.client.MinecraftClientData;
import org.mtr.mod.data.VehicleExtension;
import org.mtr.mod.render.RenderVehicles;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

@Mixin(value = RenderVehicles.class, remap = false)
public abstract class RenderVehiclesMixin {
    @Inject(method = "render(JLorg/mtr/mapping/holder/Vector3d;)V", at = @At(value = "INVOKE", target = "Lorg/mtr/libraries/it/unimi/dsi/fastutil/objects/ObjectArraySet;forEach(Ljava/util/function/Consumer;)V"))
    private static void renderScript(long millisElapsed, Vector3d cameraShakeOffset, CallbackInfo ci) {
        for(VehicleExtension vehicle : MinecraftClientData.getInstance().vehicles) {
            ObjectImmutableList<VehicleCar> cars = vehicle.vehicleExtraData.immutableVehicleCars;
            Object2ObjectOpenHashMap<String, ParsedScript> scriptsInVehicle = new Object2ObjectOpenHashMap<>();

            for (VehicleCar vehicleCar : cars) {
                String vehicleCarId = vehicleCar.getVehicleId();
                ParsedScript script = MTRContentResourceManager.getVehicleScript(vehicleCarId);
                if (script == null || scriptsInVehicle.containsKey(vehicleCarId)) continue;
                scriptsInVehicle.put(vehicleCarId, script);
            }

            for(Map.Entry<String, ParsedScript> scriptEntry : scriptsInVehicle.entrySet()) {
                VehicleScriptInstance scriptInstance = (VehicleScriptInstance)MTRScripting.getScriptManager().getInstanceManager().getInstance(new UniqueKey("mtr", "vehicle", vehicle.getHexId(), scriptEntry.getKey()), () -> new VehicleScriptInstance(new VehicleScriptContext(scriptEntry.getKey()), vehicle, scriptEntry.getValue()));
                if(!(scriptInstance instanceof VehicleScriptInstance)) continue;

                VehicleWrapper wrapperObject = new VehicleWrapper(vehicle);
                scriptInstance.setWrapperObject(wrapperObject);
                scriptInstance.getScript().invokeRenderFunctions(scriptInstance, () -> {
                    VehicleScriptContext ctx = (VehicleScriptContext) scriptInstance.getScriptContext();
                    scriptInstance.setCarSoundCalls(ctx.getCarSoundCalls());
                    scriptInstance.setAnnounceSoundCalls(ctx.getAnnounceSoundCalls());
                    scriptInstance.setCarModelDrawCalls(ctx.getCarModelDrawCalls());
                    ctx.reset();
                });
            }
        }
    }
}
