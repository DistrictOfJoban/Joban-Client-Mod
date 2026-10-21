package com.lx862.jcm.nativeapi;

import com.lx862.jcm.mod.util.JCMLogger;

import java.io.IOException;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandleProxies;
import java.lang.invoke.VarHandle;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * NativeShm — the file-backed shared memory region behind the
 * "shared memory" Java-C++ transports (layout v2).
 *
 * <h2>Region anatomy (mirrors native/include/mtr/shm_channel.h)</h2>
 * <pre>
 *   [ header 0..16K )        magic / version / geometry / epoch / mode,
 *                            written once by Java; HDR_READY is published
 *                            by the C++ poller when it attaches
 *   [ control slot @8K )      per-frame REQUEST / command, written by Java
 *   [ result slot  @8K+128 )  per-frame RESULT / command reply, written by C++
 *   [ svc ring      @8576 )    script->Java host-service requests (poller plane)
 *   [ cbq ring      @9856 )    Java->C++ module api callback ring (poller plane)
 *   [ windows 16K..end )      bump-allocated snapshot buffers
 * </pre>
 *
 * <h2>Two attachment modes</h2>
 * <ul>
 *   <li><b>FFM mode (JDK 22+)</b> — the region is a plain mapped temp file;
 *       Java hands the mapping's base address to the bridge once via the
 *       jcm_shm_init downcall and every kick is a synchronous FFM call.
 *       HDR_MODE is 0; the background poller never attaches to it.</li>
 *   <li><b>Poller mode (JDK 16..21)</b> — the region file gets the fixed
 *       name {@code jcm_shmp_<pid>.tmp} and Java writes HANDSHAKE POINTER
 *       FILES ("jcm_shm_hs_<pid>": region path, size, epoch) into every
 *       temp directory the C++ poller scans. The poller opens and maps
 *       the file itself, validates the header and publishes its epoch
 *       into HDR_READY — from that point the whole session (commands,
 *       renders, host services, api re-entry) travels ONLY through the
 *       shared pages: no native call in either direction.</li>
 * </ul>
 *
 * Two MAP_SHARED views of one file are coherent on every supported OS
 * (Linux/macOS page cache; Windows section views), so the poller's own
 * mapping sees Java's writes immediately and vice versa.
 *
 * <h2>Accessor auto-detection (Unsafe vs FFM + VarHandle)</h2>
 * The header/control/result fields are written and read through the
 * {@link Fields} accessor:
 * <ul>
 *   <li><b>JDK &lt; 22</b> — {@code sun.misc.Unsafe} volatile operations
 *       ({@code putLongRelease} / {@code getLongAcquire} with a null
 *       object, i.e. raw addresses).</li>
 *   <li><b>JDK &gt;= 22</b> — the Foreign Function &amp; Memory API +
 *       VarHandle route: {@code MemorySegment.ofBuffer(mappedBuffer)} and
 *       {@code ValueLayout.JAVA_LONG/JAVA_INT.varHandle()}, accessed with
 *       {@code setRelease}/{@code getAcquire}. Both are ordinary
 *       (unrestricted) FFM operations on a mapped buffer, and the whole
 *       route is resolved via {@link MethodHandleProxies} so the hot path
 *       stays a plain interface call with primitive arguments.</li>
 * </ul>
 * Detection happens once at region creation; any failure falls back to the
 * Unsafe accessor and logs one line.
 *
 * <h2>Window allocator (no-leak discipline)</h2>
 * Driver-owned snapshot buffers live in the windows area so that C++
 * genuinely reads Java's data straight out of the shared mapping. The
 * allocator is a bump pointer plus a coalescing free list; frees are
 * explicit ({@code NativeChannel.releaseSnapshotBuffer}) and the drivers
 * call them from every dispose/reset site. When the region is exhausted
 * the callers transparently fall back to plain
 * {@code ByteBuffer.allocateDirect} buffers, so a huge world degrades to
 * the direct-call input path instead of crashing.
 *
 * <p>Threading: allocations happen on the render thread, frees also come
 * from the client tick thread (driver dispose) — both funnel through a
 * coarse {@code synchronized}, which is uncontended in steady state
 * (zero allocator traffic per frame once buffers exist).
 *
 * <p>Package-private, JDK-only: compiled into the bare-javac test harness
 * and exercised by NativeIntegrationTest on every CI OS.
 */
final class NativeShm {
    /* ---- layout, MUST match native/include/mtr/shm_channel.h (v2) ---- */
    static final long MAGIC = 0x324D48534D59434AL; /* "JCMYSHM2" LE */
    static final int LAYOUT_VERSION = 2;
    static final int HEADER_SIZE = 8192;
    static final int CONTROL_OFFSET = 8192;
    static final int CONTROL_SIZE = 128;
    static final int RESULT_OFFSET = 8320;
    static final int RESULT_SIZE = 256;
    static final int SVC_OFFSET = 8576;
    static final int CBQ_OFFSET = 9856;
    static final int CBQ_SLOT_BYTES = 128;
    static final int CBQ_SLOTS = 8;
    static final int WINDOWS_OFFSET = 16384;

    /* header fields (offsets from the region base) */
    static final int HDR_MAGIC = 0;
    static final int HDR_VERSION = 8;
    static final int HDR_SIZE = 16;
    static final int HDR_EPOCH = 44;
    static final int HDR_READY = 48;
    static final int HDR_MODE = 56;

    /* control slot */
    static final int CTL_SEQ = 0;
    static final int CTL_MODULE = 8;
    static final int CTL_INSTANCE = 16;
    static final int CTL_KIND = 24;
    static final int CTL_SNAP_LEN = 28;
    static final int CTL_SNAP_ADDR = 32;
    static final int CTL_ARG_ADDR = 40;
    static final int CTL_ARG_LEN = 48;
    static final int CTL_RPLY_ADDR = 56;
    static final int CTL_RPLY_LEN = 64;

    /* result slot */
    static final int RES_SEQ = 0;
    static final int RES_STATUS = 8;
    static final int RES_COUNT = 12;
    static final int RES_RECORDS = 16;
    static final int RES_STRINGS = 40;
    static final int RES_PIXELS = 64;
    static final int RES_MATRICES = 88;
    static final int RES_FLOATS = 112;
    static final int RES_EXTRA = 128;
    static final int ARENA_ADDR = 0, ARENA_LEN = 8, ARENA_CAP = 16;

    /* service block ring (poller plane) — per-slot offsets; the block
       base is SVC_OFFSET + slot * SVC_SLOT_BYTES */
    static final int SVC_SEQ = 0;
    static final int SVC_RSP = 8;
    static final int SVC_ID = 16;
    static final int SVC_ARGS = 24;
    static final int SVC_RET = 280;
    static final int SVC_WORDS = 32;
    static final int SVC_SLOT_BYTES = 320;
    static final int SVC_SLOTS = 4;

    /* callback-queue ring slots (poller plane) */
    static final int CBQ_SEQ = 0;
    static final int CBQ_RSP = 8;
    static final int CBQ_FN = 16;
    static final int CBQ_USER = 24;
    static final int CBQ_REQ_ADDR = 32;
    static final int CBQ_REQ_LEN = 40;
    static final int CBQ_RPLY_ADDR = 48;
    static final int CBQ_RPLY_CAP = 56;
    static final int CBQ_RPLY_LEN = 64;

    /* command ids */
    static final int CMD_BASE = 0x8000;
    static final int CMD_OPEN = 0x8001;
    static final int CMD_CLOSE = 0x8002;
    static final int CMD_ABI = 0x8003;
    static final int CMD_SCRIPT_TYPE = 0x8004;
    static final int CMD_SCRIPT_ID = 0x8005;
    static final int CMD_STATE_SIZE = 0x8006;
    static final int CMD_DISPOSE = 0x8007;
    static final int CMD_REGISTER = 0x8008;
    static final int CMD_SHUTDOWN = 0x800A;
    static final int CMD_RECLAIMER = 0x800B;

    /* service ids (mirror the JcmFfmHostVTable entries) */
    static final int SVC_ACQUIRE_MODEL = 0;
    static final int SVC_RELEASE_MODEL = 1;
    static final int SVC_RASTERIZE_TEXT = 2;
    static final int SVC_MEASURE_FONT_TEXT = 3;
    static final int SVC_DRAW_FONT_TEXT = 4;
    static final int SVC_CREATE_RESOURCES = 5;
    static final int SVC_INVOKE_API = 6;

    /* status codes (JCM_SHM_* in the header) */
    static final int STATUS_OK = 0;
    static final int ERR_NOT_INIT = 1;
    static final int ERR_BAD_LAYOUT = 2;
    static final int ERR_MODULE = 3;
    static final int ERR_INSTANCE = 4;
    static final int ERR_CREATE = 5;
    static final int ERR_RENDER = 6;
    static final int ERR_RESOURCES = 7;
    static final int ERR_REQUEST = 8;
    static final int ERR_SNAPSHOT = 9;
    static final int ERR_OVERFLOW = 10;
    static final int ERR_DETACHED = 11;

    /** Fresh region epoch per creation (nonzero, fits the header's u32). */
    private static final AtomicLong EPOCH_SEQ = new AtomicLong(1);

    private final Path file;
    private final MappedByteBuffer mapped;
    private final long base;
    private final long size;
    private final Fields fields;
    /** Poller mode: the handshake pointer files (all deleted on close). */
    private final List<Path> handshakeFiles;

    /* window allocator state. freeOffsets/freeSizes hold ABSOLUTE
       addresses; entries are kept unique and coalesced on free. */
    private long bump = WINDOWS_OFFSET;
    private long[] freeOffsets = new long[16];
    private long[] freeSizes = new long[16];
    private int freeCount;

    private NativeShm(Path file, MappedByteBuffer mapped, long base, long size, Fields fields,
                      List<Path> handshakeFiles) {
        this.file = file;
        this.mapped = mapped;
        this.base = base;
        this.size = size;
        this.fields = fields;
        this.handshakeFiles = handshakeFiles;
    }

    long base() {
        return base;
    }

    long size() {
        return size;
    }

    /* ------------------------------------------------------------------ */
    /* creation / teardown                                                 */
    /* ------------------------------------------------------------------ */

    /**
     * Creates + maps the region. FFM mode keeps the classic random temp
     * name (the address travels via the jcm_shm_init downcall); poller
     * mode uses the fixed discoverable name and writes the handshake
     * pointer files the C++ poller scans for.
     */
    static NativeShm open(long requestedSize, boolean pollerMode) throws IOException {
        final long size = Math.max(WINDOWS_OFFSET + 4096L, requestedSize);
        final long pid = ProcessHandle.current().pid();
        final Path dir = Paths.get(System.getProperty("java.io.tmpdir", "."));
        final Path file;
        final List<Path> handshake = new ArrayList<>();
        if (pollerMode) {
            file = dir.resolve("jcm_shmp_" + pid + ".tmp");
            /* A stale file from a crashed earlier run with a reused pid must
               never be mistaken for a live region: remove, then create. */
            try { Files.deleteIfExists(file); } catch (IOException ignored) {}
            Files.createFile(file);
        } else {
            file = Files.createTempFile(dir, "jcm_shm_" + pid + "_", ".tmp");
        }
        file.toFile().deleteOnExit();
        try (FileChannel channel = FileChannel.open(file,
                StandardOpenOption.READ, StandardOpenOption.WRITE)) {
            channel.truncate(size);
            final MappedByteBuffer mapped = channel.map(FileChannel.MapMode.READ_WRITE, 0, size);
            mapped.order(ByteOrder.nativeOrder());
            final long base = NativeArenaView.addressOf(mapped);
            if (base == 0) {
                try { invokeCleaner(mapped); } catch (Throwable ignored) {}
                throw new IOException("MappedByteBuffer has no raw address on this JVM");
            }
            final Fields fields = Fields.forMapped(mapped);
            final int epoch = (int) EPOCH_SEQ.getAndIncrement();
            /* Publish the header BEFORE any reader sees the region. */
            fields.putLong(HDR_MAGIC, MAGIC);
            fields.putInt(HDR_VERSION, LAYOUT_VERSION);
            fields.putInt(12, 0);
            fields.putLong(HDR_SIZE, size);
            fields.putLong(24, CONTROL_OFFSET);
            fields.putLong(32, RESULT_OFFSET);
            fields.putLong(40, WINDOWS_OFFSET);
            fields.putInt(HDR_EPOCH, epoch);
            fields.putLong(HDR_READY, 0);            /* the poller publishes this */
            fields.putLong(HDR_MODE, pollerMode ? 1 : 0);
            fields.putLong(64, SVC_OFFSET);
            fields.putLong(72, CBQ_OFFSET);
            if (pollerMode) {
                writeHandshakeFiles(dir, file, size, epoch, handshake);
            }
            return new NativeShm(file, mapped, base, size, fields, handshake);
        } catch (Throwable t) {
            for (Path hs : handshake) {
                try { Files.deleteIfExists(hs); } catch (IOException ignored) {}
            }
            try { Files.deleteIfExists(file); } catch (IOException ignored) {}
            if (t instanceof IOException) throw (IOException) t;
            throw new IOException("Cannot create shared memory region: " + t, t);
        }
    }

    /**
     * Writes the handshake pointer file (region path / size / epoch) into
     * every candidate directory the C++ poller scans (TMPDIR / TEMP, /tmp,
     * /var/tmp on POSIX — the poller probes the same list, so at least one
     * copy is always found on every supported platform). The env-var
     * override {@code JCM_SHM_HANDSHAKE_DIR} is honored by both sides.
     */
    private static void writeHandshakeFiles(Path defaultDir, Path regionFile, long size,
                                            int epoch, List<Path> out) {
        final String content = regionFile.toAbsolutePath() + "\n" + size + "\n" + epoch + "\n";
        final List<Path> dirs = new ArrayList<>();
        final String override = System.getenv("JCM_SHM_HANDSHAKE_DIR");
        if (override != null && !override.isEmpty()) dirs.add(Paths.get(override));
        dirs.add(defaultDir);
        final String tmpdir = System.getenv("TMPDIR");
        final boolean windows = System.getProperty("os.name", "").toLowerCase().contains("win");
        if (!windows) {
            if (tmpdir != null && !tmpdir.isEmpty()) dirs.add(Paths.get(tmpdir));
            dirs.add(Paths.get("/tmp"));
            dirs.add(Paths.get("/var/tmp"));
        } else {
            final String temp = System.getenv("TEMP");
            final String tmp = System.getenv("TMP");
            if (temp != null && !temp.isEmpty()) dirs.add(Paths.get(temp));
            if (tmp != null && !tmp.isEmpty()) dirs.add(Paths.get(tmp));
        }
        for (Path dir : dirs) {
            try {
                final Path hs = dir.resolve("jcm_shm_hs_" + ProcessHandle.current().pid());
                Files.writeString(hs, content);
                hs.toFile().deleteOnExit();
                if (!out.contains(hs)) out.add(hs);
            } catch (IOException e) {
                /* One candidate being unwritable is fine — the others (or
                   the default, which equals TMPDIR/TEMP on every stock
                   Linux/macOS/Windows JVM) carry the handshake. */
                JCMLogger.debug("Native shared memory handshake write to {} failed: {}", dir, e.toString());
            }
        }
    }

    private static void invokeCleaner(MappedByteBuffer buffer) throws Exception {
        /* JDK 9+; every supported Minecraft runs on 16 or newer. Resolve by
           NAME + arity, not by exact parameter type: the signature moved from
           java.nio.Buffer to java.nio.ByteBuffer across releases and a missed
           unmap is exactly the kind of leak this close() exists to prevent. */
        final sun.misc.Unsafe unsafe = sunUnsafe();
        for (java.lang.reflect.Method method : unsafe.getClass().getMethods()) {
            if (method.getName().equals("invokeCleaner") && method.getParameterCount() == 1) {
                method.invoke(unsafe, buffer);
                return;
            }
        }
        throw new NoSuchMethodException("sun.misc.Unsafe.invokeCleaner");
    }

    private static sun.misc.Unsafe sunUnsafe() throws Exception {
        final java.lang.reflect.Field field = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
        field.setAccessible(true);
        return (sun.misc.Unsafe) field.get(null);
    }

    /**
     * Unmaps the region, deletes the handshake pointer files and the region
     * file. Callers must guarantee that no wrapper (arena view or window)
     * is used afterwards — the driver reset order in
     * NativeScriptManager.reload() does.
     */
    void close() {
        for (Path hs : handshakeFiles) {
            try { Files.deleteIfExists(hs); } catch (IOException ignored) {}
        }
        handshakeFiles.clear();
        try {
            invokeCleaner(mapped);
        } catch (Throwable t) {
            JCMLogger.warn("Native shared memory region unmap failed (process exit will reclaim it): {}", t.toString());
        }
        try {
            Files.deleteIfExists(file);
        } catch (IOException ignored) {
            // DELETE_ON_CLOSE + deleteOnExit are the safety nets
        }
    }

    /** Diagnostic name for a status code (see shm_channel.h). */
    static String statusText(int status) {
        switch (status) {
            case STATUS_OK: return "ok";
            case ERR_NOT_INIT: return "channel not initialized";
            case ERR_BAD_LAYOUT: return "layout mismatch (bridge and jar disagree)";
            case ERR_MODULE: return "module not registered";
            case ERR_INSTANCE: return "instance not registered";
            case ERR_CREATE: return "mtrCreate failed";
            case ERR_RENDER: return "mtrRender failed";
            case ERR_RESOURCES: return "host resources failed";
            case ERR_REQUEST: return "bad request";
            case ERR_SNAPSHOT: return "invalid snapshot";
            case ERR_OVERFLOW: return "reply window too small";
            case ERR_DETACHED: return "poller detached";
            default: return "unknown";
        }
    }

    /* ------------------------------------------------------------------ */
    /* slot access                                                          */
    /*                                                                     */
    /* The raw putLong/getLong below are REGION-relative (header writes);  */
    /* the control/result wrappers bake in the slot base offsets so the    */
    /* channel can address slots with their FIELD offsets directly.       */
    /* ------------------------------------------------------------------ */

    void putLong(long offset, long value) {
        fields.putLong(offset, value);
    }

    void putLongRelease(long offset, long value) {
        fields.putLongRelease(offset, value);
    }

    long getLongAcquire(long offset) {
        return fields.getLongAcquire(offset);
    }

    long getLong(long offset) {
        return fields.getLong(offset);
    }

    void putInt(long offset, int value) {
        fields.putInt(offset, value);
    }

    int getInt(long offset) {
        return fields.getInt(offset);
    }

    void putControlLong(int field, long value) {
        fields.putLong(CONTROL_OFFSET + field, value);
    }

    void putControlLongRelease(int field, long value) {
        fields.putLongRelease(CONTROL_OFFSET + field, value);
    }

    void putControlInt(int field, int value) {
        fields.putInt(CONTROL_OFFSET + field, value);
    }

    long getResultLongAcquire(int field) {
        return fields.getLongAcquire(RESULT_OFFSET + field);
    }

    int getResultInt(int field) {
        return fields.getInt(RESULT_OFFSET + field);
    }

    long getResultLong(int field) {
        return fields.getLong(RESULT_OFFSET + field);
    }

    /**
     * Bulk-copies {@code byteLength} bytes of the result slot (starting at
     * {@code field}) into {@code out} at index 0 — used by
     * NativeChannel.finishFrame to mirror the arena triples into the same
     * 128-byte descriptor buffer the direct-render downcall writes, so one
     * parser serves both planes.
     */
    void readResultInto(ByteBuffer out, int field, int byteLength) {
        for (int i = 0; i < byteLength; i += 8) {
            out.putLong(fields.getLong(RESULT_OFFSET + field + i));
        }
    }

    /* ------------------------------------------------------------------ */
    /* window allocator                                                    */
    /* ------------------------------------------------------------------ */

    /**
     * Allocates a snapshot window. Returns a reusable direct-byte-buffer
     * view of the window, or null when the region is exhausted (caller
     * falls back to {@code ByteBuffer.allocateDirect}).
     */
    synchronized ByteBuffer alloc(int requestedCapacity) {
        if (requestedCapacity <= 0) return null;
        final int capacity = (requestedCapacity + 15) & ~15;
        long address = 0;
        /* first fit */
        for (int i = 0; i < freeCount; i++) {
            if (freeSizes[i] >= capacity) {
                address = freeOffsets[i];
                final long remainder = freeSizes[i] - capacity;
                if (remainder >= 64) {
                    freeOffsets[i] = address + capacity;
                    freeSizes[i] = remainder;
                } else {
                    freeCount--;
                    freeOffsets[i] = freeOffsets[freeCount];
                    freeSizes[i] = freeSizes[freeCount];
                }
                break;
            }
        }
        if (address == 0) {
            if (size - bump >= capacity) {
                address = base + bump;
                bump += capacity;
            } else {
                return null;
            }
        }
        final ByteBuffer view = NativeArenaView.allocate();
        NativeArenaView.bind(view, address, capacity, capacity);
        return view;
    }

    /** Frees a window previously handed out by {@link #alloc}. No-op for buffers this region does not own. */
    synchronized void free(ByteBuffer window) {
        if (window == null || !window.isDirect()) return;
        final long address = NativeArenaView.addressOf(window);
        if (address < base + WINDOWS_OFFSET || address >= base + size) return;
        final int capacity = window.capacity();
        if (capacity <= 0 || (capacity & 15) != 0) return;
        if (address + capacity > base + size) return;
        /* coalesce with adjacent free blocks */
        long mergedStart = address;
        long mergedEnd = address + capacity;
        for (int i = 0; i < freeCount; ) {
            final long start = freeOffsets[i];
            final long end = start + freeSizes[i];
            if (end == mergedStart) {
                mergedStart = start;
                removeFree(i);
            } else if (start == mergedEnd) {
                mergedEnd = end;
                removeFree(i);
            } else {
                i++;
            }
        }
        if (freeCount == freeOffsets.length) {
            final long[] newOffsets = new long[freeCount * 2];
            final long[] newSizes = new long[freeCount * 2];
            System.arraycopy(freeOffsets, 0, newOffsets, 0, freeCount);
            System.arraycopy(freeSizes, 0, newSizes, 0, freeCount);
            freeOffsets = newOffsets;
            freeSizes = newSizes;
        }
        freeOffsets[freeCount] = mergedStart;
        freeSizes[freeCount] = mergedEnd - mergedStart;
        freeCount++;
    }

    private void removeFree(int index) {
        freeCount--;
        freeOffsets[index] = freeOffsets[freeCount];
        freeSizes[index] = freeSizes[freeCount];
    }

    /** True when {@code buffer} points into this region (a window or the mapping itself). */
    synchronized boolean owns(ByteBuffer buffer) {
        if (buffer == null || !buffer.isDirect()) return false;
        final long address = NativeArenaView.addressOf(buffer);
        return address >= base && address < base + size;
    }

    /* ------------------------------------------------------------------ */
    /* field accessors                                                     */
    /* ------------------------------------------------------------------ */

    interface Fields {
        void putLong(long offset, long value);

        void putLongRelease(long offset, long value);

        long getLongAcquire(long offset);

        long getLong(long offset);

        void putInt(long offset, int value);

        int getInt(long offset);

        static Fields forMapped(MappedByteBuffer mapped) {
            if (Runtime.version().feature() >= 22) {
                try {
                    final Fields ffm = FfmFields.create(mapped);
                    ffm.getLong(8); /* touch once inside the guard */
                    JCMLogger.info("Native shared memory: using Foreign Function & Memory API + VarHandle (JDK {})",
                            Runtime.version().feature());
                    return ffm;
                } catch (Throwable t) {
                    JCMLogger.warn("Native shared memory: FFM/VarHandle accessor unavailable ({}), using sun.misc.Unsafe",
                            t.toString());
                }
            }
            return new UnsafeFields(NativeArenaView.addressOf(mapped));
        }
    }

    /** JDK 16..21 (and fallback for anything newer): sun.misc.Unsafe raw-address operations. */
    private static final class UnsafeFields implements Fields {
        private static final sun.misc.Unsafe UNSAFE = sunUnsafeUnchecked();
        private final long base;

        UnsafeFields(long base) {
            this.base = base;
        }

        @Override
        public void putLong(long offset, long value) {
            UNSAFE.putLong(null, base + offset, value);
        }

        @Override
        public void putLongRelease(long offset, long value) {
            /* sun.misc.Unsafe only exposes the volatile (full-fence)
               operations — the acquire/release pair lives in the
               inaccessible jdk.internal.misc. A full fence is a
               conservative superset; the JDK 22+ FFM/VarHandle accessor
               provides true release/acquire when available. */
            UNSAFE.putLongVolatile(null, base + offset, value);
        }

        @Override
        public long getLongAcquire(long offset) {
            return UNSAFE.getLongVolatile(null, base + offset);
        }

        @Override
        public long getLong(long offset) {
            return UNSAFE.getLong(null, base + offset);
        }

        @Override
        public void putInt(long offset, int value) {
            UNSAFE.putInt(null, base + offset, value);
        }

        @Override
        public int getInt(long offset) {
            return UNSAFE.getInt(null, base + offset);
        }

        private static sun.misc.Unsafe sunUnsafeUnchecked() {
            try {
                return sunUnsafe();
            } catch (Exception e) {
                throw new IllegalStateException("Cannot access sun.misc.Unsafe", e);
            }
        }
    }

    /**
     * JDK 22+: Foreign Function &amp; Memory API + VarHandle. MemorySegment
     * view of the mapped buffer + ValueLayout var handles, all resolved
     * reflectively (the mod compiles against Java 16) and adapted into
     * primitive-only interface methods via MethodHandleProxies, so the
     * per-frame access cost is a plain (JIT-inlinable) interface call.
     */
    private static final class FfmFields implements Fields {
        private final LongWriter longRelease;
        private final LongReader longAcquire;
        private final LongWriter longSet;
        private final LongReader longGet;
        private final IntWriter intSet;
        private final IntReader intGet;

        interface LongWriter {
            void set(long offset, long value);
        }

        interface LongReader {
            long get(long offset);
        }

        interface IntWriter {
            void set(long offset, int value);
        }

        interface IntReader {
            int get(long offset);
        }

        private FfmFields(LongWriter longRelease, LongReader longAcquire, LongWriter longSet,
                          LongReader longGet, IntWriter intSet, IntReader intGet) {
            this.longRelease = longRelease;
            this.longAcquire = longAcquire;
            this.longSet = longSet;
            this.longGet = longGet;
            this.intSet = intSet;
            this.intGet = intGet;
        }

        static Fields create(MappedByteBuffer mapped) throws Throwable {
            final Class<?> valueLayout = Class.forName("java.lang.foreign.ValueLayout");
            final Object javaLong = valueLayout.getField("JAVA_LONG").get(null);
            final Object javaInt = valueLayout.getField("JAVA_INT").get(null);
            final Method varHandle = valueLayout.getMethod("varHandle");
            final VarHandle longs = (VarHandle) varHandle.invoke(javaLong);
            final VarHandle ints = (VarHandle) varHandle.invoke(javaInt);

            final Class<?> segmentClass = Class.forName("java.lang.foreign.MemorySegment");
            final Object segment = segmentClass
                    .getMethod("ofBuffer", java.nio.Buffer.class)
                    .invoke(null, mapped);

            final LongWriter longRelease = proxy(longs, VarHandle.AccessMode.SET_RELEASE, segment, LongWriter.class);
            final LongReader longAcquire = proxy(longs, VarHandle.AccessMode.GET_ACQUIRE, segment, LongReader.class);
            final LongWriter longSet = proxy(longs, VarHandle.AccessMode.SET, segment, LongWriter.class);
            final LongReader longGet = proxy(longs, VarHandle.AccessMode.GET, segment, LongReader.class);
            final IntWriter intSet = proxy(ints, VarHandle.AccessMode.SET, segment, IntWriter.class);
            final IntReader intGet = proxy(ints, VarHandle.AccessMode.GET, segment, IntReader.class);
            return new FfmFields(longRelease, longAcquire, longSet, longGet, intSet, intGet);
        }

        private static <T> T proxy(VarHandle handle, VarHandle.AccessMode mode, Object segment,
                                   Class<T> iface) throws Throwable {
            /* After bindTo(segment) the access-mode handle is exactly
               (long offset[, value]) -> [value] with primitive arguments,
               which matches the interface method signatures verbatim. */
            final MethodHandle mh = handle.toMethodHandle(mode).bindTo(segment);
            return MethodHandleProxies.asInterfaceInstance(iface, mh);
        }

        @Override
        public void putLong(long offset, long value) {
            longSet.set(offset, value);
        }

        @Override
        public void putLongRelease(long offset, long value) {
            longRelease.set(offset, value);
        }

        @Override
        public long getLongAcquire(long offset) {
            return longAcquire.get(offset);
        }

        @Override
        public long getLong(long offset) {
            return longGet.get(offset);
        }

        @Override
        public void putInt(long offset, int value) {
            intSet.set(offset, value);
        }

        @Override
        public int getInt(long offset) {
            return intGet.get(offset);
        }
    }
}
