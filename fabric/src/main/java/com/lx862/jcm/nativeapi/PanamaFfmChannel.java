package com.lx862.jcm.nativeapi;

import com.lx862.jcm.mod.util.JCMLogger;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandleProxies;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

/**
 * PanamaFfmChannel — the Foreign Function &amp; Memory API ("Panama FFM")
 * transport: the bridge's COMPLETE JNI-free invocation plane.
 *
 * <h2>What it replaces</h2>
 * Every JNI native the mod ever declared has a plain-C twin export in the
 * bridge, and this channel links all of them through
 * {@code java.lang.foreign} downcalls — on a capable JDK the pipeline
 * never touches a JNIEnv at all:
 *
 * <ul>
 *   <li><b>Loading</b> — the bridge is {@code SymbolLookup.libraryLookup}'d
 *       straight from the extracted file. No {@code System.load}, so no
 *       JNI native-method registration and no {@code JNI_OnLoad}: the
 *       legacy plane stays completely inert.</li>
 *   <li><b>Module lifecycle</b> — open / identity strings / state size /
 *       per-instance dispose / close ride {@code jcm_module_*} downcalls
 *       (once per reload and once per instance lifetime; cold paths).</li>
 *   <li><b>Per-frame</b> — the zero-argument {@code jcm_shm_kick} downcall
 *       under the shared-memory transport (adapted once into a plain Java
 *       interface through {@link MethodHandleProxies}, so the hot path
 *       performs no varargs boxing at all), or {@code jcm_module_render}
 *       when the "JNI" transport is selected on a JNI-less session.</li>
 *   <li><b>Host upcalls</b> — the seven script→Java host services (model
 *       acquire/release, text rasterize, font measure/draw, resource
 *       creation, API invocation) are {@code Linker.upcallStub} targets
 *       ({@link PanamaFfmUpcalls}) handed to the bridge as a
 *       {@code JcmFfmHostVTable} via {@code jcm_ffm_init}. The bridge
 *       prefers the vtable on every host call, so even resource and API
 *       traffic never re-enters Java through JNI.</li>
 *   <li><b>API callbacks</b> — {@code jcm_module_invoke_api} replaces the
 *       {@code nativeApiCallback} native for JS-side proxies calling into
 *       module C++ functions.</li>
 * </ul>
 *
 * <h2>Availability and fallbacks</h2>
 * The final API needs JDK 22+ ({@code java.lang.foreign} became final in
 * 22; the 21 preview is opted in for the test harness with
 * {@code --enable-preview} plus {@code -Djcm.ffm.allow_preview=true}).
 * {@link #installFromExtractedPath} is attempted FIRST by the bridge
 * loader: when it succeeds, the session is pure FFM and nothing ever
 * binds the JNI natives. When it cannot run (JDK 16..21, or a
 * pre-ABI-10 bridge binary without the {@code jcm_*} exports), the
 * loader falls back to {@code System.load} + the legacy JNI natives, and
 * this channel still tries to link the DOWNCALL half
 * ({@link #attemptInstall}) so the shm control plane upgrades where the
 * runtime allows — with the upcall vtable as a best-effort bonus on top
 * of the (also installed) JNI host.
 *
 * <h2>How it stays Java-17-compatible</h2>
 * Minecraft 1.17.1-1.20.4 runs on Java 16/17, where
 * {@code java.lang.foreign} does not exist. This class therefore
 * references NO FFM type at compile time: every FFM class, method and
 * constant is resolved reflectively once, at install time, including the
 * shape-varying members ({@code lookup/find}, {@code allocate} 1-/2-arg,
 * {@code libraryLookup}'s scope parameter, {@code upcallStub}'s arena
 * parameter).
 *
 * <p>Descriptor shapes avoid every <em>restricted</em> FFM method on the
 * DATA paths (no {@code MemorySegment.ofAddress}, no {@code reinterpret},
 * no {@code getUtf8String}): raw addresses travel as {@code JAVA_LONG}
 * (register-identical to a pointer argument on every supported platform
 * ABI), and the Java-held byte buffers that cross the boundary as real
 * {@code ADDRESS} arguments are short-lived confined-{@code Arena}
 * segments. The only restricted calls left are the machinery itself —
 * {@code libraryLookup}, {@code downcallHandle}, {@code upcallStub},
 * {@code MemorySegment.address} for the stub pointers — all cold
 * install-time or lifecycle-time calls, and the upcall adapters reach raw
 * memory through {@code sun.misc.Unsafe} helpers
 * ({@link NativeArenaView}) exactly like the shared-memory accessors.
 */
final class PanamaFfmChannel {

    /* ------------------------------------------------------------------ */
    /* state                                                               */
    /* ------------------------------------------------------------------ */

    /** Set once install() succeeds; sticky for the session. */
    private static volatile boolean ready;
    /** One-line reason when not ready (null after a successful install). */
    private static volatile String unavailableReason = "not attempted yet";
    /** True when the upcall vtable is installed in the bridge. */
    private static volatile boolean hostInstalled;
    /** One-line reason when the vtable is missing (downcall-only mode). */
    private static volatile String hostUnavailableReason = "not attempted yet";
    /** True once jcm_module_render / jcm_module_invoke_api / jcm_ffm_init
     *  linked (the extended surface — see linkDowncalls). */
    private static volatile boolean moduleSurfaceReady;

    /* FFM machinery — all null until install. */
    private static Object linker;                    /* Linker */
    private static Object JAVA_INT;                  /* ValueLayout.JAVA_INT  */
    private static Object JAVA_LONG;                 /* ValueLayout.JAVA_LONG */
    private static Object JAVA_DOUBLE;              /* ValueLayout.JAVA_DOUBLE */
    private static Object JAVA_ADDRESS;              /* ValueLayout.ADDRESS   */
    private static Method mDescriptorOf;            /* FunctionDescriptor.of(MemoryLayout, MemoryLayout...)      */
    private static Method mDescriptorOfVoid;        /* FunctionDescriptor.ofVoid(MemoryLayout...)                */
    private static Method mDowncallBound;           /* Linker.downcallHandle(MemorySegment, FunctionDescriptor, Linker.Option...) */
    private static Method mLookup;                   /* SymbolLookup.lookup(String) (22+) / find(String) (21)     */
    private static Method mArenaOfConfined;         /* Arena.ofConfined()                                        */
    private static Method mArenaAllocate;           /* Arena.allocate(long) (22+)                                */
    private static Method mArenaAllocateAligned;    /* Arena.allocate(long size, long alignment) (21 preview)     */
    private static Method mArenaClose;              /* Arena.close()                                             */
    private static Method mSegmentAsByteBuffer;     /* MemorySegment.asByteBuffer()                              */
    private static Method mSegmentAddress;           /* MemorySegment.address() — restricted; stub pointers only   */
    private static Method mSegmentOfBuffer;          /* MemorySegment.ofBuffer(ByteBuffer) — unrestricted          */
    private static Method mUpcallStub;               /* Linker.upcallStub(MethodHandle, FunctionDescriptor, Arena) */
    private static Object EMPTY_OPTIONS;             /* new Linker$Option[0] — the downcall varargs tail           */

    /* Downcall handles, one per bridge export. */
    private static MethodHandle hShmInit;
    private static MethodHandle hShmKick;
    private static MethodHandle hShmRegisterInstance;
    private static MethodHandle hShmShutdown;
    private static MethodHandle hReclaimerPending;
    private static MethodHandle hReclaimerQuiesce;
    private static MethodHandle hModuleOpen;
    private static MethodHandle hModuleClose;
    private static MethodHandle hModuleAbi;
    private static MethodHandle hModuleScriptType;
    private static MethodHandle hModuleScriptId;
    private static MethodHandle hModuleStateSize;
    private static MethodHandle hModuleDisposeInstance;
    private static MethodHandle hModuleRender;
    private static MethodHandle hModuleInvokeApi;
    private static MethodHandle hFfmInit;

    /* Hot-path kick, adapted ONCE into a plain interface — zero per-frame
       MethodHandle.invokeWithArguments cost. MethodHandleProxies demands a
       PUBLIC interface, hence the modifier (access stays package-confinable
       via the enclosing package-private class). */
    public interface KickCall {
        int kick();
    }

    private static KickCall kickCall;

    /* Session-scoped shared arena: backs the library lookup (the bridge
       stays dlopen'd for the process — its modules reference its code) and
       every upcall stub. Never closed. */
    private static Object sharedArena;

    private static final Object[] EMPTY_ARGS = {};
    private static ByteBuffer identityScratch;
    /* The same buffer wrapped once as a MemorySegment (downcall ADDRESS
       arguments must be segments, not ByteBuffers). */
    private static Object identitySegment;
    /* Per-thread reply-length slot for jcm_module_invoke_api. */
    private static final ThreadLocal<ByteBuffer> INVOKE_LEN =
            ThreadLocal.withInitial(() -> ByteBuffer.allocateDirect(16).order(ByteOrder.nativeOrder()));

    /** Layout of the JcmFfmHostVTable (mtr/ffm_host.h): abi + 7 pointers. */
    private static final int FFM_HOST_ABI = 1;
    private static final int VTABLE_BYTES = 64;

    private PanamaFfmChannel() {
    }

    /* ------------------------------------------------------------------ */
    /* availability + install                                              */
    /* ------------------------------------------------------------------ */

    /**
     * Runtime pre-filter: the final {@code java.lang.foreign} API needs
     * JDK 22+. The preview API (21) can be opted in explicitly for test
     * harnesses via {@code -Djcm.ffm.allow_preview=true} (which still
     * requires running the JVM with {@code --enable-preview}).
     */
    static boolean supportedRuntime() {
        try {
            final int feature = Runtime.version().feature();
            return feature >= 22 || (feature == 21 && Boolean.getBoolean("jcm.ffm.allow_preview"));
        } catch (Throwable t) {
            return false;
        }
    }

    static boolean ready() {
        return ready;
    }

    /** True when the upcall vtable is live (host services without JNI). */
    static boolean hostReady() {
        return hostInstalled;
    }

    /** True when the EXTENDED surface (direct render, api-callback downcall
     *  and the host vtable install entry) linked: callers whose legacy twins
     *  are only usable after System.load gate on THIS, not on
     *  {@link #ready()} — a mixed-mode session with an older bridge keeps
     *  nRender/nativeApiCallback alive for exactly those calls. */
    static boolean extendedSurface() {
        return moduleSurfaceReady;
    }

    /** One-line reason for the config/census log when not ready. */
    static String reason() {
        final String why = unavailableReason;
        return why == null ? "unknown" : why;
    }

    /** One-line reason when only the upcall half is missing. */
    static String hostReason() {
        final String why = hostUnavailableReason;
        return why == null ? "unknown" : why;
    }

    /**
     * PURE-FFM install: called by the bridge loader BEFORE any
     * System.load, with the freshly extracted bridge file. On success
     * the bridge is fully driven from this channel and the JNI natives
     * are never bound. The host vtable is REQUIRED here — without it a
     * System.load-less session could not serve host services at all.
     */
    static boolean installFromExtractedPath(String bridgePath) {
        if (ready) return true;
        if (!supportedRuntime()) {
            unavailableReason = "requires JDK 22+ (java.lang.foreign)";
            return false;
        }
        try {
            resolveFfmApi();
            final Object lookup = libraryLookupFromPath(bridgePath);
            /* Pure FFM REQUIRES the full surface: without jcm_module_render
               and the host vtable a System.load-less session could not
               render frames or serve script->Java calls at all. */
            if (!linkDowncalls(lookup, true)) {
                /* linkDowncalls recorded the concrete failure — surface it
                   verbatim instead of masking it behind a generic guess. */
                throw new IllegalStateException(unavailableReason);
            }
            installHostVtable();   /* throws when the stubs cannot be created */
            ready = true;
            unavailableReason = null;
            JCMLogger.info("Panama FFM transport ready — bridge driven entirely by java.lang.foreign (no JNI)");
            return true;
        } catch (Throwable t) {
            ready = false;
            unavailableReason = trimToReason(t);
            JCMLogger.warn("Panama FFM transport unavailable ({}); falling back to the JNI bridge",
                    unavailableReason);
            return false;
        }
    }

    /**
     * Mixed-mode install after System.load succeeded (old JDK or an
     * installFromExtractedPath failure): links the DOWNCALL half on the
     * already-loaded bridge — loaderLookup sees it — and adds the upcall
     * vtable as a best-effort upgrade on top of the JNI host. Failure of
     * the downcalls degrades the whole channel to the legacy natives with
     * a one-line reason.
     */
    static void attemptInstall() {
        if (ready) return;
        try {
            if (!supportedRuntime()) {
                unavailableReason = "requires JDK 22+ (java.lang.foreign)";
                return;
            }
            resolveFfmApi();
            final Object lookup = openLookupOnLoadedBridge();
            /* Mixed mode tolerates a bridge WITHOUT the extended surface
               (jcm_module_render / jcm_module_invoke_api / jcm_ffm_init,
               added after the first ABI 10 bridges): such a pairing keeps
               the e4ee023-era behaviour — FFM kick + JNI everything else. */
            if (!linkDowncalls(lookup, false)) {
                return;
            }
            if (moduleSurfaceReady) {
                try {
                    installHostVtable();
                } catch (Throwable t) {
                    hostUnavailableReason = trimToReason(t);
                    /* Downcalls alone still upgrade the shm control plane;
                       the JNI host keeps serving the script->Java services. */
                    JCMLogger.warn("Panama FFM host upcalls unavailable ({}); script->Java services stay on the JNI host",
                            hostUnavailableReason);
                }
            }
            ready = true;
            unavailableReason = null;
            JCMLogger.info("Panama FFM downcalls ready{}",
                    hostInstalled ? " (host upcalls too — no JNI on the frame path)"
                            : moduleSurfaceReady ? "" : " (legacy bridge: render/api stay on the JNI plane)");
        } catch (Throwable t) {
            ready = false;
            unavailableReason = trimToReason(t);
            JCMLogger.warn("Panama FFM transport unavailable ({}); the config option falls back to the JNI-triggered shared-memory channel",
                    unavailableReason);
        }
    }

    /* ------------------------------------------------------------------ */
    /* reflective FFM API                                                  */
    /* ------------------------------------------------------------------ */

    /**
     * Reflectively resolves the FFM classes, methods and constants. The
     * shapes used here are shared by the 21-preview and the 22+ final
     * API; where they differ (lookup/find, allocate 1-arg/2-arg,
     * upcallStub's third parameter) both variants are tried and the first
     * that exists is kept.
     */
    private static void resolveFfmApi() throws ReflectiveOperationException {
        final Class<?> linkerCls = Class.forName("java.lang.foreign.Linker");
        final Class<?> lookupCls = Class.forName("java.lang.foreign.SymbolLookup");
        final Class<?> valueCls = Class.forName("java.lang.foreign.ValueLayout");
        final Class<?> layoutCls = Class.forName("java.lang.foreign.MemoryLayout");
        final Class<?> descriptorCls = Class.forName("java.lang.foreign.FunctionDescriptor");
        final Class<?> segmentCls = Class.forName("java.lang.foreign.MemorySegment");
        final Class<?> arenaCls = Class.forName("java.lang.foreign.Arena");
        final Class<?> optionCls = Class.forName("java.lang.foreign.Linker$Option");

        linker = linkerCls.getMethod("nativeLinker").invoke(null);
        mDescriptorOf = descriptorCls.getMethod("of", layoutCls, layoutCls.arrayType());
        mDescriptorOfVoid = descriptorCls.getMethod("ofVoid", layoutCls.arrayType());
        /* Bound downcall with the Linker.Option... varargs tail — present in
           both the 21 preview and the 22+ final API. */
        mDowncallBound = linkerCls.getMethod("downcallHandle", segmentCls, descriptorCls,
                optionCls.arrayType());
        EMPTY_OPTIONS = java.lang.reflect.Array.newInstance(optionCls, 0);
        mLookup = methodOrNull(lookupCls, "lookup", String.class);
        if (mLookup == null) mLookup = methodOrNull(lookupCls, "find", String.class);
        if (mLookup == null) throw new NoSuchMethodException("SymbolLookup.lookup/find(String)");
        mArenaOfConfined = arenaCls.getMethod("ofConfined");
        mArenaAllocate = methodOrNull(arenaCls, "allocate", long.class);
        mArenaAllocateAligned = methodOrNull(arenaCls, "allocate", long.class, long.class);
        if (mArenaAllocate == null && mArenaAllocateAligned == null) {
            throw new NoSuchMethodException("Arena.allocate(long)/(long,long)");
        }
        mArenaClose = arenaCls.getMethod("close");
        mSegmentAsByteBuffer = segmentCls.getMethod("asByteBuffer");
        mSegmentAddress = segmentCls.getMethod("address");
        /* ofBuffer: (ByteBuffer) in older previews, (Buffer) since 21 —
           resolve by shape so both spellings work. */
        for (Method m : segmentCls.getMethods()) {
            if (!m.getName().equals("ofBuffer") || !Modifier.isStatic(m.getModifiers())) continue;
            final Class<?>[] p = m.getParameterTypes();
            if (p.length == 1 && p[0].isAssignableFrom(java.nio.ByteBuffer.class)) {
                mSegmentOfBuffer = m;
                break;
            }
        }
        if (mSegmentOfBuffer == null) {
            throw new NoSuchMethodException("MemorySegment.ofBuffer(*Buffer)");
        }
        /* upcallStub(MethodHandle, FunctionDescriptor, <scope>[, Option...]):
           an INSTANCE method of the Linker (never static), with
           (MH, FD, SegmentScope) on the 21 GA preview, (MH, FD, Arena) on
           22+, and some builds adding a Linker$Option... varargs tail —
           resolve by shape so every spelling works. */
        for (Method m : linkerCls.getMethods()) {
            if (!m.getName().equals("upcallStub") || Modifier.isStatic(m.getModifiers())) continue;
            final Class<?>[] p = m.getParameterTypes();
            final int fixed = m.isVarArgs() ? p.length - 1 : p.length;
            if (fixed < 3 || p[0] != MethodHandle.class || p[1] != descriptorCls
                    || !p[2].isAssignableFrom(arenaCls)) continue;
            mUpcallStub = m;
            break;
        }
        if (mUpcallStub == null) {
            throw new NoSuchMethodException("Linker.upcallStub(MethodHandle, FunctionDescriptor, Arena)");
        }

        JAVA_INT = valueCls.getField("JAVA_INT").get(null);
        JAVA_LONG = valueCls.getField("JAVA_LONG").get(null);
        JAVA_DOUBLE = valueCls.getField("JAVA_DOUBLE").get(null);
        JAVA_ADDRESS = valueCls.getField("ADDRESS").get(null);
        if (JAVA_INT == null || JAVA_LONG == null || JAVA_DOUBLE == null || JAVA_ADDRESS == null) {
            throw new IllegalStateException("ValueLayout constants missing");
        }
    }

    private static Method methodOrNull(Class<?> owner, String name, Class<?>... params) {
        try {
            return owner.getMethod(name, params);
        } catch (NoSuchMethodException e) {
            return null;
        }
    }

    /** Allocates {@code size} raw bytes in an arena, on whichever allocate
     *  form this JDK exposes (1-arg on 22+, (size, alignment) on the 21
     *  preview where Arena does not extend SegmentAllocator yet). */
    private static Object allocateBytes(Object arena, long size) throws ReflectiveOperationException {
        if (mArenaAllocate != null) return mArenaAllocate.invoke(arena, size);
        return mArenaAllocateAligned.invoke(arena, size, 8L);
    }

    private static Object sharedArena() throws ReflectiveOperationException {
        if (sharedArena == null) {
            sharedArena = Class.forName("java.lang.foreign.Arena")
                    .getMethod("ofShared").invoke(null);
        }
        return sharedArena;
    }

    /** libraryLookup(bridgePath, sharedArena) — the pure-FFM load path. */
    private static Object libraryLookupFromPath(String path) throws ReflectiveOperationException {
        final Class<?> lookupCls = Class.forName("java.lang.foreign.SymbolLookup");
        final Object arena = sharedArena();
        for (Method m : lookupCls.getMethods()) {
            if (!m.getName().equals("libraryLookup") || !Modifier.isStatic(m.getModifiers())) continue;
            final Class<?>[] p = m.getParameterTypes();
            if (p.length == 2 && p[0] == String.class && p[1].isInstance(arena)) {
                return m.invoke(null, path, arena);
            }
        }
        throw new NoSuchMethodException("SymbolLookup.libraryLookup(String, Arena)");
    }

    /** loaderLookup (bridge was System.load'ed) or libraryLookup on the
     *  recorded path — the mixed-mode lookup. */
    private static Object openLookupOnLoadedBridge() throws ReflectiveOperationException {
        final String path = bridgePathOrNull();
        if (path == null) {
            return Class.forName("java.lang.foreign.SymbolLookup")
                    .getMethod("loaderLookup").invoke(null);
        }
        try {
            return libraryLookupFromPath(path);
        } catch (Throwable t) {
            return Class.forName("java.lang.foreign.SymbolLookup")
                    .getMethod("loaderLookup").invoke(null);
        }
    }

    private static String bridgePathOrNull() {
        try {
            final String path = NativeScriptManager.loadedBridgePath;
            return path != null && !path.isEmpty() ? path : null;
        } catch (Throwable t) {
            return null;
        }
    }

    /**
     * Builds one downcall handle: looks the symbol up in the bridge and
     * links it against its descriptor. Descriptors describe the REAL C
     * signatures of the jcm_* exports — ADDRESS slots for Java-allocated
     * byte buffers, JAVA_LONG for raw address values.
     */
    private static MethodHandle downcall(Object lookup, Object descriptor, String symbol)
            throws ReflectiveOperationException {
        final Object optional = mLookup.invoke(lookup, symbol);
        if (!(optional instanceof Optional) || !((Optional<?>) optional).isPresent()) {
            throw new NoSuchMethodException("bridge symbol " + symbol + " not found (old bridge binary?)");
        }
        final Object addressSegment = ((Optional<?>) optional).get();
        return (MethodHandle) mDowncallBound.invoke(linker, addressSegment, descriptor, EMPTY_OPTIONS);
    }

    private static Object descriptor(Object ret, Object... args) throws ReflectiveOperationException {
        return mDescriptorOf.invoke(null, ret, newLayoutArray(args));
    }

    private static Object descriptorVoid(Object... args) throws ReflectiveOperationException {
        return mDescriptorOfVoid.invoke(null, newLayoutArray(args));
    }

    /** Reflection passes varargs array params STRICTLY typed: build a real
     *  MemoryLayout[] instead of an Object[] (which causes "argument type
     *  mismatch" at Method.invoke). */
    private static Object newLayoutArray(Object... elements) throws ReflectiveOperationException {
        final Class<?> layoutCls = Class.forName("java.lang.foreign.MemoryLayout");
        final Object array = java.lang.reflect.Array.newInstance(layoutCls, elements.length);
        for (int i = 0; i < elements.length; i++) {
            java.lang.reflect.Array.set(array, i, elements[i]);
        }
        return array;
    }

    /**
     * Links the downcall surface. The CORE surface (shm control,
     * reclaimer diagnostics, module lifecycle) has existed since the first
     * ABI 10 bridges and is REQUIRED. The EXTENDED surface
     * (jcm_module_render / jcm_module_invoke_api / jcm_ffm_init) completes
     * the JNI-free plane: mandatory for the pure-FFM install, optional in
     * mixed mode so an older bridge binary keeps the e4ee023-era behaviour
     * (FFM kick, everything else legacy).
     *
     * @param requireModuleSurface false when a missing extended surface is
     *        tolerable (System.load already bound the legacy natives)
     */
    private static boolean linkDowncalls(Object lookup, boolean requireModuleSurface) {
        try {
            hShmInit = downcall(lookup,
                    descriptor(JAVA_INT, JAVA_LONG, JAVA_LONG), "jcm_shm_init");
            hShmKick = downcall(lookup,
                    descriptor(JAVA_INT), "jcm_shm_kick");
            hShmRegisterInstance = downcall(lookup,
                    descriptorVoid(JAVA_LONG, JAVA_LONG, JAVA_ADDRESS, JAVA_INT),
                    "jcm_shm_register_instance");
            hShmShutdown = downcall(lookup,
                    descriptorVoid(), "jcm_shm_shutdown");
            hReclaimerPending = downcall(lookup,
                    descriptor(JAVA_LONG), "jcm_reclaimer_pending");
            hReclaimerQuiesce = downcall(lookup,
                    descriptorVoid(), "jcm_reclaimer_quiesce");
            hModuleOpen = downcall(lookup,
                    descriptor(JAVA_LONG, JAVA_ADDRESS, JAVA_INT), "jcm_module_open");
            hModuleClose = downcall(lookup,
                    descriptorVoid(JAVA_LONG), "jcm_module_close");
            hModuleAbi = downcall(lookup,
                    descriptor(JAVA_INT, JAVA_LONG), "jcm_module_abi");
            hModuleScriptType = downcall(lookup,
                    descriptor(JAVA_INT, JAVA_LONG, JAVA_ADDRESS, JAVA_INT), "jcm_module_script_type");
            hModuleScriptId = downcall(lookup,
                    descriptor(JAVA_INT, JAVA_LONG, JAVA_ADDRESS, JAVA_INT), "jcm_module_script_id");
            hModuleStateSize = downcall(lookup,
                    descriptor(JAVA_LONG, JAVA_LONG), "jcm_module_state_size");
            hModuleDisposeInstance = downcall(lookup,
                    descriptorVoid(JAVA_LONG, JAVA_ADDRESS, JAVA_INT, JAVA_LONG, JAVA_LONG),
                    "jcm_module_dispose_instance");
            if (identityScratch == null) {
                identityScratch = ByteBuffer.allocateDirect(128).order(ByteOrder.nativeOrder());
                identitySegment = mSegmentOfBuffer.invoke(null, identityScratch);
            }
            /* The one-per-frame hot path deserves a direct interface call
               instead of MethodHandle.invokeWithArguments. */
            kickCall = MethodHandleProxies.asInterfaceInstance(KickCall.class, hShmKick);
        } catch (Throwable t) {
            unavailableReason = trimToReason(t);
            /* Pre-concatenated: the bare-javac stub prints templates raw. */
            JCMLogger.warn("Panama FFM downcall link failed: " + t);
            return false;
        }
        try {
            hModuleRender = downcall(lookup,
                    descriptor(JAVA_INT, JAVA_LONG, JAVA_ADDRESS, JAVA_INT,
                            JAVA_LONG, JAVA_LONG, JAVA_INT, JAVA_LONG),
                    "jcm_module_render");
            hModuleInvokeApi = downcall(lookup,
                    descriptor(JAVA_LONG, JAVA_LONG, JAVA_LONG, JAVA_ADDRESS, JAVA_INT, JAVA_LONG),
                    "jcm_module_invoke_api");
            hFfmInit = downcall(lookup,
                    descriptor(JAVA_INT, JAVA_LONG, JAVA_INT), "jcm_ffm_init");
            moduleSurfaceReady = true;
        } catch (Throwable t) {
            moduleSurfaceReady = false;
            if (requireModuleSurface) {
                unavailableReason = trimToReason(t);
                JCMLogger.warn("Panama FFM extended surface link failed: " + t);
                return false;
            }
            hostUnavailableReason = trimToReason(t);
            JCMLogger.warn("Panama FFM extended surface missing on this bridge ("
                    + hostUnavailableReason + "); render/api callbacks stay on the legacy plane");
        }
        return true;
    }

    /* ------------------------------------------------------------------ */
    /* upcall vtable                                                       */
    /* ------------------------------------------------------------------ */

    /** One upcall stub from a static adapter method + its descriptor. */
    private static long upcall(MethodHandle target, Object function) throws Throwable {
        final Object stub = mUpcallStub.isVarArgs()
                ? mUpcallStub.invoke(linker, target, function, sharedArena(), EMPTY_OPTIONS)
                : mUpcallStub.invoke(linker, target, function, sharedArena());
        final Object address = mSegmentAddress.invoke(stub);
        return ((Number) address).longValue();
    }

    private static long staticUpcall(String name, MethodType type, Object function) throws Throwable {
        final MethodHandle target = MethodHandles.lookup().findStatic(
                PanamaFfmUpcalls.class, name, type);
        return upcall(target, function);
    }

    /**
     * Creates the seven upcall stubs, lays the JcmFfmHostVTable into a
     * direct buffer and installs it via the jcm_ffm_init downcall. Throws
     * on ANY failure — callers decide whether that is fatal (pure-FFM
     * install) or a best-effort downgrade (mixed mode).
     */
    private static void installHostVtable() throws Throwable {
        final long acquireModel = staticUpcall("cbAcquireModel",
                MethodType.methodType(long.class, long.class, int.class),
                descriptor(JAVA_LONG, JAVA_LONG, JAVA_INT));
        final long releaseModel = staticUpcall("cbReleaseModel",
                MethodType.methodType(void.class, long.class),
                descriptorVoid(JAVA_LONG));
        final long rasterizeText = staticUpcall("cbRasterizeText",
                MethodType.methodType(int.class, long.class, int.class, int.class, int.class,
                        int.class, int.class, int.class, int.class, long.class, int.class, int.class),
                descriptor(JAVA_INT, JAVA_LONG, JAVA_INT, JAVA_INT, JAVA_INT,
                        JAVA_INT, JAVA_INT, JAVA_INT, JAVA_INT, JAVA_LONG, JAVA_INT, JAVA_INT));
        final long measureFontText = staticUpcall("cbMeasureFontText",
                MethodType.methodType(int.class, long.class, int.class, long.class, int.class,
                        double.class, double.class, double.class, long.class),
                descriptor(JAVA_INT, JAVA_LONG, JAVA_INT, JAVA_LONG, JAVA_INT,
                        JAVA_DOUBLE, JAVA_DOUBLE, JAVA_DOUBLE, JAVA_LONG));
        final long drawFontText = staticUpcall("cbDrawFontText",
                MethodType.methodType(int.class, long.class, int.class, long.class, int.class,
                        double.class, double.class, double.class, double.class, double.class,
                        double.class, long.class, int.class, int.class, int.class,
                        int.class, long.class, int.class, int.class, long.class),
                descriptor(JAVA_INT, JAVA_LONG, JAVA_INT, JAVA_LONG, JAVA_INT,
                        JAVA_DOUBLE, JAVA_DOUBLE, JAVA_DOUBLE, JAVA_DOUBLE, JAVA_DOUBLE,
                        JAVA_DOUBLE, JAVA_LONG, JAVA_INT, JAVA_INT, JAVA_INT,
                        JAVA_INT, JAVA_LONG, JAVA_INT, JAVA_INT, JAVA_LONG));
        final long createResources = staticUpcall("cbCreateResources",
                MethodType.methodType(int.class, int.class, int.class,
                        long.class, long.class, long.class, long.class, long.class, long.class,
                        long.class),
                descriptor(JAVA_INT, JAVA_INT, JAVA_INT,
                        JAVA_LONG, JAVA_LONG, JAVA_LONG, JAVA_LONG, JAVA_LONG, JAVA_LONG,
                        JAVA_LONG));
        final long invokeApi = staticUpcall("cbInvokeApi",
                MethodType.methodType(long.class, long.class, int.class, long.class, int.class,
                        long.class),
                descriptor(JAVA_LONG, JAVA_LONG, JAVA_INT, JAVA_LONG, JAVA_INT, JAVA_LONG));

        /* JcmFfmHostVTable { u64 abi; ptr x7; } — 64 bytes, laid out with
           plain Unsafe stores (no restricted segment work). */
        final ByteBuffer vtable = ByteBuffer.allocateDirect(VTABLE_BYTES).order(ByteOrder.nativeOrder());
        final long base = NativeArenaView.addressOf(vtable);
        final sun.misc.Unsafe unsafe = NativeArenaView.rawAccess();
        unsafe.putLong(base + 0, FFM_HOST_ABI);
        unsafe.putLong(base + 8, acquireModel);
        unsafe.putLong(base + 16, releaseModel);
        unsafe.putLong(base + 24, rasterizeText);
        unsafe.putLong(base + 32, measureFontText);
        unsafe.putLong(base + 40, drawFontText);
        unsafe.putLong(base + 48, createResources);
        unsafe.putLong(base + 56, invokeApi);
        final int status = ffmInit(base, VTABLE_BYTES);
        if (status != 0) {
            throw new IllegalStateException("jcm_ffm_init rejected the vtable (status " + status + ")");
        }
        hostInstalled = true;
        hostUnavailableReason = null;
    }

    private static String trimToReason(Throwable t) {
        String message = t.toString();
        if (message.startsWith("java.lang.UnsupportedOperationException")) {
            /* Preview API classes touched without --enable-preview. */
            return "java.lang.foreign needs --enable-preview on this JDK";
        }
        if (message.length() > 120) message = message.substring(0, 120);
        return message;
    }

    /* ------------------------------------------------------------------ */
    /* native memory helpers (cold paths only)                             */
    /* ------------------------------------------------------------------ */

    /**
     * Copies UTF-8 bytes (+NUL) into a fresh confined-Arena segment and
     * invokes the handle with {@code prefixArgs + (segment, length) + suffixArgs}
     * — the string may sit anywhere in the C parameter list. The arena
     * closes when the call returns — safe because the C side copies what
     * it needs synchronously.
     */
    private static Object invokeWithUtf8(MethodHandle h, String text, Object[] prefixArgs, Object[] suffixArgs)
            throws Throwable {
        final byte[] bytes = text == null ? new byte[0] : text.getBytes(StandardCharsets.UTF_8);
        final Object arena = mArenaOfConfined.invoke(null);
        try {
            final Object segment = allocateBytes(arena, (long) bytes.length + 1L);
            final ByteBuffer view = (ByteBuffer) mSegmentAsByteBuffer.invoke(segment);
            view.order(ByteOrder.nativeOrder());
            view.put(bytes);
            view.put((byte) 0);
            final Object[] args = new Object[prefixArgs.length + suffixArgs.length + 2];
            System.arraycopy(prefixArgs, 0, args, 0, prefixArgs.length);
            args[prefixArgs.length] = segment;
            args[prefixArgs.length + 1] = bytes.length;
            System.arraycopy(suffixArgs, 0, args, prefixArgs.length + 2, suffixArgs.length);
            return h.invokeWithArguments(args);
        } finally {
            try {
                mArenaClose.invoke(arena);
            } catch (ReflectiveOperationException ignored) {
            }
        }
    }

    private static long unboxLong(Object boxed) {
        if (boxed instanceof Number) return ((Number) boxed).longValue();
        throw new IllegalStateException("unexpected return " + boxed);
    }

    private static int unboxInt(Object boxed) {
        if (boxed instanceof Number) return ((Number) boxed).intValue();
        throw new IllegalStateException("unexpected return " + boxed);
    }

    /* ------------------------------------------------------------------ */
    /* public surface (mirrors the JNI natives; package-private)           */
    /* ------------------------------------------------------------------ */

    static int shmInit(long base, long size) {
        try {
            return unboxInt(hShmInit.invokeWithArguments(base, size));
        } catch (Throwable t) {
            throw new IllegalStateException("jcm_shm_init downcall failed", t);
        }
    }

    /** Hot path: zero-argument downcall through a plain interface proxy —
     * no JNI transition, no varargs, no boxing. */
    static int shmKick() {
        return kickCall.kick();
    }

    static void shmRegisterInstance(long moduleHandle, long instanceId, String instanceKey) {
        try {
            invokeWithUtf8(hShmRegisterInstance, instanceKey,
                    new Object[]{moduleHandle, instanceId}, EMPTY_ARGS);
        } catch (Throwable t) {
            throw new IllegalStateException("jcm_shm_register_instance downcall failed", t);
        }
    }

    static void shmShutdown() {
        try {
            hShmShutdown.invokeWithArguments();
        } catch (Throwable t) {
            throw new IllegalStateException("jcm_shm_shutdown downcall failed", t);
        }
    }

    /* ---- background reclaimer (diagnostics + deterministic drain) ---- */

    static long reclaimerPending() {
        try {
            return unboxLong(hReclaimerPending.invokeWithArguments());
        } catch (Throwable t) {
            throw new IllegalStateException("jcm_reclaimer_pending downcall failed", t);
        }
    }

    static void reclaimerQuiesce() {
        try {
            hReclaimerQuiesce.invokeWithArguments();
        } catch (Throwable t) {
            throw new IllegalStateException("jcm_reclaimer_quiesce downcall failed", t);
        }
    }

    /* ---- module lifecycle (the jcm_module_* downcalls) ---------------- */

    static long moduleOpen(String path) {
        try {
            return unboxLong(invokeWithUtf8(hModuleOpen, path, EMPTY_ARGS, EMPTY_ARGS));
        } catch (Throwable t) {
            throw new IllegalStateException("jcm_module_open downcall failed", t);
        }
    }

    static void moduleClose(long handle) {
        try {
            hModuleClose.invokeWithArguments(handle);
        } catch (Throwable t) {
            throw new IllegalStateException("jcm_module_close downcall failed", t);
        }
    }

    static int moduleAbi(long handle) {
        try {
            return unboxInt(hModuleAbi.invokeWithArguments(handle));
        } catch (Throwable t) {
            throw new IllegalStateException("jcm_module_abi downcall failed", t);
        }
    }

    static String moduleScriptId(long handle) {
        return moduleIdentity(hModuleScriptId, handle);
    }

    static String moduleScriptType(long handle) {
        return moduleIdentity(hModuleScriptType, handle);
    }

    private static String moduleIdentity(MethodHandle h, long handle) {
        try {
            synchronized (PanamaFfmChannel.class) {
                identityScratch.clear();
                final Object result = h.invokeWithArguments(handle, identitySegment,
                        identityScratch.capacity());
                final int length = unboxInt(result);
                if (length < 0) return "";
                final byte[] out = new byte[length];
                identityScratch.get(0, out);
                return new String(out, StandardCharsets.UTF_8);
            }
        } catch (Throwable t) {
            throw new IllegalStateException("module identity downcall failed", t);
        }
    }

    static long moduleStateSize(long handle) {
        try {
            return unboxLong(hModuleStateSize.invokeWithArguments(handle));
        } catch (Throwable t) {
            throw new IllegalStateException("jcm_module_state_size downcall failed", t);
        }
    }

    /**
     * Per-instance dispose. {@code snapshotAddress} 0 makes the bridge use
     * its zeroed fallback snapshot, mirroring the JNI path's null
     * ByteBuffer.
     */
    static void moduleDisposeInstance(long handle, String instanceKey, long snapshotAddress, long snapshotLength) {
        try {
            invokeWithUtf8(hModuleDisposeInstance, instanceKey, new Object[]{handle},
                    new Object[]{snapshotAddress, snapshotAddress == 0 ? -1L : snapshotLength});
        } catch (Throwable t) {
            throw new IllegalStateException("jcm_module_dispose_instance downcall failed", t);
        }
    }

    /**
     * Direct per-frame render WITHOUT the shm region (the "JNI" transport
     * on a JNI-less session). {@code outDesc} is the caller's 128-byte
     * direct buffer; on return it holds status / record count / the five
     * arena triples exactly as the shm result slot would. Returns the
     * status (0 = ok).
     */
    static int moduleRender(long handle, String instanceKey, long snapshotAddress,
                            long snapshotLength, int resourceKind, ByteBuffer outDesc) {
        try {
            final Object result = invokeWithUtf8(hModuleRender, instanceKey, new Object[]{handle},
                    new Object[]{snapshotAddress, snapshotLength, resourceKind,
                            NativeArenaView.addressOf(outDesc)});
            return unboxInt(result);
        } catch (Throwable t) {
            throw new IllegalStateException("jcm_module_render downcall failed", t);
        }
    }

    /**
     * Invoke one module api callback (the FFM twin of the
     * nativeApiCallback JNI native). Returns the reply bytes.
     */
    static byte[] moduleInvokeApi(long function, long user, byte[] parameters) {
        try {
            final byte[] bytes = parameters == null ? new byte[0] : parameters;
            final ByteBuffer lengthSlot = INVOKE_LEN.get();
            lengthSlot.clear();
            final Object arena = mArenaOfConfined.invoke(null);
            final long replyAddress;
            try {
                final Object segment = allocateBytes(arena, (long) bytes.length + 1L);
                final ByteBuffer view = (ByteBuffer) mSegmentAsByteBuffer.invoke(segment);
                view.order(ByteOrder.nativeOrder());
                view.put(bytes);
                view.put((byte) 0);
                replyAddress = unboxLong(hModuleInvokeApi.invokeWithArguments(function, user,
                        segment, bytes.length, NativeArenaView.addressOf(lengthSlot)));
            } finally {
                try {
                    mArenaClose.invoke(arena);
                } catch (ReflectiveOperationException ignored) {
                }
            }
            final int length = lengthSlot.getInt(0);
            if (replyAddress == 0 || length < 0) {
                return "{\"error\":\"native callback unavailable\"}".getBytes(StandardCharsets.UTF_8);
            }
            return NativeArenaView.readBytes(replyAddress, length);
        } catch (Throwable t) {
            throw new IllegalStateException("jcm_module_invoke_api downcall failed", t);
        }
    }

    /* ---- JNI-free host vtable installation ---------------------------- */

    static int ffmInit(long vtableAddress, int vtableBytes) {
        try {
            return unboxInt(hFfmInit.invokeWithArguments(vtableAddress, vtableBytes));
        } catch (Throwable t) {
            throw new IllegalStateException("jcm_ffm_init downcall failed", t);
        }
    }
}
