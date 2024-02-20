package com.lx862.jcm.mod.render.text.font;

import com.google.common.base.Charsets;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.lx862.jcm.mod.util.JCMLogger;
import org.apache.commons.io.IOUtils;
import org.mtr.mapping.holder.Identifier;
import org.mtr.mapping.mapper.ResourceManagerHelper;

import java.util.HashMap;

public class FontManager {
    public static final HashMap<Identifier, FontSet> fonts = new HashMap<>();

    public static void initialize() {
        fonts.clear();

        JCMLogger.debug("[FontManager] Loading default fonts");
        loadVanillaFont("jsblock:deptimer");
        loadVanillaFont("jsblock:pids_lcd");
        loadVanillaFont("mtr:mtr");
    }

    public static void loadVanillaFont(String jsonPath) {
        Identifier fontId = new Identifier(jsonPath);
        if(fonts.containsKey(fontId)) return;
        String namespace = fontId.getNamespace();
        String path = "font/" + fontId.getPath() + ".json";

        ResourceManagerHelper.readResource(new Identifier(namespace, path), inputStream -> {
            try {
                JsonObject jsonObject = new JsonParser().parse(IOUtils.toString(inputStream, Charsets.UTF_8)).getAsJsonObject();
                fonts.put(fontId, new FontSet(jsonObject));
            } catch (Exception e) {
                e.printStackTrace();
                JCMLogger.warn("Failed to read vanilla font json: {}", path);
            }
        });
    }

    public static FontSet getFontSet(Identifier path) {
        return fonts.get(path);
    }
}
