package com.lx862.jcm.mod.resources;

import com.google.common.base.Charsets;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.lx862.jcm.mod.Constants;
import com.lx862.jcm.mod.JCMClient;
import com.lx862.jcm.mod.data.pids.PIDSManager;
import com.lx862.jcm.mod.render.text.TextRenderingManager;
import com.lx862.jcm.mod.util.JCMLogger;
import org.apache.commons.io.IOUtils;
import org.mtr.mapping.holder.Identifier;
import org.mtr.mapping.mapper.ResourceManagerHelper;

public class JCMResourceManager {
    private static final Identifier CUSTOM_RESOURCE_PATH = Constants.id("joban_custom_resources.json");

    public static void reload() {
        JCMClient.getMcMetaManager().reset();
        TextRenderingManager.initialize();
        PIDSManager.reset();
        parseCustomResources();
    }

    private static void parseCustomResources() {
        ResourceManagerHelper.readAllResources(CUSTOM_RESOURCE_PATH, (inputStream -> {
            try {
                String str = IOUtils.toString(inputStream, Charsets.UTF_8);
                JsonObject jsonObject = new JsonParser().parse(str).getAsJsonObject();
                PIDSManager.loadJson(jsonObject);
            } catch (Exception e) {
                JCMLogger.error("Failed to parse custom resource file!", e);
            }
        }));
    }
}
