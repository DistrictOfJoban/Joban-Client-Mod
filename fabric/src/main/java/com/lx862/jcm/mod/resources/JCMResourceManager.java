package com.lx862.jcm.mod.resources;

import com.google.common.base.Charsets;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.lx862.jcm.mod.Constants;
import com.lx862.jcm.mod.data.pids.JsonPIDSPreset;
import com.lx862.jcm.mod.data.pids.PIDSManager;
import com.lx862.jcm.mod.util.JCMLogger;
import org.apache.commons.io.IOUtils;
import org.mtr.mapping.holder.Identifier;
import org.mtr.mapping.mapper.ResourceManagerHelper;

import java.io.IOException;

public class JCMResourceManager {
    private static final Identifier CUSTOM_RESOURCE_PATH = new Identifier(Constants.MOD_ID, "joban_custom_resources.json");
    public static void reload() {
        reloadPIDSPreset();
    }

    private static void reloadPIDSPreset() {
        PIDSManager.clearAll();

        ResourceManagerHelper.readResource(CUSTOM_RESOURCE_PATH, (inputStream -> {
            try {
                String str = IOUtils.toString(inputStream, Charsets.UTF_8);
                JsonObject jsonObject = new JsonParser().parse(str).getAsJsonObject();
                jsonObject.get("pids_images").getAsJsonArray().forEach(e -> {
                    JsonPIDSPreset parsedPreset = JsonPIDSPreset.parse(e.getAsJsonObject());
                    PIDSManager.customPresetList.put(parsedPreset.getId(), parsedPreset);
                });
            } catch (IOException e) {
                e.printStackTrace();
                JCMLogger.warn("Failed to parse JCM PIDS Preset!");
            }
        }));
    }
}
