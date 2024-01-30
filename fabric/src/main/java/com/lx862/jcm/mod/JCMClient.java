package com.lx862.jcm.mod;

import com.lx862.jcm.mod.config.ClientConfig;
import com.lx862.jcm.mod.registry.RegistryHelper;
import com.lx862.jcm.mod.registry.RegistryHelperClient;
import com.lx862.jcm.mod.render.screen.ClientConfigScreen;
import com.lx862.jcm.mod.resources.JCMResourceManager;
import org.mtr.mapping.holder.Screen;
import org.mtr.mapping.holder.ScreenAbstractMapping;
import org.mtr.mapping.registry.EventRegistryClient;
import org.mtr.mapping.registry.RegistryClient;

public class JCMClient {
    public static final RegistryClient REGISTRY_CLIENT = new RegistryClient(RegistryHelper.REGISTRY);
    public static void initializeClient() {
        ClientConfig.readFile();
        RegistryHelperClient.register();

        // TODO: Proper Resource Loading
        EventRegistryClient.registerClientJoin(JCMResourceManager::reload);
    }

    public static ScreenAbstractMapping getClientConfigScreen(Screen previousScreen) {
        return new ClientConfigScreen().withPreviousScreen(previousScreen);
    }
}