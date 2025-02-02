package com.lx862.jcm.mod.resources;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.lx862.jcm.mod.Constants;
import com.lx862.jcm.mod.util.JCMLogger;
import com.lx862.mtrscripting.scripting.ParsedScript;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import org.mtr.mapping.holder.Identifier;
import org.mtr.mapping.mapper.ResourceManagerHelper;
import org.mtr.mod.Init;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/** Temporary class to hold resource handling regarding MTR/NTE contents, until they are upstreamed */
public class MTRContentResourceManager {
    private static final HashMap<String, ParsedScript> eyecandyScripts = new HashMap<>();

    public static void reload() {
        eyecandyScripts.clear();
        registerEyecandyScripts();
    }

    private static void registerEyecandyScripts() {
        ResourceManagerHelper.readDirectory("eyecandies", (identifier, inputStream) -> {
            if (identifier.getNamespace().equals(Init.MOD_ID_NTE) && identifier.getPath().endsWith(".json")) {
                try(InputStreamReader isp = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
                    final JsonObject jsonObject = JsonParser.parseReader(isp).getAsJsonObject();

                    for(Map.Entry<String, JsonElement> entry : jsonObject.entrySet()) {
                        final String id = entry.getKey();
                        final JsonObject entryObject = entry.getValue().getAsJsonObject();

                        final Map<Identifier, String> scripts = new Object2ObjectArrayMap<>();
                        if (entryObject.has("scriptFiles") || jsonObject.has("scriptTexts")) {
                            if(entryObject.has("scriptFiles")) {
                                JsonArray scriptFilesArray = entryObject.get("scriptFiles").getAsJsonArray();
                                for(int i = 0; i < scriptFilesArray.size(); i++) {
                                    scripts.put(new Identifier(scriptFilesArray.get(i).getAsString()), null);
                                }
                            }

                            if(entryObject.has("scriptTexts")) {
                                JsonArray scriptTextArray = entryObject.get("scriptTexts").getAsJsonArray();
                                for(int i = 0; i < scriptTextArray.size(); i++) {
                                    scripts.put(new Identifier(Constants.MOD_ID, "script_texts/eyecandy/" + id + "/line" + i), scriptTextArray.get(i).getAsString());
                                }
                            }
                        }
                        ParsedScript ps = new ParsedScript("Eyecandy", scripts);
                        eyecandyScripts.put(id, ps);
                    }
                } catch (Exception e) {
                    JCMLogger.error("", e);
                }
            }
        });
    }

    public static ParsedScript getEyecandyScript(String id) {
        return eyecandyScripts.get(id);
    }
}
