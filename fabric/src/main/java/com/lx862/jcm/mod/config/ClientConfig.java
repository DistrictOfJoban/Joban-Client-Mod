package com.lx862.jcm.mod.config;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.lx862.jcm.mod.util.JCMLogger;
import org.mtr.mapping.holder.MinecraftClient;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;

public class ClientConfig {
    private static final Path CONFIG_PATH = MinecraftClient.getInstance().getRunDirectoryMapped().toPath().resolve("config").resolve("jsblock_client.json");
    public static void readFile() {
        if(!Files.exists(CONFIG_PATH)) {
            JCMLogger.info("Config not found, generating one!");
            writeFile();
            readFile();
        } else {
            JCMLogger.info("Loading client config...");
            try {
                final JsonObject jsonConfig = new JsonParser().parse(String.join("", Files.readAllLines(CONFIG_PATH))).getAsJsonObject();

                for(ConfigEntry entry : ConfigEntry.values()) {
                    if(jsonConfig.has(entry.getKeyName())) {
                        if(entry.is(String.class)) {
                            entry.set(jsonConfig.get(entry.getKeyName()).getAsString());
                        }

                        if(entry.is(Integer.class)) {
                            entry.set(jsonConfig.get(entry.getKeyName()).getAsInt());
                        }

                        if(entry.is(Boolean.class)) {
                            entry.set(jsonConfig.get(entry.getKeyName()).getAsBoolean());
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                JCMLogger.warn("Failed to read config file, config may be left at it's default state.");
            }
        }
    }

    public static boolean writeFile() {
        final JsonObject jsonConfig = new JsonObject();
        for(ConfigEntry entry : ConfigEntry.values()) {
            String key = entry.getKeyName();

            if (entry.is(String.class)) {
                jsonConfig.addProperty(key, entry.getString());
            }

            if (entry.is(Integer.class)) {
                jsonConfig.addProperty(key, entry.getInt());
            }

            if (entry.is(Boolean.class)) {
                jsonConfig.addProperty(key, entry.getBool());
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

    public static void resetConfig() {
        for(ConfigEntry configEntry : ConfigEntry.values()) {
            configEntry.reset();
        }
    }
}
