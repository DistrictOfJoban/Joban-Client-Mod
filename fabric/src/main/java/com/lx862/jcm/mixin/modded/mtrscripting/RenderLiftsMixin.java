package com.lx862.jcm.mixin.modded.mtrscripting;

import com.lx862.mtrscripting.core.primitive.ScriptInstance;
import com.lx862.mtrscripting.core.primitive.UniqueKey;
import com.lx862.mtrscripting.core.util.ScriptVector3f;
import com.lx862.mtrscripting.core.util.render.ScriptRenderManager;
import com.lx862.mtrscripting.core.util.sound.ScriptSoundManager;
import com.lx862.mtrscripting.mod.impl.mtr.MTRContentScripting;
import com.lx862.mtrscripting.mod.impl.mtr.lift.LiftScriptContext;
import com.lx862.mtrscripting.mod.impl.mtr.lift.LiftScriptInstance;
import com.lx862.mtrscripting.mod.impl.mtr.lift.LiftWrapper;
import com.lx862.mtrscripting.mod.impl.mtr.vehicle.VehicleScriptContext;
import com.lx862.mtrscripting.mod.impl.mtr.vehicle.VehicleScriptInstance;
import com.lx862.mtrscripting.mod.resource.MTRContentResourceManager;
import org.mtr.core.data.Lift;
import org.mtr.libraries.com.logisticscraft.occlusionculling.util.Vec3d;
import org.mtr.libraries.it.unimi.dsi.fastutil.ints.IntObjectImmutablePair;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.mtr.mapping.holder.*;
import org.mtr.mod.client.MinecraftClientData;
import org.mtr.mod.render.PositionAndRotation;
import org.mtr.mod.render.RenderLifts;
import org.mtr.mod.render.StoredMatrixTransformations;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(value = RenderLifts.class, remap = false)
public class RenderLiftsMixin {
    @Inject(method = "render(JLorg/mtr/mapping/holder/Vector3d;)V", at = @At(value = "INVOKE", target = "Lorg/mtr/libraries/it/unimi/dsi/fastutil/objects/ObjectCollection;forEach(Ljava/util/function/Consumer;)V"))
    private static void jsblock$executeScript(long millisElapsed, Vector3d cameraShakeOffset, CallbackInfo ci) {
        for(MinecraftClientData.LiftWrapper liftWrapper : MinecraftClientData.getInstance().liftWrapperList.values()) {
            String style = liftWrapper.getLift().getStyle();

            MTRContentResourceManager.LiftScriptConfiguration scriptEntry = MTRContentResourceManager.getLiftScript(style);
            if(scriptEntry == null) continue;

            LiftWrapper wrapperObject = new LiftWrapper(liftWrapper, World.cast(MinecraftClient.getInstance().getWorldMapped()));
            LiftScriptInstance scriptInstance = (LiftScriptInstance) MTRContentScripting.getScriptManager().getInstanceManager().getInstance(new UniqueKey("lift", liftWrapper.getLift().getHexId()), () -> new LiftScriptInstance(new LiftScriptContext(liftWrapper.getLift()), wrapperObject, scriptEntry.parsedScript()));
            if(scriptInstance == null) continue;

            scriptInstance.setWrapperObject(wrapperObject);

            scriptInstance.getScript().invokeRenderFunctions(scriptInstance, () -> {
                LiftScriptContext ctx = (LiftScriptContext) scriptInstance.getContextObject();
                scriptInstance.updateRenderer(ctx.getRenderManager());
                scriptInstance.updateSound(ctx.getSoundManager());
                ctx.resetForNextRun();
            });
        }
    }

    @Inject(method = "lambda$render$6", at = @At(value = "INVOKE", target = "Lorg/mtr/mod/model/ModelLift1;render(Lorg/mtr/mod/render/StoredMatrixTransformations;Lorg/mtr/core/data/NameColorDataBase;Lorg/mtr/mapping/holder/Identifier;IFFZIIZZZZZ)V"), locals = LocalCapture.CAPTURE_FAILSOFT)
    private static void jsblock$drawScript(boolean isHoldingRefresher, ClientWorld clientWorld, ObjectArrayList cullingTasks, Vec3d camera, Vector3d cameraShakeOffset, boolean canRide, ClientPlayerEntity clientPlayerEntity, long millisElapsed, MinecraftClientData.LiftWrapper liftWrapper, CallbackInfo ci, Lift lift, PositionAndRotation absolutePositionAndRotation, IntObjectImmutablePair ridingVehicleCarNumberAndOffset, PositionAndRotation ridingCarPositionAndRotation, Vector3d offsetVector, Double offsetRotation, PositionAndRotation renderingPositionAndRotation, ObjectArrayList floorsAndDoorways, ObjectArrayList openDoorways, Box doorway1, Box doorway2, boolean doorway1Open, boolean doorway2Open, StoredMatrixTransformations storedMatrixTransformations) {
        ScriptInstance<?> scriptInstance = MTRContentScripting.getScriptManager().getInstanceManager().getInstance(new UniqueKey("lift", lift.getHexId()));
        if(!(scriptInstance instanceof LiftScriptInstance)) return;

        World world = World.cast(MinecraftClient.getInstance().getWorldMapped());
        ScriptRenderManager scriptRenderManager = ((LiftScriptInstance)scriptInstance).getRenderManager();
        ScriptSoundManager scriptSoundManager = ((LiftScriptInstance)scriptInstance).getSoundManager();
        scriptRenderManager.invoke(world, storedMatrixTransformations.copy(), Direction.NORTH, absolutePositionAndRotation.light);
        scriptSoundManager.invoke(world, new ScriptVector3f((int)absolutePositionAndRotation.position.x, (int)absolutePositionAndRotation.position.y, (int)absolutePositionAndRotation.position.z));
    }
}
