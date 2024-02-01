package com.lx862.jcm.mod.resources;

import com.google.common.base.Charsets;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.lx862.jcm.mod.Constants;
import com.lx862.jcm.mod.data.pids.preset.JsonPIDSPreset;
import com.lx862.jcm.mod.data.pids.PIDSManager;
import com.lx862.jcm.mod.render.text.FontManager;
import com.lx862.jcm.mod.render.text.TextRenderingManager;
import com.lx862.jcm.mod.util.JCMLogger;
import org.apache.commons.io.IOUtils;
import org.mtr.mapping.holder.Identifier;
import org.mtr.mapping.mapper.ResourceManagerHelper;

public class JCMResourceManager {
    private static final Identifier CUSTOM_RESOURCE_PATH = new Identifier(Constants.MOD_ID, "joban_custom_resources.json");
    public static void reload() {
        reloadPIDSPreset();
        TextRenderingManager.initialize();
        FontManager.initialize();
    }

    private static void reloadPIDSPreset() {
        PIDSManager.clearAll();

        ResourceManagerHelper.readResource(CUSTOM_RESOURCE_PATH, (inputStream -> {
            try {
                String str = IOUtils.toString(inputStream, Charsets.UTF_8);
                JsonObject jsonObject = new JsonParser().parse(str).getAsJsonObject();
                jsonObject.get("pids_images").getAsJsonArray().forEach(e -> {
                    JsonPIDSPreset parsedPreset = JsonPIDSPreset.parse(e.getAsJsonObject());
                    PIDSManager.presetList.put(parsedPreset.getId(), parsedPreset);
                });
            } catch (Exception e) {
                e.printStackTrace();
                JCMLogger.warn("Failed to parse JCM PIDS Preset!");
            }
        }));
    }
}
