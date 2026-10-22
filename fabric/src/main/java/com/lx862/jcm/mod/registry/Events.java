package com.lx862.jcm.mod.registry;

import com.lx862.jcm.mod.JCMClient;
import com.lx862.jcm.mod.config.JCMClientConfig;
import com.lx862.jcm.mod.data.JCMServerStats;
import com.lx862.jcm.mod.extra.JCMPatchForMTR;
import com.lx862.mtrscripting.mod.gui.hud.MTRScriptDebugOverlay;
import com.lx862.jcm.mod.resource.JCMResourceManager;
import com.lx862.mtrscripting.mod.resource.MtrScriptingResourceManager;
import com.lx862.jcm.mod.scripting.JCMScripting;
import com.lx862.mtrscripting.mod.impl.mtr.MTRContentScripting;
import com.lx862.mtrscripting.core.util.model.ModelManagerJS;
import com.lx862.mtrscripting.mod.impl.mtr.vehicle.VehicleDataCache;
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
            JCMPatchForMTR.tick();

            if(MinecraftClient.getInstance().getWorldMapped() == null) {
                VehicleDataCache.clearData();
            }
        });

        JCMRegistryClient.REGISTRY_CLIENT.eventRegistryClient.registerClientJoin(() -> {
            /* On behalf of MTR Scripting */
            MTRContentScripting.getScriptManager().scriptErrorNotifier.flush();
            JCMScripting.getScriptManager().scriptErrorNotifier.flush();
            JCMPatchForMTR.clearClientCaches();
        });

        JCMRegistryClient.REGISTRY_CLIENT.eventRegistryClient.registerEndClientTick(() -> {
            if(JCMClientConfig.INSTANCE.scripting.scriptDebugMode.value() && KeyBinds.SCRIPT_DEBUG_SOURCE_PREV.wasPressed()) {
                MTRScriptDebugOverlay.previousSource();
            }
            if(JCMClientConfig.INSTANCE.scripting.scriptDebugMode.value() && KeyBinds.SCRIPT_DEBUG_SOURCE_NEXT.wasPressed()) {
                MTRScriptDebugOverlay.nextSource();
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
                    /* Yanyang: reset the C++ runtime on this path too — the
                       same reasons onClientReloadResource() does it apply
                       here, only worse: without it this keybind re-parses
                       the JS scripts while every dlopen'd C++ module stays
                       running (an updated .so with the same script id is
                       silently ignored), and the native instance-id cache
                       keeps serving ids the bridge has long forgotten
                       (ERR_INSTANCE, permanently blank train LCDs) — the
                       user pressing this very key to "fix" a dead LCD
                       could never revive it. */
                    com.lx862.jcm.nativeapi.NativeScriptManager.reload();
                    MTRContentScripting.reset();
                    MtrScriptingResourceManager.reload();
                }
            }
        });

        JCMRegistryClient.REGISTRY_CLIENT.eventRegistryClient.registerGuiRendering(graphicsHolder -> {
            MTRScriptDebugOverlay.render(graphicsHolder);
        });
    }

    public static void onClientReloadResource() {
        /* Yanyang: reset the C++ script runtime BEFORE the JS side re-parses.
           Without this, F3+T kept every dlopen'd module running (an updated
           .so with the same script id was silently ignored — the declaration
           guard in loadManyFromDeclaration is idempotent), SKIPPED/INPUTS
           entries for removed packs leaked forever, and NativeHost kept
           model handles that ModelManagerJS.reset() below was about to
           close — a use-after-free on the render thread for every native
           draw call after the reload. reload() disposes the modules,
           clears the per-instance states, draw registries and driver
           memos, and drops the NativeHost model/texture references first. */
        com.lx862.jcm.nativeapi.NativeScriptManager.reload();
        JCMScripting.reset();
        MTRContentScripting.reset();
        ModelManagerJS.reset();
        JCMResourceManager.reload();
        MtrScriptingResourceManager.reload();
    }
}
