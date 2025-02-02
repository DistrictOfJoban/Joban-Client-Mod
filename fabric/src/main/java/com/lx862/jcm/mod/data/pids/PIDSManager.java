package com.lx862.jcm.mod.data.pids;

import com.google.gson.JsonObject;
import com.lx862.jcm.mod.Constants;
import com.lx862.mtrscripting.api.ScriptingAPI;
import com.lx862.jcm.mod.data.pids.preset.JsonPIDSPreset;
import com.lx862.jcm.mod.data.pids.preset.PIDSPresetBase;
import com.lx862.jcm.mod.data.pids.preset.ScriptPIDSPreset;
import com.lx862.jcm.mod.scripting.pids.TextWrapper;
import com.lx862.jcm.mod.scripting.pids.TextureWrapper;
import com.lx862.jcm.mod.scripting.util.MTRUtil;
import com.lx862.jcm.mod.scripting.util.TextUtil;
import com.lx862.jcm.mod.util.JCMLogger;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import org.mtr.mod.Keys;
import org.mtr.mod.client.MinecraftClientData;
import vendor.com.lx862.jcm.org.mozilla.javascript.NativeJavaClass;

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

                if(jsonObject.has("scriptFiles") || jsonObject.has("scriptTexts")) {
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
                JCMLogger.error("Failed to parse PIDS Preset \"" + presetId + "\"!", ex);
            }
        });
    }

    public static void registerScripting() {
        String mtrModVersion = null;
        try {
            mtrModVersion = (String) Keys.class.getField("MOD_VERSION").get(null);
        } catch (ReflectiveOperationException ignored) {
        }
        ScriptingAPI.registerAddonVersion("mtr", mtrModVersion);
        ScriptingAPI.registerAddonVersion("jcm", Constants.MOD_VERSION);

        ScriptingAPI.onParseScript((contextName, context, scriptable) -> {
            // On behalf of MTR
            scriptable.put("MTRUtil", scriptable, new NativeJavaClass(scriptable, MTRUtil.class));
            scriptable.put("MTRClientData", scriptable, new NativeJavaClass(scriptable, MinecraftClientData.class));
            scriptable.put("TextUtil", scriptable, new NativeJavaClass(scriptable, TextUtil.class));

            if(contextName.equals("PIDS")) {
                scriptable.put("Text", scriptable, new NativeJavaClass(scriptable, TextWrapper.class));
                scriptable.put("Texture", scriptable, new NativeJavaClass(scriptable, TextureWrapper.class));
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
