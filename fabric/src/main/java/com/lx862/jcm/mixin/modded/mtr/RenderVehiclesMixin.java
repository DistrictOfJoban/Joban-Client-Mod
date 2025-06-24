package com.lx862.jcm.mixin.modded.mtr;

import com.lx862.jcm.mod.resources.MTRContentResourceManager;
import com.lx862.jcm.mod.scripting.mtr.MTRScripting;
import com.lx862.jcm.mod.scripting.mtr.vehicle.VehicleScriptContext;
import com.lx862.jcm.mod.scripting.mtr.vehicle.VehicleScriptInstance;
import com.lx862.jcm.mod.scripting.mtr.vehicle.VehicleWrapper;
import com.lx862.mtrscripting.core.ParsedScript;
import com.lx862.mtrscripting.data.UniqueKey;
import org.mtr.core.data.VehicleCar;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.ObjectImmutableList;
import org.mtr.mapping.holder.*;
import org.mtr.mod.client.MinecraftClientData;
import org.mtr.mod.data.VehicleExtension;
import org.mtr.mod.render.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = RenderVehicles.class, remap = false)
public abstract class RenderVehiclesMixin {
    @Inject(method = "render(JLorg/mtr/mapping/holder/Vector3d;)V", at = @At(value = "INVOKE", target = "Lorg/mtr/libraries/it/unimi/dsi/fastutil/objects/ObjectArraySet;forEach(Ljava/util/function/Consumer;)V"))
    private static void renderScript(long millisElapsed, Vector3d cameraShakeOffset, CallbackInfo ci) {
        for(VehicleExtension vehicle : MinecraftClientData.getInstance().vehicles) {
            ObjectImmutableList<VehicleCar> cars = vehicle.vehicleExtraData.immutableVehicleCars;
            for(int i = 0; i < cars.size(); i++) {
                final int carIndex = i;
                VehicleCar vehicleCar = cars.get(carIndex);
                ParsedScript script = MTRContentResourceManager.getVehicleScript(vehicleCar.getVehicleId());
                if(script == null) continue;

                VehicleScriptInstance scriptInstance = (VehicleScriptInstance)MTRScripting.getScriptManager().getInstanceManager().getInstance(new UniqueKey("mtr", "vehicle", vehicle.getHexId(), vehicleCar.getVehicleId()), () -> new VehicleScriptInstance(new VehicleScriptContext(vehicleCar.getVehicleId(), carIndex), vehicle, script));
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
