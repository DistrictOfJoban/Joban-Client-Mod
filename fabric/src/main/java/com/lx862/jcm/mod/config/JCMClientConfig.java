package com.lx862.jcm.mod.config;

import com.lx862.jcm.mapping.LoaderImpl;
import folk.sisby.kaleido.api.ReflectiveConfig;
import folk.sisby.kaleido.lib.quiltconfig.api.annotations.ChangeWarning;
import folk.sisby.kaleido.lib.quiltconfig.api.annotations.Comment;
import folk.sisby.kaleido.lib.quiltconfig.api.annotations.DisplayName;
import folk.sisby.kaleido.lib.quiltconfig.api.values.TrackedValue;

@DisplayName("Joban Client Mod Config")
public class JCMClientConfig extends ReflectiveConfig {
    public static final JCMClientConfig INSTANCE = createToml(LoaderImpl.getConfigPath(), "jsblock", "client", JCMClientConfig.class);

    @Comment("Disable rendering for all JCM's Block Entity to workaround crashes.")
    @Comment("Only disable if you know what you are doing! This is a common issue causing JCM blocks to appear non-functional.")
    public final TrackedValue<Boolean> disableRendering = value(false);

    @Comment("Reveal additional debug logs to aid troubleshooting")
    public final TrackedValue<Boolean> debugMode = value(false);

    @ChangeWarning(folk.sisby.kaleido.lib.quiltconfig.api.metadata.ChangeWarning.Type.Unsafe)
    @Comment("Enable the (now disused) alternate text renderer, which first draws a string onto a texture and render a single quad out to improve performance.")
    @Comment("This has been phased out due to instability and has not been tested since, use at your own risk!")
    public final TrackedValue<Boolean> useAlternateTextRenderer = value(false);

    @Comment("Config related to the scripting feature in JCM")
    public final Scripting scripting = new Scripting();

    public static class Scripting extends Section {
        @Comment("Enable script debug mode to troubleshoot script errors, quick reload and more.")
        public final TrackedValue<Boolean> scriptDebugMode = value(false);

        @Comment("Disable the built-in java class restrictions, allowing scripts to freely use any java package")
        @Comment("NOTE: Disabled by default for security reasons, enable at your own risk!")
        public final TrackedValue<Boolean> disableScriptRestrictions = value(false);

        @Comment("When using print() statement or the Console API in scripts, also output the script file & line the print statement is triggered at.")
        public final TrackedValue<Boolean> showLogSource = value(false);
    }

    public static void init() {
        // Static init
    }
}
