package com.lx862.jcm.mod.data.pids;

import com.google.gson.JsonObject;
import com.lx862.jcm.mod.Constants;
import com.lx862.jcm.mod.data.pids.preset.*;
import com.lx862.jcm.mod.util.JCMLogger;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import org.mtr.mapping.holder.Identifier;

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
                PIDSPresetBase customPreset;

                if(jsonObject.has("file")) {
                    customPreset = CustomComponentPIDSPreset.parse(jsonObject);
                } else {
                    customPreset = JsonPIDSPreset.parse(e.getAsJsonObject());
                }

                if(presetList.containsKey(customPreset.getId())) {
                    JCMLogger.error("Preset \"{}\" already added!", presetId);
                } else {
                    presetList.put(customPreset.getId(), customPreset);
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