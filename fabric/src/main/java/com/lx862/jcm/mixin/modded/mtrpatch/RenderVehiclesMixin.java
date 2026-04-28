package com.lx862.jcm.mixin.modded.mtrpatch;

import com.lx862.jcm.mod.config.JCMClientConfig;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.ObjectObjectImmutablePair;
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

@Mixin(value = RenderVehicles.class, remap = false)
public class RenderVehiclesMixin {
    @Inject(method = "lambda$render$5", at = @At("HEAD"), cancellable = true)
    private static void jsblock$hideRidingTrainBogie(Vector3d offsetVector, Double offsetRotation, PositionAndRotation ridingCarPositionAndRotation, Vector3d cameraShakeOffset, VehicleResource vehicleResource, VehicleExtension vehicle, PositionAndRotation absoluteVehicleCarPositionAndRotation, int carNumber, int[] scrollingDisplayIndexTracker, boolean fromResourcePackCreator, int bogieIndex, PositionAndRotation absoluteBogiePositionAndRotation, CallbackInfo ci) {
        if(JCMClientConfig.INSTANCE.mtrPatch.hideRidingVehicle.value() && VehicleRidingMovement.isRiding(vehicle.getId())) ci.cancel();
    }

    @Inject(method = "lambda$render$12", at = @At("HEAD"), cancellable = true)
    private static void jsblock$hideRidingTrainModel(StoredMatrixTransformations storedMatrixTransformations, VehicleExtension vehicle, int carNumber, int[] scrollingDisplayIndexTracker, PositionAndRotation absoluteVehicleCarPositionAndRotation, ObjectArrayList openDoorways, boolean fromResourcePackCreator, ObjectArrayList previousGangwayPositionsList, ObjectArrayList previousBarrierPositionsList, VehicleResource vehicleResource, PositionAndRotation vehicleCarRenderingPositionAndRotation, Vector3d offsetVector, ObjectObjectImmutablePair vehicleCarDetails, double oscillationAmount, int modelIndex, DynamicVehicleModel model, CallbackInfo ci) {
        if(JCMClientConfig.INSTANCE.mtrPatch.hideRidingVehicle.value() && VehicleRidingMovement.isRiding(vehicle.getId())) ci.cancel();
    }
}
