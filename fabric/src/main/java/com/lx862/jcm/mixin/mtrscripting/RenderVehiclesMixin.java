package com.lx862.jcm.mixin.mtrscripting;

import com.lx862.jcm.nativeapi.NativeVehicleDriver;
import com.lx862.mtrscripting.core.primitive.ScriptInstance;
import com.lx862.mtrscripting.core.util.render.ScriptRenderManager;
import com.lx862.mtrscripting.mod.impl.mtr.vehicle.NTETrainWrapper;
import com.lx862.mtrscripting.mod.impl.mtr.vehicle.VehicleScriptContext;
import com.lx862.mtrscripting.mod.impl.mtr.vehicle.VehicleScriptInstance;
import com.lx862.mtrscripting.mod.impl.mtr.vehicle.VehicleWrapper;
import com.lx862.mtrscripting.mod.resource.MtrScriptingResourceManager;
import com.lx862.mtrscripting.mod.impl.mtr.MTRContentScripting;
import com.lx862.mtrscripting.core.primitive.UniqueKey;
import com.lx862.mtrscripting.mod.resource.VehicleResourceProvider;
import org.mtr.core.data.VehicleCar;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.ObjectImmutableList;
import org.mtr.mapping.holder.*;
import org.mtr.mapping.mapper.OptimizedRenderer;
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
        /* Yanyang: Iris/OptiFine shadow passes re-run RenderVehicles.render —
           drive the scripts exactly once per frame (check cached via
           OptimizedRendererMixin redirect). */
        if(OptimizedRenderer.renderingShadows()) return;
        NativeVehicleDriver.beginFrame();
        /* Per-frame live-vehicle id set for the JS instances' shouldInvalidate
           (see VehicleScriptInstance.beginFrame) — one O(V) pass here replaces
           one O(V) stream scan per scripted vehicle per frame. */
        VehicleScriptInstance.beginFrame();

        for(VehicleExtension vehicle : MinecraftClientData.getInstance().vehicles) {
            /* Yanyang: C++ (language=cpp) scripts take the native route and
               never build a VehicleScriptInstance — drive them first so their
               per-car draw calls are captured before MTR starts queueing cars.
               Idle for every vehicle whose script id has no native modules. */
            NativeVehicleDriver.render(vehicle);

            ObjectImmutableList<VehicleCar> cars = vehicle.vehicleExtraData.immutableVehicleCars;

            /* Lazily allocated: most trains carry no script at all, and a fresh
               map per vehicle per frame was pure allocation churn. */
            Object2ObjectOpenHashMap<String, VehicleResourceProvider.VehicleScriptConfiguration> scriptsInVehicle = null;
            for (VehicleCar vehicleCar : cars) {
                String scriptEntryId = MtrScriptingResourceManager.vehicle.getVehicleScriptEntryId(vehicleCar.getVehicleId());
                VehicleResourceProvider.VehicleScriptConfiguration script = MtrScriptingResourceManager.vehicle.getVehicleScript(scriptEntryId);
                if (script == null) continue;
                if (scriptsInVehicle == null) scriptsInVehicle = new Object2ObjectOpenHashMap<>();
                if (scriptsInVehicle.containsKey(scriptEntryId)) continue;
                scriptsInVehicle.put(scriptEntryId, script);
            }
            if (scriptsInVehicle == null) continue;
            /* Per-frame car sound positions for BOTH script sides (native
               replay in invokeCar, JS replay in VehicleResourceMixin). */
            NativeVehicleDriver.computeCarPositions(vehicle);

            for(Map.Entry<String, VehicleResourceProvider.VehicleScriptConfiguration> scriptEntry : scriptsInVehicle.entrySet()) {
                String scriptEntryId = scriptEntry.getKey();
                /* Yanyang: a native-only entry is also registered here so
                   NativeVehicleDriver can read its dataFetchMode, but it
                   carries no ParsedScript — driving it through the JS instance
                   pipeline would NPE on .parsedScript(); it is rendered above
                   by NativeVehicleDriver.render(). Mixed entries (a .js part
                   plus C++ modules under the same id) run BOTH: the native
                   part above, the JS part here — each side captures into its
                   own replay registry, so draw calls never collide. */
                if(scriptEntry.getValue().parsedScript() == null) continue;
                /* Plain count-then-fill: the stream allocated a pipeline, a
                   lambda and a builder per script per vehicle per frame. */
                int scriptCarCount = 0;
                for(int i = 0; i < cars.size(); i++) {
                    if(scriptEntryId.equals(MtrScriptingResourceManager.vehicle.getVehicleScriptEntryId(cars.get(i).getVehicleId()))) scriptCarCount++;
                }
                int[] carsArray = new int[scriptCarCount];
                for(int i = 0, j = 0; i < cars.size(); i++) {
                    if(scriptEntryId.equals(MtrScriptingResourceManager.vehicle.getVehicleScriptEntryId(cars.get(i).getVehicleId()))) carsArray[j++] = i;
                }

                VehicleScriptContext.DataFetchMode dataFetchMode = scriptEntry.getValue().dataFetchMode();
                VehicleScriptInstance scriptInstance = (VehicleScriptInstance) MTRContentScripting.getScriptManager().getInstanceManager().getInstance(new UniqueKey("vehicle", vehicle.getHexId(), scriptEntryId), () -> new VehicleScriptInstance(new VehicleScriptContext(vehicle, scriptEntryId, carsArray), vehicle, scriptEntry.getValue().parsedScript()));
                if(scriptInstance == null) continue;

                /* Legacy stuff */
                if(((VehicleScriptContext)scriptInstance.getContextObject()).getFetchModeOverride() != null) {
                    dataFetchMode = ((VehicleScriptContext)scriptInstance.getContextObject()).getFetchModeOverride();
                }

                /* Build the wrapper (a full position tree + per-car door
                   world scans) ONLY on frames that actually run the script —
                   while a slow script's task is in flight that is every
                   frame, and the wrapper was built and discarded each time,
                   plus once more per frame for mixed JS+native entries. */
                if(!scriptInstance.getScript().willInvokeRender(scriptInstance)) {
                    continue;
                }

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
        String scriptGroupId = MtrScriptingResourceManager.vehicle.getVehicleScriptEntryId(vehicleCar.getVehicleId());
        if(scriptGroupId == null) return;
        com.lx862.jcm.nativeapi.NativeVehicleDriver.invokeBogie(vehicle,scriptGroupId,carNumber,bogieIndex,storedMatrixTransformations,absoluteVehicleCarPositionAndRotation.light);

        ScriptInstance<?> scriptInstance = MTRContentScripting.getScriptManager().getInstanceManager().getInstance(new UniqueKey("vehicle", vehicle.getHexId(), scriptGroupId));
        if(!(scriptInstance instanceof VehicleScriptInstance)) return;

        World world = World.cast(MinecraftClient.getInstance().getWorldMapped());
        List<ScriptRenderManager> bogieRenderManagers = ((VehicleScriptInstance)scriptInstance).capturedScriptCalls.carBogieRenderers.get(carNumber);
        if(bogieRenderManagers != null && bogieIndex < bogieRenderManagers.size()) {
            bogieRenderManagers.get(bogieIndex).invoke(world, storedMatrixTransformations.copy(), Direction.NORTH, absoluteVehicleCarPositionAndRotation.light);
        }
    }
}
