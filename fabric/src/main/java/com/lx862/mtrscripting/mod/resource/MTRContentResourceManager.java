package com.lx862.mtrscripting.mod.resource;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.lx862.jcm.mod.config.JCMClientConfig;
import com.lx862.mtrscripting.core.util.DataReaderJS;
import com.lx862.mtrscripting.core.util.ScriptResourceUtil;
import com.lx862.mtrscripting.mod.MTRScriptingMod;
import com.lx862.mtrscripting.mod.impl.mtr.MTRContentScripting;
import com.lx862.mtrscripting.mod.impl.mtr.eyecandy.config.EyecandyCustomConfig;
import com.lx862.mtrscripting.mod.impl.mtr.vehicle.VehicleDataCache;
import com.lx862.mtrscripting.core.primitive.ParsedScript;
import com.lx862.mtrscripting.core.primitive.ScriptContent;
import com.lx862.mtrscripting.mod.MTRScriptingModClient;
import com.lx862.mtrscripting.core.util.ConsoleJS;
import com.lx862.mtrscripting.mod.impl.mtr.vehicle.VehicleScriptContext;
import com.lx862.mtrscripting.mod.util.JsonUtil;
import org.apache.commons.io.FilenameUtils;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.mtr.mapping.holder.*;
import org.mtr.mapping.mapper.ResourceManagerHelper;
import org.mtr.mapping.mapper.TextHelper;
import org.mtr.mod.Init;
import org.mtr.mod.client.CustomResourceLoader;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MTRContentResourceManager {
    private static final JsonParser JSON_PARSER = new JsonParser();

    private static final Map<String, VehicleScriptConfiguration> vehicleScripts = new HashMap<>();
    private static final Map<String, EyecandyScriptConfiguration> eyecandyScripts = new HashMap<>();
    private static final Map<String, String> vehicleScriptIds = new HashMap<>();
    private static final Map<String, String> eyecandyScriptIds = new HashMap<>();

    private static final List<String> vehiclesWithDisplayCubeHidden = new ObjectArrayList<>();

    public static void reload() {
        MTRScriptingModClient.LOGGER.info("[MTR Scripting via JCM] Loading MTR Vehicle/Eyecandy scripts...");
        eyecandyScripts.clear();
        vehicleScripts.clear();
        vehicleScriptIds.clear();
        eyecandyScriptIds.clear();
        vehiclesWithDisplayCubeHidden.clear();
        VehicleDataCache.clearData();
        MTRContentScripting.getScriptManager().scriptErrorNotifier.reset();

        if(JCMClientConfig.INSTANCE.scripting.skipScriptParsing.value()) {
            MTRScriptingModClient.LOGGER.info("[MTR Scripting via JCM] Scripting has been disabled, not parsing any script.");
        } else {
            CustomResourceLoader.OPTIMIZED_RENDERER_WRAPPER.beginReload();
            ConsoleJS consoleJS = new ConsoleJS();
            if(JCMClientConfig.INSTANCE.scripting.scriptDebugMode.value()) {
                consoleJS.time("MTR Script Load Time");
            }
            readNteEyecandy();
            readMtrCustomResources(false);
            readMtrCustomResources(true);
            if(JCMClientConfig.INSTANCE.scripting.scriptDebugMode.value()) {
                consoleJS.timeEnd("MTR Script Load Time");
            }
            CustomResourceLoader.OPTIMIZED_RENDERER_WRAPPER.finishReload();
        }

        if(MinecraftClient.getInstance().getPlayerMapped() != null) {
            MTRContentScripting.getScriptManager().scriptErrorNotifier.flush();
        }
    }

    /**
     * Read legacy script entry (for MTR-NTE based eyecandy entries)
     */
    private static void readNteEyecandy() {
        ResourceManagerHelper.readDirectory("eyecandies", (identifier, inputStream) -> {
            boolean fileIsNteEyecandy = identifier.getNamespace().equals(Init.MOD_ID_NTE) && identifier.getPath().endsWith(".json");
            if (fileIsNteEyecandy) {
                try(InputStreamReader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
                    final JsonObject rootObject = JSON_PARSER.parse(reader).getAsJsonObject();

                    Map<String, JsonElement> objectsInJson = new HashMap<>();
                    if(rootObject.has("model")) {
                        // Single entry registration
                        String id = FilenameUtils.getBaseName(identifier.getPath());
                        objectsInJson.put(id, rootObject);
                    } else {
                        // Multi-entry registration
                        rootObject.entrySet().forEach((entry) -> {
                            objectsInJson.put(entry.getKey(), entry.getValue());
                        });
                    }

                    // Do registration
                    for(Map.Entry<String, JsonElement> entry : objectsInJson.entrySet()) {
                        final String id = entry.getKey();
                        final JsonObject entryObject = entry.getValue().getAsJsonObject();
                        final EyecandyCustomConfig eyecandyCustomConfig = parseEyecandyCustomConfig(entryObject, true);

                        ParsedScript parsedScript = tryParseScript(id, "eyecandy", "Block", entryObject, false, false);
                        if(parsedScript != null) {
                            eyecandyScriptIds.put(id, id);
                            eyecandyScripts.put(id, new EyecandyScriptConfiguration(parsedScript, eyecandyCustomConfig));
                        }
                    }
                } catch (Exception e) {
                    logError("parsing NTE Eyecandy scripts", e);
                }
            }
        });
    }

    private static void readMtrCustomResources(boolean pendingMigration) {
        String fileName = pendingMigration ? CustomResourceLoader.CUSTOM_RESOURCES_PENDING_MIGRATION_ID + ".json" : CustomResourceLoader.CUSTOM_RESOURCES_ID + ".json";

        ResourceManagerHelper.readAllResources(new Identifier(Init.MOD_ID, fileName), (inputStream) -> {
            try(InputStreamReader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
                final JsonObject rootObject = JSON_PARSER.parse(reader).getAsJsonObject();
                final boolean isLegacyResource = rootObject.has("custom_trains"); // Whether train format are in MTR 3
                final JsonElement vehicleElement = isLegacyResource ? rootObject.get("custom_trains") : rootObject.get("vehicles");
                final JsonElement mtr4EyecandyElement = rootObject.get("objects"); // MTR 4

                if(vehicleElement != null) {
                    if(isLegacyResource) { // MTR 3
                        final JsonObject vehicleObject = vehicleElement.getAsJsonObject();
                        for (Map.Entry<String, JsonElement> map : vehicleObject.entrySet()) {
                            String baseId = "mtr_custom_train_" + map.getKey();
                            try {
                                JsonObject vehicleResource = map.getValue().getAsJsonObject();
                                if(vehicleResource.has("script_id")) { // Referencing a (presumably already-parsed) script entry defined in MTR 4 format
                                    String scriptIdOverride = vehicleResource.get("script_id").getAsString();
                                    vehicleScriptIds.put(baseId + "_cab_1", scriptIdOverride);
                                    vehicleScriptIds.put(baseId + "_cab_2", scriptIdOverride);
                                    vehicleScriptIds.put(baseId + "_cab_3", scriptIdOverride);
                                    vehicleScriptIds.put(baseId + "_trailer", scriptIdOverride);
                                } else {
                                    ParsedScript parsedScript = tryParseScript(baseId, "vehicle", "Vehicle", vehicleResource, false, true);
                                    if (parsedScript != null) {
                                        vehicleScripts.put(baseId, new VehicleScriptConfiguration(parsedScript, VehicleScriptContext.DataFetchMode.MANDATORY));
                                        vehicleScriptIds.put(baseId + "_cab_1", baseId);
                                        vehicleScriptIds.put(baseId + "_cab_2", baseId);
                                        vehicleScriptIds.put(baseId + "_cab_3", baseId);
                                        vehicleScriptIds.put(baseId + "_trailer", baseId);
                                    }
                                }

                                // Allow hiding display cube, so scripts can render their own, even with MTR's built-in model
                                boolean shouldHideDisplayCube = vehicleResource.has("hide_display_cube") && vehicleResource.get("hide_display_cube").getAsBoolean();
                                if(shouldHideDisplayCube) {
                                    vehiclesWithDisplayCubeHidden.add(baseId + "_cab_1");
                                    vehiclesWithDisplayCubeHidden.add(baseId + "_cab_2");
                                    vehiclesWithDisplayCubeHidden.add(baseId + "_cab_3");
                                    vehiclesWithDisplayCubeHidden.add(baseId + "_trailer");
                                }
                            } catch (Exception e) {
                                logError("parsing legacy vehicle script '" + baseId + "' in mtr_custom_resources.json", e);
                            }
                        }
                    } else { // MTR 4
                        final JsonElement scriptsElement = rootObject.get("vehicleScripts");
                        final JsonArray vehicleArray = vehicleElement.getAsJsonArray();

                        for(JsonElement vehicleEntry : vehicleArray) {
                            JsonObject vehicleObject = vehicleEntry.getAsJsonObject();
                            String baseId = vehicleObject.get("id").getAsString();
                            if(vehicleObject.has("scriptId")) { // For MTR 4, we put all scripting related fields into a sub-entry
                                vehicleScriptIds.put(baseId, vehicleObject.get("scriptId").getAsString());
                            }

                            boolean shouldHideDisplayCube = vehicleObject.has("hideDisplayParts") && vehicleObject.get("hideDisplayParts").getAsBoolean();
                            if(shouldHideDisplayCube) {
                                vehiclesWithDisplayCubeHidden.add(baseId);
                            }
                        }

                        if(scriptsElement != null) {
                            JsonArray scriptArray = scriptsElement.getAsJsonArray();

                            for(JsonElement entryElement : scriptArray) {
                                JsonObject scriptObject = entryElement.getAsJsonObject();
                                String scriptEntryId = scriptObject.get("id").getAsString();
                                boolean forceLoad = scriptObject.has("forceLoad") && scriptObject.get("forceLoad").getAsBoolean();
                                boolean entryReferenced = vehicleScriptIds.values().stream().anyMatch(e -> e.equals(scriptEntryId));

                                if(entryReferenced || forceLoad) {
                                    ParsedScript parsedScript = tryParseScript(scriptEntryId, "vehicle", "Vehicle", scriptObject, true, false);
                                    VehicleScriptContext.DataFetchMode dataFetchMode = scriptObject.has("dataFetchMode") ? VehicleScriptContext.DataFetchMode.valueOf(scriptObject.get("dataFetchMode").getAsString()) : VehicleScriptContext.DataFetchMode.SKIP;
                                    if (parsedScript != null) {
                                        vehicleScripts.put(scriptEntryId, new VehicleScriptConfiguration(parsedScript, dataFetchMode));
                                    }
                                } else {
                                    MTRScriptingModClient.LOGGER.warn("[MTR Scripting via JCM] Skip parsing vehicle scripts \"{}\", which is not referenced by any vehicle!", scriptEntryId);
                                }
                            }
                        }
                    }
                }

                // Parse MTR 4 Eyecandy
                if(mtr4EyecandyElement != null) {
                    final JsonElement scriptsElement = rootObject.get("objectScripts");
                    JsonArray eyecandyObjects = mtr4EyecandyElement.getAsJsonArray();
                    for(JsonElement jsonElement : eyecandyObjects) {
                        final JsonObject eyecandyEntry = jsonElement.getAsJsonObject();
                        final String id = eyecandyEntry.get("id").getAsString();
                        if(eyecandyEntry.has("scriptId")) { // For MTR 4, we put all scripting related fields into a sub-entry
                            String eyecandyScriptId = eyecandyEntry.get("scriptId").getAsString();
                            eyecandyScriptIds.put(id, eyecandyScriptId);
                        }
                    }

                    if(scriptsElement != null) {
                        JsonArray scriptArray = scriptsElement.getAsJsonArray();
                        for(JsonElement entryElement : scriptArray) {
                            JsonObject scriptObject = entryElement.getAsJsonObject();
                            String scriptEntryId = scriptObject.get("id").getAsString();
                            EyecandyCustomConfig customConfig = parseEyecandyCustomConfig(scriptObject, false);
                            boolean entryReferenced = eyecandyScriptIds.values().stream().anyMatch(e -> e.equals(scriptEntryId));

                            if(entryReferenced) {
                                ParsedScript parsedScript = tryParseScript(scriptEntryId, "eyecandy", "Block", scriptObject, true, false);
                                if(parsedScript != null) eyecandyScripts.put(scriptEntryId, new EyecandyScriptConfiguration(parsedScript, customConfig));
                            } else {
                                MTRScriptingModClient.LOGGER.warn("[MTR Scripting via JCM] Skip parsing eyecandy script \"{}\", which is not referenced by any eyecandy object!", scriptEntryId);
                            }
                        }
                    }

                }
            } catch (Exception e) {
                logError("parsing scripts in mtr_custom_resources.json", e);
            }
        });

        validateScriptEntries();
    }

    private static EyecandyCustomConfig parseEyecandyCustomConfig(JsonObject rootObject, boolean legacy) {
        String customConfigKey = legacy ? "scriptCustomConfig" : "customConfig";
        String customConfigFileKey = legacy ? "scriptCustomConfigFile" : "customConfigFile";

        if(rootObject.has(customConfigKey)) {
            JsonObject customConfigObject = rootObject.get(customConfigKey).getAsJsonObject();
            return parseEyecandyCustomConfigJson(customConfigObject);
        }
        if(rootObject.has(customConfigFileKey)) {
            String configLocation = rootObject.get(customConfigFileKey).getAsString();
            DataReaderJS dataReader = ScriptResourceUtil.read(new Identifier(configLocation));
            if(dataReader == null) throw new IllegalStateException(String.format("Custom config location \"%s\" does not exists!", configLocation));
            String configContent = dataReader.asString();

            JsonObject customConfigObject = JSON_PARSER.parse(configContent).getAsJsonObject();
            return parseEyecandyCustomConfigJson(customConfigObject);
        }
        return null;
    }

    private static EyecandyCustomConfig parseEyecandyCustomConfigJson(JsonObject jsonObject) {
        String description = JsonUtil.getString("description", null, jsonObject);

        List<EyecandyCustomConfig.Entry> entries = new ArrayList<>();
        if(jsonObject.has("entries")) {
            JsonArray entriesArray = jsonObject.get("entries").getAsJsonArray();
            for(JsonElement jsonElement : entriesArray) {
                JsonObject entryObject = jsonElement.getAsJsonObject();
                String id = JsonUtil.getString("id", entryObject);
                String name = JsonUtil.getString("name", id, entryObject);
                EyecandyCustomConfig.Entry.Type type = EyecandyCustomConfig.Entry.Type.valueOf(JsonUtil.getString("type", entryObject));
                String validation = JsonUtil.getString("validation", null, entryObject);
                String validationMessage = JsonUtil.getString("validationMessage", String.format("Value is not %s!", validation), entryObject);
                String defaultValue = JsonUtil.getString("defaultValue", null, entryObject);

                EyecandyCustomConfig.Entry entry = new EyecandyCustomConfig.Entry(id, name, type, validation == null ? null : new EyecandyCustomConfig.Entry.ValidationCallback.Regex(validation, validationMessage), defaultValue);
                entries.add(entry);
            }
        }

        return new EyecandyCustomConfig(description, entries);
    }

    private static void validateScriptEntries() {
        // Vehicle validation
        for(Map.Entry<String, String> vehicleEntry : vehicleScriptIds.entrySet()) {
            String entryId = vehicleEntry.getKey();
            String scriptEntryId = vehicleEntry.getValue();

            if(!vehicleScripts.containsKey(scriptEntryId)) {
                MTRScriptingModClient.LOGGER.warn("[MTR Scripting via JCM] Vehicle script \"{}\" is either missing or failed to load! (Used by vehicle {})", scriptEntryId, entryId);
            }
        }

        // Eyecandy validation
        for(Map.Entry<String, String> scriptEntry : eyecandyScriptIds.entrySet()) {
            String entryId = scriptEntry.getKey();
            String scriptEntryId = scriptEntry.getValue();

            if(!eyecandyScriptIds.containsKey(scriptEntryId)) {
                MTRScriptingModClient.LOGGER.warn("[MTR Scripting via JCM] Eyecandy script \"{}\" is either missing or failed to load! (Used by entry {})", scriptEntryId, entryId);
            }
        }
    }

    private static ParsedScript tryParseScript(String id, String scriptType, String contextName, JsonObject jsonObject, boolean isParsingMTR4, boolean useSnakeCase) {
        final List<ScriptContent> scripts = new ObjectArrayList<>();
        final String scriptFilesKey = isParsingMTR4 ? "scriptLocations" : useSnakeCase ? "script_files" : "scriptFiles";
        final String scriptTextsKey = isParsingMTR4 ? "prependExpressions" : useSnakeCase ? "script_texts" : "scriptTexts";
        final String scriptInputKey = isParsingMTR4 ? "input" : "scriptInput";

        if (jsonObject.has(scriptFilesKey) || jsonObject.has(scriptTextsKey)) {
            // Parse script input and pass to the script
            if(jsonObject.has(scriptInputKey)) {
                String str = jsonObject.get(scriptInputKey).toString();
                Identifier scriptLocationSource = MTRScriptingMod.id("internal/script_input/" + scriptType + "/" + id);
                scripts.add(new ScriptContent(scriptLocationSource, "const SCRIPT_INPUT = " + str + ";"));
            }

            if(jsonObject.has(scriptTextsKey)) {
                JsonArray scriptTextArray = jsonObject.get(scriptTextsKey).getAsJsonArray();
                for(int i = 0; i < scriptTextArray.size(); i++) {
                    Identifier scriptLocationSource = MTRScriptingMod.id("internal/script_texts/" + scriptType + "/" + id + "/line" + i);
                    String scriptText = scriptTextArray.get(i).getAsString();
                    scripts.add(new ScriptContent(scriptLocationSource, scriptText));
                }
            }

            if(jsonObject.has(scriptFilesKey)) {
                JsonArray scriptFilesArray = jsonObject.get(scriptFilesKey).getAsJsonArray();
                for(int i = 0; i < scriptFilesArray.size(); i++) {
                    Identifier scriptLocationSource = new Identifier(scriptFilesArray.get(i).getAsString());
                    String scriptText = ResourceManagerHelper.readResource(scriptLocationSource);
                    if(scriptText.isEmpty()) {
                        MTRScriptingModClient.LOGGER.warn("[MTR Scripting via JCM] Script {}:{} is missing (or empty)!", scriptLocationSource.getNamespace(), scriptLocationSource.getPath());
                        continue;
                    }

                    scripts.add(new ScriptContent(scriptLocationSource, scriptText));
                }
            }
        }

        try {
            return scripts.isEmpty() ? null : MTRContentScripting.getScriptManager().parseScript(id + " (" + scriptType + ")", contextName, scripts);
        } catch (Exception e) {
            logError("parsing " + scriptType + " script (" + id + ")", e);
            return null;
        }
    }

    private static void logError(String action, Exception e) {
        if(JCMClientConfig.INSTANCE.scripting.scriptDebugMode.value()) {
            MTRScriptingModClient.LOGGER.error("[MTR] Error while {}!", action, e);
            MTRContentScripting.getScriptManager().scriptErrorNotifier.queue(() -> {
                MinecraftClient.getInstance().getPlayerMapped().sendMessage(Text.cast(TextHelper.setStyle(TextHelper.literal("[MTR] Error while " + action + "!"), Style.getEmptyMapped().withColor(TextFormatting.RED))), false);
                MinecraftClient.getInstance().getPlayerMapped().sendMessage(Text.cast(TextHelper.setStyle(TextHelper.literal("See Console for details."), Style.getEmptyMapped().withColor(TextFormatting.RED))), false);
            });
        } else {
            MTRScriptingModClient.LOGGER.error("[MTR] Error while {}: {}", action, e.getMessage());
            MTRScriptingModClient.LOGGER.error("(Enable debug mode to see more information)");
        }
    }

    /* Getters */
    public static EyecandyScriptConfiguration getEyecandyScript(String modelId) {
        return eyecandyScripts.get(eyecandyScriptIds.getOrDefault(modelId, modelId));
    }

    public static VehicleScriptConfiguration getVehicleScript(String scriptEntryId) {
        return vehicleScripts.get(scriptEntryId);
    }

    public static String getVehicleScriptEntryId(String str) {
        return vehicleScriptIds.getOrDefault(str, str);
    }

    public static boolean shouldHideDisplayParts(String vehicleId) {
        return vehiclesWithDisplayCubeHidden.contains(vehicleId);
    }

    public record EyecandyScriptConfiguration(ParsedScript parsedScript, EyecandyCustomConfig eyecandyCustomConfig) {}

    public record VehicleScriptConfiguration(ParsedScript parsedScript, VehicleScriptContext.DataFetchMode dataFetchMode) {}
}
