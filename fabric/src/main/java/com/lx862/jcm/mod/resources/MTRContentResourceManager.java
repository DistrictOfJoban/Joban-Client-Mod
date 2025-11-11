package com.lx862.jcm.mod.resources;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.lx862.jcm.mod.Constants;
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

    public static void reload() {
        JCMLogger.info("Loading scripts on-behalf of MTR...");
        eyecandyScripts.clear();
        vehicleScripts.clear();
        registerMtr3EyecandyScripts();
        registerMtr3VehicleScripts();
    }

    private static void registerMtr3EyecandyScripts() {
        ResourceManagerHelper.readDirectory("eyecandies", (identifier, inputStream) -> {
            if (identifier.getNamespace().equals(Init.MOD_ID_NTE) && identifier.getPath().endsWith(".json")) {
                try(InputStreamReader isp = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
                    final JsonObject rootObject = new JsonParser().parse(isp).getAsJsonObject();

                    if(rootObject.has("model")) {
                        String id = FilenameUtils.getBaseName(identifier.getPath());
                        ParsedScript ps = tryParseScript(id, "eyecandy", "Block", rootObject, false);
                        eyecandyScripts.put(id, ps);
                    } else {
                        for (Map.Entry<String, JsonElement> entry : rootObject.entrySet()) {
                            final String id = entry.getKey();
                            final JsonObject entryObject = entry.getValue().getAsJsonObject();

                            ParsedScript ps = tryParseScript(id, "eyecandy", "Block", entryObject, false);
                            eyecandyScripts.put(id, ps);
                        }
                    }
                } catch (Exception e) {
                    if(JCMClient.getConfig().debug) {
                        JCMLogger.error("Error when parsing eyecandy scripts!", e);
                    } else {
                        JCMLogger.error("Error when parsing eyecandy scripts: " + e.getMessage());
                        JCMLogger.error("(Enable debug mode to see more information)");
                    }
                }
            }
        });
    }

    private static void registerMtr3VehicleScripts() {
        ResourceManagerHelper.readAllResources(new Identifier("mtr", "mtr_custom_resources.json"), (inputStream) -> {
            try(InputStreamReader isp = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
                final JsonObject rootObject = new JsonParser().parse(isp).getAsJsonObject();
                if(rootObject.has("custom_trains")) {
                    final JsonObject vehicleObject = rootObject.get("custom_trains").getAsJsonObject();
                    for(Map.Entry<String, JsonElement> map : vehicleObject.entrySet()) {
                        String baseId = "mtr_custom_train_" + map.getKey();
                        JsonObject vehicleResource = map.getValue().getAsJsonObject();
                        ParsedScript ps = tryParseScript(baseId, "vehicle", "Vehicle", vehicleResource, true);
                        if(ps != null) {
                            vehicleScripts.put(baseId + "_trailer", ps);
                            vehicleScripts.put(baseId + "_cab_1", ps);
                            vehicleScripts.put(baseId + "_cab_2", ps);
                            vehicleScripts.put(baseId + "_cab_3", ps);
                        }
                    }
                }
            } catch (Exception e) {
                if(JCMClient.getConfig().debug) {
                    JCMLogger.error("Error when parsing vehicle scripts!", e);
                } else {
                    JCMLogger.error("Error when parsing vehicle scripts: " + e.getMessage());
                    JCMLogger.error("(Enable debug mode to see more information)");
                }
            }
        });
    }

    private static ParsedScript tryParseScript(String id, String name, String contextName, JsonObject jsonObject, boolean useSnakeCase) throws Exception {
        final List<ScriptContent> scripts = new ObjectArrayList<>();
        final String scriptFilesKey = useSnakeCase ? "script_files" : "scriptFiles";
        final String scriptTextsKey = useSnakeCase ? "script_texts" : "scriptTexts";

        if (jsonObject.has(scriptFilesKey) || jsonObject.has(scriptTextsKey)) {
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

        // Parse script input and pass to the script
        if(jsonObject.has("scriptInput")) {
            String str = jsonObject.get("scriptInput").toString();
            Identifier scriptLocationSource = new Identifier("mtr", "internal/script_input/" + contextName.toLowerCase() + "/" + name + "/" + id);
            scripts.add(new ScriptContent(scriptLocationSource, "const SCRIPT_INPUT = " + str + ";"));
        }

        return scripts.isEmpty() ? null : MTRScripting.getScriptManager().parseScript(id + " (" + name + ")", contextName, scripts);
    }

    public static ParsedScript getEyecandyScript(String modelId) {
        return eyecandyScripts.get(modelId);
    }

    public static ParsedScript getVehicleScript(String carriageId) {
        return vehicleScripts.get(carriageId);
    }
}
