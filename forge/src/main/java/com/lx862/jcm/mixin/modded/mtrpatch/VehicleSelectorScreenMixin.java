package com.lx862.jcm.mixin.modded.mtrpatch;

import com.lx862.jcm.mod.util.TextUtil;
import org.mtr.core.data.Siding;
import org.mtr.libraries.it.unimi.dsi.fastutil.longs.LongCollection;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.ObjectImmutableList;
import org.mtr.mapping.holder.MutableText;
import org.mtr.mapping.holder.Screen;
import org.mtr.mapping.holder.TextFormatting;
import org.mtr.mapping.mapper.GraphicsHolder;
import org.mtr.mapping.mapper.ScreenExtension;
import org.mtr.mod.client.CustomResourceLoader;
import org.mtr.mod.resource.VehicleResource;
import org.mtr.mod.screen.DashboardListItem;
import org.mtr.mod.screen.DashboardListSelectorScreen;
import org.mtr.mod.screen.VehicleSelectorScreen;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Little messy code to provide car auto-filling feature in vehicle selections.
 */
@Mixin(value = VehicleSelectorScreen.class, remap = false)
public abstract class VehicleSelectorScreenMixin extends DashboardListSelectorScreen {
    @Shadow
    @Final
    private Siding siding;

    @Shadow
    protected abstract int drawWrappedText(GraphicsHolder graphicsHolder, MutableText component, int y, int color);

    @Unique
    private static final Map<Long, VehicleResource> listIdToVehicleResource = new HashMap<>();

    @Unique
    private static final Map<String, VehicleResource> vehicleIdToVehicleResource = new HashMap<>();

    @Unique
    private static final Map<String, Long> vehicleResourceToListId = new HashMap<>();

    @Unique
    private static int oldListSize = 0;

    public VehicleSelectorScreenMixin(ObjectImmutableList<DashboardListItem> allData, LongCollection selectedIds, boolean isSingleSelect, boolean canRepeat, ScreenExtension previousScreenExtension) {
        super(allData, selectedIds, isSingleSelect, canRepeat, previousScreenExtension);
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void jsblock$populateIds(Siding siding, ScreenExtension previousScreenExtension, CallbackInfo ci) {
        oldListSize = 0;
        listIdToVehicleResource.clear();
        final long[] id = {0};
        CustomResourceLoader.iterateVehicles(siding.getTransportMode(), vehicleResource -> {
            listIdToVehicleResource.put(id[0], vehicleResource);
            vehicleIdToVehicleResource.put(vehicleResource.getId(), vehicleResource);
            vehicleResourceToListId.put(vehicleResource.getId(), id[0]);
            id[0]++;
        });
    }

    @Inject(method = "lambda$renderAdditional$1", at = @At("TAIL"), locals = LocalCapture.CAPTURE_FAILSOFT)
    private void jsblock$showAutoFillFormation(GraphicsHolder graphicsHolder, VehicleResource vehicleResource, CallbackInfo ci, int y) {
        String baseId = vehicleResource.getId()
                .replace("_trailer", "")
                .replace("_cab_1", "")
                .replace("_cab_2", "")
                .replace("_cab_3", "");

        VehicleResource headVehicle = vehicleIdToVehicleResource.get(baseId + "_cab_1");
        VehicleResource trailerVehicle = vehicleIdToVehicleResource.get(baseId + "_trailer");
        VehicleResource tailVehicle = vehicleIdToVehicleResource.get(baseId + "_cab_2");

        if(headVehicle != null && trailerVehicle != null && tailVehicle != null) {
            double remainingLength = siding.getRailLength() - headVehicle.getLength() - tailVehicle.getLength();

            if(remainingLength >= trailerVehicle.getLength()) {
                int trailerCars = (int)Math.floor(remainingLength/ trailerVehicle.getLength());

                MutableText reminderText = TextUtil.literal("\nJCM Autofill formation:\n").formatted(TextFormatting.AQUA);
                reminderText.append("1-" + trailerCars + "-1   (" + (trailerCars+2) + "-cars)\n")
                        .append("\nTo use, hold SHIFT then add car.");

                drawWrappedText(graphicsHolder, reminderText, y, ARGB_WHITE);
            }
        }
    }

    @Inject(method = "updateList", at = @At("HEAD"))
    private void jsblock$fillCar(CallbackInfo ci) {
        int listSize = selectedIds.size();
        if(Screen.hasShiftDown()) {
            if(listSize > oldListSize) {
                List<Long> longList = new ArrayList<>(selectedIds);
                long justAddedId = longList.get(longList.size()-1);
                VehicleResource resource = listIdToVehicleResource.get(justAddedId);

                if(resource != null) {
                    String baseId = resource.getId()
                            .replace("_trailer", "")
                            .replace("_cab_1", "")
                            .replace("_cab_2", "")
                            .replace("_cab_3", "");

                    VehicleResource headVehicle = vehicleIdToVehicleResource.get(baseId + "_cab_1");
                    VehicleResource trailerVehicle = vehicleIdToVehicleResource.get(baseId + "_trailer");
                    VehicleResource tailVehicle = vehicleIdToVehicleResource.get(baseId + "_cab_2");
                    if(headVehicle != null && trailerVehicle != null && tailVehicle != null) {
                        double headAndTailLength = headVehicle.getLength() + tailVehicle.getLength();
                        double remainingTrailerLength = siding.getRailLength() - headAndTailLength;
                        // Not enough to autofill
                        if(remainingTrailerLength <= 0) return;

                        selectedIds.clear();
                        selectedIds.add(vehicleResourceToListId.get(headVehicle.getId()));

                        while(true) {
                            if(remainingTrailerLength >= trailerVehicle.getLength()) {
                                remainingTrailerLength -= trailerVehicle.getLength();
                                selectedIds.add(vehicleResourceToListId.get(trailerVehicle.getId()));
                            } else {
                                break;
                            }
                        }

                        selectedIds.add(vehicleResourceToListId.get(tailVehicle.getId()));
                        listSize = selectedIds.size();
                    }
                }
            } else if(listSize < oldListSize) { // Item removal
                selectedIds.clear();
                listSize = 0;
            }
        }
        oldListSize = listSize;
    }
}
