package com.lx862.jcm.mod.data.pids;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.lx862.jcm.mod.config.JCMClientConfig;
import com.lx862.jcm.mod.data.pids.preset.JsonPIDSPreset;
import com.lx862.jcm.mod.data.pids.preset.PIDSPresetBase;
import com.lx862.jcm.mod.data.pids.preset.ScriptPIDSPreset;
import com.lx862.jcm.mod.util.JCMLogger;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class PIDSManager {
    private static final Map<String, PIDSPresetBase> presetList = new HashMap<>();

    public static void loadJson(JsonObject customResourceJson) {
        /* A pack that customizes only other JCM features has no pids_images
           section — the raw get() NPE'd and was reported as a confusing
           "Failed to parse custom resource file!" on every reload. */
        JsonElement pidsImages = customResourceJson.get("pids_images");
        if (pidsImages == null || !pidsImages.isJsonArray()) return;
        pidsImages.getAsJsonArray().forEach(e -> {
            String presetId = "(Unknown preset)";

            try {
                JsonObject jsonObject = e.getAsJsonObject();
                presetId = jsonObject.get("id").getAsString();

                /* Yanyang: script presets can also be declared through the
                   2.3.0-beta.2 scriptLocations key (JS + C++ mixed entries)
                   or the native nativeLibrary/nativeLibraries fields. */
                boolean isScriptPreset = jsonObject.has("scriptFiles") || jsonObject.has("scriptTexts")
                        || jsonObject.has("scriptLocations") || jsonObject.has("nativeLibrary") || jsonObject.has("nativeLibraries");
                PIDSPresetBase preset;

                if(isScriptPreset && JCMClientConfig.INSTANCE.scripting.skipScriptParsing.value()) {
                    JCMLogger.info("Not parsing scripted PIDS Preset {} as scripting is disabled.", presetId);
                    return;
                }

                if(isScriptPreset) {
                    preset = ScriptPIDSPreset.parse(jsonObject);
                } else {
                    preset = JsonPIDSPreset.parse(e.getAsJsonObject());
                }

                if(preset != null) {
                    if(presetList.containsKey(preset.getId()) && !preset.builtin) {
                        JCMLogger.error("Custom preset \"{}\" already added!", presetId);
                    } else {
                        presetList.put(preset.getId(), preset);
                    }
                }
            } catch (Exception ex) {
                JCMLogger.error("Failed to parse PIDS Preset \"" + presetId + "\"!", ex);
            }
        });
    }

    public static PIDSPresetBase getPreset(String id, PIDSPresetBase defaultPreset) {
        return presetList.getOrDefault(id, defaultPreset);
    }

    public static PIDSPresetBase getPreset(String id) {
        return getPreset(id, null);
    }

    public static List<PIDSPresetBase> getBuiltInPresets() {
        return presetList.values().stream().filter(e -> e.builtin).collect(Collectors.toList());
    }

    public static List<PIDSPresetBase> getCustomPresets() {
        return presetList.values().stream().filter(e -> !e.builtin).collect(Collectors.toList());
    }

    public static void reset() {
        presetList.entrySet().removeIf(e -> !e.getValue().builtin);
    }
}
