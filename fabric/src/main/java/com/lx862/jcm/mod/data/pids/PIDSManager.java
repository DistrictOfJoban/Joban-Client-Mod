package com.lx862.jcm.mod.data.pids;

import com.lx862.jcm.mod.data.pids.base.PIDSPresetBase;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;

public class PIDSManager {
    public static final Object2ObjectArrayMap<String, PIDSPresetBase> customPresetList = new Object2ObjectArrayMap<>();
    private static final Object2ObjectArrayMap<String, PIDSPresetBase> defaultPresetList = new Object2ObjectArrayMap<>();

    public static PIDSPresetBase getPreset(String id, PIDSPresetBase defaultPreset) {
        if(defaultPresetList.containsKey(id)) {
            return defaultPresetList.get(id);
        }
        if(customPresetList.containsKey(id)) {
            return customPresetList.get(id);
        }
        return defaultPreset;
    }

    public static PIDSPresetBase getBuiltInPreset(String id) {
        return defaultPresetList.get(id);
    }

    public static void clearAll() {
        customPresetList.clear();
    }

    static {
        defaultPresetList.put("rv_pids", new RVPIDSPreset());
    }
}
