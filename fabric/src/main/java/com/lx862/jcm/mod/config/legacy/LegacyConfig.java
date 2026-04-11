package com.lx862.jcm.mod.config.legacy;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.lx862.jcm.mapping.LoaderImpl;
import com.lx862.jcm.mod.util.JCMLogger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;

@Deprecated
public abstract class LegacyConfig {
    private final Path configPath;
    private final Path migratedPath;

    public LegacyConfig(Path configPath, Path migratedPath) {
        this.configPath = configPath;
        this.migratedPath = migratedPath;
    }

    public void migrate() {
        if(Files.exists(configPath) && !Files.exists(migratedPath)) {
            try {
                JsonObject jsonObject = new JsonParser().parse(String.join("", Files.readAllLines(configPath))).getAsJsonObject();
                fromJson(jsonObject);
                doMigration();
                JCMLogger.info("Config migrated.");
            } catch (Exception e) {
                JCMLogger.error("Error reading the config file: ", e);
            }
        }
    }

    protected abstract void doMigration();

    protected abstract void fromJson(JsonObject jsonObject);

    protected abstract JsonObject toJson();
}
