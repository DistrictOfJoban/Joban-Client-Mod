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
    public static final Object2ObjectArrayMap<String, PIDSPresetBase> presetList = new Object2ObjectArrayMap<>();

    public static void loadJson(JsonObject customResourceJson) {
        customResourceJson.get("pids_images").getAsJsonArray().forEach(e -> {
            String presetName = "Unnamed preset";

            try {
                JsonObject jsonObject = e.getAsJsonObject();
                presetName = jsonObject.get("id").getAsString();
                PIDSPresetBase customPreset;

                if(jsonObject.has("file")) {
                    customPreset = CustomComponentPIDSPreset.parse(jsonObject);
                } else {
                    customPreset = JsonPIDSPreset.parse(e.getAsJsonObject());
                }

                presetList.put(customPreset.getId(), customPreset);
            } catch (Exception ex) {
                ex.printStackTrace();
                JCMLogger.error("Failed to parse PIDS Preset \"{}\"!", presetName);
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
        presetList.put("door_cls_psd", new RVPIDSVariantsPreset("door_cls_psd", "RV PIDS (Door Closing PSD)", new Identifier(Constants.MOD_ID, "textures/block/pids/rv_door_cls_psd.png")));
        presetList.put("door_cls_apg", new RVPIDSVariantsPreset("door_cls_apg", "RV PIDS (Door Closing APG)", new Identifier(Constants.MOD_ID, "textures/block/pids/rv_door_cls_apg.png")));
        presetList.put("door_cls_train", new RVPIDSVariantsPreset("door_cls_train", "RV PIDS (Door Closing)", new Identifier(Constants.MOD_ID, "textures/block/pids/rv_door_cls_train.png")));
        presetList.put("lcd_pids", new LCDPIDSPreset());
    }

    public static void reset() {
        presetList.entrySet().stream().filter(e -> !e.getValue().builtin).collect(Collectors.toList()).clear();
    }
}
