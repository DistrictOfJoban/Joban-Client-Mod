package com.lx862.jcm.mod.data.pids;

import com.lx862.jcm.mod.data.pids.preset.PIDSPresetBase;
import com.lx862.jcm.mod.data.pids.preset.RVPIDSPreset;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class PIDSManager {
    public static final Object2ObjectArrayMap<String, PIDSPresetBase> presetList = new Object2ObjectArrayMap<>();

    public static PIDSPresetBase getPreset(String id, PIDSPresetBase defaultPreset) {
        if(presetList.containsKey(id)) {
            return presetList.get(id);
        }
        return defaultPreset;
    }

    public static PIDSPresetBase getPreset(String id) {
        return getPreset(id, null);
    }

    public static void clearAll() {
        presetList.entrySet().stream().filter(e -> !e.getValue().builtin).collect(Collectors.toList()).clear();
    }

    public static List<PIDSPresetBase> getBuiltInPresets() {
        return presetList.values().stream().filter(e -> e.builtin).collect(Collectors.toList());
    }

    public static List<PIDSPresetBase> getCustomPresets() {
        return presetList.values().stream().filter(e -> !e.builtin).collect(Collectors.toList());
    }

    public static List<PIDSPresetBase> getPresets() {
        return new ArrayList<>(presetList.values());
    }

    static {
        presetList.put("rv_pids", new RVPIDSPreset());
    }
}
