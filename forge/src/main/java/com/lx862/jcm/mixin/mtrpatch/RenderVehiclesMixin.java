package com.lx862.jcm.mixin.mtrpatch;

import com.lx862.jcm.mapping.LoaderImplClient;
import com.lx862.jcm.mod.config.JCMClientConfig;
import org.mtr.core.data.VehicleCar;
import org.mtr.libraries.com.logisticscraft.occlusionculling.OcclusionCullingInstance;
import org.mtr.libraries.com.logisticscraft.occlusionculling.util.Vec3d;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.ObjectObjectImmutablePair;
import org.mtr.mapping.holder.*;
import org.mtr.mapping.mapper.OptimizedRenderer;
import org.mtr.mod.client.CustomResourceLoader;
import org.mtr.mod.client.VehicleRidingMovement;
import org.mtr.mod.data.VehicleExtension;
import org.mtr.mod.render.DynamicVehicleModel;
import org.mtr.mod.render.PositionAndRotation;
import org.mtr.mod.render.RenderVehicles;
import org.mtr.mod.render.StoredMatrixTransformations;
import org.mtr.mod.resource.VehicleResource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
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

    @Inject(method = "org/mtr/mod/render/RenderVehicles.lambda$render$15(Lorg/mtr/libraries/it/unimi/dsi/fastutil/objects/ObjectArrayList;Lorg/mtr/mod/data/VehicleExtension;Lorg/mtr/libraries/com/logisticscraft/occlusionculling/util/Vec3d;Lorg/mtr/mapping/holder/Vector3d;Ljava/lang/Double;Lorg/mtr/mod/render/PositionAndRotation;Lorg/mtr/mapping/holder/Vector3d;Lorg/mtr/mapping/holder/ClientPlayerEntity;IZLorg/mtr/libraries/it/unimi/dsi/fastutil/objects/ObjectArrayList;Lorg/mtr/libraries/it/unimi/dsi/fastutil/objects/ObjectArrayList;Lorg/mtr/mapping/holder/MinecraftClient;Lorg/mtr/mapping/holder/ClientWorld;JLorg/mtr/mod/render/RenderVehicles$PreviousGangwayMovementPositions;ILorg/mtr/libraries/it/unimi/dsi/fastutil/objects/ObjectObjectImmutablePair;)V", at = @At("HEAD"))
    private static void jsblock$playCulledVehicleMotorSound(ObjectArrayList cullingTasks, VehicleExtension vehicle, Vec3d camera, Vector3d offsetVector, Double offsetRotation, PositionAndRotation ridingCarPositionAndRotation, Vector3d cameraShakeOffset, ClientPlayerEntity clientPlayerEntity, int ridingCarNumber, boolean canRide, ObjectArrayList previousGangwayPositionsList, ObjectArrayList previousBarrierPositionsList, MinecraftClient minecraftClient, ClientWorld clientWorld, long millisElapsed, @Coerce /* Private Class (PreviousGangwayMovementPositions) */ Object previousGangwayMovementPositions, int carNumber, ObjectObjectImmutablePair vehicleCarDetails, CallbackInfo ci) {
        if(OptimizedRenderer.renderingShadows()) return;

        if(!vehicle.persistentVehicleData.rayTracing[carNumber] && !VehicleRidingMovement.isRiding(vehicle.getId())) {
            CustomResourceLoader.getVehicleById(vehicle.getTransportMode(), ((VehicleCar)vehicleCarDetails.left()).getVehicleId(), (vehicleResourceDetails) -> {
                VehicleResource vehicleResource = vehicleResourceDetails.left();
                PositionAndRotation absoluteVehicleCarPositionAndRotation = (PositionAndRotation) ((ObjectObjectImmutablePair) vehicleCarDetails.right()).right();
                vehicle.playMotorSound(vehicleResource, carNumber, absoluteVehicleCarPositionAndRotation.position);
            });
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
