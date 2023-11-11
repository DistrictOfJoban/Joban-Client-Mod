package com.lx862.jcm.mod;

import com.lx862.jcm.mod.config.ClientConfig;
import com.lx862.jcm.mod.gui.ClientConfigScreen;
import com.lx862.jcm.mod.registry.Registry;
import com.lx862.jcm.mod.render.data.McMetaManager;
import org.mtr.mapping.holder.Identifier;
import org.mtr.mapping.holder.Screen;
import org.mtr.mapping.holder.ScreenAbstractMapping;
import org.mtr.mapping.registry.EventRegistryClient;

public class JCMClient {
    public static void initializeClient() {
        ClientConfig.readFile();
        Registry.registerClient();

        EventRegistryClient.registerClientJoin(() -> {
            // TODO: Proper Resource Loading
//            McMetaManager.load(new Identifier(Constants.MOD_ID, "textures/block/pids/t1.mcmeta"));
        });
    }

    public static ScreenAbstractMapping getClientConfigScreen(Screen previousScreen) {
        return new ClientConfigScreen().withPreviousScreen(previousScreen);
    }
}