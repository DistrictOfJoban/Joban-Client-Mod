package com.lx862.jcm.mixin.modded.mtr;

import com.lx862.jcm.mod.resources.MTRContentResourceManager;
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
        String vehicleId = vehicle.getVehicleCarsAndPositions().get(carNumber).left().getVehicleId();
        if(type == PartType.DISPLAY && MTRContentResourceManager.shouldHideDisplayParts(vehicleId)) {
            ci.cancel();
        }
    }
}
