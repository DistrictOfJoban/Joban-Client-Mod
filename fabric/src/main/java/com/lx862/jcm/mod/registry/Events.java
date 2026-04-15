package com.lx862.jcm.mod.registry;

import com.lx862.jcm.mod.JCMClient;
import com.lx862.jcm.mod.config.JCMClientConfig;
import com.lx862.jcm.mod.data.JCMServerStats;
import com.lx862.jcm.mod.render.gui.ScriptDebugOverlay;
import com.lx862.jcm.mod.resources.JCMResourceManager;
import com.lx862.jcm.mod.resources.MTRContentResourceManager;
import com.lx862.jcm.mod.scripting.jcm.JCMScripting;
import com.lx862.jcm.mod.scripting.mtr.MTRContentScripting;
import com.lx862.mtrscripting.util.model.ModelManagerJS;
import com.lx862.jcm.mod.scripting.mtr.vehicle.VehicleDataCache;
import com.lx862.jcm.mod.util.TextUtil;
import org.mtr.mapping.holder.*;
import org.mtr.mapping.mapper.TextHelper;

public class Events {
    public static void register() {
        // Start Tick Event for counting tick
        JCMRegistry.REGISTRY.eventRegistry.registerStartServerTick(JCMServerStats::incrementGameTick);
    }

    public static void registerClient() {
        JCMRegistryClient.REGISTRY_CLIENT.eventRegistryClient.registerStartClientTick(() -> {
            JCMClient.getMcMetaManager().tick();
            JCMScripting.tick();
            MTRContentScripting.tick();
            VehicleDataCache.tick();

            if(MinecraftClient.getInstance().getWorldMapped() == null) {
                VehicleDataCache.clearData();
            }
        });

        JCMRegistryClient.REGISTRY_CLIENT.eventRegistryClient.registerEndClientTick(() -> {
            if(JCMClientConfig.INSTANCE.scripting.scriptDebugMode.value() && KeyBinds.SCRIPT_DEBUG_SOURCE_PREV.wasPressed()) {
                ScriptDebugOverlay.previousSource();
            }
            if(JCMClientConfig.INSTANCE.scripting.scriptDebugMode.value() && KeyBinds.SCRIPT_DEBUG_SOURCE_NEXT.wasPressed()) {
                ScriptDebugOverlay.nextSource();
            }
            if(JCMClientConfig.INSTANCE.scripting.scriptDebugMode.value() && KeyBinds.SCRIPT_DEBUG_RELOAD.wasPressed()) {
                boolean isShiftPressed = MinecraftClient.getInstance().getOptionsMapped().getKeySneakMapped().isPressed();
                boolean isCtrlPressed = MinecraftClient.getInstance().getOptionsMapped().getKeySprintMapped().isPressed();
                if(!isShiftPressed && !isCtrlPressed) return;
                boolean shouldReloadJCM = isShiftPressed;

                if(MinecraftClient.getInstance().getPlayerMapped() != null) {
                    MutableText prefix = TextUtil.literal("[JCM] ").formatted(TextFormatting.AQUA);

                    if(shouldReloadJCM) {
                        MinecraftClient.getInstance().getPlayerMapped().sendMessage(Text.cast(TextHelper.append(prefix, TextUtil.literal("Reloading all JCM PIDS Scripts...").formatted(TextFormatting.YELLOW))), false);
                    } else {
                        MinecraftClient.getInstance().getPlayerMapped().sendMessage(Text.cast(TextHelper.append(prefix, TextUtil.literal("Reloading all MTR Scripts...").formatted(TextFormatting.YELLOW))), false);
                    }
                }

                if(shouldReloadJCM) {
                    JCMScripting.reset();
                    JCMResourceManager.reload();
                } else {
                    MTRContentScripting.reset();
                    MTRContentResourceManager.reload();
                }
            }
        });

        JCMRegistryClient.REGISTRY_CLIENT.eventRegistryClient.registerGuiRendering(graphicsHolder -> {
            ScriptDebugOverlay.render(graphicsHolder);
        });
    }

    public static void onClientReloadResource() {
        JCMScripting.reset();
        MTRContentScripting.reset();
        ModelManagerJS.reset();
        JCMResourceManager.reload();
        MTRContentResourceManager.reload();
    }
}
