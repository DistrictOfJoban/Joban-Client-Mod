package com.lx862.jcm.mod.resources;

import com.google.common.base.Charsets;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.lx862.jcm.mod.Constants;
import com.lx862.jcm.mod.data.pids.PIDSManager;
import com.lx862.jcm.mod.data.pids.preset.PIDSPresetBase;
import com.lx862.jcm.mod.render.text.font.FontManager;
import com.lx862.jcm.mod.render.text.TextRenderingManager;
import com.lx862.jcm.mod.resources.mcmeta.McMetaManager;
import com.lx862.jcm.mod.util.JCMLogger;
import org.apache.commons.io.IOUtils;
import org.mtr.mapping.holder.Identifier;
import org.mtr.mapping.mapper.ResourceManagerHelper;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;

public class JCMResourceManager {
    private static final Identifier CUSTOM_RESOURCE_PATH = new Identifier(Constants.MOD_ID, "joban_custom_resources.json");

    public static void reload() {
        FontManager.initialize();
        TextRenderingManager.initialize();
        McMetaManager.reset();
        reloadResources();
    }

    public static void reloadResources() {
        PIDSManager.clearPresets();

        ResourceManagerHelper.readAllResources(CUSTOM_RESOURCE_PATH, (inputStream -> {
            try {
                String str = IOUtils.toString(inputStream, Charsets.UTF_8);
                JsonObject jsonObject = new JsonParser().parse(str).getAsJsonObject();
                PIDSManager.loadJson(jsonObject);
            } catch (Exception e) {
                e.printStackTrace();
                JCMLogger.error("Failed to parse custom resource file!");
            }
        }));
    }

    public static void updatePresetInResourcePack(PIDSPresetBase oldPreset, PIDSPresetBase updatedPreset, String packName) {
        File resourcePackFolder = new File(org.mtr.mapping.holder.MinecraftClient.getInstance().getRunDirectoryMapped(), "resourcepacks");
        File jsonFile = resourcePackFolder.toPath().resolve(packName).resolve("assets").resolve(Constants.MOD_ID).resolve("joban_custom_resources.json").toFile();

        int preexistingPreset = -1;
        List<PIDSPresetBase> presets = new ArrayList<>();

        if(jsonFile.exists()) {
            try {
                JsonArray jsonPresets = new JsonParser().parse(new FileReader(jsonFile)).getAsJsonObject().get("pids_images").getAsJsonArray();
                for (int i = 0; i < jsonPresets.size(); i++) {
                    PIDSPresetBase preset = PIDSManager.parsePreset(jsonPresets.get(i).getAsJsonObject());
                    if (preset != null) {
                        // Skip removed preset
                        if(oldPreset.getId().equals(preset.getId()) && updatedPreset == null) continue;

                        // Preset renamed, skip
                        if (oldPreset.getId().equals(preset.getId()) && updatedPreset.getId() != oldPreset.getId()) continue;

                        if(preset.getId().equals(updatedPreset.getId())) {
                            preexistingPreset = i;
                        }
                        presets.add(preset);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if(preexistingPreset != -1) {
            presets.set(preexistingPreset, updatedPreset);
        } else {
            presets.add(updatedPreset);
        }

        writePresetInResourcePack(presets, packName);
    }

    public static void writePresetInResourcePack(List<PIDSPresetBase> presets, String packName) {
        File resourcePackFolder = new File(org.mtr.mapping.holder.MinecraftClient.getInstance().getRunDirectoryMapped(), "resourcepacks");
        File jsonFile = resourcePackFolder.toPath().resolve(packName).resolve("assets").resolve(Constants.MOD_ID).resolve("joban_custom_resources.json").toFile();

        JsonArray presetArray = new JsonArray();
        for(PIDSPresetBase preset : presets) {
            presetArray.add(preset.toJson());
        }

        JsonObject rootObject = new JsonObject();
        rootObject.add("pids_images", presetArray);

        try {
            FileWriter writer = new FileWriter(jsonFile);
            new GsonBuilder().setPrettyPrinting().create().toJson(rootObject, writer);
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
            JCMLogger.error("Failed to save the resource pack json file");
        }
    }
}
