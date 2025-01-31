package com.lx862.jcm.mod.config;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.lx862.jcm.mod.util.JCMLogger;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;

public abstract class Config {
    private final Path configPath;

    public Config(Path configPath) {
        this.configPath = configPath;
    }

    public void read() {
        if(!Files.exists(configPath)) {
            write();
            read();
        } else {
            try {
                JsonObject jsonObject = new JsonParser().parse(String.join("", Files.readAllLines(configPath))).getAsJsonObject();
                fromJson(jsonObject);
            } catch (Exception e) {
                JCMLogger.error("Error reading the config file: ", e);
                write();
                JCMLogger.warn("Failed to read config file, config may be left at it's default state.");
            }
        }
    }

    public void write() {
        try {
            configPath.getParent().toFile().mkdirs();
            Files.write(configPath, Collections.singleton(new GsonBuilder().setPrettyPrinting().create().toJson(toJson())));
        } catch (Exception e) {
            JCMLogger.error("", e);
        }
    }

    public final void reset() {
        fromJson(new JsonObject());
        write();
    }

    protected abstract void fromJson(JsonObject jsonObject);

    protected abstract JsonObject toJson();
}
