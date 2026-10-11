package com.lx862.jcm.nativeapi;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.lx862.jcm.mod.util.JCMLogger;
import org.mtr.mapping.holder.Identifier;
import org.mtr.mapping.mapper.ResourceManagerHelper;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * NativeScriptManager — JNI host for C++ script modules.
 *
 * Drop-in companion to JCM v2.3's ScriptManager/Rhino pipeline:
 * resource packs declare native scripts in mtr_custom_resources.json
 * ...
 *
 *   "vehicleScripts": [{
 *       "id": "demo:kcx_lcd",
 *       "language": "cpp",
 *
 *       // Form A — platform-agnostic stem: resolves on EVERY OS via the
 *       // natives/{platform}/ convention (see resolveNativeLibraryPath):
 *       "nativeLibrary": "demo:natives/kcx_lcd"
 *       //     -> demo:natives/linux-x64/libkcx_lcd.so
 *       //     -> demo:natives/windows-x64/kcx_lcd.dll
 *       //     -> demo:natives/macos-arm64/libkcx_lcd.dylib
 *
 *       // Form B — per-platform map: declare exactly which library runs
 *       // on which OS. Keys: "windows" / "linux" / "macos", optionally
 *       // arch-specific ("windows-x64", "linux-x64", "macos-x64",
 *       // "macos-arm64") which take precedence over the generic key.
 *       // Values may again be full paths (with extension) or stems.
 *       "nativeLibrary": {
 *           "windows":    "demo:natives/windows-x64/kcx_lcd.dll",
 *           "linux":      "demo:natives/linux-x64/libkcx_lcd.so",
 *           "macos-arm64": "demo:natives/macos-arm64/libkcx_lcd.dylib"
 *       }
 *   }]
 *
 * The running OS/arch is detected once from os.name / os.arch
 * (see getPlatformKey / getPlatformKeyExact). When the current platform
 * has no entry (Form B without a matching key, or an extension-form
 * string for another OS), the script is NOT executed on this system:
 * a one-line hint goes to latest.log (JCMLogger) and, when JCM's
 * scripting debug mode is enabled, to the in-game debug HUD/chat via
 * the DebugNotifier hook wired by MtrScriptingResourceManager.
 *
 * ... and the SAME mixins that drive JS scripts (RenderVehiclesMixin,
 * RenderEyeCandyMixin, ScriptPIDSPreset) dispatch to this manager when
 * the script entry is native. Draw-call records produced by the module
 * are translated into ScriptRenderManager draw calls, so the capture-
 * and-replay path on the render thread is byte-for-byte identical to
 * the JS route — JS and native scripts coexist in the same world.
 *
 * Performance model (see native/bench for the measured numbers):
 *   - ONE JNI boundary crossing per frame per instance: the snapshot
 *     is marshalled into a direct ByteBuffer, mtrRender runs, and the
 *     frame records come back in the same call.
 *   - No reflection, no boxing, no per-call Java object allocation on
 *     the script path; the module bump-allocates records internally.
 *
 * This file is reference integration code: it compiles against the
 * MTR/JCM mappings used by JCM v2.3 plus the JNI headers shipped with
 * the JDK (native/jni_bridge.cpp implements the C side).
 */
public final class NativeScriptManager {
    /* Must match MTR_NATIVE_ABI_VERSION in native/include/mtr/mtr_native.h.
       v7 appends resource-font callbacks; the v6 host-table prefix and
       snapshots remain compatible, so previously built v6 modules still load.
       v6 = mtrInit(): the host must construct the per-instance state object
       instead of treating a zeroed block as a valid one. A zeroed block is NOT
       a valid State when State holds std::string / std::vector — libstdc++
       keeps the SSO buffer pointer inline, so a zeroed std::string dereferences
       null (immediate SIGSEGV on Linux/macOS); only MSVC's layout tolerated it.
       v5 = JcmStop.route_circular_state (per-stop route CircularState, so the
       LCD port's 环线检测 can walk the stop list like circular.js does). */
    private static final int ABI_VERSION = 7;

    private static boolean supportedAbi(int version) {
        return version == 6 || version == ABI_VERSION;
    }

    /* Route.CircularState mapping (v2 snapshots). */
    public static final int CIRCULAR_NONE = 0;
    public static final int CIRCULAR_CLOCKWISE = 1;
    public static final int CIRCULAR_ANTICLOCKWISE = 2;

    /* Loaded modules keyed by script id.
     *
     * MULTI-MODULE (Yanyang): an id maps to a LIST because MTR's vehicle
     * schema has exactly one `scriptId` per vehicle entry, while a resource
     * pack often needs several C++ libraries to drive one train (our pack
     * ports main.js AND train_num.js, which the JS route used to load as two
     * entries of the same `scriptLocations` array). Declare them with
     * "nativeLibraries": [ ... ] and every module runs for the same script
     * id; their draw-call frames are concatenated in declaration order, which
     * is the same capture/replay order the JS pipeline produced. */
    private static final Map<String, List<NativeScriptModule>> MODULES = new ConcurrentHashMap<>();

    /* Platform-skipped declarations keyed by script id (reason string).
       Filled by loadFromDeclaration when the running OS/arch has no
       nativeLibrary entry — consumed by validate() messages and the
       debug-mode overlay (MTRScriptDebugOverlay). */
    private static final Map<String, String> SKIPPED = new ConcurrentHashMap<>();

    /* Per-instance state blocks (mirrors JCM's per-instance `state`
       JS object): instanceKey -> zeroed direct ByteBuffer. */
    private static final Map<String, ByteBuffer> INSTANCE_STATES = new ConcurrentHashMap<>();

    private static final int SNAPSHOT_CAPACITY = 16 * 1024; /* generous worst case */

    /**
     * DebugNotifier — pluggable sink for "JCM debug mode" hints. The
     * mtrscripting layer wires this to scriptErrorNotifier (yellow chat
     * line) in MtrScriptingResourceManager.reload(); the debug overlay
     * additionally renders SKIPPED entries directly. Keeps this class
     * free of direct client/gui dependencies.
     */
    public interface DebugNotifier {
        void accept(String message);
    }

    private static volatile DebugNotifier debugNotifier;

    private NativeScriptManager() {}

    private static native void nativeSetHost(NativeHost host);

    private static boolean installBridgeHost() {
        nativeSetHost(NativeHost.get());
        return true;
    }

    /* ------------------------------------------------------------------ */
    /* Platform detection                                                  */
    /* ------------------------------------------------------------------ */

    /** Generic platform key: "windows" | "linux" | "macos". */
    public static String getPlatformKey() {
        final String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        if (os.contains("win")) return "windows";
        if (os.contains("mac") || os.contains("darwin")) return "macos";
        return "linux";
    }

    /**
     * Arch-specific platform key: "windows-x64" | "linux-x64" |
     * "macos-x64" | "macos-arm64". Matches the directory convention
     * used by resolveNativeLibraryPath and resource packs.
     */
    public static String getPlatformKeyExact() {
        final String arch = System.getProperty("os.arch", "").toLowerCase(Locale.ROOT);
        if (getPlatformKey().equals("macos")) {
            return arch.contains("aarch64") || arch.contains("arm64") ? "macos-arm64" : "macos-x64";
        }
        return getPlatformKey() + "-x64";
    }

    /** Human-readable OS description for log/overlay messages. */
    public static String getOsDescription() {
        return System.getProperty("os.name", "?") + " (" + System.getProperty("os.arch", "?") + ")";
    }

    /* ------------------------------------------------------------------ */
    /* Loading                                                             */
    /* ------------------------------------------------------------------ */

    public static void reload() {
        final int before = getModules().size();
        if (before > 0) {
            JCMLogger.info("Native scripts: reloading, dropping {} loaded module(s)", before);
        }
        MODULES.values().forEach(list -> list.forEach(NativeScriptModule::dispose));
        MODULES.clear();
        SKIPPED.clear();
        INSTANCE_STATES.clear();
        /* Every per-instance native state and every JVM-side resource the old
           modules created is invalid now: the modules they came from are gone,
           so the handles would dangle. */
        NativeVehicleDriver.reset();
        NativeHost.get().reset();
        /* Actual discovery walks the same mtr_custom_resources.json
           entries as MTRContentResourceManager, filtering
           language == "cpp", and calls load(...) below. */
    }

    /** Diagnostics: log the post-discovery module census once per reload. */
    public static void logCensus() {
        final int total = getModules().size();
        if (total == 0 && SKIPPED.isEmpty()) {
            JCMLogger.warn("Native scripts: no module loaded and no declaration skipped — "
                    + "no mtr_custom_resources.json entry with \"language\": \"cpp\" was seen");
            return;
        }
        JCMLogger.info("Native scripts: {} module(s) loaded, {} declaration(s) skipped",
                total, SKIPPED.size());
        for (Map.Entry<String, List<NativeScriptModule>> e : MODULES.entrySet()) {
            for (NativeScriptModule m : e.getValue()) {
                JCMLogger.info("  id '{}' <- {} ({})", e.getKey(), m.getScriptId(), m.getSourcePath());
            }
        }
        SKIPPED.forEach((id, why) -> JCMLogger.info("  skipped '{}': {}", id, why));
    }

    /**
     * Extract the native library from the resource pack (resource
     * packs can't be dlopen'd from inside a jar/zip) and load it.
     */
    public static NativeScriptModule load(String scriptId, String libraryPath) {
        if (!NativeScriptModule.isBridgeAvailable()) {
            /* No bridge → no C++ scripts at all. Keep the id in SKIPPED so
               validate() and the debug overlay report it instead of
               claiming the script is "missing". */
            SKIPPED.put(scriptId, "native bridge unavailable (jcm_native_bridge not loaded)");
            return null;
        }
        try {
            final Identifier libId = new Identifier(libraryPath);
            final byte[] lib = readResourceBytes(libId);
            if (lib == null || lib.length == 0) {
                JCMLogger.warn("Native script library {} not found!", libraryPath);
                return null;
            }

            /* Extract to a temp file — dlopen needs a real file. */
            final String realName = libId.getPath().substring(libId.getPath().lastIndexOf('/') + 1);
            final Path extracted = Files.createTempFile("jcm_native_" + sanitize(scriptId) + "_", "_" + realName);
            Files.write(extracted, lib);
            extracted.toFile().deleteOnExit();

            final NativeScriptModule module = new NativeScriptModule(scriptId, extracted.toAbsolutePath().toString());
            if (module.getNativeHandle() == 0) {
                JCMLogger.error("Native script {}: could not open {} (see the bridge log above).",
                        scriptId, libraryPath);
                return null;
            }
            if (!supportedAbi(module.abiVersion)) {
                JCMLogger.error("Native script {} has ABI version {} (host {}), refusing to load.",
                        scriptId, module.abiVersion, ABI_VERSION);
                module.dispose();
                return null;
            }
            addModule(scriptId, module);
            return module;
        } catch (IOException e) {
            JCMLogger.error("Failed to load native script {}: {}", scriptId, e.getMessage());
            return null;
        }
    }

    /** Append one module to a script id's module list (declaration order). */
    private static void addModule(String scriptId, NativeScriptModule module) {
        MODULES.computeIfAbsent(scriptId, k -> Collections.synchronizedList(new ArrayList<>()))
               .add(module);
    }

    private static byte[] readResourceBytes(Identifier id) {
        /* Binary-safe read: readResource() decodes as text (String),
           which corrupts .so/.dll/.dylib bytes through a UTF-8
           round-trip. readAllResources hands us the raw stream. */
        final byte[][] sink = new byte[1][];
        ResourceManagerHelper.readAllResources(id, inputStream -> {
            try (java.io.InputStream in = inputStream) {
                sink[0] = in.readAllBytes();
            } catch (IOException e) {
                JCMLogger.error("Failed to read native library {}: {}", id, e.getMessage());
            }
        });
        return sink[0];
    }

    /**
     * Entry point used by the resource providers: take the raw
     * nativeLibrary JSON declaration, auto-detect the running
     * OS/arch, resolve the concrete library and load it.
     *
     * When this platform is not declared (per-platform map without a
     * matching key, or an extension-form path for another OS), the
     * script is skipped: NOT executed on this system, the reason is
     * recorded in SKIPPED, a hint goes to latest.log, and — when JCM
     * scripting debug mode is on — to the in-game debug interface
     * through the DebugNotifier hook.
     *
     * @return the loaded module, or null when skipped/failed.
     */
    public static NativeScriptModule loadFromDeclaration(String scriptId, JsonElement declared) {
        final String resolved = resolvePlatformLibrary(declared);
        if (resolved == null) {
            skipScript(scriptId, declared);
            return null;
        }
        final NativeScriptModule module = load(scriptId, resolved);
        if (module == null) {
            /* load() already logged the concrete failure (missing
               file / ABI mismatch); keep it visible in the debug
               overlay too. */
            SKIPPED.put(scriptId, "failed to load " + resolved + " (see latest.log)");
        }
        return module;
    }

    /**
     * MULTI-MODULE entry point — one script id, N native libraries.
     *
     * MTR's vehicle schema carries a single `scriptId`, but a pack often
     * needs several C++ libraries for one train (our WR2-A03 pack ports
     * main.js AND train_num.js, which the JS route loaded as two entries
     * of one `scriptLocations` array). Declaring
     *
     *     "vehicleScripts": [{
     *         "id": "wr2a03", "language": "cpp",
     *         "nativeLibraries": [
     *             "mtr:wr2a03/natives/wr2a03_lcd",
     *             "mtr:wr2a03/natives/wr2a03_train_num"
     *         ]
     *     }]
     *
     * loads every library under the same id; each entry may be a
     * platform-agnostic stem, a full path, or a per-platform object —
     * exactly like the singular `nativeLibrary` field. The single
     * `nativeLibrary` field keeps working (it is treated as a one-element
     * list), so existing packs need no change.
     *
     * @return number of modules that loaded successfully.
     */
    public static int loadManyFromDeclaration(String scriptId, JsonElement declared) {
        if (declared == null || declared.isJsonNull()) {
            skipScript(scriptId, declared);
            return 0;
        }
        /* Idempotent on purpose. MtrScriptingResourceManager parses
           mtr_custom_resources.json TWICE per reload — once for the normal file
           and once for the pending-migration variant — so every native
           declaration reaches this method twice. Without the guard each library
           is dlopen'd twice, registered twice, and mtrCreate'd twice against the
           SAME per-instance state block: the debug overlay grew by two entries
           per reload, and the duplicate modules each replayed their draw calls. */
        final List<NativeScriptModule> already = getModules(scriptId);
        if (!already.isEmpty()) {
            JCMLogger.debug("Native script {}: declaration seen again, keeping {} module(s)",
                    scriptId, already.size());
            return already.size();
        }
        final List<JsonElement> entries = new ArrayList<>();
        if (declared.isJsonArray()) {
            for (JsonElement e : declared.getAsJsonArray()) entries.add(e);
        } else {
            entries.add(declared);
        }

        int loaded = 0;
        final List<String> failed = new ArrayList<>();
        for (JsonElement entry : entries) {
            final String resolved = resolvePlatformLibrary(entry);
            if (resolved == null) {
                failed.add(String.valueOf(entry));
                continue;
            }
            if (load(scriptId, resolved) != null) {
                loaded++;
            } else {
                failed.add(resolved);
            }
        }

        if (loaded == 0) {
            SKIPPED.put(scriptId, "no nativeLibrary entry of "
                    + entries.size() + " could be loaded on " + getPlatformKeyExact()
                    + " (see latest.log)");
            notifyDebug("Native script " + scriptId + ": none of the "
                    + entries.size() + " declared libraries loaded — see latest.log");
        } else {
            if (!failed.isEmpty()) {
                /* partial success: the id IS running, but say what is missing */
                JCMLogger.warn("Native script {}: {} of {} libraries loaded; failed/skipped: {}",
                        scriptId, loaded, entries.size(), String.join(", ", failed));
            }
            SKIPPED.remove(scriptId);
        }
        return loaded;
    }

    private static void skipScript(String scriptId, JsonElement declared) {
        final String declaredText = declared == null || declared.isJsonNull()
                ? "(no nativeLibrary declared)"
                : declared.toString();
        final String reason = String.format(
                "no nativeLibrary entry for %s (%s) — the script will not run on this system. " +
                "Add a \"%s\" (or \"%s\") entry to its mtr_custom_resources.json declaration, " +
                "or use a platform-agnostic library path.",
                getPlatformKeyExact(), getOsDescription(), getPlatformKey(), getPlatformKeyExact());
        SKIPPED.put(scriptId, reason);

        /* latest.log — always. */
        JCMLogger.warn("Native script {} skipped on {}: {}", scriptId, getPlatformKeyExact(), reason);
        JCMLogger.warn("  nativeLibrary declaration was: {}", declaredText);

        /* JCM debug mode interface (chat + overlay) — only when the
           debug notifier is wired and debug mode is enabled. */
        notifyDebug(String.format("Native script %s skipped: %s", scriptId, reason));
    }

    private static void notifyDebug(String message) {
        final DebugNotifier notifier = debugNotifier;
        if (notifier != null) {
            notifier.accept(message);
        }
    }

    /** Wire the debug-mode sink; called by MtrScriptingResourceManager. */
    public static void setDebugNotifier(DebugNotifier notifier) {
        debugNotifier = notifier;
    }

    /* ------------------------------------------------------------------ */
    /* Platform-aware declaration resolution                               */
    /* ------------------------------------------------------------------ */

    /**
     * Resolve a nativeLibrary declaration for the RUNNING platform.
     * Accepted forms:
     *
     *  - "ns:path/libfoo.so|.dll|.dylib"  full path: only runs on the
     *    OS implied by the extension; other systems skip the script.
     *  - "ns:path/foo"                    platform-agnostic stem:
     *    expanded via the natives/{platform}/ convention for every OS.
     *  - {"windows": ..., "linux": ..., "macos": ...}  per-platform
     *    map; arch-specific keys ("macos-arm64", ...) win over the
     *    generic key; values may be full paths or stems.
     *
     * @return the concrete resource path for this OS/arch, or null
     *         when this platform is not declared (caller must skip
     *         the script and surface the hint).
     */
    public static String resolvePlatformLibrary(JsonElement declared) {
        if (declared == null || declared.isJsonNull()) {
            return null;
        }
        if (declared.isJsonPrimitive()) {
            final String value = declared.getAsString();
            final String extPlatform = platformOfExtension(value);
            if (extPlatform != null) {
                /* Extension-form: bound to the OS the extension
                   implies — auto-detected, other platforms skip. */
                return getPlatformKey().equals(extPlatform) ? value : null;
            }
            return resolveNativeLibraryPath(value);
        }
        if (declared.isJsonObject()) {
            final JsonObject map = declared.getAsJsonObject();
            /* Arch-specific key first, then the generic key, then
               common aliases. */
            JsonElement hit = map.get(getPlatformKeyExact());
            if (hit == null || !hit.isJsonPrimitive()) {
                hit = map.get(getPlatformKey());
            }
            if ((hit == null || !hit.isJsonPrimitive()) && getPlatformKey().equals("windows")) {
                hit = map.get("win");
            }
            if ((hit == null || !hit.isJsonPrimitive()) && getPlatformKey().equals("macos")) {
                hit = map.has("osx") ? map.get("osx") : map.get("darwin");
            }
            if (hit == null || !hit.isJsonPrimitive() || hit.isJsonNull()) {
                return null;
            }
            final String value = hit.getAsString();
            if (platformOfExtension(value) != null) {
                return value;
            }
            return resolveNativeLibraryPath(value);
        }
        return null;
    }

    /** Map a library file extension to its platform key, or null. */
    private static String platformOfExtension(String path) {
        final String lower = path.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".dll")) return "windows";
        if (lower.endsWith(".dylib")) return "macos";
        if (lower.endsWith(".so")) return "linux";
        return null;
    }

    /**
     * Resolve a platform-agnostic nativeLibrary declaration to the
     * concrete resource path for the running OS/arch. Convention:
     *
     *   "yanyang:natives/jslcd_vehicle"
     *     -> yanyang:natives/linux-x64/libjslcd_vehicle.so
     *     -> yanyang:natives/windows-x64/jslcd_vehicle.dll
     *     -> yanyang:natives/macos-arm64/libjslcd_vehicle.dylib
     *
     * A value that already carries a library extension is used as-is
     * (single-platform packs stay valid).
     */
    public static String resolveNativeLibraryPath(String declared) {
        final String lower = declared.toLowerCase();
        if (lower.endsWith(".so") || lower.endsWith(".dll") || lower.endsWith(".dylib")) {
            return declared;
        }
        final String os = System.getProperty("os.name", "").toLowerCase();
        final String arch = System.getProperty("os.arch", "");
        final String osDir;
        final String ext;
        final String prefix;
        if (os.contains("win")) { osDir = "windows-x64"; ext = ".dll"; prefix = ""; }
        else if (os.contains("mac") || os.contains("darwin")) {
            osDir = "aarch64".equals(arch) ? "macos-arm64" : "macos-x64";
            ext = ".dylib"; prefix = "lib";
        }
        else { osDir = "linux-x64"; ext = ".so"; prefix = "lib"; }
        final int colon = declared.indexOf(':');
        final String ns = colon >= 0 ? declared.substring(0, colon) : "minecraft";
        final String base = colon >= 0 ? declared.substring(colon + 1) : declared;
        final String file = base.substring(base.lastIndexOf('/') + 1);
        final String dir = base.contains("/") ? base.substring(0, base.lastIndexOf('/') + 1) : "";
        return ns + ':' + dir + osDir + '/' + prefix + file + ext;
    }

    private static String sanitize(String id) {
        return id.replaceAll("[^a-zA-Z0-9_]", "_");
    }

    /* ------------------------------------------------------------------ */
    /* Registries (validate() + debug overlay)                             */
    /* ------------------------------------------------------------------ */

    public static boolean isLoaded(String scriptId) {
        return MODULES.containsKey(scriptId);
    }

    public static boolean isSkipped(String scriptId) {
        return SKIPPED.containsKey(scriptId);
    }

    /** Unmodifiable, flattened view of loaded modules (debug overlay). */
    public static List<NativeScriptModule> getModules() {
        final List<NativeScriptModule> out = new ArrayList<>();
        for (List<NativeScriptModule> list : MODULES.values()) {
            synchronized (list) { out.addAll(list); }
        }
        return Collections.unmodifiableList(out);
    }

    /** Loaded modules for one script id, in declaration order (never null). */
    public static List<NativeScriptModule> getModules(String scriptId) {
        final List<NativeScriptModule> list = MODULES.get(scriptId);
        if (list == null) return Collections.emptyList();
        synchronized (list) { return Collections.unmodifiableList(new ArrayList<>(list)); }
    }

    /** Unmodifiable view of skipped declarations (id -> reason). */
    public static Map<String, String> getSkippedScripts() {
        return Collections.unmodifiableMap(SKIPPED);
    }

    /* ------------------------------------------------------------------ */
    /* Per-frame driving (called by the same mixins as JS scripts)        */
    /* ------------------------------------------------------------------ */

    /**
     * One frame of a vehicle script. Mirrors RenderVehiclesMixin: the caller
     * marshals the snapshot ONCE (and skips this call entirely if that fails),
     * then every module registered for the id runs against it.
     *
     * @param snapshotBuf a buffer already filled by
     *        {@link NativeSnapshot#build}; must have been written successfully —
     *        the bridge hands the module the WHOLE capacity, so a partial
     *        snapshot would be read as a valid one.
     *
     * The returned frames are in declaration order — the host replays them
     * in that order, which reproduces the JS pipeline's record order for a
     * multi-file `scriptLocations` entry.
     *
     * @return one NativeFrame per module that rendered this frame (possibly
     *         empty), never null.
     */
    public static List<NativeFrame> renderVehicle(String instanceKey, String scriptId,
                                                   ByteBuffer snapshotBuf) {
        final List<NativeScriptModule> modules = getModules(scriptId);
        if (modules.isEmpty()) return Collections.emptyList();
        if (snapshotBuf == null) return Collections.emptyList();

        /* Tell the host which instance owns the handles it is about to see:
           texture/model handles are per-instance SLOTS, so both the resource
           creation (first frame) and every later upload need the key. */
        final List<NativeFrame> frames = new ArrayList<>(modules.size());
        for (NativeScriptModule module : modules) {
            if (!"vehicle".equals(module.scriptType)) continue;
            // A script entry can contain several libraries. Their texture slots
            // start at zero independently, so ownership must include the module.
            final String moduleInstanceKey = instanceKey + "/module/" + module.nativeHandle;
            NativeHost.get().setPendingInstance(moduleInstanceKey);
            final NativeFrame frame = module.render(moduleInstanceKey, snapshotBuf, MTR_RESOURCE_VEHICLE);
            if (frame != null) {
                frame.instanceKey = moduleInstanceKey;
                frames.add(frame);
            }
        }
        return frames;
    }

    /* Resource kind ids, must match MtrResourceKind in mtr_native.h. */
    public static final int MTR_RESOURCE_VEHICLE = 0;
    public static final int MTR_RESOURCE_PIDS = 1;
    public static final int MTR_RESOURCE_EYECANDY = 2;

    /**
     * NativeFrame — draw-call records + arenas read back from the
     * module. The replay step (render thread) walks the records and
     * creates the same MainRenderer.scheduleRender calls the JS
     * pipeline issues; see native/jni_bridge.cpp for the struct
     * layouts (they are plain C structs over this ByteBuffer).
     */
    public static final class NativeFrame {
        /** Host resource owner; set before the frame reaches the replay driver. */
        public String instanceKey;
        public final ByteBuffer records;   /* JcmDraw* array */
        public final int recordCount;
        public final ByteBuffer stringArena;
        public final ByteBuffer pixelArena;
        public final ByteBuffer matrixArena;

        NativeFrame(ByteBuffer records, int recordCount, ByteBuffer stringArena,
                    ByteBuffer pixelArena, ByteBuffer matrixArena) {
            this.records = records;
            this.recordCount = recordCount;
            this.stringArena = stringArena;
            this.pixelArena = pixelArena;
            this.matrixArena = matrixArena;
        }
    }

    /**
     * VehicleSnapshotBuilder — historical hook.
     *
     * <p>The snapshot is now marshalled by the caller (NativeVehicleDriver)
     * BEFORE {@link #renderVehicle} is reached, so a marshal failure can skip
     * the native call instead of handing mtrRender a half-written buffer. The
     * JNI bridge passes {@code GetDirectBufferCapacity} to the module, i.e. the
     * whole 256 KB block regardless of how much was actually written, so a
     * partial snapshot means the script reads a garbage car_count/stop_count and
     * walks off the end of the blob.
     */
    @Deprecated
    public interface VehicleSnapshotBuilder {
        ByteBuffer build(ByteBuffer out);
    }

    /* ------------------------------------------------------------------ */
    /* Module wrapper                                                      */
    /* ------------------------------------------------------------------ */

    public static final class NativeScriptModule {
        /**
         * The C-side dispatcher lives in ONE shared library
         * (jcm_native_bridge.{dll,so,dylib}), not in each script module.
         *
         * The bridge is a JNI library, so it must live on
         * java.library.path (or be extracted next to the loader) — resource
         * packs cannot System.load() from inside a zip. When it is missing
         * we DO NOT throw: an UnsatisfiedLinkError inside a static
         * initialiser would turn a missing optional file into a
         * NoClassDefFoundError for every caller (including the resource
         * providers that only want to ask isLoaded()). Instead the class
         * loads, the bridge is reported unavailable, and load() returns
         * null with a single actionable log line.
         */
        private static final boolean BRIDGE_AVAILABLE = loadBridge();

        /**
         * Load the JNI bridge.
         *
         * <p><b>Why this is not just {@code System.loadLibrary}:</b> on a modern
         * launcher {@code java.library.path} points at the loader's own natives
         * directory (e.g. {@code .minecraft/versions/fabric-loader-…-natives}),
         * NOT at {@code mods/}. {@code System.loadLibrary} searches only
         * {@code java.library.path}, so a bridge sitting right next to the JCM
         * jar — which is the documented, obvious thing to do — is never found:
         *
         * <pre>UnsatisfiedLinkError: no jcm_native_bridge in java.library.path: …</pre>
         *
         * <p>So try the name first (honours an explicit
         * {@code -Djava.library.path}), then {@code System.load()} the absolute
         * path inside the known real locations. {@code user.dir} is the game
         * instance directory and the JCM jar's own directory is on the code
         * source, which covers both flat {@code mods/} installs and
         * {@code mods/.fabric/} nested ones.
         */
        private static boolean loadBridge() {
            final String bundled = "/assets/jcm/natives/" + getPlatformKeyExact() + "/" + bridgeFileName();
            try (java.io.InputStream stream = NativeScriptManager.class.getResourceAsStream(bundled)) {
                if (stream != null) {
                    final String suffix = bridgeFileName().substring(bridgeFileName().lastIndexOf('.'));
                    final Path extracted = Files.createTempFile("jcm_native_bridge_", suffix);
                    extracted.toFile().deleteOnExit();
                    Files.copy(stream, extracted, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    System.load(extracted.toAbsolutePath().toString());
                    JCMLogger.info("Native scripting bridge loaded from bundled {}", bundled);
                    return installBridgeHost();
                }
            } catch (IOException | UnsatisfiedLinkError e) {
                JCMLogger.warn("Bundled native scripting bridge {} failed to load: {}", bundled, e.toString());
            }
            try {
                System.loadLibrary("jcm_native_bridge");
                return installBridgeHost();
            } catch (UnsatisfiedLinkError e) {
                /* fall through to the explicit search */
            }

            final String fileName = bridgeFileName();
            final List<File> candidates = bridgeCandidates(fileName);
            for (File candidate : candidates) {
                if (!candidate.isFile()) continue;
                try {
                    System.load(candidate.getAbsolutePath());
                    JCMLogger.info("Native scripting bridge loaded from {}", candidate);
                    return installBridgeHost();
                } catch (UnsatisfiedLinkError e) {
                    JCMLogger.warn("Native scripting bridge {} exists but failed to load: {}",
                            candidate, e.getMessage());
                }
            }

            JCMLogger.warn("Native scripting bridge ({}) not found — C++ ('language': 'cpp') "
                    + "scripts cannot run. Drop it into {}{}mods{} (next to the JCM jar) "
                    + "or set -Djava.library.path. Looked in: {}",
                    fileName, System.getProperty("user.dir"), File.separator, File.separator,
                    candidates);
            return false;
        }

        /** Where the bridge may live, in priority order. */
        private static List<File> bridgeCandidates(String fileName) {
            final List<File> out = new ArrayList<>();
            final String sep = File.separator;

            /* 1) the game instance's mods/ — the documented location */
            final String userDir = System.getProperty("user.dir");
            if (userDir != null && !userDir.isEmpty()) {
                out.add(new File(userDir + sep + "mods" + sep + fileName));
                /* some launchers keep ISOLATED mods under .minecraft/mods */
                out.add(new File(userDir + sep + ".minecraft" + sep + "mods" + sep + fileName));
            }

            /* 2) next to the JCM jar itself (flat mods/ install) */
            File jarDir = codeSourceDir();
            if (jarDir != null) {
                out.add(new File(jarDir, fileName));
                /* 3) mods/.fabric/ nested layout: the jar sits one level deeper */
                File parent = jarDir.getParentFile();
                if (parent != null) out.add(new File(parent, fileName));
            }
            return out;
        }

        /** Directory containing the JCM classes/jar, or null when unknown. */
        private static File codeSourceDir() {
            try {
                final java.security.CodeSource cs =
                        NativeScriptManager.class.getProtectionDomain().getCodeSource();
                if (cs == null || cs.getLocation() == null) return null;
                final File loc = new File(cs.getLocation().toURI());
                return loc.isDirectory() ? loc : loc.getParentFile();
            } catch (Throwable t) {
                return null;
            }
        }

        private static String bridgeFileName() {
            if (getPlatformKey().equals("windows")) return "jcm_native_bridge.dll";
            if (getPlatformKey().equals("macos")) return "libjcm_native_bridge.dylib";
            return "libjcm_native_bridge.so";
        }

        /** True when the JNI bridge library loaded (C++ scripts can run). */
        public static boolean isBridgeAvailable() {
            return BRIDGE_AVAILABLE;
        }

        private long nativeHandle;       /* dlopen handle */

        final int abiVersion;
        final String scriptType;
        final String scriptId;
        final long stateSize;
        /** Platform the module was loaded on (for the debug overlay). */
        final String loadedPlatform;
        /** Resolved library resource path (for the debug overlay). */
        final String sourcePath;

        private native long nOpen(String path);
        private native void nClose(long handle);
        private native int nAbiVersion(long handle);
        private native String nScriptType(long handle);
        private native String nScriptId(long handle);
        private native long nStateSize(long handle);
        private native NativeFrame nRender(long handle, String instanceKey,
                                           ByteBuffer snapshot, int resourceKind);

        NativeScriptModule(String scriptId, String path) {
            this.nativeHandle = nOpen(path);
            if (this.nativeHandle == 0) {
                /* nOpen already logged the concrete dlopen/LoadLibrary
                   failure; surface the script fields as unknown so the
                   caller can skip the module instead of NPE-ing. */
                this.abiVersion = 0;
                this.scriptType = "";
                this.scriptId = scriptId;
                this.stateSize = 0;
                this.loadedPlatform = getPlatformKeyExact();
                this.sourcePath = path;
                return;
            }
            this.abiVersion = nAbiVersion(nativeHandle);
            this.scriptType = nScriptType(nativeHandle);
            this.scriptId = nScriptId(nativeHandle);
            this.stateSize = nStateSize(nativeHandle);
            this.loadedPlatform = getPlatformKeyExact();
            this.sourcePath = path;
            if (!scriptId.equals(this.scriptId)) {
                /* Expected, not a problem: MTR's vehicle schema carries ONE
                   scriptId, so a pack that needs several libraries declares them
                   together under one entry ("wr2a03") while each library keeps
                   its own self-describing id ("wr2a03:lcd"). The MODULES
                   registry is keyed by the ENTRY id, which is also what the
                   driver looks up, so the mismatch is by design. Logged at
                   debug level to stop it reading like an error. */
                JCMLogger.debug("Native library {} declares id {} (entry id is {})",
                        sourcePath, this.scriptId, scriptId);
            }
        }

        /** False when the module failed to open or failed the ABI check. */
        public boolean isValid() {
            return nativeHandle != 0 && supportedAbi(abiVersion);
        }

        /** Raw dlopen/LoadLibrary handle (0 = the module failed to open). */
        public long getNativeHandle() {
            return nativeHandle;
        }

        public String getScriptId() {
            return scriptId;
        }

        public String getScriptType() {
            return scriptType;
        }

        public int getAbiVersion() {
            return abiVersion;
        }

        public long getStateSize() {
            return stateSize;
        }

        public String getLoadedPlatform() {
            return loadedPlatform;
        }

        public String getSourcePath() {
            return sourcePath;
        }

        NativeFrame render(String instanceKey, ByteBuffer snapshot, int resourceKind) {
            /* Per-instance state block, zeroed once (mirrors the JS
               `state` object created per ScriptInstance). */
            INSTANCE_STATES.computeIfAbsent(instanceKey,
                    k -> ByteBuffer.allocateDirect((int) Math.max(1, stateSize))
                                   .order(ByteOrder.nativeOrder()));

            return nRender(nativeHandle, instanceKey, snapshot, resourceKind);
        }

        void dispose() {
            if (nativeHandle != 0) {
                nClose(nativeHandle);
                nativeHandle = 0;
            }
        }
    }
}
