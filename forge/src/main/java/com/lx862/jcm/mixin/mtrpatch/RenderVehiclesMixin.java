package com.lx862.jcm.mixin.mtrpatch;

import com.lx862.jcm.mapping.LoaderImplClient;
import com.lx862.jcm.mod.config.JCMClientConfig;
import org.mtr.libraries.com.logisticscraft.occlusionculling.OcclusionCullingInstance;
import org.mtr.libraries.com.logisticscraft.occlusionculling.util.Vec3d;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.ObjectObjectImmutablePair;
import org.mtr.mapping.holder.Box;
import org.mtr.mapping.holder.Vector3d;
import org.mtr.mod.client.VehicleRidingMovement;
import org.mtr.mod.data.VehicleExtension;
import org.mtr.mod.render.DynamicVehicleModel;
import org.mtr.mod.render.PositionAndRotation;
import org.mtr.mod.render.RenderVehicles;
import org.mtr.mod.render.StoredMatrixTransformations;
import org.mtr.mod.resource.VehicleResource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = RenderVehicles.class, remap = false)
public class RenderVehiclesMixin {
    @Inject(method = "lambda$render$3", at = @At("HEAD"), cancellable = true)
    private static void jsblock$vehicleFrustumCulling(VehicleExtension vehicle, int carNumber, ObjectObjectImmutablePair vehicleCarDetails, Vec3d camera, OcclusionCullingInstance occlusionCullingInstance, CallbackInfoReturnable<Runnable> cir) {
        if(JCMClientConfig.INSTANCE.mtrPatch.enableFrustumCullingVehicle.value()) {
            double longestDimension = vehicle.persistentVehicleData.longestDimensions[carNumber];
            Vec3d startVector = new Vec3d(((PositionAndRotation) ((ObjectObjectImmutablePair) vehicleCarDetails.right()).right()).position.x - longestDimension, ((PositionAndRotation) ((ObjectObjectImmutablePair) vehicleCarDetails.right()).right()).position.y - (double) 8.0F, ((PositionAndRotation) ((ObjectObjectImmutablePair) vehicleCarDetails.right()).right()).position.z - longestDimension);
            Vec3d endVector = new Vec3d(((PositionAndRotation) ((ObjectObjectImmutablePair) vehicleCarDetails.right()).right()).position.x + longestDimension, ((PositionAndRotation) ((ObjectObjectImmutablePair) vehicleCarDetails.right()).right()).position.y + (double) 8.0F, ((PositionAndRotation) ((ObjectObjectImmutablePair) vehicleCarDetails.right()).right()).position.z + longestDimension);

            Box zbx = new Box(startVector.getX(), startVector.getY(), startVector.getZ(), endVector.getX(), endVector.getY(), endVector.getZ());
            if (!LoaderImplClient.isFrustumVisible(zbx)) {
                cir.setReturnValue(() -> vehicle.persistentVehicleData.rayTracing[carNumber] = false);
            }
        }
    }

    @Inject(method = "lambda$render$5", at = @At("HEAD"), cancellable = true)
    private static void jsblock$hideRidingTrainBogie(Vector3d offsetVector, Double offsetRotation, PositionAndRotation ridingCarPositionAndRotation, Vector3d cameraShakeOffset, VehicleResource vehicleResource, VehicleExtension vehicle, PositionAndRotation absoluteVehicleCarPositionAndRotation, int carNumber, int[] scrollingDisplayIndexTracker, boolean fromResourcePackCreator, int bogieIndex, PositionAndRotation absoluteBogiePositionAndRotation, CallbackInfo ci) {
        if(JCMClientConfig.INSTANCE.mtrPatch.hideRidingVehicle.value() && VehicleRidingMovement.isRiding(vehicle.getId())) ci.cancel();
    }

    @Inject(method = "lambda$render$12", at = @At("HEAD"), cancellable = true)
    private static void jsblock$hideRidingTrainModel(StoredMatrixTransformations storedMatrixTransformations, VehicleExtension vehicle, int carNumber, int[] scrollingDisplayIndexTracker, PositionAndRotation absoluteVehicleCarPositionAndRotation, ObjectArrayList openDoorways, boolean fromResourcePackCreator, ObjectArrayList previousGangwayPositionsList, ObjectArrayList previousBarrierPositionsList, VehicleResource vehicleResource, PositionAndRotation vehicleCarRenderingPositionAndRotation, Vector3d offsetVector, ObjectObjectImmutablePair vehicleCarDetails, double oscillationAmount, int modelIndex, DynamicVehicleModel model, CallbackInfo ci) {
        if(JCMClientConfig.INSTANCE.mtrPatch.hideRidingVehicle.value() && VehicleRidingMovement.isRiding(vehicle.getId())) ci.cancel();
    }
}
