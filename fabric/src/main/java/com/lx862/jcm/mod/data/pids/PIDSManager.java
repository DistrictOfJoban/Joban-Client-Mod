package com.lx862.jcm.mod.data.pids;

import com.google.gson.JsonObject;
import com.lx862.jcm.mod.data.pids.preset.CustomComponentPIDSPreset;
import com.lx862.jcm.mod.data.pids.preset.JsonPIDSPreset;
import com.lx862.jcm.mod.data.pids.preset.PIDSPresetBase;
import com.lx862.jcm.mod.data.pids.preset.ScriptPIDSPreset;
import com.lx862.jcm.mod.util.JCMLogger;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;

import java.util.List;
import java.util.stream.Collectors;

public class PIDSManager {
    private static final Object2ObjectArrayMap<String, PIDSPresetBase> presetList = new Object2ObjectArrayMap<>();

    public static void loadJson(JsonObject customResourceJson) {
        customResourceJson.get("pids_images").getAsJsonArray().forEach(e -> {
            String presetId = "(Unknown preset)";

            try {
                JsonObject jsonObject = e.getAsJsonObject();
                presetId = jsonObject.get("id").getAsString();
                PIDSPresetBase preset;

                if(jsonObject.has("file")) {
                    preset = CustomComponentPIDSPreset.parse(jsonObject);
                } else if(jsonObject.has("script")) {
                    preset = ScriptPIDSPreset.parse(jsonObject);
                } else {
                    preset = JsonPIDSPreset.parse(e.getAsJsonObject());
                }

                if(presetList.containsKey(preset.getId()) && !preset.builtin) {
                    JCMLogger.error("Custom preset \"{}\" already added!", presetId);
                } else {
                    presetList.put(preset.getId(), preset);
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                JCMLogger.error("Failed to parse PIDS Preset \"{}\"!", presetId);
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
