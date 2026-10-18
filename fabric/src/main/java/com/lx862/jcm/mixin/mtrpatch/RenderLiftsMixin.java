package com.lx862.jcm.mixin.mtrpatch;

import com.lx862.jcm.mapping.LoaderImplClient;
import com.lx862.jcm.mod.config.JCMClientConfig;
import org.mtr.core.data.Lift;
import org.mtr.libraries.com.logisticscraft.occlusionculling.OcclusionCullingInstance;
import org.mtr.libraries.com.logisticscraft.occlusionculling.util.Vec3d;
import org.mtr.mapping.holder.Box;
import org.mtr.mod.client.MinecraftClientData;
import org.mtr.mod.client.VehicleRidingMovement;
import org.mtr.mod.render.PositionAndRotation;
import org.mtr.mod.render.RenderLifts;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = RenderLifts.class, remap = false)
public class RenderLiftsMixin {
    @Inject(method = "lambda$render$3", at = @At("HEAD"), cancellable = true)
    private static void jsblock$liftFrustumCulling(Lift lift, PositionAndRotation absolutePositionAndRotation, Vec3d camera, MinecraftClientData.LiftWrapper liftWrapper, OcclusionCullingInstance occlusionCullingInstance, CallbackInfoReturnable<Runnable> cir) {
        if(JCMClientConfig.INSTANCE.mtrPatch.enableFrustumCullingLift.value()) {
            double longestDimension = Math.max(lift.getHeight(), Math.max(lift.getWidth(), lift.getDepth()));
            Vec3d startVector = new Vec3d(absolutePositionAndRotation.position.x - longestDimension, absolutePositionAndRotation.position.y - longestDimension, absolutePositionAndRotation.position.z - longestDimension);
            Vec3d endVector = new Vec3d(absolutePositionAndRotation.position.x + longestDimension, absolutePositionAndRotation.position.y + longestDimension, absolutePositionAndRotation.position.z + longestDimension);

            Box zbx = new Box(startVector.getX(), startVector.getY(), startVector.getZ(), endVector.getX(), endVector.getY(), endVector.getZ());
            /* Riding escape hatch: MTR gates the WHOLE lift block —
               VehicleRidingMovement.startRiding, floor checks and movePlayer —
               on liftWrapper.shouldRender. The vehicle path has this override;
               without it a riding player who looks away in third person was
               frozen mid-shaft while lift.tick() kept moving the car. */
            if(!LoaderImplClient.isFrustumVisible(zbx) && !VehicleRidingMovement.isRiding(lift.getId())) {
                cir.setReturnValue(() -> liftWrapper.shouldRender = false);
            }
        }
    }
}
