package com.lx862.mtrscripting.mod.resource;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.lx862.mtrscripting.core.primitive.ParsedScript;
import com.lx862.mtrscripting.core.util.DataReaderJS;
import com.lx862.mtrscripting.core.util.ScriptResourceUtil;
import com.lx862.mtrscripting.mod.MTRScriptingModClient;
import com.lx862.mtrscripting.mod.impl.mtr.eyecandy.config.EyecandyCustomConfig;
import com.lx862.mtrscripting.mod.util.JsonUtil;
import org.apache.commons.io.FilenameUtils;
import org.mtr.mapping.holder.Identifier;
import org.mtr.mapping.mapper.ResourceManagerHelper;
import org.mtr.mod.Init;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class EyecandyResourceProvider implements ScriptResourceProvider {
    private final Map<String, EyecandyScriptConfiguration> eyecandyScripts = new HashMap<>();
    private final Map<String, String> eyecandyScriptIds = new HashMap<>();
    private final Set<String> scriptControlledRendering = new HashSet<>();

    @Override
    public void parseCustom() {
        readLegacyEyecandy();
    }

    @Override
    public void parseMtrResources(JsonObject rootObject, boolean isLegacyResource) {
        if(isLegacyResource) return;

        // Parse MTR 4 Eyecandy
        final JsonElement eyecandyElement = rootObject.get("objects"); // MTR 4
        if(eyecandyElement != null) {
            final JsonElement scriptsElement = rootObject.get("objectScripts");
            JsonArray eyecandyObjects = eyecandyElement.getAsJsonArray();
            for(JsonElement jsonElement : eyecandyObjects) {
                final JsonObject eyecandyEntry = jsonElement.getAsJsonObject();
                final String id = eyecandyEntry.get("id").getAsString();
                if(eyecandyEntry.has("scriptId")) { // For MTR 4, we put all scripting related fields into a sub-entry
                    String eyecandyScriptId = eyecandyEntry.get("scriptId").getAsString();
                    eyecandyScriptIds.put(id, eyecandyScriptId);
                }
                if(JsonUtil.getBoolean("isScriptRendered", false, eyecandyEntry)) {
                    scriptControlledRendering.add(id);
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
                        ParsedScript parsedScript = ScriptResourceProvider.tryParseScript(scriptEntryId, "eyecandy", "Block", scriptObject, true, false);
                        if(parsedScript != null) eyecandyScripts.put(scriptEntryId, new EyecandyScriptConfiguration(parsedScript, customConfig));
                    } else {
                        MTRScriptingModClient.LOGGER.warn("[MTR Scripting via JCM] Skip parsing eyecandy script \"{}\", which is not referenced by any eyecandy object!", scriptEntryId);
                    }
                }
            }
        }
    }

    @Override
    public void validate() {
        // Eyecandy validation
        for(Map.Entry<String, String> scriptEntry : eyecandyScriptIds.entrySet()) {
            String entryId = scriptEntry.getKey();
            String scriptEntryId = scriptEntry.getValue();

            if(!eyecandyScripts.containsKey(scriptEntryId)) {
                MTRScriptingModClient.LOGGER.warn("[MTR Scripting via JCM] Eyecandy script \"{}\" is either missing or failed to load! (Used by entry {})", scriptEntryId, entryId);
            }
        }
    }

    @Override
    public void reset() {
        eyecandyScripts.clear();
        eyecandyScriptIds.clear();
        scriptControlledRendering.clear();
    }

    public EyecandyScriptConfiguration getScriptEntry(String id) {
        return eyecandyScripts.get(eyecandyScriptIds.getOrDefault(id, id));
    }

    public boolean isScriptControlledRendering(String id) {
        return scriptControlledRendering.contains(id);
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

            JsonObject customConfigObject = new JsonParser().parse(configContent).getAsJsonObject();
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

    /**
     * Read legacy script entry (for MTR-NTE based eyecandy entries)
     */
    private void readLegacyEyecandy() {
        ResourceManagerHelper.readDirectory("eyecandies", (identifier, inputStream) -> {
            boolean fileIsNteEyecandy = identifier.getNamespace().equals(Init.MOD_ID_NTE) && identifier.getPath().endsWith(".json");
            if (fileIsNteEyecandy) {
                try(InputStreamReader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
                    final JsonObject rootObject = new JsonParser().parse(reader).getAsJsonObject();

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

                        if(JsonUtil.getBoolean("isScriptRendered", false, entryObject)) {
                            scriptControlledRendering.add(id);
                        }

                        ParsedScript parsedScript = ScriptResourceProvider.tryParseScript(id, "eyecandy", "Block", entryObject, false, false);
                        if(parsedScript != null) {
                            eyecandyScriptIds.put(id, id);
                            eyecandyScripts.put(id, new EyecandyScriptConfiguration(parsedScript, eyecandyCustomConfig));
                        }
                    }
                } catch (Exception e) {
                    ScriptResourceProvider.logError("parsing NTE Eyecandy scripts", e);
                }
            }
        });
    }

    public record EyecandyScriptConfiguration(ParsedScript parsedScript, EyecandyCustomConfig eyecandyCustomConfig) {}
}
