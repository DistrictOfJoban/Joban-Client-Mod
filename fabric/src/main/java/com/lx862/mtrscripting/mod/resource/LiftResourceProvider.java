package com.lx862.mtrscripting.mod.resource;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.lx862.mtrscripting.core.primitive.ParsedScript;
import com.lx862.mtrscripting.mod.MTRScriptingModClient;

import java.util.HashMap;
import java.util.Map;

public class LiftResourceProvider implements ScriptResourceProvider<LiftResourceProvider.LiftScriptConfiguration> {
    private final Map<String, LiftScriptConfiguration> liftScripts = new HashMap<>();
    private final Map<String, String> liftScriptIds = new HashMap<>();

    @Override
    public void parseCustom() {
    }

    @Override
    public void parseMtrResources(JsonObject rootObject, boolean isLegacyResource) {
        if(isLegacyResource) return;
        // Parse lift
        final JsonElement liftsElement = rootObject.get("lifts");
        final JsonElement liftScriptsElement = rootObject.get("liftScripts");
        if(liftsElement != null) {
            JsonArray liftsArray = liftsElement.getAsJsonArray();
            for(JsonElement liftEntry : liftsArray) {
                JsonObject liftObject = liftEntry.getAsJsonObject();
                String baseId = liftObject.get("id").getAsString();
                if(liftObject.has("scriptId")) { // For MTR 4, we put all scripting related fields into a sub-entry
                    liftScriptIds.put(baseId, liftObject.get("scriptId").getAsString());
                }
            }

            if(liftScriptsElement != null) {
                JsonArray scriptArray = liftScriptsElement.getAsJsonArray();

                for(JsonElement entryElement : scriptArray) {
                    JsonObject scriptObject = entryElement.getAsJsonObject();
                    String scriptEntryId = scriptObject.get("id").getAsString();
                    boolean entryReferenced = liftScriptIds.values().stream().anyMatch(e -> e.equals(scriptEntryId));

                    if(entryReferenced) {
                        ParsedScript parsedScript = ScriptResourceProvider.tryParseScript(scriptEntryId, "lift", "Lift", scriptObject, true, false);
                        if (parsedScript != null) {
                            liftScripts.put(scriptEntryId, new LiftScriptConfiguration(parsedScript));
                        }
                    } else {
                        MTRScriptingModClient.LOGGER.warn("[MTR Scripting via JCM] Skip parsing lift scripts \"{}\", which is not referenced by any vehicle!", scriptEntryId);
                    }
                }
            }
        }
    }

    @Override
    public void validate() {
        // Lift validation
        for(Map.Entry<String, String> scriptEntry : liftScriptIds.entrySet()) {
            String entryId = scriptEntry.getKey();
            String scriptEntryId = scriptEntry.getValue();

            if(!liftScripts.containsKey(scriptEntryId)) {
                MTRScriptingModClient.LOGGER.warn("[MTR Scripting via JCM] Lift script \"{}\" is either missing or failed to load! (Used by entry {})", scriptEntryId, entryId);
            }
        }
    }

    @Override
    public void reset() {
        liftScripts.clear();
        liftScriptIds.clear();
    }

    @Override
    public LiftScriptConfiguration getScriptEntry(String id) {
        return liftScripts.get(liftScriptIds.getOrDefault(id, id));
    }

    public record LiftScriptConfiguration(ParsedScript parsedScript) {}
}
