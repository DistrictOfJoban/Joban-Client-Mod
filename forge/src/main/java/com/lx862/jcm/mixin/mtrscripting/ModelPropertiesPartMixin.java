package com.lx862.jcm.mixin.mtrscripting;

import com.lx862.mtrscripting.mod.resource.MtrScriptingResourceManager;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.ObjectDoubleImmutablePair;
import org.mtr.mapping.holder.Box;
import org.mtr.mapping.holder.Identifier;
import org.mtr.mod.data.VehicleExtension;
import org.mtr.mod.generated.resource.ModelPropertiesPartSchema;
import org.mtr.mod.render.StoredMatrixTransformations;
import org.mtr.mod.resource.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ModelPropertiesPart.class, remap = false)
public class ModelPropertiesPartMixin extends ModelPropertiesPartSchema {
    protected ModelPropertiesPartMixin(PartCondition condition, RenderStage renderStage, PartType type, double displayXPadding, double displayYPadding, String displayColorCjk, String displayColor, double displayMaxLineHeight, double displayCjkSizeRatio, double displayPadZeros, DisplayType displayType, String displayDefaultText, double doorXMultiplier, double doorZMultiplier, DoorAnimationType doorAnimationType, long renderFromOpeningDoorTime, long renderUntilOpeningDoorTime, long renderFromClosingDoorTime, long renderUntilClosingDoorTime, long flashOffTime, long flashOnTime) {
        super(condition, renderStage, type, displayXPadding, displayYPadding, displayColorCjk, displayColor, displayMaxLineHeight, displayCjkSizeRatio, displayPadZeros, displayType, displayDefaultText, doorXMultiplier, doorZMultiplier, doorAnimationType, renderFromOpeningDoorTime, renderUntilOpeningDoorTime, renderFromClosingDoorTime, renderUntilClosingDoorTime, flashOffTime, flashOnTime);
    }

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void jsblock$hideDisplayParts(Identifier texture, StoredMatrixTransformations storedMatrixTransformations, VehicleExtension vehicle, int carNumber, int[] scrollingDisplayIndexTracker, int light, ObjectArrayList<ObjectDoubleImmutablePair<Box>> openDoorways, boolean fromResourcePackCreator, CallbackInfo ci) {
        /* Only DISPLAY parts ever consult the script registry, and the car's
           vehicle id is available O(1) from the immutable car list. The old
           body ran vehicle.getVehicleCarsAndPositions() here — a FULL
           position-tree rebuild per model part per car per frame (P x N
           builds where 1 already happens elsewhere each frame) — and NPE'd
           when MTR passes a null vehicle (stored-model render path with
           optimized rendering off). */
        if(type != PartType.DISPLAY || vehicle == null) return;
        final int carCount = vehicle.vehicleExtraData.immutableVehicleCars.size();
        if(carNumber < 0 || carNumber >= carCount) return;
        final String vehicleId = vehicle.vehicleExtraData.immutableVehicleCars.get(carNumber).getVehicleId();
        if(MtrScriptingResourceManager.vehicle.shouldHideDisplayParts(vehicleId)) {
            ci.cancel();
        }
    }
}
