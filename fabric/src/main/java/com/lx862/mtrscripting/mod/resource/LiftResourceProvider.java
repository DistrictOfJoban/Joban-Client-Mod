package com.lx862.mtrscripting.mod.resource;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.lx862.jcm.nativeapi.NativeScriptManager;
import com.lx862.mtrscripting.core.primitive.ParsedScript;
import com.lx862.mtrscripting.mod.MTRScriptingModClient;
import com.lx862.mtrscripting.mod.util.JsonUtil;

import java.util.*;

public class LiftResourceProvider implements ScriptResourceProvider {
    private final Map<String, LiftScriptConfiguration> liftScripts = new HashMap<>();
    private final Map<String, String> liftScriptIds = new HashMap<>();
    private final Set<String> scriptControlledRendering = new HashSet<>();

    @Override
    public void parseCustom() {
    }

    @Override
    public void parseMtrResources(JsonObject rootObject, boolean isLegacyResource) {
        if(isLegacyResource) return; // No lift customization in MTR 3

        final JsonElement liftsElement = rootObject.get("lifts");
        final JsonElement liftScriptsElement = rootObject.get("liftScripts");
        if(liftsElement != null) {
            JsonArray liftsArray = liftsElement.getAsJsonArray();
            for(JsonElement liftEntry : liftsArray) {
              try {
                JsonObject liftObject = liftEntry.getAsJsonObject();
                String liftId = liftObject.get("id").getAsString();
                if(liftObject.has("scriptId")) { // For MTR 4, we put all scripting related fields into a sub-entry
                    liftScriptIds.put(liftId, liftObject.get("scriptId").getAsString());
                }
                /* Yanyang: upstream beta.2 renamed the field to isScriptControlled; accept
                   both so beta.1 resource packs keep working. */
                if(liftObject.has("isScriptControlled") ? JsonUtil.getBoolean("isScriptControlled", false, liftObject) : JsonUtil.getBoolean("isScriptRendered", false, liftObject)) {
                    scriptControlledRendering.add(liftId);
                }
              } catch (Exception e) {
                ScriptResourceProvider.logError("parsing lift entry in mtr_custom_resources.json (entry skipped)", e);
              }
            }

            if(liftScriptsElement != null) {
                JsonArray scriptArray = liftScriptsElement.getAsJsonArray();

                for(JsonElement entryElement : scriptArray) {
                  try {
                    JsonObject scriptObject = entryElement.getAsJsonObject();
                    String scriptEntryId = scriptObject.get("id").getAsString();
                    boolean entryReferenced = liftScriptIds.values().stream().anyMatch(e -> e.equals(scriptEntryId));

                    if(entryReferenced) {
                        ParsedScript parsedScript = ScriptResourceProvider.tryParseScript(scriptEntryId, "lift", "Lift", scriptObject, true, false);
                        if (parsedScript != null || NativeScriptManager.isLoaded(scriptEntryId)) {
                            liftScripts.put(scriptEntryId, new LiftScriptConfiguration(parsedScript));
                        }
                    } else {
                        /* wording: these are LIFT scripts, not vehicle scripts. */
                        MTRScriptingModClient.LOGGER.warn("[MTR Scripting via JCM] Skip parsing lift scripts \"{}\", which is not referenced by any lift!", scriptEntryId);
                    }
                  } catch (Exception e) {
                    ScriptResourceProvider.logError("parsing lift script entry '" + VehicleResourceProvider.safeId(entryElement) + "' in mtr_custom_resources.json (entry skipped)", e);
                  }
                }
            }
        }
    }

    @Override
    public void validate() {
        for(Map.Entry<String, String> scriptEntry : liftScriptIds.entrySet()) {
            String liftId = scriptEntry.getKey();
            String scriptId = scriptEntry.getValue();

            if(!liftScripts.containsKey(scriptId)) {
                /* Yanyang: native-only ids are driven by NativeScriptManager
                   (mixed ids are registered above, with their ParsedScript). */
                if(NativeScriptManager.isLoaded(scriptId)) {
                    continue;
                }
                if(NativeScriptManager.isSkipped(scriptId)) {
                    MTRScriptingModClient.LOGGER.warn("[MTR Scripting via JCM] Lift native script \"{}\" is skipped on this platform ({}) — see the earlier nativeLibrary hint.", scriptId, NativeScriptManager.getPlatformKeyExact());
                    continue;
                }
                MTRScriptingModClient.LOGGER.warn("[MTR Scripting via JCM] Lift script \"{}\" is either missing or failed to load! (Used by entry {})", scriptId, liftId);
            }
        }
    }

    @Override
    public void reset() {
        liftScripts.clear();
        liftScriptIds.clear();
        scriptControlledRendering.clear();
    }

    public LiftScriptConfiguration getScriptEntry(String id) {
        return liftScripts.get(liftScriptIds.getOrDefault(id, id));
    }

    public String getScriptEntryId(String id) { return liftScriptIds.getOrDefault(id, id); }

    public boolean isScriptControlledRendering(String id) {
        return scriptControlledRendering.contains(id);
    }

    public record LiftScriptConfiguration(ParsedScript parsedScript) {}
}
