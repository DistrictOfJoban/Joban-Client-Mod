package com.lx862.jcm.mod.resources;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.lx862.jcm.mod.JCMClient;
import com.lx862.jcm.mod.scripting.mtr.MTRScripting;
import com.lx862.jcm.mod.util.JCMLogger;
import com.lx862.mtrscripting.core.ParsedScript;
import com.lx862.mtrscripting.data.ScriptContent;
import org.apache.commons.io.FilenameUtils;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.mtr.mapping.holder.Identifier;
import org.mtr.mapping.mapper.ResourceManagerHelper;
import org.mtr.mod.Init;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Temporary class to hold resource handling regarding MTR/NTE contents, until they are upstreamed */
public class MTRContentResourceManager {
    private static final HashMap<String, ParsedScript> vehicleScripts = new HashMap<>();
    private static final HashMap<String, ParsedScript> eyecandyScripts = new HashMap<>();
    private static final HashMap<String, String> vehicleToVehicleScripts = new HashMap<>();
    private static final JsonParser JSON_PARSER = new JsonParser();

    public static void reload() {
        JCMLogger.info("Loading scripts on-behalf of MTR...");
        eyecandyScripts.clear();
        vehicleScripts.clear();
        readNteEyecandy();
        readMtrCustomResources();
    }

    /**
     * Read legacy script entry in MTR-NTE (for MTR 3)
     */
    private static void readNteEyecandy() {
        ResourceManagerHelper.readDirectory("eyecandies", (identifier, inputStream) -> {
            if (identifier.getNamespace().equals(Init.MOD_ID_NTE) && identifier.getPath().endsWith(".json")) {
                try(InputStreamReader isp = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
                    final JsonObject rootObject = JSON_PARSER.parse(isp).getAsJsonObject();

                    if(rootObject.has("model")) {
                        String id = FilenameUtils.getBaseName(identifier.getPath());
                        ParsedScript ps = tryParseScript(id, "eyecandy", "Block", rootObject, false, false);
                        eyecandyScripts.put(id, ps);
                    } else {
                        for (Map.Entry<String, JsonElement> entry : rootObject.entrySet()) {
                            final String id = entry.getKey();
                            final JsonObject entryObject = entry.getValue().getAsJsonObject();

                            ParsedScript ps = tryParseScript(id, "eyecandy", "Block", entryObject, false, false);
                            eyecandyScripts.put(id, ps);
                        }
                    }
                } catch (Exception e) {
                    logException("parsing NTE Eyecandy scripts", e);
                }
            }
        });
    }

    private static void readMtrCustomResources() {
        vehicleToVehicleScripts.clear();
        ResourceManagerHelper.readAllResources(new Identifier("mtr", "mtr_custom_resources.json"), (inputStream) -> {
            try(InputStreamReader isp = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
                final JsonObject rootObject = JSON_PARSER.parse(isp).getAsJsonObject();
                final boolean isVehicleLegacyResource = rootObject.has("custom_trains"); // Whether train format are in MTR 3
                final JsonElement vehicleElement = isVehicleLegacyResource ? rootObject.get("custom_trains") : rootObject.get("vehicles");
                final JsonElement objectsElement = rootObject.get("objects");

                if(vehicleElement != null) {
                    if(isVehicleLegacyResource) { // MTR 3
                        final JsonObject vehicleObject = vehicleElement.getAsJsonObject();
                        for (Map.Entry<String, JsonElement> map : vehicleObject.entrySet()) {
                            String baseId = "mtr_custom_train_" + map.getKey();
                            try {
                                JsonObject vehicleResource = map.getValue().getAsJsonObject();
                                ParsedScript parsedScript = tryParseScript(baseId, "vehicle", "Vehicle", vehicleResource, false, true);
                                if (parsedScript != null) {
                                    vehicleScripts.put(baseId, parsedScript);
                                    vehicleToVehicleScripts.put(baseId + "_cab_1", baseId);
                                    vehicleToVehicleScripts.put(baseId + "_cab_2", baseId);
                                    vehicleToVehicleScripts.put(baseId + "_cab_3", baseId);
                                    vehicleToVehicleScripts.put(baseId + "_trailer", baseId);
                                }
                            } catch (Exception e) {
                                logException("parsing legacy vehicle script '" + baseId + "' in mtr_custom_resources.json", e);
                            }
                        }
                    } else { // MTR 4
                        final JsonElement vehicleScriptsElement = rootObject.get("vehicleScripts");
                        final JsonArray vehicleArray = vehicleElement.getAsJsonArray();

                        Map<String, String> entryToScriptId = new HashMap<>();

                        for(JsonElement vehicleEntry : vehicleArray) {
                            JsonObject vehicleObject = vehicleEntry.getAsJsonObject();
                            String baseId = vehicleObject.get("id").getAsString();
                            if(vehicleObject.has("scripting")) { // For MTR 4, we put all scripting related fields into a sub-entry
                                entryToScriptId.put(baseId, vehicleObject.get("scripting").getAsString());
                            }
                        }

                        if(vehicleScriptsElement != null) {
                            JsonArray vehicleScriptsArray = vehicleScriptsElement.getAsJsonArray();

                            for(JsonElement entryElement : vehicleScriptsArray) {
                                JsonObject scriptObject = entryElement.getAsJsonObject();
                                String vehicleScriptEntryId = scriptObject.get("id").getAsString();

                                ParsedScript parsedScript = tryParseScript(vehicleScriptEntryId, "vehicle", "Vehicle", scriptObject, true, false);
                                if (parsedScript != null) {
                                    vehicleScripts.put(vehicleScriptEntryId, parsedScript);
                                }
                            }
                        }

                        vehicleToVehicleScripts.putAll(entryToScriptId);
                    }

                    // Validation
                    for(Map.Entry<String, String> vehicleEntry : vehicleToVehicleScripts.entrySet()) {
                        String vehicleId = vehicleEntry.getKey();
                        String vehicleGroupId = vehicleEntry.getValue();

                        if(!vehicleScripts.containsKey(vehicleGroupId)) {
                            JCMLogger.warn("Vehicle script \"{}\" is either missing or failed to load! (Used by vehicle {})", vehicleGroupId, vehicleId);
                        }
                    }
                }

                if(objectsElement != null) { // Only MTR 4 specifies eyecandy in mtr_custom_resources
                    JsonArray eyecandyObjects = objectsElement.getAsJsonArray();
                    for(JsonElement jsonElement : eyecandyObjects) {
                        final JsonObject entryObject = jsonElement.getAsJsonObject();
                        final String id = entryObject.get("id").getAsString();
                        if(entryObject.has("scripting")) { // For MTR 4, we put all scripting related fields into a sub-entry
                            JsonObject scriptObject = entryObject.get("scripting").getAsJsonObject();
                            try {
                                ParsedScript parsedScript = tryParseScript(id, "eyecandy", "Block", scriptObject, true, false);
                                if(parsedScript != null) eyecandyScripts.put(id, parsedScript);
                            } catch (Exception e) {
                                logException("parsing object '" + id + "' in mtr_custom_resources.json", e);
                            }
                        }
                    }
                }
            } catch (Exception e) {
                logException("parsing scripts in mtr_custom_resources.json", e);
            }
        });
    }

    private static ParsedScript tryParseScript(String id, String name, String contextName, JsonObject jsonObject, boolean isParsingMTR4, boolean useSnakeCase) {
        final List<ScriptContent> scripts = new ObjectArrayList<>();
        final String scriptFilesKey = isParsingMTR4 ? "scriptLocations" : useSnakeCase ? "script_files" : "scriptFiles";
        final String scriptTextsKey = isParsingMTR4 ? "prependExpressions" : useSnakeCase ? "script_texts" : "scriptTexts";
        final String scriptInputKey = isParsingMTR4 ? "input" : "scriptInput";

        if (jsonObject.has(scriptFilesKey) || jsonObject.has(scriptTextsKey)) {
            // Parse script input and pass to the script
            if(jsonObject.has(scriptInputKey)) {
                String str = jsonObject.get(scriptInputKey).toString();
                Identifier scriptLocationSource = new Identifier("mtr", "internal/script_input/" + contextName.toLowerCase() + "/" + name + "/" + id);
                scripts.add(new ScriptContent(scriptLocationSource, "const SCRIPT_INPUT = " + str + ";"));
            }

            if(jsonObject.has(scriptTextsKey)) {
                JsonArray scriptTextArray = jsonObject.get(scriptTextsKey).getAsJsonArray();
                for(int i = 0; i < scriptTextArray.size(); i++) {
                    Identifier scriptLocationSource = new Identifier("mtr", "internal/script_texts/" + contextName.toLowerCase() + "/" + name + "/" + id + "/line" + i);
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
                        JCMLogger.warn("Script {}:{} is either missing, or the file content is empty!", scriptLocationSource.getNamespace(), scriptLocationSource.getPath());
                        continue;
                    }

                    scripts.add(new ScriptContent(scriptLocationSource, scriptText));
                }
            }
        }

        return scripts.isEmpty() ? null : MTRScripting.getScriptManager().parseScript(id + " (" + name + ")", contextName, scripts);
    }

    public static ParsedScript getEyecandyScript(String modelId) {
        return eyecandyScripts.get(modelId);
    }

    public static ParsedScript getVehicleScript(String carriageId) {
        return vehicleScripts.get(carriageId);
    }

    public static String getVehicleScriptEntryId(String str) {
        return vehicleToVehicleScripts.getOrDefault(str, str);
    }

    private static void logException(String action, Exception e) {
        if(JCMClient.getConfig().debug) {
            JCMLogger.error("Error while " + action + "!", e);
        } else {
            JCMLogger.error("Error while " + action + ": " + e.getMessage());
            JCMLogger.error("(Enable debug mode to see more information)");
        }
    }
}
