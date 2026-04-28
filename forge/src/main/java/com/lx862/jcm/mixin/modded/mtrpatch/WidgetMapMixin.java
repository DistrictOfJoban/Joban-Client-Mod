package com.lx862.jcm.mixin.modded.mtrpatch;

import com.lx862.jcm.mod.config.JCMClientConfig;
import com.lx862.jcm.mod.util.TextUtil;
import org.mtr.core.data.Platform;
import org.mtr.core.data.Route;
import org.mtr.core.data.SavedRailBase;
import org.mtr.core.data.Siding;
import org.mtr.mapping.holder.*;
import org.mtr.mapping.mapper.GuiDrawing;
import org.mtr.mapping.mapper.TextHelper;
import org.mtr.mod.client.MinecraftClientData;
import org.mtr.mod.screen.WidgetMap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

import static com.lx862.jcm.mod.render.RenderHelper.ARGB_BLACK;

@Mixin(WidgetMap.class)
public class WidgetMapMixin {
    @Inject(method = "lambda$render$2", at = @At("HEAD"), remap = false)
    private void renderPlatformTooltip(GuiDrawing guiDrawing, SavedRailBase savedRail, double x1, double z1, double x2, double z2, CallbackInfo ci) {
        if(!JCMClientConfig.INSTANCE.mtrPatch.showDashboardTooltip.value()) return;

        Platform platform = (Platform)savedRail;
        final MutableText platformText = TextUtil.literal("Platform " + platform.getName()).formatted(TextFormatting.RED);
        final MutableText keyDwell = TextUtil.literal("Dwell: ").formatted(TextFormatting.GOLD);
        final MutableText valueDwell = TextUtil.literal((platform.getDwellTime() / 1000d) + "s").formatted(TextFormatting.WHITE);
        final MutableText keyRouteVia = TextUtil.literal("Route Via: ").formatted(TextFormatting.GOLD);

        final List<MutableText> tooltipComponents = new ArrayList<>();
        tooltipComponents.add(platformText);
        tooltipComponents.add(TextHelper.append(keyDwell, valueDwell));
        tooltipComponents.add(keyRouteVia);

        for (Route route : MinecraftClientData.getDashboardInstance().routes) {
            if (route.getRoutePlatforms().stream().anyMatch(e -> e.getPlatform().getId() == platform.getId())) {
                MutableText routeText = TextUtil.literal(route.getName().replace("|", " "));
                TextHelper.setStyle(routeText, Style.getEmptyMapped().withColor(TextColor.fromRgb(ARGB_BLACK | route.getColor())));
                if(route.getHidden()) {
                    TextHelper.append(routeText, TextUtil.literal(" (Hidden)").formatted(TextFormatting.GRAY));
                }
                tooltipComponents.add(routeText);
            }
        }

        if(tooltipComponents.isEmpty()) {
            tooltipComponents.add(TextUtil.literal("(None)"));
        }

        #if MC_VERSION >= "11903"
        MinecraftClient.getInstance().getCurrentScreenMapped().data.setTooltipForNextRenderPass(tooltipComponents.stream().map(e -> e.data.getVisualOrderText()).toList());
        #endif
    }

    @Inject(method = "lambda$render$4", at = @At("HEAD"), remap = false)
    private void renderSidingTooltip(GuiDrawing guiDrawing, SavedRailBase savedRail, double x1, double z1, double x2, double z2, CallbackInfo ci) {
        if(!JCMClientConfig.INSTANCE.mtrPatch.showDashboardTooltip.value()) return;

        final Siding siding = (Siding)savedRail;
        final MutableText platformText = TextUtil.literal("Siding " + siding.getName()).formatted(TextFormatting.YELLOW);
        final MutableText keyRailLength = TextUtil.literal("Rail Length: ").formatted(TextFormatting.GOLD);
        final MutableText valueRailLength = TextUtil.literal((int)Math.round(siding.getRailLength()) + "m").formatted(TextFormatting.WHITE);
        final MutableText keyVehicleLength = TextUtil.literal("Vehicle Length: ").formatted(TextFormatting.GOLD);

        int roundedVehicleLength = (int)Math.round(Siding.getTotalVehicleLength(siding.getVehicleCars()));
        String carPlural = siding.getVehicleCars().size() == 1 ? "car" : "cars";
        final MutableText valueVehicleLength = TextUtil.literal(String.format("%dm (%d-%s)", roundedVehicleLength, siding.getVehicleCars().size(), carPlural)).formatted(TextFormatting.WHITE);

        final MutableText keyMaxVehicles = TextUtil.literal("Max Vehicle(s): ").formatted(TextFormatting.GOLD);
        final MutableText valueMaxVehicles = TextUtil.literal(siding.getIsUnlimited() ? "Unlimited" : String.valueOf(siding.getMaxVehicles())).formatted(siding.getIsUnlimited() ? TextFormatting.GREEN : TextFormatting.WHITE);
        final MutableText keyManual = TextUtil.literal("Manual: ").formatted(TextFormatting.GOLD);
        final MutableText valueManual = TextUtil.literal("Yes").formatted(TextFormatting.GREEN);

        final List<MutableText> tooltipComponents = new ArrayList<>();
        tooltipComponents.add(platformText);
        tooltipComponents.add(TextHelper.append(keyVehicleLength, valueVehicleLength));
        tooltipComponents.add(TextHelper.append(keyRailLength, valueRailLength));
        tooltipComponents.add(TextHelper.append(keyMaxVehicles, valueMaxVehicles));

        if(siding.getIsManual()) {
            tooltipComponents.add(TextHelper.append(keyManual, valueManual));
        }

        #if MC_VERSION >= "11903"
        MinecraftClient.getInstance().getCurrentScreenMapped().data.setTooltipForNextRenderPass(tooltipComponents.stream().map(e -> e.data.getVisualOrderText()).toList());
        #endif
    }
}
