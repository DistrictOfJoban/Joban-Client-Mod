package com.lx862.jcm.mod.trm;

import com.google.common.base.Charsets;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.lx862.jcm.mod.util.JCMLogger;
import org.apache.commons.io.IOUtils;
import org.mtr.mapping.holder.Identifier;
import org.mtr.mapping.mapper.ResourceManagerHelper;

import java.awt.*;
import java.io.InputStream;
import java.util.HashMap;

public class FontManager {
    public static final HashMap<Identifier, Font> fontList = new HashMap<>();

    public static void initializes() {
        fontList.clear();

        JCMLogger.debug("[FontManager] Loading default fonts");
        loadVanillaFont("jsblock:deptimer");
    }

    private static void loadFontFile(Identifier id, Identifier path) {
        ResourceManagerHelper.readResource(path, inputStream -> {
            putFont(id, inputStream);
        });
    }

    public static void loadVanillaFont(String jsonPath) {
        Identifier fontId = new Identifier(jsonPath);
        if(fontList.containsKey(fontId)) return;
        String namespace = fontId.getNamespace();
        String path = "font/" + fontId.getPath() + ".json";

        ResourceManagerHelper.readResource(new Identifier(namespace, path), inputStream -> {
            try {
                JsonObject jsonObject = new JsonParser().parse(IOUtils.toString(inputStream, Charsets.UTF_8)).getAsJsonObject();
                JsonObject firstFont = jsonObject.getAsJsonArray("providers").get(0).getAsJsonObject();
                String fontFile = firstFont.get("file").getAsString();
                Identifier fontFileId = new Identifier(fontFile);
                String fontFilePath = "font/" + fontFileId.getPath();

                loadFontFile(fontId, new Identifier(fontFileId.getNamespace(), fontFilePath));
            } catch (Exception e) {
                e.printStackTrace();
                JCMLogger.warn("Failed to read vanilla font json: {}", path);
            }
        });
    }

    private static void putFont(Identifier id, InputStream inputStream) {
        try {
            Font createdFont = Font.createFont(Font.TRUETYPE_FONT, inputStream);
            fontList.put(id, createdFont);
        } catch (Exception e) {
            e.printStackTrace();
            JCMLogger.warn("Failed to load font: {}", id);
        }
    }

    public static Font getFont(Identifier path) {
        return fontList.get(path);
    }
}
