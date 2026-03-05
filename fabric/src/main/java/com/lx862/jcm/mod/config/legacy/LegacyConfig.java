package com.lx862.jcm.mod.config.legacy;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.lx862.jcm.mod.util.JCMLogger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;

@Deprecated
public abstract class LegacyConfig {
    private final Path configPath;

    public LegacyConfig(Path configPath) {
        this.configPath = configPath;
    }

    public void migrate() {
        if(Files.exists(configPath)) {
            try {
                JsonObject jsonObject = new JsonParser().parse(String.join("", Files.readAllLines(configPath))).getAsJsonObject();
                fromJson(jsonObject);
            } catch (Exception e) {
                JCMLogger.error("Error reading the config file: ", e);
            } finally {
                JCMLogger.info("Config migrated, deleting legacy config file.");
                selfDestruct();
            }
        }
    }

    public void write() {
        try {
            Files.createDirectories(configPath.getParent());
            Files.write(configPath, Collections.singleton(new GsonBuilder().setPrettyPrinting().create().toJson(toJson())));
        } catch (IOException e) {
            JCMLogger.error("", e);
        }
    }

    public void selfDestruct() {
        configPath.toFile().delete();
    }

    public final void reset() {
        fromJson(new JsonObject());
        write();
    }

    protected abstract void fromJson(JsonObject jsonObject);

    protected abstract JsonObject toJson();
}
