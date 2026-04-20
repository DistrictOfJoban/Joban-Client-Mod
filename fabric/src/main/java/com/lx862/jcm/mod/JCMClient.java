package com.lx862.jcm.mod;

import com.lx862.jcm.mod.config.JCMClientConfig;
import com.lx862.jcm.mod.config.legacy.LegacyClientConfig;
import com.lx862.jcm.mod.registry.JCMRegistryClient;
import com.lx862.jcm.mod.render.gui.screen.ClientConfigScreen;
import com.lx862.jcm.mod.resource.mcmeta.McMetaManager;
import com.lx862.jcm.mod.scripting.JCMScripting;
import com.lx862.mtrscripting.mod.MTRScriptingMod;
import org.mtr.mapping.holder.Screen;
import org.mtr.mapping.holder.ScreenAbstractMapping;

public class JCMClient {
    private static final McMetaManager mcMetaManager = new McMetaManager();

    public static void initialize() {
        new LegacyClientConfig().migrate();
        JCMClientConfig.init();
        MTRScriptingMod.init();
        JCMRegistryClient.register();
        JCMScripting.register();
    }

    public static ScreenAbstractMapping getClientConfigScreen(Screen previousScreen) {
        return new ClientConfigScreen().withPreviousScreen(previousScreen);
    }

    public static McMetaManager getMcMetaManager() {
        return mcMetaManager;
    }
}