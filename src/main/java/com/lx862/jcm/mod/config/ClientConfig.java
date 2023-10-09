package com.lx862.jcm.mod.config;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.lx862.jcm.mod.util.JCMLogger;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.mtr.mapping.holder.MinecraftClient;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;

@Environment(EnvType.CLIENT)
public class ClientConfig {
    private static final Path CONFIG_PATH = MinecraftClient.getInstance().getRunDirectoryMapped().toPath().resolve("config").resolve("jsblock.json");
    private static final ConfigStorage configStorage = new ConfigStorage();
    public static final ConfigEntry<Boolean> DISABLE_RENDERING = configStorage.registerConfig("disable_rendering", new ConfigEntry<>(Boolean.class,false, "Disable Rendering", "This disables the rendering of all JCM Blocks"));

    public static void readFile() {
        if(!Files.exists(CONFIG_PATH)) {
            JCMLogger.info("Config not found, generating one!");
            writeFile();
            readFile();
        } else {
            JCMLogger.info("Reading Config...");
            try {
                final JsonObject jsonConfig = new JsonParser().parse(String.join("", Files.readAllLines(CONFIG_PATH))).getAsJsonObject();

                for(Map.Entry<String, ConfigEntry<?>> entry : configStorage.configList.entrySet()) {
                    String key = entry.getKey();
                    ConfigEntry<?> configEntry = entry.getValue();
                    if(jsonConfig.has(key)) {
                        if(configEntry.targetClass == String.class) {
                            configEntry.setValue(jsonConfig.get(key).getAsString());
                        }

                        if(configEntry.targetClass == Boolean.class) {
                            configEntry.setValue(jsonConfig.get(key).getAsBoolean());
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                JCMLogger.warn("Failed to read config file, all config will be left at it's default state.");
            }
        }
    }

    public static boolean writeFile() {
        final JsonObject jsonConfig = new JsonObject();
        for (Map.Entry<String, ConfigEntry<?>> configEntry : configStorage.configList.entrySet()) {
            String key = configEntry.getKey();
            ConfigEntry<?> value = configEntry.getValue();

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
