package com.lx862.jcm.mod;

import com.lx862.jcm.mod.config.ClientConfig;
import com.lx862.jcm.mod.registry.JCMRegistryClient;
import com.lx862.jcm.mod.render.gui.screen.ClientConfigScreen;
import com.lx862.jcm.mod.resources.mcmeta.McMetaManager;
import com.lx862.jcm.mod.scripting.jcm.JCMScripting;
import com.lx862.jcm.mod.scripting.mtr.MTRScripting;
import org.mtr.mapping.holder.Screen;
import org.mtr.mapping.holder.ScreenAbstractMapping;

public class JCMClient {
    private static final McMetaManager mcMetaManager = new McMetaManager();
    private static final ClientConfig config = new ClientConfig();

    public static void initializeClient() {
        config.read();
        JCMRegistryClient.register();
        JCMScripting.register();
        MTRScripting.register();
    }

    public static ScreenAbstractMapping getClientConfigScreen(Screen previousScreen) {
        return new ClientConfigScreen().withPreviousScreen(previousScreen);
    }

    public static ClientConfig getConfig() {
        return config;
    }

    public static McMetaManager getMcMetaManager() {
        return mcMetaManager;
    }
}