package com.lx862.jcm.mod.resources;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.lx862.jcm.mod.Constants;
import com.lx862.jcm.mod.scripting.mtr.MTRScripting;
import com.lx862.jcm.mod.util.JCMLogger;
import com.lx862.mtrscripting.core.ParsedScript;
import com.lx862.mtrscripting.data.ScriptContent;
import org.apache.commons.io.FilenameUtils;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.mtr.mapping.holder.Identifier;
import org.mtr.mapping.mapper.ResourceManagerHelper;
import org.mtr.mod.Init;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
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
                    final JsonObject rootObject = new JsonParser().parse(isp).getAsJsonObject();

                    if(rootObject.has("model")) {
                        tryRegisterScript(FilenameUtils.getBaseName(identifier.getPath()), rootObject);
                    } else {
                        for (Map.Entry<String, JsonElement> entry : rootObject.entrySet()) {
                            final String id = entry.getKey();
                            final JsonObject entryObject = entry.getValue().getAsJsonObject();

                            tryRegisterScript(id, entryObject);
                        }
                    }
                } catch (Exception e) {
                    JCMLogger.error("", e);
                }
            }
        });
    }

    private static void tryRegisterScript(String id, JsonObject jsonObject) throws Exception {
        final List<ScriptContent> scripts = new ObjectArrayList<>();

        if (jsonObject.has("scriptFiles") || jsonObject.has("scriptTexts")) {
            if(jsonObject.has("scriptFiles")) {
                JsonArray scriptFilesArray = jsonObject.get("scriptFiles").getAsJsonArray();
                for(int i = 0; i < scriptFilesArray.size(); i++) {
                    Identifier scriptLocation = new Identifier(scriptFilesArray.get(i).getAsString());
                    String scriptText = ResourceManagerHelper.readResource(scriptLocation);
                    if(scriptText.isEmpty()) {
                        JCMLogger.warn("Script {}:{} is either missing, or the file content is empty!", scriptLocation.getNamespace(), scriptLocation.getPath());
                        continue;
                    }

                    scripts.add(new ScriptContent(scriptLocation, scriptText));
                }
            }

            if(jsonObject.has("scriptTexts")) {
                JsonArray scriptTextArray = jsonObject.get("scriptTexts").getAsJsonArray();
                for(int i = 0; i < scriptTextArray.size(); i++) {
                    Identifier scriptLocation = new Identifier(Constants.MOD_ID, "script_texts/eyecandy/" + id + "/line" + i);
                    String scriptText = scriptTextArray.get(i).getAsString();
                    scripts.add(new ScriptContent(scriptLocation, scriptText));
                }
            }
        }

        if(!scripts.isEmpty()) {
            ParsedScript ps = MTRScripting.getScriptManager().parseScript("Block", scripts);
            eyecandyScripts.put(id, ps);
        }
    }

    public static ParsedScript getEyecandyScript(String id) {
        return eyecandyScripts.get(id);
    }
}
