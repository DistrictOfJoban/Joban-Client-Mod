package com.lx862.jcm.mod;

import com.lx862.jcm.mod.config.JCMClientConfig;
import com.lx862.jcm.mod.config.legacy.LegacyClientConfig;
import com.lx862.jcm.mod.registry.JCMRegistryClient;
import com.lx862.jcm.mod.gui.screen.ClientConfigScreen;
import com.lx862.jcm.mod.resource.mcmeta.McMetaManager;
import com.lx862.jcm.mod.scripting.JCMScripting;
import com.lx862.jcm.nativeapi.NativeChannel;
import com.lx862.mtrscripting.mod.MTRScriptingModClient;
import org.mtr.mapping.holder.Screen;
import org.mtr.mapping.holder.ScreenAbstractMapping;

public class JCMClient {
    private static final McMetaManager mcMetaManager = new McMetaManager();

    public static void initialize() {
        new LegacyClientConfig().migrate();
        JCMClientConfig.init();
        configureNativeTransportFromConfig();
        MTRScriptingModClient.init();
        JCMRegistryClient.register();
        JCMScripting.register();
    }

    /** Reads the "Java 与 C++ 通信方式" config value into the native
     *  channel. The channel itself degrades gracefully (FFM → shm,
     *  shm → JNI) when the selected transport cannot run. */
    public static void configureNativeTransportFromConfig() {
        NativeChannel.Transport selected;
        try {
            selected = NativeChannel.Transport.valueOf(
                    JCMClientConfig.INSTANCE.scripting.javaCppCommunication.value().name());
        } catch (IllegalArgumentException unknownValue) {
            selected = NativeChannel.DEFAULT;
        }
        NativeChannel.configure(selected);
    }

    public static ScreenAbstractMapping getClientConfigScreen(Screen previousScreen) {
        return new ClientConfigScreen().withPreviousScreen(previousScreen);
    }

    public static McMetaManager getMcMetaManager() {
        return mcMetaManager;
    }
}