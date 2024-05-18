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

        JCMLogger.debug("[FontManager] Loading TTF fonts");

        ResourceManagerHelper.readDirectory("font", (path, is) -> {
            if(path.getPath().endsWith(".json")) {
                loadVanillaFont(path);
            }
        });
    }

    public static void loadVanillaFont(Identifier path) {
        Identifier fontId = new Identifier(path.getNamespace(), path.getPath().replace("font/", "").replace(".json", ""));

        if(fonts.containsKey(fontId)) return;
        ResourceManagerHelper.readResource(path, inputStream -> {
            try {
                JsonObject jsonObject = new JsonParser().parse(IOUtils.toString(inputStream, Charsets.UTF_8)).getAsJsonObject();
                JCMLogger.debug("[FontManager] Try load TTF font {}:{} from {}:{}", fontId.getNamespace(), fontId.getPath(), path.getNamespace(), path.getPath());
                fonts.put(fontId, new FontSet(jsonObject));
            } catch (NoTTFFontException e) {
                JCMLogger.debug("[FontManager] No TTF Font found for font: {}", path.getNamespace() + ":" + path.getPath());
            } catch (Exception e) {
                JCMLogger.error("[FontManager] Failed to read font json: {} ({})", path.getNamespace() + ":" + path.getPath(), e.getMessage());
            }
        });
    }
//
//    public static void loadVanillaFont(String fontIdStr) {
//        Identifier fontId = new Identifier(fontIdStr);
//        loadVanillaFont(new Identifier(fontId.getNamespace(), "font/" + fontId.getPath() + ".json"));
//    }

    public static FontSet getFontSet(Identifier path) {
        return fonts.get(path);
    }
}
