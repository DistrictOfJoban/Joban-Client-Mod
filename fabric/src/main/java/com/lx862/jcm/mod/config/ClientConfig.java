package com.lx862.jcm.mod.config;

import com.google.gson.JsonObject;
import com.lx862.jcm.mod.util.JCMLogger;
import org.mtr.mapping.holder.MinecraftClient;

import java.nio.file.Path;

public class ClientConfig extends Config {

    private static final Path CONFIG_PATH = MinecraftClient.getInstance().getRunDirectoryMapped().toPath().resolve("config").resolve("jsblock_client.json");
    public boolean disableRendering;
    public boolean debug;
    public boolean useNewTextRenderer;
    public boolean disableScriptingRestriction;

    public ClientConfig() {
        super(CONFIG_PATH);
    }

    @Override
    public void fromJson(JsonObject jsonConfig) {
        JCMLogger.info("Loading client config...");
        this.disableRendering = jsonConfig.get("disable_rendering").getAsBoolean();
        this.debug = jsonConfig.get("debug_mode").getAsBoolean();
        this.useNewTextRenderer = jsonConfig.get("new_text_renderer").getAsBoolean();
        this.disableScriptingRestriction = jsonConfig.get("disable_scripting_restriction") != null && jsonConfig.get("disable_scripting_restriction").getAsBoolean();
    }

    @Override
    public JsonObject toJson() {
        JCMLogger.info("Writing client config...");
        final JsonObject jsonConfig = new JsonObject();
        jsonConfig.addProperty("disable_rendering", disableRendering);
        jsonConfig.addProperty("debug_mode", debug);
        jsonConfig.addProperty("new_text_renderer", useNewTextRenderer);
        jsonConfig.addProperty("disable_scripting_restriction", disableScriptingRestriction);
        return jsonConfig;
    }

    public boolean newTextRenderer() {
        return this.useNewTextRenderer;
    }
}
