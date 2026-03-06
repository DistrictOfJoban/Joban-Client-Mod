package com.lx862.jcm.mod.config;

import com.lx862.jcm.mapping.LoaderImpl;
import folk.sisby.kaleido.api.ReflectiveConfig;
import folk.sisby.kaleido.lib.quiltconfig.api.annotations.Comment;
import folk.sisby.kaleido.lib.quiltconfig.api.values.TrackedValue;

public class JCMServerConfig extends ReflectiveConfig {
    public static final JCMServerConfig INSTANCE = createToml(LoaderImpl.getConfigPath(), "jsblock", "server", JCMServerConfig.class);

    @Comment("Reveal additional debug logs to aid troubleshooting")
    public final TrackedValue<Boolean> debugMode = value(false);

    @Comment("Disable client request to fetch stops data. For debugging only")
    public final TrackedValue<Boolean> disableScriptStopsFetching = value(false);

    @Comment("Disable client request to fetch MTR data. For debugging only")
    public final TrackedValue<Boolean> disableScriptDataFetching = value(false);

    public static void init() {
        // Static init
    }
}
