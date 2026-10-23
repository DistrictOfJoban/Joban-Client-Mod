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
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * NativeScriptManager — the JNI-free host for C++ script modules.
 *
 * Drop-in companion to JCM v2.3's ScriptManager/Rhino pipeline:
 * resource packs declare native scripts in mtr_custom_resources.json.
 *
 * Since 2.3.0-beta-2.yanyang.2 the C++ modules are declared directly in the
 * scriptLocations array of a script entry — no "language" field, the mod
 * classifies every entry itself (*.js is JavaScript, everything else is a
 * C++ module), and any number of both may be mixed in one entry:
 *
 *   "vehicleScripts": [{
 *       "id": "cst_sp1900_lcd",
 *       "prependExpressions": ["print('Hello world')"],
 *       "scriptLocations": [
 *           "mtr:my_pack/js/sp1900_lcd/main.js",      // JavaScript
 *           "mtr:my_pack/natives/sp1900_train_num"    // C++ (auto-detected)
 *       ],
 *       "input": { "yearEra": 2004 },
 *       "dataFetchMode": "ALL"
 *   }]
 *
 * A scriptLocations entry may take three native forms (identical to the
 * legacy nativeLibrary field):
 *
 *   - "demo:natives/vehicle_status"
 *       Form A — platform-agnostic stem: resolves on EVERY OS via the
 *       natives/{platform}/ convention (see resolveNativeLibraryPath):
 *           -> demo:natives/linux-x64/libvehicle_status.so
 *           -> demo:natives/windows-x64/vehicle_status.dll
 *           -> demo:natives/macos-arm64/libvehicle_status.dylib
 *
 *   - "demo:natives/linux-x64/libvehicle_status.so"
 *       Form B — full path: bound to the OS its extension implies;
 *       other systems skip that entry.
 *
 *   - {"windows": ..., "linux": ..., "macos": ...}
 *       Form C — per-platform map: declare exactly which library runs
 *       on which OS. Keys: "windows" / "linux" / "macos", optionally
 *       arch-specific ("windows-x64", "linux-x64", "macos-x64",
 *       "macos-arm64") which take precedence over the generic key.
 *       Values may again be full paths (with extension) or stems.
 *
 * The pre-beta-2 declarations keep working unchanged: "language": "cpp"
 * with "nativeLibrary"/"nativeLibraries" stays a native-only entry, and
 * those fields may also be combined with .js scriptLocations entries
 * without a language field.
 *
 * The running OS/arch is detected once from os.name / os.arch
 * (see getPlatformKey / getPlatformKeyExact). When the current platform
 * has no entry (Form C without a matching key, or an extension-form
 * string for another OS), the script is NOT executed on this system:
 * a one-line hint goes to latest.log (JCMLogger) and, when JCM's
 * scripting debug mode is enabled, to the in-game debug HUD/chat via
 * the DebugNotifier hook wired by MtrScriptingResourceManager.
 *
 * ... and the SAME mixins that drive JS scripts (RenderVehiclesMixin,
 * RenderEyeCandyMixin, ScriptPIDSPreset) dispatch to this manager when
 * the script entry has native modules — including MIXED entries, where
 * the JS part and the C++ part of one script id run side by side, each
 * capturing into its own replay registry. Draw-call records produced by
 * the module are translated into ScriptRenderManager draw calls, so the
 * capture-and-replay path on the render thread is byte-for-byte identical
 * to the JS route — JS and native scripts coexist in the same world, in
 * the same train, and in the same script entry.
 *
 * Compact snapshot data and draw records historically shared one JNI call
 * per frame; since 2.3.0-beta-2.yanyang.2 (layout v2) the mod contains
 * NO JNI at all — the "Java 与 C++ 通信方式" setting (see NativeChannel)
 * selects between two JNI-free invocation planes: the Panama FFM plane
 * (libraryLookup + downcalls + upcall vtable, JDK 22+) and the
 * shared-memory poller plane (filesystem handshake + shared-page commands
 * + SVC host services + CBQ api re-entry, every JVM from 16 up). The
 * default shared-memory transport keeps one trigger per frame on the FFM
 * plane and not even that on the poller plane.
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
       v5 = JcmStop.route_circular_state (per-stop route CircularState).
       v8 appends direct Java API access and interchange transport modes.
       v9 appends the canonical transport TYPE ids (MAGIC MTR Addon route
       types) to the vehicle header and to every interchange, and removes the
       per-frame snapshot memcpy + ByteBuffer churn on the JNI bridge.
       v10 appends JcmHostServices.defer_free (background reclamation hook)
       plus the bridge's plain-C jcm_* export surface for the Panama FFM
       downcall transport; ABI 9 modules keep their snapshot layout and are
       still served (groupOf maps 10 to the ABI 9 blob). */
    private static final int ABI_VERSION = 10;

    private static boolean supportedAbi(int version) {
        return version == 6 || version == 7 || version == 8 || version == 9 || version == ABI_VERSION;
    }

    /* Route.CircularState mapping (v2 snapshots). */
    public static final int CIRCULAR_NONE = 0;
    public static final int CIRCULAR_CLOCKWISE = 1;
    public static final int CIRCULAR_ANTICLOCKWISE = 2;

    /* Loaded modules keyed by script id.
     *
     * MULTI-MODULE (Yanyang): an id maps to a LIST because MTR's vehicle
     * schema has exactly one `scriptId` per vehicle entry, while a resource
     * pack often needs several C++ libraries to drive one train (a resource pack
     * ports main.js AND overlay.js, which the JS route used to load as two
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

    /** Dispatches the JS-proxy -> module C++ callback: an FFM session uses
     *  the jcm_module_invoke_api downcall, older JVMs the shared-memory
     *  callback queue (drained by the bridge's poller thread). Both are
     *  JNI-free planes. */
    static byte[] apiCallbackDispatch(long function, long user, byte[] parameters) {
        if (PanamaFfmChannel.extendedSurface()) {
            return PanamaFfmChannel.moduleInvokeApi(function, user, parameters);
        }
        return ShmPoller.invokeCallback(function, user, parameters);
    }

    /** Absolute path of the loaded bridge (null when only a bare
     *  System.loadLibrary name resolved). PanamaFfmChannel prefers it for
     *  SymbolLookup.libraryLookup. */
    static String loadedBridgePath;

    private static void noteBridgePath(String path) {
        if (path != null && loadedBridgePath == null) loadedBridgePath = path;
    }

    private static final Map<String, JsonElement> INPUTS = new ConcurrentHashMap<>();
    /** Fresh temp-file counter for module extractions (see load()). */
    private static final java.util.concurrent.atomic.AtomicLong EXTRACT_SEQ = new java.util.concurrent.atomic.AtomicLong();
    private static final Map<String, String> RESOURCE_BASES = new ConcurrentHashMap<>();
    public static void configure(String id, JsonElement input, String base) {
        INPUTS.put(id, input == null ? com.google.gson.JsonNull.INSTANCE : input);
        if(base!=null) RESOURCE_BASES.put(id,base);
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
     * Arch-specific platform key: "windows-x64" | "windows-arm64" |
     * "linux-x64" | "linux-arm64" | "macos-x64" | "macos-arm64".
     * Matches the directory convention used by resolveNativeLibraryPath,
     * resource packs and the bundled bridges under assets/jcm/natives.
     *
     * Linux+aarch64 covers desktop ARM (Raspberry Pi/Asahi), Pojav-style
     * Android launchers (glibc JVM reports os.name=Linux) and JVM-on-iOS
     * sandboxes — those two resolve their real image via the sibling
     * fallback in loadBridge()/siblingLibraryCandidates().
     */
    public static String getPlatformKeyExact() {
        final String arch = System.getProperty("os.arch", "").toLowerCase(Locale.ROOT);
        final boolean arm = arch.contains("aarch64") || arch.contains("arm64");
        final String os = getPlatformKey();
        if (os.equals("macos")) {
            return arm ? "macos-arm64" : "macos-x64";
        }
        return os + (arm ? "-arm64" : "-x64");
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
        NativeApiSession.quiesceAll();
        MODULES.values().forEach(list -> list.forEach(NativeScriptModule::dispose));
        NativeApiSession.reset();
        NativeContentDriver.reset();
        INPUTS.clear(); RESOURCE_BASES.clear();
        MODULES.clear();
        SKIPPED.clear();
        /* Every per-instance native state and every JVM-side resource the old
           modules created is invalid now: the modules they came from are gone,
           so the handles would dangle. */
        NativeVehicleDriver.reset();
        NativeHost.get().reset();
        /* Channel teardown LAST: the driver resets above release their shm
           snapshot windows, so closing the mapping here leaves no live
           wrappers pointing into unmapped memory. */
        NativeChannel.reset();
        /* Actual discovery walks the same mtr_custom_resources.json
           entries as MTRContentResourceManager: scriptLocations entries
           without .js (auto-detected C++), or the legacy language=cpp +
           nativeLibrary/nativeLibraries declarations, and calls load(...)
           below. */
    }

    /** Diagnostics: log the post-discovery module census once per reload. */
    public static void logCensus() {
        final int total = getModules().size();
        if (total == 0 && SKIPPED.isEmpty()) {
            JCMLogger.warn("Native scripts: no module loaded and no declaration skipped — "
                    + "no C++ script location was seen in mtr_custom_resources.json "
                    + "(scriptLocations entries without .js, or \"language\": \"cpp\" + nativeLibrary)");
            return;
        }
        JCMLogger.info("Native scripts: {} module(s) loaded, {} declaration(s) skipped (transport: {})",
                total, SKIPPED.size(), NativeChannel.describe());
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

            /* Extract to a FRESH temp file per load — dlopen needs a real
               file, and reusing a name whose previous extraction may still
               be MAPPED is forbidden: glibc defers a dlclose'd DSO's unload
               while any live thread still has TLS destructors pending from
               it (and ABI 10 deliberately keeps module mappings loaded on
               close as well), so overwriting the file in place would
               rewrite the pages under a live mapping and corrupt the ELF
               tables the next symbol lookup walks — a crash the first
               reload cycle surfaced in the FFM integration test. A per-load
               counter keeps names unique (and old files are simply
               deleteOnExit'd). */
            final String realName = libId.getPath().substring(libId.getPath().lastIndexOf('/') + 1);
            final Path extracted = Files.createTempFile(
                    "jcm_native_" + sanitize(scriptId) + "_" + EXTRACT_SEQ.incrementAndGet() + "_",
                    "_" + realName);
            Files.write(extracted, lib);
            extracted.toFile().deleteOnExit();

            final NativeScriptModule module = new NativeScriptModule(scriptId, extracted.toAbsolutePath().toString(), libraryPath);
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
        } catch (RuntimeException | Error e) {
            /* new Identifier() throws IllegalArgumentException for
               non-identifier chars (uppercase paths, spaces — the
               scriptLocations classification deliberately accepts those
               case-insensitively), and the native open can raise
               UnsatisfiedLinkError. Degrade to a skipped script instead
               of escaping into the resource-pack parse loop, where it
               used to abort every remaining script entry of the pack. */
            JCMLogger.error("Failed to load native script {}: {}", scriptId, e.toString());
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
        if (!NativeScriptModule.isBridgeAvailable()) {
            skipNoBridge(scriptId);
            return null;
        }
        final String resolved = resolvePlatformLibrary(declared);
        if (resolved == null) {
            skipScript(scriptId, declared);
            return null;
        }
        for (String candidate : siblingLibraryCandidates(resolved)) {
            final NativeScriptModule module = load(scriptId, candidate);
            if (module != null) {
                return module;
            }
        }
        /* load() already logged the concrete failure (missing
           file / ABI mismatch); keep it visible in the debug
           overlay too. */
        SKIPPED.put(scriptId, "failed to load " + resolved + " (see latest.log)");
        return null;
    }

    /** Sibling-platform retry list for a resolved library path.
     *
     * Linux+aarch64 hosts may not actually be glibc Linux: JVM-on-iOS
     * sandboxes and some launchers report os.name=Linux while their
     * dynamic loader wants a Mach-O/bionic image. When the primary
     * linux-arm64 library fails to load, retry the same library from
     * the ios-arm64 (.dylib) and android-arm64 directories so a pack
     * that ships all ARM variants works everywhere without per-platform
     * declarations. Every other platform resolves to just itself. */
    private static List<String> siblingLibraryCandidates(String resolved) {
        if (!resolved.contains("/linux-arm64/") || !getPlatformKeyExact().equals("linux-arm64")) {
            return Collections.singletonList(resolved);
        }
        final List<String> out = new ArrayList<>(3);
        out.add(resolved);
        out.add(resolved.replace("/linux-arm64/", "/ios-arm64/").replaceAll("\\.so$", ".dylib"));
        out.add(resolved.replace("/linux-arm64/", "/android-arm64/"));
        return out;
    }

    /**
     * MULTI-MODULE entry point — one script id, N native libraries.
     *
     * MTR's vehicle schema carries a single `scriptId`, but a pack often
     * needs several C++ libraries for one train (our resource pack ports
     * main.js AND overlay.js, which the JS route loaded as two entries
     * of one `scriptLocations` array). Declaring
     *
     *     "vehicleScripts": [{
     *         "id": "demo:vehicle", "language": "cpp",
     *         "nativeLibraries": [
     *             "demo:natives/vehicle_overlay",
     *             "demo:natives/display_overlay"
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
        if (!NativeScriptModule.isBridgeAvailable()) {
            skipNoBridge(scriptId);
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
            NativeScriptModule module = null;
            for (String candidate : siblingLibraryCandidates(resolved)) {
                module = load(scriptId, candidate);
                if (module != null) break;
            }
            if (module != null) {
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

    /* A jar built without -Pnative_bridge_dir (the plain push/PR CI
       build, or a bare `gradlew build`) ships no jcm_native_bridge at
       all. Without the bridge NO C++ module can ever load, so say that
       plainly up front — the generic "no nativeLibrary entry could be
       loaded" census line sends users hunting for a bug in their
       resource pack when the mod jar itself is the thing missing its
       bridge binary. */
    private static void skipNoBridge(String scriptId) {
        final String reason = "native bridge unavailable — this jar contains no "
                + "jcm_native_bridge for " + getPlatformKeyExact()
                + " (built without -Pnative_bridge_dir; use a complete-build/release jar)";
        SKIPPED.put(scriptId, reason);
        JCMLogger.warn("Native script {}: {}. The declared libraries are fine — "
                + "the mod jar itself is missing its bridge binary.", scriptId, reason);
        notifyDebug("Native script " + scriptId + ": " + reason);
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
     *   "example:natives/script"
     *     -> yanyang:natives/linux-x64/libscript.so
     *     -> yanyang:natives/windows-x64/script.dll
     *     -> yanyang:natives/macos-arm64/libscript.dylib
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
        final boolean arm = arch.contains("aarch64") || arch.contains("arm64");
        final String osDir;
        final String ext;
        final String prefix;
        if (os.contains("win")) { osDir = arm ? "windows-arm64" : "windows-x64"; ext = ".dll"; prefix = ""; }
        else if (os.contains("mac") || os.contains("darwin")) {
            osDir = arm ? "macos-arm64" : "macos-x64";
            ext = ".dylib"; prefix = "lib";
        }
        else { osDir = arm ? "linux-arm64" : "linux-x64"; ext = ".so"; prefix = "lib"; }
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
     * @param snapshots per-ABI-group snapshot buffers owned by the vehicle's
     *        driver state; the layout (interchange stride, header size)
     *        differs between ABI 6/7, 8 and 9, so one group cannot read
     *        another group's blob.
     *
     * @param contextFactory supplies (and resets) the per-module
     *        VehicleScriptContext bound to the native API session.
     *
     * The returned frames are in declaration order — the host replays them
     * in that order, which reproduces the JS pipeline's record order for a
     * multi-file `scriptLocations` entry.
     *
     * @return one NativeFrame per module that rendered this frame (possibly
     *         empty), never null.
     */
    public static List<NativeFrame> renderVehicle(String instanceKey, String scriptId,
            VehicleSnapshotBuffers snapshots, com.lx862.mtrscripting.mod.impl.mtr.vehicle.VehicleWrapper wrapper,
            java.util.function.Function<NativeScriptModule,Object> contextFactory) {
        /* The wrapper is BOTH the snapshot source (per-ABI-group marshal) and
           the native API session's `wrapper` root — C++ scripts reach it via
           api.wrapper() exactly like JavaScript does. */
        return render(instanceKey, scriptId, null, snapshots, wrapper,
                MTR_RESOURCE_VEHICLE, contextFactory, wrapper, null);
    }

    public static List<NativeFrame> render(String instanceKey, String scriptId, ByteBuffer snapshotBuf,
            int kind, java.util.function.Function<NativeScriptModule,Object> contextFactory,
            Object wrapper, java.util.function.BiConsumer<NativeScriptModule,Object> after) {
        return render(instanceKey, scriptId, snapshotBuf, null, null, kind, contextFactory, wrapper, after);
    }

    public static List<NativeFrame> render(String instanceKey, String scriptId, ByteBuffer snapshotBuf,
            VehicleSnapshotBuffers snapshots,
            com.lx862.mtrscripting.mod.impl.mtr.vehicle.VehicleWrapper vehicleWrapper,
            int kind, java.util.function.Function<NativeScriptModule,Object> contextFactory,
            Object wrapper, java.util.function.BiConsumer<NativeScriptModule,Object> after) {
        List<NativeFrame> frames = new ArrayList<>();
        List<NativeScriptModule> modules = MODULES.get(scriptId);
        if (modules == null) return frames;
        /* Hoisted once per render call instead of looked up per module per
           frame (two ConcurrentHashMap gets). */
        final JsonElement scriptInput = INPUTS.get(scriptId);
        final String scriptBase = RESOURCE_BASES.get(scriptId);
        final String keyPrefix = instanceKey + "/module/";
        /* Iterate the module list under its own monitor instead of copying
           it — getModules() allocated an unmodifiable ArrayList per call,
           i.e. per vehicle per frame. The list only mutates while a resource
           pack is loading. */
        synchronized (modules) {
            /* Snapshot marshalled at most once per ABI group per render call —
               modules sharing a group share the blob. */
            int builtGroup = 0;
            ByteBuffer builtSnapshot = null;
            for (NativeScriptModule module : modules) {
                if (!typeName(kind).equals(module.scriptType)) continue;
                String moduleKey = keyPrefix + module.nativeHandle;
                Object context = contextFactory.apply(module);
                NativeApiSession.bind(moduleKey, context, wrapper, scriptInput,
                        scriptBase != null ? scriptBase : module.resourceBase);
                NativeHost.get().setPendingInstance(moduleKey);
                ByteBuffer snapshot = snapshotBuf;
                if (kind == MTR_RESOURCE_VEHICLE && vehicleWrapper != null) {
                    /* Per-ABI-group snapshot: ABI 9, 8 and 6/7 modules cannot
                       share one blob (different interchange strides). */
                    if (snapshots != null) {
                        final int group = NativeSnapshot.groupOf(module.abiVersion);
                        if (builtSnapshot == null || builtGroup != group) {
                            builtSnapshot = snapshots.snapshotFor(group, vehicleWrapper);
                            builtGroup = group;
                        }
                        snapshot = builtSnapshot;
                    } else if (module.abiVersion < 8) {
                        /* Historical single-buffer fallback (tests). */
                        snapshot = NativeSnapshot.build(ByteBuffer.allocateDirect(NativeSnapshot.CAPACITY), vehicleWrapper, module.abiVersion);
                    }
                }
                NativeFrame frame = module.render(moduleKey, snapshot, kind);
                if (frame != null) {
                    frame.instanceKey = moduleKey;
                    frame.moduleHandle = module.nativeHandle;
                    frames.add(frame);
                }
                if (after != null) after.accept(module, context);
            }
        }
        return frames;
    }

    private static String typeName(int kind) {
        switch (kind) {
            case MTR_RESOURCE_VEHICLE: return "vehicle";
            case MTR_RESOURCE_PIDS: return "pids";
            case MTR_RESOURCE_EYECANDY: return "eyecandy";
            case MTR_RESOURCE_LIFT: return "lift";
            default: throw new IllegalArgumentException("Unknown native script kind " + kind);
        }
    }

    /**
     * VehicleSnapshotBuffers — one snapshot buffer + reusable builder per
     * ABI layout group, owned by the vehicle driver's per-vehicle state.
     *
     * <p>Vehicles whose modules all target the same ABI (the normal case)
     * marshal exactly one blob per frame. A mixed ABI 9 + ABI 8 script list
     * marshals one blob per distinct group; groups are keyed 7/8/9, so at
     * most three buffers exist per vehicle.
     */
    public static final class VehicleSnapshotBuffers {
        private final Map<Integer, GroupBuffer> groups = new java.util.LinkedHashMap<>();

        public static final class GroupBuffer {
            /* Channel-provided buffer: a window of the shared memory region
               when the shm transport is active (so C++ reads Java's data
               straight out of the shared mapping), a plain direct buffer
               otherwise. Capacity is the snapshot worst case either way. */
            final ByteBuffer buffer = NativeChannel.newSnapshotBuffer(NativeSnapshot.CAPACITY);
            final NativeSnapshot.Builder builder = new NativeSnapshot.Builder();
        }

        /** Marshals the snapshot for one layout group this frame. */
        ByteBuffer snapshotFor(int group, com.lx862.mtrscripting.mod.impl.mtr.vehicle.VehicleWrapper wrapper) {
            GroupBuffer groupBuffer = groups.get(group);
            if (groupBuffer == null) {
                groupBuffer = new GroupBuffer();
                groups.put(group, groupBuffer);
            }
            return NativeSnapshot.build(groupBuffer.buffer, wrapper, group, groupBuffer.builder);
        }

        /** Current content of a group's buffer WITHOUT re-marshalling —
         *  used at dispose time so mtrDispose sees the snapshot the module
         *  actually rendered last, instead of the instance's first frame. */
        ByteBuffer peek(int group) {
            final GroupBuffer groupBuffer = groups.get(group);
            return groupBuffer == null ? null : groupBuffer.buffer;
        }

        /** Frees every group's shm window (plain direct buffers are a
         *  no-op). Called when the owning state dies — vehicle forget or
         *  driver reset — AFTER disposeVehicleInstance, which reads the
         *  live buffers one last time. */
        void release() {
            for (GroupBuffer groupBuffer : groups.values()) {
                NativeChannel.releaseSnapshotBuffer(groupBuffer.buffer);
            }
            groups.clear();
        }
    }

    /**
     * Dispose without a live snapshot: the module then receives its
     * module-owned ZEROED fallback snapshot (a valid ABI struct with zero
     * counts — an "empty" train/PIDS/lift) instead of real data. Prefer
     * the overloads that hand over the actual buffer.
     */
    public static void disposeInstance(String instanceKey, String scriptId) {
        disposeInstance(instanceKey, scriptId, null);
    }

    /** Dispose with the content driver's live snapshot buffer. */
    public static void disposeInstance(String instanceKey, String scriptId, ByteBuffer snapshot) {
        for (NativeScriptModule module : getModules(scriptId)) {
            String moduleKey = instanceKey + "/module/" + module.nativeHandle;
            synchronized (module) {
                NativeApiSession.quiesce(moduleKey);
                try {
                    if (module.nativeHandle != 0) {
                        /* Hand the LIVE buffer this instance was last rendered
                           with: the retained copy in the bridge never refreshed
                           (the reusable buffers keep their address), so dispose()
                           used to receive the instance's first frame forever. */
                        ByteBuffer buffer = snapshot;
                        int len = 0;
                        if (buffer != null) {
                            len = buffer.limit();
                        }
                        module.disposeInstanceNative(moduleKey, buffer, len);
                    }
                } finally {
                    NativeApiSession.dispose(moduleKey);
                    NativeHost.get().releaseInstance(moduleKey);
                    /* The bridge forgot the shm instance id together with the
                       instance ("ids never outlive their instances") — drop
                       the Java cache entry in the same breath, or the next
                       render ships a dead id (ERR_INSTANCE, blank LCD). */
                    NativeChannel.forgetInstance(moduleKey);
                }
            }
        }
    }

    /** Dispose a vehicle instance: each module gets its OWN ABI-group buffer. */
    public static void disposeVehicleInstance(String instanceKey, String scriptId, VehicleSnapshotBuffers snapshots) {
        for (NativeScriptModule module : getModules(scriptId)) {
            String moduleKey = instanceKey + "/module/" + module.nativeHandle;
            synchronized (module) {
                NativeApiSession.quiesce(moduleKey);
                try {
                    if (module.nativeHandle != 0) {
                        ByteBuffer buffer = snapshots == null
                                ? null : snapshots.peek(NativeSnapshot.groupOf(module.abiVersion));
                        int len = buffer == null ? 0 : buffer.limit();
                        module.disposeInstanceNative(moduleKey, buffer, len);
                    }
                } finally {
                    NativeApiSession.dispose(moduleKey);
                    NativeHost.get().releaseInstance(moduleKey);
                    /* Same as disposeInstance: the bridge's shm id dies with
                       the instance, so the Java cache entry must die too. */
                    NativeChannel.forgetInstance(moduleKey);
                }
            }
        }
    }

    /* Resource kind ids, must match MtrResourceKind in mtr_native.h. */
    public static final int MTR_RESOURCE_VEHICLE = 0;
    public static final int MTR_RESOURCE_PIDS = 1;
    public static final int MTR_RESOURCE_EYECANDY = 2;
    public static final int MTR_RESOURCE_LIFT = 3;

    /* ------------------------------------------------------------------ */
    /* shared sweep tuning (NativeVehicleDriver / NativeContentDriver)    */
    /* ------------------------------------------------------------------ */

    /**
     * Idle valve, shared by both driver sweeps.
     *
     * <p>Both sweeps used to dispose a script instance after 5 s without a
     * RENDER. But render hooks only run for things that are actually being
     * drawn: a train behind the camera frustum, or a PIDS on the far side of
     * a wall, is invisible — not gone. The 5 s rule disposed its instance
     * (about a dozen GL textures and 60–90 uploaded quad models for a real
     * LCD script), and the moment it re-entered view one single frame paid
     * the whole rebuild: mtrCreate + the createResources host upcall + a
     * full-canvas repaint upload. Turning the camera was therefore a burst
     * of multi-millisecond hitches with nothing in the log — the "intermittent
     * stutter" reported against yanyang.6.
     *
     * <p>The valve separates the two conditions: an instance is disposed
     * promptly only when its owner is provably gone (vehicle despawned /
     * block-entity removed); an instance that is merely UNRENDERED is kept
     * warm so resuming it costs nothing, and is released only after it has
     * been idle for this long (or, for vehicles, past the warm-instance
     * retention cap below).
     *
     * <p>{@code -Djcm.nativeIdleMillis=<ms>} overrides; {@code 0} disables
     * time-based eviction entirely (instances of living owners are then kept
     * until the owner is removed — JavaScript-script parity).
     */
    static final long IDLE_DISPOSE_NANOS = idleDisposeNanos();

    /**
     * How many vehicle instances may stay warm while unrendered (behind the
     * camera, far away) before the oldest is released. In-view rendering is
     * never capped — only the invisible warm set is. Bounded so a huge world
     * cannot pin unbounded VRAM forever; generous enough that a normal
     * world's whole fleet stays warm and frustum re-entry is always free.
     *
     * <p>{@code -Djcm.nativeIdleMax=<count>} overrides (0 = no cap).
     */
    static final int IDLE_RETAIN_MAX = idleRetainMax();

    private static long idleDisposeNanos() {
        long millis = 90_000L;
        try {
            final String property = System.getProperty("jcm.nativeIdleMillis");
            if (property != null) millis = Long.parseLong(property.trim());
        } catch (Throwable ignored) {
        }
        return millis <= 0 ? Long.MAX_VALUE : Math.min(millis, 3_600_000L) * 1_000_000L;
    }

    private static int idleRetainMax() {
        int max = 32;
        try {
            final String property = System.getProperty("jcm.nativeIdleMax");
            if (property != null) max = Integer.parseInt(property.trim());
        } catch (Throwable ignored) {
        }
        return Math.max(0, Math.min(max, 1024));
    }

    /**
     * NativeFrame — draw-call records + arenas read back from the
     * module. The replay step (render thread) walks the records and
     * creates the same MainRenderer.scheduleRender calls the JS
     * pipeline issues; see native/jni_bridge.cpp for the struct
     * layouts (they are plain C structs over this ByteBuffer).
     *
     * <p>Fields are mutable ON PURPOSE: the shared-memory transport reuses
     * ONE frame object per module and re-points it at each frame's arenas
     * (see NativeChannel.finishFrame), keeping the per-frame hot path at
     * zero allocations. Frames are consumed synchronously by the replay
     * driver before the next render — the same capture-then-replay
     * discipline that bounds the JNI path's cached arena views.</p>
     */
    public static final class NativeFrame {
        /** Host resource owner; set before the frame reaches the replay driver. */
        public String instanceKey;
        /** Owning module's native handle; avoids re-parsing instanceKey. */
        public long moduleHandle;
        public ByteBuffer records;   /* JcmDraw* array */
        public int recordCount;
        public ByteBuffer stringArena;
        public ByteBuffer pixelArena;
        public ByteBuffer matrixArena;
        public ByteBuffer floatArena;

        NativeFrame() {
        }

        NativeFrame(ByteBuffer records, int recordCount, ByteBuffer stringArena,
                    ByteBuffer pixelArena, ByteBuffer matrixArena, ByteBuffer floatArena) {
            assign(records, recordCount, stringArena, pixelArena, matrixArena, floatArena);
        }

        void assign(ByteBuffer records, int recordCount, ByteBuffer stringArena,
                    ByteBuffer pixelArena, ByteBuffer matrixArena, ByteBuffer floatArena) {
            this.records = records;
            this.recordCount = recordCount;
            this.stringArena = stringArena;
            this.pixelArena = pixelArena;
            this.matrixArena = matrixArena;
            this.floatArena = floatArena;
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
        /** Bundled bridge search list: {platformDir, fileName} pairs in
         *  priority order. The first entry matches the native platform;
         *  Linux aarch64 additionally retries ios-arm64 (Mach-O) and
         *  android-arm64 (bionic) because JVM-on-iOS / Android launchers
         *  report os.name=Linux while their dynamic loader needs a
         *  different image format than glibc's ELF. (Declared BEFORE
         *  BRIDGE_AVAILABLE: static initializers run in declaration
         *  order and loadBridge() reads this list.) */
        private static final List<String[]> BRIDGE_SEARCH = buildBridgeSearch();

        private static List<String[]> buildBridgeSearch() {
            final String arch = System.getProperty("os.arch", "").toLowerCase(Locale.ROOT);
            final boolean arm = arch.contains("aarch64") || arch.contains("arm64");
            final String os = getPlatformKey();
            final List<String[]> out = new ArrayList<>(3);
            if (os.equals("windows")) {
                out.add(new String[]{"windows-x64", "jcm_native_bridge.dll"});
            } else if (os.equals("macos")) {
                out.add(new String[]{arm ? "macos-arm64" : "macos-x64", "libjcm_native_bridge.dylib"});
            } else if (arm) {
                out.add(new String[]{"linux-arm64", "libjcm_native_bridge.so"});
                out.add(new String[]{"ios-arm64", "libjcm_native_bridge.dylib"});
                out.add(new String[]{"android-arm64", "libjcm_native_bridge.so"});
            } else {
                out.add(new String[]{"linux-x64", "libjcm_native_bridge.so"});
            }
            return Collections.unmodifiableList(out);
        }

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
            for (final String[] candidate : BRIDGE_SEARCH) {
                final String dir = candidate[0];
                final String fileName = candidate[1];
                final String bundled = "/assets/jcm/natives/" + dir + "/" + fileName;
                try (java.io.InputStream stream = NativeScriptManager.class.getResourceAsStream(bundled)) {
                    if (stream != null) {
                        final String suffix = fileName.substring(fileName.lastIndexOf('.'));
                        final Path extracted = Files.createTempFile("jcm_native_bridge_", suffix);
                        extracted.toFile().deleteOnExit();
                        Files.copy(stream, extracted, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                        final String absolute = extracted.toAbsolutePath().toString();
                        /* PURE-FFM FIRST (JDK 22+): the bridge is driven
                           entirely by java.lang.foreign — libraryLookup +
                           downcalls + upcall vtable. No System.load, so no
                           JNI native-method registration ever runs. */
                        if (PanamaFfmChannel.installFromExtractedPath(absolute)) {
                            noteBridgePath(absolute);
                            JCMLogger.info("Native scripting bridge loaded from bundled {} (Panama FFM, no JNI)", bundled);
                            return true;
                        }
                        /* JDK 16..21: System.load exists ONLY to run the
                           bridge's constructor, which starts the JNI-free
                           poller thread. The library has no JNI exports
                           and this class declares no native methods, so
                           nothing ever binds through the JNI machinery. */
                        System.load(absolute);
                        noteBridgePath(absolute);
                        JCMLogger.info("Native scripting bridge loaded from bundled {} (System.load for the poller constructor; no JNI surface)", bundled);
                        return true;
                    }
                } catch (IOException | UnsatisfiedLinkError e) {
                    JCMLogger.warn("Bundled native scripting bridge {} failed to load: {}", bundled, e.toString());
                }
            }
            try {
                System.loadLibrary("jcm_native_bridge");
                return true;
            } catch (UnsatisfiedLinkError e) {
                /* fall through to the explicit search */
            }

            final List<File> searched = new ArrayList<>();
            for (final String[] candidate : BRIDGE_SEARCH) {
                final List<File> candidates = bridgeCandidates(candidate[1]);
                searched.addAll(candidates);
                for (File file : candidates) {
                    if (!file.isFile()) continue;
                    try {
                        final String absolute = file.getAbsolutePath();
                        /* Same FFM-first treatment for user-placed bridges: a
                           current bridge runs JNI-free via java.lang.foreign,
                           an older JVM loads it for the poller constructor. */
                        if (PanamaFfmChannel.installFromExtractedPath(absolute)) {
                            noteBridgePath(absolute);
                            JCMLogger.info("Native scripting bridge loaded from {} (Panama FFM, no JNI)", file);
                            return true;
                        }
                        System.load(absolute);
                        noteBridgePath(absolute);
                        JCMLogger.info("Native scripting bridge loaded from {} (System.load for the poller constructor; no JNI surface)", file);
                        return true;
                    } catch (UnsatisfiedLinkError e) {
                        JCMLogger.warn("Native scripting bridge {} exists but failed to load: {}",
                                file, e.getMessage());
                    }
                }
            }

            JCMLogger.warn("Native scripting bridge ({}) not found — C++ ('language': 'cpp') "
                    + "scripts cannot run. Drop it into {}{}mods{} (next to the JCM jar) "
                    + "or set -Djava.library.path. Looked in: {}",
                    bridgeFileName(), System.getProperty("user.dir"), File.separator, File.separator,
                    searched);
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
        final String resourceBase;

        NativeScriptModule(String scriptId, String path, String resourceBase) {
            this.resourceBase = resourceBase;
            /* Lifecycle dispatch: an FFM session drives the whole module
               lifecycle through the jcm_module_* downcalls; every other
               JVM drives it through the shared-memory poller commands
               (ShmPoller) — both JNI-free, both landing on the identical C
               open_module/dispose cores. Single-assignment style so every
               final field is definitely assigned on every path. */
            long handle = 0;
            int abi = 0;
            String type = "";
            String id = scriptId;
            long state = 0;
            if (PanamaFfmChannel.ready()) {
                handle = PanamaFfmChannel.moduleOpen(path);
                if (handle != 0) {
                    abi = PanamaFfmChannel.moduleAbi(handle);
                    type = PanamaFfmChannel.moduleScriptType(handle);
                    id = PanamaFfmChannel.moduleScriptId(handle);
                    state = PanamaFfmChannel.moduleStateSize(handle);
                }
            } else if (NativeChannel.ensurePollerPlane()) {
                /* Module open is the FIRST command the poller must serve,
                   so the region (and the poller attach) has to exist
                   before any module loads. */
                final com.lx862.jcm.nativeapi.NativeShm region = ShmPoller.region();
                handle = ShmPoller.openModule(region, path);
                if (handle != 0) {
                    abi = ShmPoller.moduleAbi(region, handle);
                    type = ShmPoller.moduleIdentity(region, handle, NativeShm.CMD_SCRIPT_TYPE);
                    id = ShmPoller.moduleIdentity(region, handle, NativeShm.CMD_SCRIPT_ID);
                    state = ShmPoller.moduleStateSize(region, handle);
                }
            }
            /* handle == 0: open_module already logged the concrete
               dlopen/LoadLibrary failure; the fields stay "unknown" so the
               caller can skip the module instead of NPE-ing. */
            this.nativeHandle = handle;
            this.abiVersion = abi;
            this.scriptType = type;
            this.scriptId = id;
            this.stateSize = state;
            this.loadedPlatform = getPlatformKeyExact();
            this.sourcePath = path;
            if (handle != 0 && !scriptId.equals(this.scriptId)) {
                /* Expected, not a problem: MTR's vehicle schema carries ONE
                   scriptId, so a pack that needs several libraries declares them
                   together under one entry ("demo:vehicle") while each library keeps
                   its own self-describing id ("demo:overlay"). The MODULES
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

        /**
         * Per-instance dispose. Cold (vehicle despawn) path: dispatches to the
         * jcm_module_dispose_instance downcall on an FFM session and to the
         * shared-memory poller command otherwise — both route through the
         * same dispose_instance_core, including the deferred reclamation
         * of the instance's payloads. Called with `this` monitored.
         *
         * TRANSPORT FAILURES ARE CONTAINED HERE: a wedged poller or a broken
         * FFM downcall used to throw IllegalStateException straight into the
         * vehicle-forget / driver-reset path — i.e. a transport hiccup at
         * despawn time could crash the game tick loop. Despawn degrades
         * quietly: the instance is dropped either way, and the bridge's
         * close_module_core sweep disposes leftovers at reload.
         */
        void disposeInstanceNative(String moduleKey, java.nio.ByteBuffer snapshot, int len) {
            if (nativeHandle == 0) return;
            try {
                if (PanamaFfmChannel.ready()) {
                    final long address = snapshot == null ? 0 : NativeArenaView.addressOf(snapshot);
                    PanamaFfmChannel.moduleDisposeInstance(nativeHandle, moduleKey, address,
                            address == 0 ? -1 : len);
                    return;
                }
                final NativeShm region = ShmPoller.region();
                if (region != null && ShmPoller.active()) {
                    ShmPoller.disposeInstance(region, nativeHandle, moduleKey, snapshot, len);
                }
            } catch (Throwable t) {
                JCMLogger.warn("Native dispose for {} degraded ({}); the instance is dropped anyway",
                        moduleKey, t.toString());
            }
        }

        synchronized NativeFrame render(String instanceKey, ByteBuffer snapshot, int resourceKind) {
            if (nativeHandle == 0) return null;
            /* No slice(): the snapshot arrives position 0 / limit = written
               length, so the buffer IS the view — slicing allocated a fresh
               ByteBuffer object per module per frame for nothing. The written
               length is passed explicitly; the bridge no longer infers it
               from the buffer (which would report the full 256 KB capacity). */
            snapshot.order(ByteOrder.nativeOrder());
            /* Transport dispatch (direct call / shared memory / Panama FFM):
               NativeChannel owns the how; everything else in the pipeline
               is transport-agnostic. */
            return NativeChannel.render(this, instanceKey, snapshot, snapshot.limit(), resourceKind);
        }

        synchronized void dispose() {
            if (nativeHandle != 0) {
                /* Same containment as disposeInstanceNative: reload() drives
                   this for every module, so a wedged transport here could
                   abort the whole teardown halfway through. */
                try {
                    if (PanamaFfmChannel.ready()) {
                        PanamaFfmChannel.moduleClose(nativeHandle);
                    } else {
                        final NativeShm region = ShmPoller.region();
                        if (region != null && ShmPoller.active()) {
                            ShmPoller.closeModule(region, nativeHandle);
                        }
                    }
                } catch (Throwable t) {
                    JCMLogger.warn("Native module close degraded ({}); the bookkeeping is dropped",
                            t.toString());
                }
                nativeHandle = 0;
            }
        }
    }
}
