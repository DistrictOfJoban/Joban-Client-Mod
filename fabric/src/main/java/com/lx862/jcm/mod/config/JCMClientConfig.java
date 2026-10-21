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

    @Comment("Config related to the scripting feature in JCM")
    public final Scripting scripting = new Scripting();

    /**
     * "Java 与 C++ 通信方式" — how the JVM exchanges per-frame data with
     * the C++ script modules. Constant names mirror
     * com.lx862.jcm.nativeapi.NativeChannel.Transport one-to-one; the
     * config screen cycles through them and JCMClient maps them onto the
     * channel at boot (and on save). The name "JNI" is kept ONLY for
     * existing config-file compatibility — the option is a direct-call
     * transport that runs JNI-free on every plane.
     */
    public enum JavaCppCommunication {
        /** 快照随每次调用传输（FFM downcall）—— 性能一般，行为最直观；旧 JVM 自动降级共享内存（同样零 JNI） */
        JNI,
        /** 性能中等；JDK 22+ 全程零 JNI（加载/调用/回调全部走 Foreign Function & Memory API），旧 JVM 自动使用共享内存轮询平面（同样零 JNI） */
        PANAMA_FFM,
        /** 性能最好，但小概率出现稳定性问题；两个调用平面（FFM / 轮询）均零 JNI */
        SHARED_MEMORY
    }

    @Comment("Config related to MTR modifications made by JCM")
    public final MTRPatch mtrPatch = new MTRPatch();

    public static class Scripting extends Section {
        @Comment("If true, do not parse any scripts, effectively disabling the scripting feature.")
        @ChangeWarning(folk.sisby.kaleido.lib.quiltconfig.api.metadata.ChangeWarning.Type.RequiresRestart)
        public final TrackedValue<Boolean> skipScriptParsing = value(false);

        @Comment("Enable script debug mode to troubleshoot script errors, quick reload and more.")
        public final TrackedValue<Boolean> scriptDebugMode = value(false);

        @Comment("Disable the built-in java class restrictions, allowing scripts to freely use any java package")
        @Comment("NOTE: Disabled by default for security reasons, enable at your own risk!")
        public final TrackedValue<Boolean> disableScriptRestrictions = value(false);

        @Comment("When using print() statement or the Console API in scripts, also output the script file & line the print statement is triggered at.")
        public final TrackedValue<Boolean> showLogSource = value(false);

        @Comment("How Java exchanges per-frame data with C++ script modules.")
        @Comment("SHARED_MEMORY (default) maps a file-backed region and lets C++ read Java's snapshot straight out of it - fastest, tiny stability risk.")
        @Comment("Every transport now runs JNI-free on every JVM: the FFM plane (libraryLookup + downcalls + upcall vtable, JDK 22+) or the shared-memory poller plane (filesystem handshake + shared-page commands + SVC host services, JDK 16+). The 'JNI' name is kept only so existing config files keep parsing.")
        public final TrackedValue<JavaCppCommunication> javaCppCommunication = value(JavaCppCommunication.SHARED_MEMORY);
    }

    @DisplayName("MTR Enhancements")
    public static class MTRPatch extends Section {
        @Comment("Disable rendering of MTR's rail")
        @Comment("Used to avoid distraction when locating specific blocks beneath the rail, and for performance debugging.")
        public final TrackedValue<Boolean> disableRailRendering = value(false);

        @Comment("Do not show the vehicles you are currently riding.")
        @Comment("Useful for cab-view video recording, where the appearance of the vehicle model may be an obstruction.")
        public final TrackedValue<Boolean> hideRidingVehicle = value(false);

        @Comment("Whether to show platform/siding tooltip when hovered in MTR Dashboard.")
        public final TrackedValue<Boolean> showDashboardTooltip = value(true);

        @Comment("Whether to enable a camera-lowering feature, togglable via sneaking on vehicles.")
        @Comment("Equivalent to MTR 3's sitting feature.")
        public final TrackedValue<Boolean> allowCameraLowering = value(true);

        @Comment("Whether to enable Frustum Culling for Lifts")
        public final TrackedValue<Boolean> enableFrustumCullingLift = value(true);

        @Comment("Whether to enable Frustum Culling for Vehicles")
        public final TrackedValue<Boolean> enableFrustumCullingVehicle = value(true);

        @Comment("Whether to enable Frustum Culling for Rails")
        public final TrackedValue<Boolean> enableFrustumCullingRails = value(true);

        @Comment("Rail Culling Improvements")
        @ChangeWarning(folk.sisby.kaleido.lib.quiltconfig.api.metadata.ChangeWarning.Type.Experimental)
        public final TrackedValue<Boolean> railCullingImprovement = value(false);
    }

    public static void init() {
        // Static init
    }
}
