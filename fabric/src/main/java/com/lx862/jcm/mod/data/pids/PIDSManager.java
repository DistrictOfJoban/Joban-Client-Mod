package com.lx862.jcm.mod.data.pids;

import com.google.gson.JsonObject;
import com.lx862.jcm.mod.data.pids.preset.JsonPIDSPreset;
import com.lx862.jcm.mod.data.pids.preset.PIDSPresetBase;
import com.lx862.jcm.mod.data.pids.preset.RVPIDSPreset;
import com.lx862.jcm.mod.util.JCMLogger;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;

import java.util.List;
import java.util.stream.Collectors;

public class PIDSManager {
    public static final Object2ObjectArrayMap<String, PIDSPresetBase> presetList = new Object2ObjectArrayMap<>();

    public static void reload(JsonObject customResourceJson) {
        clearAll();

        customResourceJson.get("pids_images").getAsJsonArray().forEach(e -> {
            try {
                JsonPIDSPreset parsedPreset = JsonPIDSPreset.parse(e.getAsJsonObject());
                presetList.put(parsedPreset.getId(), parsedPreset);
            } catch (Exception ex) {
                ex.printStackTrace();
                JCMLogger.error("Failed to parse JCM PIDS Preset!");
            }
        });
    }

    public static PIDSPresetBase getPreset(String id, PIDSPresetBase defaultPreset) {
        if(presetList.containsKey(id)) {
            return presetList.get(id);
        }

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

    static {
        presetList.put("rv_pids", new RVPIDSPreset());
    }

    private static void clearAll() {
        presetList.entrySet().stream().filter(e -> !e.getValue().builtin).collect(Collectors.toList()).clear();
    }
}
