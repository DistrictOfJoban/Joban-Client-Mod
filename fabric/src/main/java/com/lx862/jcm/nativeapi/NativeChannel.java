package com.lx862.jcm.nativeapi;

import com.lx862.jcm.mod.util.JCMLogger;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.Map;

/**
 * NativeChannel — selects and drives HOW Java exchanges data with the C++
 * script modules. Configurable from the Mod Menu config screen
 * ("Java 与 C++ 通信方式"); the default is {@link Transport#SHARED_MEMORY}.
 *
 * <h2>Transports</h2>
 * <ul>
 *   <li><b>{@link Transport#JNI}</b> — the original pipeline: the snapshot
 *       ByteBuffer and the five output arenas ride the per-frame JNI call
 *       ({@code nRender}). Slower — one JNI transition plus a jstring, a
 *       NativeFrame object and (pre-ABI-9) fresh ByteBuffer wrappers per
 *       module per frame — but battle-tested, and it is the automatic
 *       fallback whenever the shared-memory path cannot start.</li>
 *   <li><b>{@link Transport#SHARED_MEMORY}</b> — the default. The
 *       per-frame snapshot is marshalled into a window of a file-backed
 *       shared region ({@link NativeShm}, a MappedByteBuffer) and C++
 *       reads it straight from that mapping; Java then writes a request
 *       into the CONTROL slot and makes an EMPTY-ARGUMENT
 *       {@code nativeShmKick()} call. The C++ bridge runs the identical
 *       render pipeline and writes the output arena descriptors into the
 *       RESULT slot, which Java re-binds into REUSED ByteBuffer views
 *       ({@link NativeArenaView}). Zero allocations and zero data copies
 *       on the hot path. Small residual risk by design — raw-address
 *       views depend on the module reporting honest arena sizes — hence
 *       the config description "性能最好，但小概率出现稳定性问题".</li>
 *   <li><b>{@link Transport#PANAMA_FFM}</b> — the Foreign Function &amp;
 *       Memory API downcall transport ({@link PanamaFfmChannel}, ABI 10):
 *       same shared-memory data plane, but every bridge invocation
 *       (module lifecycle + the per-frame kick) is an FFM downcall into
 *       the bridge's plain-C exports instead of a JNI native method.
 *       Needs JDK 22+ (final java.lang.foreign) — on older JVMs it
 *       degrades to the JNI-triggered shared-memory channel with a
 *       one-line reason in the census.</li>
 * </ul>
 *
 * <h2>Threading</h2>
 * Everything the channel itself touches (control/result slots, the
 * region, the instance registry) is guarded by the {@link NativeChannel}
 * class monitor: one uncontended lock acquisition per frame. The module
 * mutex in the bridge serializes per-module work as before, and all
 * render/dispose lifecycle semantics are identical to the JNI transport.
 *
 * <p>Package-private helpers (buffer facade, render dispatch) are used by
 * NativeScriptManager / the drivers; the public surface is the transport
 * enum plus configure/reset for the config screen bootstrap. JDK-only: no
 * Minecraft classes, so the bare-javac JNI test harness compiles it and
 * NativeIntegrationTest exercises the whole shared-memory path on every
 * CI platform.
 */
public final class NativeChannel {

    public enum Transport {
        JNI, PANAMA_FFM, SHARED_MEMORY
    }

    /** Config default: shared memory ("性能最好，但小概率出现稳定性问题"). */
    public static final Transport DEFAULT = Transport.SHARED_MEMORY;

    private static volatile Transport transport = DEFAULT;

    /* Region lifecycle. All state below is created/destroyed under the
       class monitor; reads of `shm` on the render thread are stable
       because renders also hold the monitor. */
    private static NativeShm shm;
    private static boolean shmBroken;

    /* ------------------------------------------------------------------ */
    /* invocation-plane dispatch (JNI natives vs FFM downcalls)            */
    /* ------------------------------------------------------------------ */

    /** True when the Panama FFM downcall layer is installed and linked. */
    static boolean ffmActive() {
        return PanamaFfmChannel.ready();
    }

    /* The four shm natives + the module lifecycle dispatch through these
       package-private wrappers: PANAMA_FFM (or any session where the FFM
       layer linked) uses the jcm_* downcalls; otherwise the JNI natives.
       All sites hold the class monitor (render/reset), so no extra sync. */

    static int shmInitCall(long base, long size) {
        return ffmActive() ? PanamaFfmChannel.shmInit(base, size) : nativeShmInit(base, size);
    }

    static int shmKickCall() {
        return ffmActive() ? PanamaFfmChannel.shmKick() : nativeShmKick();
    }

    static void shmRegisterInstanceCall(long moduleHandle, long instanceId, String instanceKey) {
        if (ffmActive()) {
            PanamaFfmChannel.shmRegisterInstance(moduleHandle, instanceId, instanceKey);
        } else {
            nativeShmRegisterInstance(moduleHandle, instanceId, instanceKey);
        }
    }

    static void shmShutdownCall() {
        if (ffmActive()) {
            PanamaFfmChannel.shmShutdown();
        } else {
            nativeShmShutdown();
        }
    }

    /* moduleKey -> shm instance id. Render-thread only (cleared on reset);
       ids stay stable for an instance's whole life so the C++ side only
       ever receives the (rare) registration, never per-frame strings. */
    private static final Map<String, Long> INSTANCE_IDS = new HashMap<>();
    private static long nextInstanceId = 1;
    private static long controlSeq = 1;

    /* Reusable per-module frame views, keyed by native handle and scanned
       linearly: modules are few (single digits per pack), and a primitive
       scan keeps the per-frame path allocation-free where a Map<Long, ?>
       would box the handle every frame. Cleared on reset. */
    private static final java.util.ArrayList<Views> VIEWS = new java.util.ArrayList<>();

    /* diagnostics */
    private static int loggedStatuses;
    private static boolean warnedExhausted;
    private static final int STATUS_STALE_ECHO = 64;

    private static native int nativeShmInit(long base, long size);

    private static native int nativeShmKick();

    private static native void nativeShmRegisterInstance(long handle, long instanceId, String instanceKey);

    private static native void nativeShmShutdown();

    /** Reusable per-module frame surfaces: five arena views + one frame. */
    private static final class Views {
        final long handle;
        final ByteBuffer records = NativeArenaView.allocate();
        final ByteBuffer strings = NativeArenaView.allocate();
        final ByteBuffer pixels = NativeArenaView.allocate();
        final ByteBuffer matrices = NativeArenaView.allocate();
        final ByteBuffer floats = NativeArenaView.allocate();
        final NativeScriptManager.NativeFrame frame = new NativeScriptManager.NativeFrame();

        Views(long handle) {
            this.handle = handle;
        }
    }

    private NativeChannel() {
    }

    /* ------------------------------------------------------------------ */
    /* selection                                                           */
    /* ------------------------------------------------------------------ */

    /** Called by the client bootstrap (JCMClient) and by the config screen. */
    public static void configure(Transport selected) {
        transport = selected == null ? DEFAULT : selected;
        JCMLogger.info("Java-C++ transport: {}", transport == Transport.SHARED_MEMORY
                ? "shared memory" : transport == Transport.PANAMA_FFM ? "Panama FFM" : "JNI");
    }

    public static Transport activeTransport() {
        return transport;
    }

    /** One-line human summary of the ACTIVE transport, for the post-reload
     *  census in latest.log — so a player can verify from the log that the
     *  "Java 与 C++ 通信方式" selection really engaged (and see when the
     *  shared-memory channel degraded to JNI). */
    public static String describe() {
        final Transport selected = transport;
        final NativeShm region = shm;
        final StringBuilder sb = new StringBuilder();
        if (selected == Transport.SHARED_MEMORY) {
            sb.append(region != null ? "shared-memory" : "shared-memory (region not started yet)");
        } else if (selected == Transport.PANAMA_FFM) {
            if (PanamaFfmChannel.ready()) {
                sb.append("panama-ffm (FFM downcalls, shared-memory data plane)");
            } else {
                sb.append("panama-ffm (unavailable: ").append(PanamaFfmChannel.reason())
                        .append("; using JNI-triggered shared-memory)");
            }
        } else {
            sb.append("jni");
        }
        if (shmBroken && selected != Transport.JNI) {
            sb.append(" [unavailable this session — JNI calls]");
        }
        if (region != null) {
            sb.append(", region ").append(region.size() / 1024).append(" KB");
        }
        return sb.toString();
    }

    /** Effective transport for THIS session (PANAMA_FFM degrades to shm). */
    static Transport effectiveTransport() {
        final Transport t = transport;
        return t == Transport.PANAMA_FFM ? Transport.SHARED_MEMORY : t;
    }
    /* ------------------------------------------------------------------ */
    /* driver buffer facade                                                */
    /* ------------------------------------------------------------------ */

    /**
     * Snapshot buffer for a driver state. Shared-memory mode hands out a
     * window of the mapped region (so C++ reads Java's data directly out
     * of the shared mapping); JNI mode keeps plain direct buffers.
     */
    public static ByteBuffer newSnapshotBuffer(int capacity) {
        if (effectiveTransport() == Transport.SHARED_MEMORY && !shmBroken) {
            try {
                final NativeShm region = ensureShm();
                if (region != null) {
                    final ByteBuffer window = region.alloc(capacity);
                    if (window != null) return window;
                    /* Region exhausted: plain direct buffer, zero-copy still
                       holds (the address travels per frame). */
                    if (!warnedExhausted) {
                        warnedExhausted = true;
                        JCMLogger.warn("Native shared memory region full ({} KB) — new snapshots use plain direct buffers", region.size() / 1024);
                    }
                }
            } catch (Throwable t) {
                noteShmFailure("alloc", t);
            }
        }
        return ByteBuffer.allocateDirect(capacity).order(ByteOrder.nativeOrder());
    }

    /** Doubles the capacity of a snapshot buffer, releasing the old window. */
    public static ByteBuffer growSnapshotBuffer(ByteBuffer old) {
        final ByteBuffer next = newSnapshotBuffer(Math.max(1, old.capacity()) * 2);
        releaseSnapshotBuffer(old);
        return next;
    }

    /** Frees a window owned by the region; no-op for plain direct buffers. */
    public static void releaseSnapshotBuffer(ByteBuffer buffer) {
        final NativeShm region = shm;
        if (region != null) region.free(buffer);
    }

    /* ------------------------------------------------------------------ */
    /* per-frame dispatch (NativeScriptModule.render)                      */
    /* ------------------------------------------------------------------ */

    static NativeScriptManager.NativeFrame render(NativeScriptManager.NativeScriptModule module,
                                                  String instanceKey, ByteBuffer snapshot,
                                                  int snapshotLen, int resourceKind) {
        final Transport selected = effectiveTransport();
        if (selected != Transport.SHARED_MEMORY || shmBroken) {
            return module.nRender(module.getNativeHandle(), instanceKey, snapshot, snapshotLen, resourceKind);
        }
        try {
            final long address = NativeArenaView.addressOf(snapshot);
            if (address == 0) {
                /* heap buffer (test fixtures / exotic callers): the shm
                   protocol only speaks raw addresses — use the JNI call. */
                return module.nRender(module.getNativeHandle(), instanceKey, snapshot, snapshotLen, resourceKind);
            }
            final NativeShm region = ensureShm();
            if (region == null) {
                return module.nRender(module.getNativeHandle(), instanceKey, snapshot, snapshotLen, resourceKind);
            }
            final long handle = module.getNativeHandle();
            final long instanceId = instanceIdFor(handle, instanceKey);
            final long seq;
            final int status;
            final NativeScriptManager.NativeFrame frame;
            /* Control/result are single-slot and frame-sequential; the
               monitor also excludes reset() closing the region mid-frame. */
            synchronized (NativeChannel.class) {
                if (shm != region || shmBroken) {
                    return module.nRender(module.getNativeHandle(), instanceKey, snapshot, snapshotLen, resourceKind);
                }
                seq = controlSeq++;
                region.putControlLong(NativeShm.CTL_MODULE, handle);
                region.putControlLong(NativeShm.CTL_INSTANCE, instanceId);
                region.putControlInt(NativeShm.CTL_KIND, resourceKind);
                region.putControlInt(NativeShm.CTL_SNAP_LEN, snapshotLen);
                region.putControlLong(NativeShm.CTL_SNAP_ADDR, address);
                region.putControlLongRelease(NativeShm.CTL_SEQ, seq);   /* request publish LAST */
                status = shmKickCall();
                frame = status == NativeShm.STATUS_OK ? finishFrame(region, handle, seq) : null;
            }
            if (status != NativeShm.STATUS_OK) {
                noteStatus(status);
            }
            return frame;
        } catch (Throwable t) {
            /* Old bridge binary (missing shm exports), broken region, or an
               unexpected condition: degrade to the JNI path for the rest of
               the session instead of failing scripts. */
            noteShmFailure("render", t);
            return module.nRender(module.getNativeHandle(), instanceKey, snapshot, snapshotLen, resourceKind);
        }
    }

    /** Reads the result slot and re-binds the per-module views; monitor held. */
    private static NativeScriptManager.NativeFrame finishFrame(NativeShm region, long moduleHandle, long seq) {
        if (region.getResultLongAcquire(NativeShm.RES_SEQ) != seq) {
            noteStatus(STATUS_STALE_ECHO);
            return null;
        }
        final int status = region.getResultInt(NativeShm.RES_STATUS);
        if (status != NativeShm.STATUS_OK) {
            noteStatus(status);
            return null;
        }
        final int recordCount = region.getResultInt(NativeShm.RES_COUNT);
        Views views = null;
        for (int i = 0; i < VIEWS.size(); i++) {
            final Views candidate = VIEWS.get(i);
            if (candidate.handle == moduleHandle) {
                views = candidate;
                break;
            }
        }
        if (views == null) {
            views = new Views(moduleHandle);
            VIEWS.add(views);
        }
        bindArena(views.records, region, NativeShm.RES_RECORDS);
        bindArena(views.strings, region, NativeShm.RES_STRINGS);
        bindArena(views.pixels, region, NativeShm.RES_PIXELS);
        bindArena(views.matrices, region, NativeShm.RES_MATRICES);
        bindArena(views.floats, region, NativeShm.RES_FLOATS);
        /* (bindArena resolves each arena triple through the result slot) */
        views.frame.assign(views.records, recordCount, views.strings, views.pixels,
                views.matrices, views.floats);
        return views.frame;
    }

    private static void bindArena(ByteBuffer view, NativeShm region, int resultOffset) {
        final long address = region.getResultLong(resultOffset + NativeShm.ARENA_ADDR);
        final long len = region.getResultLong(resultOffset + NativeShm.ARENA_LEN);
        final long cap = region.getResultLong(resultOffset + NativeShm.ARENA_CAP);
        if (address == 0 || len < 0) {
            NativeArenaView.bind(view, 0, 0, 0);
            return;
        }
        /* Pre-ABI-9 modules report capacity 0: wrap at the exact length
           (the same behaviour the JNI bridge had for them). */
        final long capacity = cap > 0 ? cap : len;
        NativeArenaView.bind(view, address,
                (int) Math.min(len, Integer.MAX_VALUE),
                (int) Math.min(capacity, Integer.MAX_VALUE));
    }

    /** Long instance id per moduleKey; registers with the bridge on first use. */
    private static long instanceIdFor(long moduleHandle, String moduleKey) {
        final Long existing = INSTANCE_IDS.get(moduleKey);
        if (existing != null) return existing;
        final long id = nextInstanceId++;
        INSTANCE_IDS.put(moduleKey, id);
        shmRegisterInstanceCall(moduleHandle, id, moduleKey);
        return id;
    }

    /* ------------------------------------------------------------------ */
    /* region lifecycle                                                    */
    /* ------------------------------------------------------------------ */

    private static long defaultRegionSize() {
        long size = 64L << 20;
        try {
            final String property = System.getProperty("jcm.shm_region_bytes");
            if (property != null) size = Long.parseLong(property.trim());
        } catch (Throwable ignored) {
        }
        return Math.max(4L << 20, Math.min(256L << 20, size));
    }

    private static NativeShm ensureShm() {
        final NativeShm region = shm;
        if (region != null) return region;
        if (shmBroken) return null;
        synchronized (NativeChannel.class) {
            if (shm != null) return shm;
            if (shmBroken) return null;
            try {
                if (!NativeArenaView.selfCheck()) {
                    JCMLogger.warn("Native arena views unavailable on this JVM — falling back to JNI transport");
                    shmBroken = true;
                    return null;
                }
                final NativeShm created = NativeShm.open(defaultRegionSize());
                final int status = shmInitCall(created.base(), created.size());
                if (status != NativeShm.STATUS_OK) {
                    JCMLogger.warn("Native shared memory channel rejected by the bridge (status " + status + " " + NativeShm.statusText(status) + ") - falling back to JNI transport");
                    created.close();
                    shmBroken = true;
                    return null;
                }
                shm = created;
                JCMLogger.info("Native shared memory channel ready ({} KB region, MappedByteBuffer + {} accessor)",
                        created.size() / 1024,
                        Runtime.version().feature() >= 22 ? "FFM/VarHandle" : "sun.misc.Unsafe");
                return created;
            } catch (Throwable t) {
                shmBroken = true;
                JCMLogger.warn("Native shared memory channel unavailable (" + t + "), falling back to JNI transport");
                return null;
            }
        }
    }

    /**
     * Full teardown for NativeScriptManager.reload(): closes the region
     * (unmap + delete), forgets instances and views, and re-arms the
     * fallback so a later reload can retry. Drivers must have reset their
     * states (which release their windows) before this runs —
     * NativeScriptManager.reload() orders it that way.
     */
    public static void reset() {
        synchronized (NativeChannel.class) {
            INSTANCE_IDS.clear();
            VIEWS.clear();
            controlSeq = 1;
            loggedStatuses = 0;
            final NativeShm region = shm;
            shm = null;
            if (region != null) {
                try {
                    shmShutdownCall();
                } catch (Throwable ignored) {
                    // an old bridge without the export: nothing to forget
                }
                region.close();
            }
            /* shmBroken stays sticky across reloads for genuine
               incompatibilities but a fresh F3+T may retry after a
               transient failure (tmpdir full etc.) — clearing it is safe
               because every failure path re-detects on next use. */
            shmBroken = false;
        }
    }

    /* ------------------------------------------------------------------ */
    /* diagnostics                                                         */
    /* ------------------------------------------------------------------ */

    private static void noteStatus(int status) {
        final int bit = 1 << Math.min(status, 31);
        if ((loggedStatuses & bit) != 0) return;
        loggedStatuses |= bit;
        /* Pre-concatenated on purpose: the bare-javac test stub prints the
           template raw, and ctest needs the actual number in the log. */
        JCMLogger.warn("Native shared-memory channel reported status " + status + " (" + statusText(status) + ")");
    }

    private static String statusText(int status) {
        switch (status) {
            case NativeShm.STATUS_OK: return "ok";
            case NativeShm.ERR_NOT_INIT: return "channel not initialized";
            case NativeShm.ERR_BAD_LAYOUT: return "layout mismatch (bridge and jar disagree)";
            case NativeShm.ERR_MODULE: return "module not registered";
            case NativeShm.ERR_INSTANCE: return "instance not registered";
            case NativeShm.ERR_CREATE: return "mtrCreate failed";
            case NativeShm.ERR_RENDER: return "mtrRender failed";
            case NativeShm.ERR_RESOURCES: return "host resources failed";
            case NativeShm.ERR_REQUEST: return "bad request";
            case NativeShm.ERR_SNAPSHOT: return "invalid snapshot";
            case STATUS_STALE_ECHO: return "stale result echo";
            default: return "unknown";
        }
    }

    private static void noteShmFailure(String where, Throwable t) {
        if (!shmBroken) {
            shmBroken = true;
            JCMLogger.warn("Native shared-memory transport failed at " + where + " (" + t + "); falling back to JNI for this session");
        }
    }
}
