package com.lx862.jcm.nativeapi;

import com.lx862.jcm.mod.util.JCMLogger;

import java.lang.invoke.MethodHandle;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

/**
 * PanamaFfmChannel — the Foreign Function &amp; Memory API ("Panama FFM")
 * transport, fully implemented as of ABI 10.
 *
 * <h2>What it replaces</h2>
 * Where the shared-memory transport still crosses the JNI boundary once
 * per frame (the empty-argument {@code nativeShmKick()} trigger), this
 * transport flips the PER-FRAME invocation plane to {@code java.lang.foreign}
 * downcalls into the bridge's plain-C export surface — the same cores the
 * JNI natives route through, so behaviour is identical:
 *
 * <ul>
 *   <li>per-frame kick via the zero-argument {@code jcm_shm_kick} downcall —
 *       still zero allocations on the hot path, and an FFM transition
 *       instead of a JNI one (no JNIEnv bookkeeping, no local-ref frame,
 *       no JNI return-to-Java boundary);</li>
 *   <li>shm channel control (init / instance registration / shutdown) via
 *       {@code jcm_shm_*} downcalls — once per session and once per
 *       instance lifetime;</li>
 *   <li>background-reclaimer diagnostics + deterministic drain via
 *       {@code jcm_reclaimer_pending / jcm_reclaimer_quiesce};</li>
 *   <li>data stays in the shared-memory region exactly as with the
 *       SHARED_MEMORY transport; only the calls change.</li>
 * </ul>
 *
 * Module LIFECYCLE (open/identity/dispose/close) deliberately stays on the
 * JNI natives: those calls share the same open_module/dispose cores with
 * the bridge's {@code jcm_module_*} exports, they are cold (reload-time),
 * and keeping them on the long-battle-tested JNI path avoids dlopen/dlsym
 * cycles driven from FFM stubs — the dynamic loader's scope bookkeeping is
 * process-wide state that misbehaves when churned from unusual call stacks.
 * The {@code jcm_module_*} C exports remain available for SDK tooling.
 *
 * Host upcalls (script -> Java resources) still flow through the bridge's
 * JNI host table: the bridge is loaded by the JVM and owns the NativeHost
 * binding — re-implementing those upcalls as upcall stubs would mean
 * duplicating the bridge's resource-resolution logic in Java for no
 * behavioural gain. The "bridge optional entirely" end-state from the
 * original design sketch remains future work.
 *
 * <h2>How it stays Java-17-compatible</h2>
 * Minecraft 1.17.1-1.20.4 runs on Java 16/17, where {@code
 * java.lang.foreign} does not exist (the 21 preview variant needs
 * {@code --enable-preview}). This class therefore references NO FFM type
 * at compile time: every FFM class, method and constant is resolved
 * reflectively once, at install time. On the final API (JDK 22+) nothing
 * else is required. On 21 the preview API can be enabled for the test
 * harness with {@code --enable-preview} plus
 * {@code -Djcm.ffm.allow_preview=true}. On 17/21 the install fails fast
 * with a one-line reason and the transport degrades to the JNI-triggered
 * shared-memory channel — identical rendering, just the old call path.
 *
 * <p>Descriptor shapes avoid every <em>restricted</em> FFM method (no
 * {@code MemorySegment.ofAddress}, no {@code reinterpret}, no
 * {@code getUtf8String}): raw addresses travel as {@code JAVA_LONG}
 * (register-identical to a pointer argument on every supported platform
 * ABI), and the three Java-owned byte buffers (module paths, instance
 * keys, identity-string outputs) are real {@code ADDRESS} arguments backed
 * by short-lived confined {@code Arena} segments. Calls go through
 * {@link MethodHandle#invokeWithArguments} — the only form usable with
 * reflectively produced handles and compile-time-unknown argument types;
 * the adaptation cost is irrelevant on cold paths and the hot
 * zero-argument kick pays none of it.
 */
final class PanamaFfmChannel {

    /* ------------------------------------------------------------------ */
    /* state                                                               */
    /* ------------------------------------------------------------------ */

    /** Set once install() succeeds; sticky for the session. */
    private static volatile boolean ready;
    /** One-line reason when not ready (null after a successful install). */
    private static volatile String unavailableReason = "not attempted yet";

    /* FFM machinery — all null until ready. */
    private static Object linker;                    /* Linker */
    private static Object JAVA_INT;                  /* ValueLayout.JAVA_INT  */
    private static Object JAVA_LONG;                 /* ValueLayout.JAVA_LONG */
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
    private static Object EMPTY_OPTIONS;             /* new Linker$Option[0] — the downcall varargs tail           */

    /* Downcall handles, one per bridge export (hot path + shm control +
       reclaimer diagnostics; the module-lifecycle jcm_* exports are SDK
       tooling surface only). */
    private static MethodHandle hShmInit;
    private static MethodHandle hShmKick;
    private static MethodHandle hShmRegisterInstance;
    private static MethodHandle hShmShutdown;
    private static MethodHandle hReclaimerPending;
    private static MethodHandle hReclaimerQuiesce;

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

    /** One-line reason for the config/census log when not ready. */
    static String reason() {
        final String why = unavailableReason;
        return why == null ? "unknown" : why;
    }

    /**
     * Reflectively resolves the FFM classes, methods and constants. The
     * shapes used here are shared by the 21-preview and the 22+ final
     * API; where they differ (lookup/find, allocate 1-arg/2-arg) both
     * variants are tried and the first that exists is kept.
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

        JAVA_INT = valueCls.getField("JAVA_INT").get(null);
        JAVA_LONG = valueCls.getField("JAVA_LONG").get(null);
        JAVA_ADDRESS = valueCls.getField("ADDRESS").get(null);
        if (JAVA_INT == null || JAVA_LONG == null || JAVA_ADDRESS == null) {
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

    /**
     * Symbol lookup: {@code SymbolLookup.libraryLookup(bridgePath, arena)}
     * when the bridge's absolute path is known (most reliable), else
     * {@code SymbolLookup.loaderLookup()} (the bridge was System.load'ed
     * by this classloader). The 21-preview libraryLookup signature took a
     * SegmentScope; the shared Arena implements it, so the method is found
     * by shape.
     */
    private static Object openLookup(Object arena) throws ReflectiveOperationException {
        final Class<?> lookupCls = Class.forName("java.lang.foreign.SymbolLookup");
        final String path = bridgePathOrNull();
        if (path != null) {
            for (Method m : lookupCls.getMethods()) {
                if (!m.getName().equals("libraryLookup") || !Modifier.isStatic(m.getModifiers())) continue;
                final Class<?>[] p = m.getParameterTypes();
                if (p.length == 2 && p[0] == String.class && p[1].isInstance(arena)) {
                    return m.invoke(null, path, arena);
                }
            }
        }
        return lookupCls.getMethod("loaderLookup").invoke(null);
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
     * One-shot install, called after the JNI bridge has been System.load'ed
     * (NativeScriptManager.installBridgeHost). On any failure the reason is
     * recorded and the callers degrade to the JNI natives — nothing throws
     * out of this method.
     */
    static void attemptInstall() {
        if (ready) return;
        try {
            if (!supportedRuntime()) {
                unavailableReason = "requires JDK 22+ (java.lang.foreign)";
                return;
            }
            final Class<?> arenaCls = Class.forName("java.lang.foreign.Arena");
            /* A SHARED arena backs the library lookup lifetime: the bridge
               stays loaded for the whole session anyway, and closing the
               lookup arena would dlopen-decrement the library across reload
               churn. Never closed explicitly. */
            final Object arena = arenaCls.getMethod("ofShared").invoke(null);
            resolveFfmApi();
            final Object lookup = openLookup(arena);

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

            ready = true;
            unavailableReason = null;
            JCMLogger.info("Panama FFM transport ready (java.lang.foreign downcalls into the bridge)");
        } catch (Throwable t) {
            ready = false;
            unavailableReason = trimToReason(t);
            JCMLogger.warn("Panama FFM transport unavailable ({}); the config option falls back to the JNI-triggered shared-memory channel",
                    unavailableReason);
        }
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
     * invokes the handle with {@code prefixArgs... + (segment, length)}.
     * The arena closes when the call returns — safe because the C side
     * copies what it needs synchronously.
     */
    private static void invokeWithUtf8(MethodHandle h, String text, Object... fullArgs) throws Throwable {
        final byte[] bytes = text == null ? new byte[0] : text.getBytes(StandardCharsets.UTF_8);
        final Object arena = mArenaOfConfined.invoke(null);
        try {
            final Object segment = allocateBytes(arena, (long) bytes.length + 1L);
            final ByteBuffer view = (ByteBuffer) mSegmentAsByteBuffer.invoke(segment);
            view.order(ByteOrder.nativeOrder());
            view.put(bytes);
            view.put((byte) 0);
            final Object[] args = new Object[fullArgs.length + 2];
            System.arraycopy(fullArgs, 0, args, 0, fullArgs.length);
            args[fullArgs.length] = segment;
            args[fullArgs.length + 1] = bytes.length;
            h.invokeWithArguments(args);
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

    /** Hot path: zero-argument downcall, no allocation, no JNI transition. */
    static int shmKick() {
        try {
            return unboxInt(hShmKick.invokeWithArguments());
        } catch (Throwable t) {
            throw new IllegalStateException("jcm_shm_kick downcall failed", t);
        }
    }

    static void shmRegisterInstance(long moduleHandle, long instanceId, String instanceKey) {
        try {
            invokeWithUtf8(hShmRegisterInstance, instanceKey, moduleHandle, instanceId);
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
}
