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

/**
 * NativeShm — the file-backed shared memory region behind the
 * "shared memory" Java-C++ transport.
 *
 * <h2>Region anatomy (mirrors native/include/mtr/shm_channel.h)</h2>
 * <pre>
 *   [ header 0..16K )        magic / version / geometry, written once
 *   [ control slot @8K )     per-frame REQUEST written by Java
 *   [ result slot  @8K+128 )  per-frame RESULT written by C++
 *   [ windows 16K..end )       bump-allocated snapshot buffers
 * </pre>
 *
 * The file is mapped ONCE with {@code FileChannel#map} — a
 * {@link MappedByteBuffer} — and stays strongly referenced for the
 * lifetime of the transport. C++ reaches the same pages through the
 * mapping's base address, which it receives once via
 * {@code NativeChannel.nativeShmInit}; it never opens the file itself.
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
 * the JNI-style input path instead of crashing.
 *
 * <p>Threading: allocations happen on the render thread, frees also come
 * from the client tick thread (driver dispose) — both funnel through a
 * coarse {@code synchronized}, which is uncontended in steady state
 * (zero allocator traffic per frame once buffers exist).
 *
 * <p>Package-private, JDK-only: compiled into the bare-javac JNI test
 * harness and exercised by NativeIntegrationTest on every CI OS.
 */
final class NativeShm {
    /* ---- layout, MUST match native/include/mtr/shm_channel.h ---- */
    static final long MAGIC = 0x314D48534D59434AL; /* "JCMYSHM1" LE */
    static final int LAYOUT_VERSION = 1;
    static final int HEADER_SIZE = 8192;
    static final int CONTROL_OFFSET = 8192;
    static final int CONTROL_SIZE = 128;
    static final int RESULT_OFFSET = 8320;
    static final int RESULT_SIZE = 256;
    static final int WINDOWS_OFFSET = 16384;

    /* control slot */
    static final int CTL_SEQ = 0;
    static final int CTL_MODULE = 8;
    static final int CTL_INSTANCE = 16;
    static final int CTL_KIND = 24;
    static final int CTL_SNAP_LEN = 28;
    static final int CTL_SNAP_ADDR = 32;

    /* result slot */
    static final int RES_SEQ = 0;
    static final int RES_STATUS = 8;
    static final int RES_COUNT = 12;
    static final int RES_RECORDS = 16;
    static final int RES_STRINGS = 40;
    static final int RES_PIXELS = 64;
    static final int RES_MATRICES = 88;
    static final int RES_FLOATS = 112;
    static final int ARENA_ADDR = 0, ARENA_LEN = 8, ARENA_CAP = 16;

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

    private final Path file;
    private final MappedByteBuffer mapped;
    private final long base;
    private final long size;
    private final Fields fields;

    /* window allocator state. freeOffsets/freeSizes hold ABSOLUTE
       addresses; entries are kept unique and coalesced on free. */
    private long bump = WINDOWS_OFFSET;
    private long[] freeOffsets = new long[16];
    private long[] freeSizes = new long[16];
    private int freeCount;

    private NativeShm(Path file, MappedByteBuffer mapped, long base, long size, Fields fields) {
        this.file = file;
        this.mapped = mapped;
        this.base = base;
        this.size = size;
        this.fields = fields;
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

    static NativeShm open(long requestedSize) throws IOException {
        final long size = Math.max(WINDOWS_OFFSET + 4096L, requestedSize);
        final Path dir = Paths.get(System.getProperty("java.io.tmpdir", "."));
        final Path file = Files.createTempFile(dir, "jcm_shm_" + ProcessHandle.current().pid() + "_", ".tmp");
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
            /* Publish the header BEFORE nativeShmInit reads it. */
            fields.putLong(0, MAGIC);
            fields.putInt(8, LAYOUT_VERSION);
            fields.putInt(12, 0);
            fields.putLong(16, size);
            fields.putLong(24, CONTROL_OFFSET);
            fields.putLong(32, RESULT_OFFSET);
            fields.putLong(40, WINDOWS_OFFSET);
            return new NativeShm(file, mapped, base, size, fields);
        } catch (Throwable t) {
            try { Files.deleteIfExists(file); } catch (IOException ignored) {}
            if (t instanceof IOException) throw (IOException) t;
            throw new IOException("Cannot create shared memory region: " + t, t);
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
     * Unmaps the region and deletes the file. Callers must guarantee that
     * no wrapper (arena view or window) is used afterwards — the driver
     * reset order in NativeScriptManager.reload() does.
     */
    void close() {
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
            default: return "unknown";
        }
    }

    /* ------------------------------------------------------------------ */
    /* slot access                                                          */
    /*                                                                     */
    /* The raw putLong/getLong below are REGION-relative (header writes);  */
    /* the control/result wrappers bake in the slot base offsets so the   */
    /* channel can address slots with their FIELD offsets directly — the  */
    /* bug class where CTL_* was silently applied to the header area is   */
    /* exactly why these exist.                                            */
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
