package com.lx862.jcm.config;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.minecraft.client.MinecraftClient;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ConfigStorage {
    private static final Path CONFIG_PATH = MinecraftClient.getInstance().runDirectory.toPath().resolve("config").resolve("jsblock.json");
    public static HashMap<String, ConfigEntry> configs = new HashMap<>();

    public static void registerClient() {
        configs.put("disable_rendering", new ConfigEntry<>(false, "Disable Rendering", "This disables the rendering of all JCM Blocks"));
    }

    public static void readFile() {

    }

    public static boolean writeFile() {
        final JsonObject jsonConfig = new JsonObject();
        for (Map.Entry<String, ConfigEntry> configEntry : configs.entrySet()) {
            String key = configEntry.getKey();
            ConfigEntry value = configEntry.getValue();

            if (value.getValue() instanceof String) {
                jsonConfig.addProperty(key, (String) value.getValue());
            }

            if (value.getValue() instanceof Boolean) {
                jsonConfig.addProperty(key, (Boolean) value.getValue());
            }
        }

        try {
            Files.write(CONFIG_PATH, Collections.singleton(new GsonBuilder().setPrettyPrinting().create().toJson(jsonConfig)));
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
