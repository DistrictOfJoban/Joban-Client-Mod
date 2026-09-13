package com.lx862.mtrscripting.mod.resource;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.lx862.mtrscripting.core.primitive.ParsedScript;
import com.lx862.mtrscripting.mod.MTRScriptingModClient;
import com.lx862.mtrscripting.mod.impl.mtr.vehicle.VehicleDataCache;
import com.lx862.mtrscripting.mod.impl.mtr.vehicle.VehicleScriptContext;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.ObjectArrayList;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VehicleResourceProvider implements ScriptResourceProvider {
    private final Map<String, VehicleScriptConfiguration> vehicleScripts = new HashMap<>();
    private final Map<String, String> vehicleScriptIds = new HashMap<>();
    private final List<String> vehiclesWithDisplayCubeHidden = new ObjectArrayList<>();

    @Override
    public void parseCustom() {
    }

    @Override
    public void parseMtrResources(JsonObject rootObject, boolean isLegacyResource) {
        final JsonElement vehicleElement = isLegacyResource ? rootObject.get("custom_trains") : rootObject.get("vehicles");
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
                            ParsedScript parsedScript = ScriptResourceProvider.tryParseScript(baseId, "vehicle", "Vehicle", vehicleResource, false, true);
                            if (parsedScript != null) {
                                vehicleScripts.put(baseId, new VehicleResourceProvider.VehicleScriptConfiguration(parsedScript, VehicleScriptContext.DataFetchMode.MANDATORY));
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
                        ScriptResourceProvider.logError("parsing legacy vehicle script '" + baseId + "' in mtr_custom_resources.json", e);
                    }
                }
            } else { // MTR 4
                final JsonElement vehicleScriptsElement = rootObject.get("vehicleScripts");
                final JsonArray vehiclesArray = vehicleElement.getAsJsonArray();

                for(JsonElement vehicleEntry : vehiclesArray) {
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

                if(vehicleScriptsElement != null) {
                    JsonArray scriptArray = vehicleScriptsElement.getAsJsonArray();

                    for(JsonElement entryElement : scriptArray) {
                        JsonObject scriptObject = entryElement.getAsJsonObject();
                        String scriptEntryId = scriptObject.get("id").getAsString();
                        boolean forceLoad = scriptObject.has("forceLoad") && scriptObject.get("forceLoad").getAsBoolean();
                        boolean entryReferenced = vehicleScriptIds.values().stream().anyMatch(e -> e.equals(scriptEntryId));

                        if(entryReferenced || forceLoad) {
                            ParsedScript parsedScript = ScriptResourceProvider.tryParseScript(scriptEntryId, "vehicle", "Vehicle", scriptObject, true, false);
                            VehicleScriptContext.DataFetchMode dataFetchMode = scriptObject.has("dataFetchMode") ? VehicleScriptContext.DataFetchMode.valueOf(scriptObject.get("dataFetchMode").getAsString()) : VehicleScriptContext.DataFetchMode.SKIP;
                            if (parsedScript != null) {
                                vehicleScripts.put(scriptEntryId, new VehicleResourceProvider.VehicleScriptConfiguration(parsedScript, dataFetchMode));
                            }
                        } else {
                            MTRScriptingModClient.LOGGER.warn("[MTR Scripting via JCM] Skip parsing vehicle scripts \"{}\", which is not referenced by any vehicle!", scriptEntryId);
                        }
                    }
                }
            }
        }
    }

    @Override
    public void validate() {
        // Vehicle validation
        for(Map.Entry<String, String> vehicleEntry : vehicleScriptIds.entrySet()) {
            String entryId = vehicleEntry.getKey();
            String scriptEntryId = vehicleEntry.getValue();

            if(!vehicleScripts.containsKey(scriptEntryId)) {
                MTRScriptingModClient.LOGGER.warn("[MTR Scripting via JCM] Vehicle script \"{}\" is either missing or failed to load! (Used by vehicle {})", scriptEntryId, entryId);
            }
        }
    }

    @Override
    public void reset() {
        vehicleScripts.clear();
        vehicleScriptIds.clear();
        vehiclesWithDisplayCubeHidden.clear();
        VehicleDataCache.clearData();
    }

    public VehicleResourceProvider.VehicleScriptConfiguration getVehicleScript(String scriptEntryId) {
        return vehicleScripts.get(scriptEntryId);
    }

    public String getVehicleScriptEntryId(String str) {
        return vehicleScriptIds.getOrDefault(str, str);
    }

    public boolean shouldHideDisplayParts(String vehicleId) {
        return vehiclesWithDisplayCubeHidden.contains(vehicleId);
    }

    public record VehicleScriptConfiguration(ParsedScript parsedScript, VehicleScriptContext.DataFetchMode dataFetchMode) {}
}
