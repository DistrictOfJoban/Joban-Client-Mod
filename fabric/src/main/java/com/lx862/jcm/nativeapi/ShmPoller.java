package com.lx862.jcm.nativeapi;

import com.lx862.jcm.mod.util.JCMLogger;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicLong;

/**
 * ShmPoller — the Java side of the JNI-free fallback invocation plane
 * (see {@code native/include/mtr/shm_channel.h}, layout v2).
 *
 * <h2>What this is</h2>
 * On JVMs without the final Foreign Function &amp; Memory API (JDK 16..21 —
 * Minecraft 1.17.1 through 1.20.4 on their stock runtimes) there is NO
 * native-call API available that could replace JNI. This class removes the
 * need for one entirely: the bridge's constructor-started POLLER thread
 * discovers the shared region through a filesystem handshake and serves
 * every request straight from the mapped pages, while this class
 *
 * <ul>
 *   <li><b>publishes</b> renders and lifecycle commands into the control
 *       slot and waits for the result, servicing host-service requests
 *       while it waits;</li>
 *   <li><b>answers</b> the script->Java host services (the SVC block) by
 *       reusing the exact raw-address adapters that serve the Panama FFM
 *       upcall vtable ({@link PanamaFfmUpcalls}) — identical semantics on
 *       both planes, same thread-context guarantees (the service runs on
 *       the very Java thread whose command is being served);</li>
 *   <li><b>re-enters</b> module C++ api callbacks (JS-side proxies) through
 *       the CBQ ring, which the poller drains in every wait loop — the
 *       structural fix for the nested reentrancy that would otherwise
 *       deadlock a single-slot design.</li>
 * </ul>
 *
 * <h2>Threading rules (all deadlock analysis lives here)</h2>
 * <ul>
 *   <li>Every command publish+wait runs under the {@link #COMMAND_LOCK}
 *       monitor: the control slot is single, so renders, lifecycle and
 *       dispose calls serialize briefly (the poller answers in
 *       microseconds).</li>
 *   <li>Every wait loop — command wait and CBQ wait — services pending SVC
 *       requests, claimed exactly-once across all waiting threads with a
 *       compare-and-set on the served sequence.</li>
 *   <li>CBQ publishing is serialized by a re-entrant-friendly monitor
 *       ({@code synchronized} is re-entrant, so a nested proxy call from
 *       the same thread takes a fresh ring slot; other threads queue);
 *       the poller drains every ring slot in BOTH of its wait loops, so
 *       no reentrancy combination can deadlock.</li>
 * </ul>
 *
 * <p>Package-private, JDK-only (no Minecraft classes): compiled into the
 * bare-javac harness and exercised end-to-end by NativeIntegrationTest on
 * every CI platform with the same fixtures as the FFM plane.</p>
 */
final class ShmPoller {

    /* wait budgets: generous enough that a slow machine never trips them,
       small enough that a broken bridge degrades within one frame. */
    private static final long READY_WAIT_MS = 15_000;
    private static final long COMMAND_WAIT_MS = 15_000;
    private static final long CBQ_WAIT_MS = 20_000;

    private static final Object COMMAND_LOCK = new Object();

    /** The poller-plane region this session attached (null when absent). */
    private static volatile NativeShm region;
    /** Epoch of {@link #region}; 0 = no region. */
    private static volatile int epoch;
    /** True once the poller published HDR_READY == epoch. */
    private static volatile boolean ready;
    /** One-line reason when the plane is unavailable. */
    private static volatile String unavailableReason = "not attempted yet";

    /** Last SVC request seq this JVM has fully answered, per ring slot
     *  (exactly-once claim). The poller takes slots by nesting depth, so
     *  several requests may be outstanding concurrently — the
     *  single-slot design deadlocked when a nested service overwrote an
     *  unanswered outer one. */
    private static final AtomicLong[] SVC_SERVED = new AtomicLong[NativeShm.SVC_SLOTS];
    static {
        for (int i = 0; i < SVC_SERVED.length; i++) SVC_SERVED[i] = new AtomicLong();
    }
    /** CBQ publish counter — slots are taken round-robin, nesting gets a fresh slot. */
    private static long cbqCounter;
    /** Global publish sequence for CBQ entries (guarded by COMMAND_LOCK). */
    private static long cbqSeq;

    /** Reused identity-reply scratch (script type / id are tiny, cold paths). */
    private static final ThreadLocal<ByteBuffer> REPLY_SCRATCH = ThreadLocal.withInitial(() ->
            ByteBuffer.allocateDirect(256).order(ByteOrder.nativeOrder()));

    private ShmPoller() {
    }

    /* ------------------------------------------------------------------ */
    /* attach / detach                                                     */
    /* ------------------------------------------------------------------ */

    /** The active poller-plane region (null when none). */
    static NativeShm region() {
        return region;
    }

    /** True when the poller plane is live (region created + poller ready). */
    static boolean active() {
        final NativeShm current = region;
        return current != null && ready && current.getLongAcquire(NativeShm.HDR_READY) == epoch;
    }

    /** One-line reason when not active (census / config screen). */
    static String reason() {
        final String why = unavailableReason;
        return why == null ? "unknown" : why;
    }

    /**
     * Publishes a freshly created poller-mode region and waits for the C++
     * poller to attach. The bridge's poller thread scans the handshake
     * candidate directories on a 25 ms cadence, so this normally completes
     * within a few dozen milliseconds. Returns false (and logs the reason)
     * when the bridge is missing, too old for the poller plane, or the
     * attach did not happen within the budget.
     */
    static boolean attach(NativeShm created) {
        synchronized (COMMAND_LOCK) {
            region = created;
            epoch = created.getInt(NativeShm.HDR_EPOCH);
            ready = false;
            for (AtomicLong served : SVC_SERVED) served.set(0);
            cbqCounter = 0;
            cbqSeq = 0;
        }
        final long deadline = System.nanoTime() + READY_WAIT_MS * 1_000_000L;
        while (System.nanoTime() < deadline) {
            if (created.getLongAcquire(NativeShm.HDR_READY) == epoch) {
                ready = true;
                unavailableReason = null;
                JCMLogger.info("Native shared-memory poller attached (epoch {}, no JNI: commands, host services and api re-entry all travel the mapped region)",
                        epoch);
                return true;
            }
            try {
                Thread.sleep(5);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        unavailableReason = "bridge poller did not attach to the shared region (old bridge binary?)";
        JCMLogger.warn("Native shared-memory poller attach failed: {}", unavailableReason);
        detach();
        return false;
    }

    /** Clears the session association (NativeChannel.reset). */
    static void detach() {
        synchronized (COMMAND_LOCK) {
            region = null;
            epoch = 0;
            ready = false;
        }
    }

    /** True when the shared region was created in poller mode for this session. */
    static boolean pollerModeRegion() {
        return region != null;
    }

    /* ------------------------------------------------------------------ */
    /* waits (each one services host-service requests while it waits)      */
    /* ------------------------------------------------------------------ */

    /**
     * Waits for the poller to answer the request just published into the
     * control slot (seq). Services host-service requests meanwhile.
     * Returns the result status, or throws when the plane vanished or the
     * answer did not arrive within the budget (the caller degrades the
     * frame exactly like a status error).
     */
    static int awaitResult(NativeShm active, long seq) {
        final long deadline = System.nanoTime() + COMMAND_WAIT_MS * 1_000_000L;
        long spins = 0;
        for (;;) {
            servicePendingRequests(active);
            if (active.getResultLongAcquire(NativeShm.RES_SEQ) == seq) {
                return active.getResultInt(NativeShm.RES_STATUS);
            }
            if ((++spins & 0x3FF) == 0) {
                if (System.nanoTime() > deadline) {
                    throw new IllegalStateException("poller did not answer request " + seq);
                }
                if (active.getLongAcquire(NativeShm.HDR_READY) != epoch || region != active) {
                    throw new IllegalStateException("poller detached mid-request");
                }
                try {
                    Thread.sleep(0, 120_000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("interrupted while waiting for the poller");
                }
            } else {
                Thread.onSpinWait();
            }
        }
    }

    /* ------------------------------------------------------------------ */
    /* host-service dispatch (the SVC block; poller -> Java)               */
    /* ------------------------------------------------------------------ */

    /**
     * Services pending host-service requests on ANY ring slot, if any.
     * Exactly-once across all waiting threads via a per-slot
     * compare-and-set on the served seq; the poller takes slots by
     * nesting depth, so several requests may be outstanding at once and
     * each must be answered on ITS slot. The adapters are the SAME static
     * methods that back the Panama FFM upcall vtable, so service
     * semantics (raw-address reads/writes, per-thread reply buffers,
     * exception containment) are identical on both planes.
     */
    private static void servicePendingRequests(NativeShm active) {
        for (int slot = 0; slot < NativeShm.SVC_SLOTS; slot++) {
            final long block = NativeShm.SVC_OFFSET
                    + (long) slot * NativeShm.SVC_SLOT_BYTES;
            final long seq = active.getLongAcquire(block + NativeShm.SVC_SEQ);
            if (seq == 0) continue;
            final AtomicLong served = SVC_SERVED[slot];
            final long last = served.get();
            if (seq <= last) continue;
            if (!served.compareAndSet(last, seq)) continue;   /* another waiter took it */
            try {
                final int id = active.getInt(block + NativeShm.SVC_ID);
                long ret = dispatchService(active, block, id);
                active.putLong(block + NativeShm.SVC_RET, ret);
            } catch (Throwable t) {
                /* A service may NEVER stall the protocol: publish a failure
                   return (the adapters degrade on their own, but belt and
                   braces for dispatch-level errors). */
                JCMLogger.warn("Native host service dispatch failed ({}); the script call degrades", t.toString());
                active.putLong(block + NativeShm.SVC_RET, 0);
            }
            /* Publish the answer LAST: the poller spins on SVC_RSP. */
            active.putLongRelease(block + NativeShm.SVC_RSP, seq);
        }
    }

    private static long dispatchService(NativeShm active, long block, int id) {
        final long args = block + NativeShm.SVC_ARGS;
        switch (id) {
            case NativeShm.SVC_ACQUIRE_MODEL:
                return PanamaFfmUpcalls.cbAcquireModel(
                        active.getLong(args), (int) active.getLong(args + 8));
            case NativeShm.SVC_RELEASE_MODEL:
                PanamaFfmUpcalls.cbReleaseModel((int) active.getLong(args));
                return 0;
            case NativeShm.SVC_RASTERIZE_TEXT:
                return PanamaFfmUpcalls.cbRasterizeText(
                        active.getLong(args), (int) active.getLong(args + 8),
                        (int) active.getLong(args + 16), (int) active.getLong(args + 24),
                        (int) active.getLong(args + 32), (int) active.getLong(args + 40),
                        (int) active.getLong(args + 48), (int) active.getLong(args + 56),
                        active.getLong(args + 64), (int) active.getLong(args + 72),
                        (int) active.getLong(args + 80));
            case NativeShm.SVC_MEASURE_FONT_TEXT:
                return PanamaFfmUpcalls.cbMeasureFontText(
                        active.getLong(args), (int) active.getLong(args + 8),
                        active.getLong(args + 16), (int) active.getLong(args + 24),
                        Double.longBitsToDouble(active.getLong(args + 32)),
                        Double.longBitsToDouble(active.getLong(args + 40)),
                        Double.longBitsToDouble(active.getLong(args + 48)),
                        active.getLong(args + 56));
            case NativeShm.SVC_DRAW_FONT_TEXT:
                return PanamaFfmUpcalls.cbDrawFontText(
                        active.getLong(args), (int) active.getLong(args + 8),
                        active.getLong(args + 16), (int) active.getLong(args + 24),
                        Double.longBitsToDouble(active.getLong(args + 32)),
                        Double.longBitsToDouble(active.getLong(args + 40)),
                        Double.longBitsToDouble(active.getLong(args + 48)),
                        Double.longBitsToDouble(active.getLong(args + 56)),
                        Double.longBitsToDouble(active.getLong(args + 64)),
                        Double.longBitsToDouble(active.getLong(args + 72)),
                        active.getLong(args + 80),
                        (int) active.getLong(args + 88), (int) active.getLong(args + 96),
                        (int) active.getLong(args + 104), (int) active.getLong(args + 112),
                        active.getLong(args + 120), (int) active.getLong(args + 128),
                        (int) active.getLong(args + 136), active.getLong(args + 144));
            case NativeShm.SVC_CREATE_RESOURCES:
                return PanamaFfmUpcalls.cbCreateResources(
                        (int) active.getLong(args), (int) active.getLong(args + 8),
                        active.getLong(args + 16), active.getLong(args + 24),
                        active.getLong(args + 32), active.getLong(args + 40),
                        active.getLong(args + 48), active.getLong(args + 56),
                        active.getLong(args + 64));
            case NativeShm.SVC_INVOKE_API:
                return PanamaFfmUpcalls.cbInvokeApi(
                        active.getLong(args), (int) active.getLong(args + 8),
                        active.getLong(args + 16), (int) active.getLong(args + 24),
                        active.getLong(args + 32));
            default:
                JCMLogger.warn("Native host service request {} has no dispatcher; degrading", id);
                return 0;
        }
    }

    /* ------------------------------------------------------------------ */
    /* commands (Java -> poller)                                           */
    /* ------------------------------------------------------------------ */

    /** Result of one command: status + 64-bit return value. */
    static final class CommandResult {
        final int status;
        final long extra;

        CommandResult(int status, long extra) {
            this.status = status;
            this.extra = extra;
        }
    }

    /**
     * Publishes one lifecycle command and waits for the reply. {@code args}
     * may be null. {@code replyCapacity} allocates a reply window for
     * string-returning commands (0 when none is needed). Caller holds no
     * other bridge locks (the COMMAND_LOCK serializes the control slot).
     */
    private static CommandResult command(NativeShm active, int kind, long module, long instance,
                                         byte[] args, int replyCapacity) {
        synchronized (COMMAND_LOCK) {
            final ByteBuffer argBuffer = args == null || args.length == 0 ? null
                    : ByteBuffer.allocateDirect(args.length).order(ByteOrder.nativeOrder());
            final ByteBuffer replyBuffer = replyCapacity > 0
                    ? ByteBuffer.allocateDirect(replyCapacity).order(ByteOrder.nativeOrder()) : null;
            if (argBuffer != null) argBuffer.put(args).flip();
            final long seq = nextSeq(active);
            active.putControlLong(NativeShm.CTL_MODULE, module);
            active.putControlLong(NativeShm.CTL_INSTANCE, instance);
            active.putControlInt(NativeShm.CTL_KIND, kind);
            active.putControlLong(NativeShm.CTL_ARG_ADDR, argBuffer == null ? 0 : NativeArenaView.addressOf(argBuffer));
            active.putControlLong(NativeShm.CTL_ARG_LEN, argBuffer == null ? 0 : args.length);
            active.putControlLong(NativeShm.CTL_RPLY_ADDR, replyBuffer == null ? 0 : NativeArenaView.addressOf(replyBuffer));
            active.putControlLong(NativeShm.CTL_RPLY_LEN, replyBuffer == null ? 0 : replyCapacity);
            active.putControlLongRelease(NativeShm.CTL_SEQ, seq);   /* request LAST */
            final int status = awaitResult(active, seq);
            return new CommandResult(status, active.getResultLong(NativeShm.RES_EXTRA));
        }
    }

    /** The control-slot sequence counter lives on the region header's
       discipline: NativeChannel owns per-frame seq for renders; commands
       share the SAME slot, so they must share the counter. NativeChannel
       hands the seq out under its class monitor; commands run under
       COMMAND_LOCK — both funnel through here. */
    private static long nextSeq(NativeShm active) {
        return NativeChannel.nextControlSeq();
    }

    /* ---- module lifecycle over commands (cold paths) -------------------- */

    /** Opens one module library; returns the handle (0 = failure). */
    static long openModule(NativeShm active, String path) {
        final byte[] bytes = path.getBytes(StandardCharsets.UTF_8);
        final CommandResult result = command(active, NativeShm.CMD_OPEN, 0, 0, bytes, 0);
        return result.status == NativeShm.STATUS_OK ? result.extra : 0;
    }

    /** Reads one module identity string (script type / id). */
    static String moduleIdentity(NativeShm active, long handle, int kind) {
        final ByteBuffer scratch = REPLY_SCRATCH.get();
        scratch.clear();
        final CommandResult result = commandWithReply(active, kind, handle, scratch);
        if (result.status != NativeShm.STATUS_OK) return "";
        final int length = (int) Math.min(result.extra, scratch.capacity());
        if (length <= 0) return "";
        final byte[] out = new byte[length];
        scratch.get(0, out, 0, length);
        return new String(out, StandardCharsets.UTF_8);
    }

    /** Command variant that reuses the thread's identity scratch as the reply window. */
    private static CommandResult commandWithReply(NativeShm active, int kind, long handle,
                                                  ByteBuffer replyWindow) {
        synchronized (COMMAND_LOCK) {
            final long seq = nextSeq(active);
            active.putControlLong(NativeShm.CTL_MODULE, handle);
            active.putControlLong(NativeShm.CTL_INSTANCE, 0);
            active.putControlInt(NativeShm.CTL_KIND, kind);
            active.putControlLong(NativeShm.CTL_ARG_ADDR, 0);
            active.putControlLong(NativeShm.CTL_ARG_LEN, 0);
            active.putControlLong(NativeShm.CTL_RPLY_ADDR, NativeArenaView.addressOf(replyWindow));
            active.putControlLong(NativeShm.CTL_RPLY_LEN, replyWindow.capacity());
            active.putControlLongRelease(NativeShm.CTL_SEQ, seq);
            final int status = awaitResult(active, seq);
            return new CommandResult(status, active.getResultLong(NativeShm.RES_EXTRA));
        }
    }

    static int moduleAbi(NativeShm active, long handle) {
        final CommandResult result = command(active, NativeShm.CMD_ABI, handle, 0, null, 0);
        return result.status == NativeShm.STATUS_OK ? (int) result.extra : 0;
    }

    static long moduleStateSize(NativeShm active, long handle) {
        final CommandResult result = command(active, NativeShm.CMD_STATE_SIZE, handle, 0, null, 0);
        return result.status == NativeShm.STATUS_OK ? result.extra : 0;
    }

    static void closeModule(NativeShm active, long handle) {
        command(active, NativeShm.CMD_CLOSE, handle, 0, null, 0);
    }

    static void disposeInstance(NativeShm active, long handle, String instanceKey,
                                 ByteBuffer snapshot, int snapshotLen) {
        final byte[] key = instanceKey.getBytes(StandardCharsets.UTF_8);
        final byte[] args = new byte[16 + key.length];
        final long address = snapshot == null ? 0 : NativeArenaView.addressOf(snapshot);
        java.nio.ByteBuffer.wrap(args).order(ByteOrder.nativeOrder())
                .putLong(0, address)
                .putLong(8, address == 0 || snapshotLen < 0 ? -1L : snapshotLen);
        System.arraycopy(key, 0, args, 16, key.length);
        command(active, NativeShm.CMD_DISPOSE, handle, 0, args, 0);
    }

    static void registerInstance(NativeShm active, long handle, long instanceId, String instanceKey) {
        final byte[] key = instanceKey.getBytes(StandardCharsets.UTF_8);
        final byte[] args = new byte[8 + key.length];
        java.nio.ByteBuffer.wrap(args).order(ByteOrder.nativeOrder())
                .putLong(0, instanceId);
        System.arraycopy(key, 0, args, 8, key.length);
        command(active, NativeShm.CMD_REGISTER, handle, 0, args, 0);
    }

    /** Best-effort shutdown (NativeChannel.reset): tells the poller to detach. */
    static void requestShutdown(NativeShm active) {
        try {
            command(active, NativeShm.CMD_SHUTDOWN, 0, 0, null, 0);
        } catch (Throwable t) {
            /* Force close follows immediately: the poller revalidates the
               region header every request and self-detaches when the epoch
               or the files disappear. */
            JCMLogger.warn("Native poller shutdown command failed ({}); closing the region anyway", t.toString());
        }
    }

    /**
     * Reclaimer diagnostics over the poller plane: drains the deferred
     * reclamation queue and returns the pending count (zero once drained).
     * Returns -1 when no region is attached (e.g. right after a reload).
     * The poller-plane twin of the jcm_reclaimer_quiesce/pending exports.
     */
    static long reclaimerQuiesce(NativeShm active) {
        if (active == null) return -1;
        final CommandResult result = command(active, NativeShm.CMD_RECLAIMER, 0, 0, null, 0);
        return result.status == NativeShm.STATUS_OK ? result.extra : -1;
    }

    /* ------------------------------------------------------------------ */
    /* api re-entry (Java -> C++; the CBQ ring)                           */
    /* ------------------------------------------------------------------ */

    private static final byte[] CALLBACK_ERROR =
            "{\"error\":\"native callback unavailable\"}".getBytes(StandardCharsets.UTF_8);

    /**
     * Invokes one module C++ api callback (the JS-proxy re-entry). Blocks
     * until the poller has drained the queue entry. The re-entrant
     * {@code synchronized} lets a nested call from the SAME thread (a
     * proxy invoked inside a host-service handler) take a fresh ring slot
     * while other threads queue.
     */
    static byte[] invokeCallback(long function, long user, byte[] parameters) {
        final NativeShm active = region;
        if (active == null || !ready) return CALLBACK_ERROR;
        synchronized (COMMAND_LOCK) {
            /* Nested publish from the same thread (service handler -> proxy
               -> service handler ...): pick the next ring slot BEFORE the
               blocking call below, so each nesting level owns its slot. */
            final int slot = (int) (cbqCounter % NativeShm.CBQ_SLOTS);
            cbqCounter++;
            final byte[] params = parameters == null ? new byte[0] : parameters;
            final ByteBuffer request = ByteBuffer.allocateDirect(params.length + 1)
                    .order(ByteOrder.nativeOrder());
            request.put(params).put((byte) 0);
            final int replyCapacity = (int) Math.min(4L << 20,
                    Math.max(4096L, (long) params.length * 4 + 65_536L));
            final ByteBuffer reply = ByteBuffer.allocateDirect(replyCapacity)
                    .order(ByteOrder.nativeOrder());
            final long slotAddress = NativeShm.CBQ_OFFSET
                    + (long) slot * NativeShm.CBQ_SLOT_BYTES;
            final long seq = ++cbqSeq;   /* globally unique, never 0, guarded by COMMAND_LOCK */

            active.putLong(slotAddress + NativeShm.CBQ_FN, function);
            active.putLong(slotAddress + NativeShm.CBQ_USER, user);
            active.putLong(slotAddress + NativeShm.CBQ_REQ_ADDR, NativeArenaView.addressOf(request));
            active.putLong(slotAddress + NativeShm.CBQ_REQ_LEN, params.length);
            active.putLong(slotAddress + NativeShm.CBQ_RPLY_ADDR, NativeArenaView.addressOf(reply));
            active.putLong(slotAddress + NativeShm.CBQ_RPLY_CAP, replyCapacity);
            active.putLongRelease(slotAddress + NativeShm.CBQ_SEQ, seq);

            final long deadline = System.nanoTime() + CBQ_WAIT_MS * 1_000_000L;
            long spins = 0;
            for (;;) {
                servicePendingRequests(active);
                if (active.getLongAcquire(slotAddress + NativeShm.CBQ_RSP) == seq) {
                    final long length = active.getLong(slotAddress + NativeShm.CBQ_RPLY_LEN);
                    if (length <= 0) return CALLBACK_ERROR;
                    return NativeArenaView.readBytes(NativeArenaView.addressOf(reply),
                            (int) Math.min(length, replyCapacity));
                }
                if ((++spins & 0x3FF) == 0) {
                    if (System.nanoTime() > deadline) {
                        JCMLogger.warn("Native poller did not drain callback queue slot {} within {} ms; degrading",
                                slot, CBQ_WAIT_MS);
                        return CALLBACK_ERROR;
                    }
                    if (active.getLongAcquire(NativeShm.HDR_READY) != epoch || region != active) {
                        return CALLBACK_ERROR;
                    }
                    try {
                        Thread.sleep(0, 120_000);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return CALLBACK_ERROR;
                    }
                } else {
                    Thread.onSpinWait();
                }
            }
        }
    }

    /* ------------------------------------------------------------------ */
    /* census                                                              */
    /* ------------------------------------------------------------------ */

    /** Invocation-plane fragment for NativeChannel.describe(). */
    static String describePlane() {
        if (active()) return "shm poller (no JNI)";
        if (region != null) return "shm poller (attach failed: " + reason() + ")";
        return "shm poller (region not started)";
    }
}
