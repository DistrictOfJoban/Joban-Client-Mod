package com.lx862.mtrscripting.mod.resource;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.lx862.jcm.mod.config.JCMClientConfig;
import com.lx862.mtrscripting.mod.impl.mtr.MTRContentScripting;
import com.lx862.mtrscripting.mod.MTRScriptingModClient;
import com.lx862.mtrscripting.core.util.ConsoleJS;
import org.mtr.mapping.holder.*;
import org.mtr.mapping.mapper.ResourceManagerHelper;
import org.mtr.mod.Init;
import org.mtr.mod.client.CustomResourceLoader;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class MtrScriptingResourceManager {
    public static final VehicleResourceProvider vehicle = new VehicleResourceProvider();
    public static final EyecandyResourceProvider eyecandy = new EyecandyResourceProvider();
    public static final LiftResourceProvider lift = new LiftResourceProvider();

    public static void reload() {
        MTRScriptingModClient.LOGGER.info("[MTR Scripting via JCM] Loading MTR scripts...");

        vehicle.reset();
        eyecandy.reset();
        lift.reset();

        MTRContentScripting.getScriptManager().scriptErrorNotifier.reset();

        if(JCMClientConfig.INSTANCE.scripting.skipScriptParsing.value()) {
            MTRScriptingModClient.LOGGER.info("[MTR Scripting via JCM] Scripting has been disabled, not parsing any script.");
        } else {
            CustomResourceLoader.OPTIMIZED_RENDERER_WRAPPER.beginReload();
            ConsoleJS consoleJS = new ConsoleJS();
            if(JCMClientConfig.INSTANCE.scripting.scriptDebugMode.value()) {
                consoleJS.time("MTR Script Load Time");
            }
            vehicle.parseCustom();
            eyecandy.parseCustom();
            lift.parseCustom();
            parseCustomResources(false);
            parseCustomResources(true);
            if(JCMClientConfig.INSTANCE.scripting.scriptDebugMode.value()) {
                consoleJS.timeEnd("MTR Script Load Time");
            }
            CustomResourceLoader.OPTIMIZED_RENDERER_WRAPPER.finishReload();
        }

        if(MinecraftClient.getInstance().getPlayerMapped() != null) {
            MTRContentScripting.getScriptManager().scriptErrorNotifier.flush();
        }
    }

    private static void parseCustomResources(boolean pendingMigration) {
        String fileName = pendingMigration ? CustomResourceLoader.CUSTOM_RESOURCES_PENDING_MIGRATION_ID + ".json" : CustomResourceLoader.CUSTOM_RESOURCES_ID + ".json";

        ResourceManagerHelper.readAllResources(new Identifier(Init.MOD_ID, fileName), (inputStream) -> {
            try(InputStreamReader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
                final JsonObject rootObject = new JsonParser().parse(reader).getAsJsonObject();
                final boolean isLegacyResource = rootObject.has("custom_trains") || rootObject.has("custom_signs");

                vehicle.parseMtrResources(rootObject, isLegacyResource);
                eyecandy.parseMtrResources(rootObject, isLegacyResource);
                lift.parseMtrResources(rootObject, isLegacyResource);
            } catch (Exception e) {
                ScriptResourceProvider.logError("parsing scripts in mtr_custom_resources.json", e);
            }
        });

        validateScriptEntries();
    }

    private static void validateScriptEntries() {
        vehicle.validate();
        eyecandy.validate();
        lift.validate();
    }
}
