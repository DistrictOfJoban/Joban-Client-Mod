package com.lx862.jcm.mod.data.pids;

import com.google.gson.JsonObject;
import com.lx862.jcm.mod.Constants;
import com.lx862.jcm.mod.data.pids.preset.*;
import com.lx862.jcm.mod.util.JCMLogger;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import org.mtr.mapping.holder.Identifier;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class PIDSManager {
    public static final Object2ObjectOpenHashMap<String, PIDSPresetBase> presetList = new Object2ObjectOpenHashMap<>();

    public static void loadJson(JsonObject customResourceJson) {
        customResourceJson.get("pids_images").getAsJsonArray().forEach(e -> {
            String presetName = "Unnamed preset";

            try {
                JsonObject jsonObject = e.getAsJsonObject();
                presetName = jsonObject.get("id").getAsString();
                PIDSPresetBase customPreset = parsePreset(jsonObject);

                if(hasPresets(customPreset.getId())) {
                    JCMLogger.error("PIDS Preset ID \"{}\" already exists!", presetName);
                } else {
                    presetList.put(customPreset.getId(), customPreset);
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                JCMLogger.error("Failed to parse PIDS Preset \"{}\"!", presetName);
            }
        });
    }

    /**
     * Parse and return a PIDS Preset (Usually JSON-config-based or Component-Based)
     * @param jsonObject The Json Object of the PIDS Preset Entry
     * @return A PIDS Preset, null if it failed to parse
     */
    public static @Nullable PIDSPresetBase parsePreset(JsonObject jsonObject) {
        if(jsonObject.has("file")) {
            return CustomComponentPIDSPreset.parse(jsonObject);
        } else if(jsonObject.has("TODO")) {
            return null;
        } else {
            return JsonPIDSPreset.parse(jsonObject);
        }
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

    public static boolean hasPresets(String presetId) {
        return presetList.values().stream().anyMatch(e -> e.getId().equals(presetId));
    }

    static {
        presetList.put("rv_pids", new RVPIDSPreset());
        presetList.put("door_cls_psd", new RVPIDSVariantsPreset("door_cls_psd", "RV PIDS (Door Closing PSD)", new Identifier(Constants.MOD_ID, "textures/block/pids/rv_door_cls_psd.png")));
        presetList.put("door_cls_apg", new RVPIDSVariantsPreset("door_cls_apg", "RV PIDS (Door Closing APG)", new Identifier(Constants.MOD_ID, "textures/block/pids/rv_door_cls_apg.png")));
        presetList.put("door_cls_train", new RVPIDSVariantsPreset("door_cls_train", "RV PIDS (Door Closing)", new Identifier(Constants.MOD_ID, "textures/block/pids/rv_door_cls_train.png")));
        presetList.put("lcd_pids", new LCDPIDSPreset());
    }

    public static void clearPresets() {
        for(Map.Entry<String, PIDSPresetBase> presetEntry : presetList.entrySet()) {
            PIDSPresetBase preset = presetEntry.getValue();
            if(!preset.builtin) {
                presetList.remove(presetEntry.getKey());
            }
        }
    }
}
