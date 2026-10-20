package com.lx862.jcm.nativeapi;

import com.lx862.jcm.mod.util.JCMLogger;

import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * NativeArenaView — creates REUSED NIO {@link ByteBuffer} views over raw
 * native addresses without a single JNI call.
 *
 * <h2>Why</h2>
 * The shared-memory transport's result slot carries the module-owned output
 * arenas as raw {@code (address, length, capacity)} triples (see
 * {@code native/include/mtr/shm_channel.h}). Java must read those arenas
 * with the exact same {@code NativeFrame} surface the JNI transport
 * produces — which is five ByteBuffers — but unlike the JNI path there is
 * no {@code NewDirectByteBuffer} call to wrap them. This class constructs
 * real {@code sun.nio.ch.DirectByteBuffer} instances via
 * {@code sun.misc.Unsafe.allocateInstance} and pokes the
 * {@code address / position / limit / capacity / mark} fields directly, so
 * one wrapper object per arena is allocated ONCE per module and re-bound
 * every frame with plain field writes. Bounds checking stays intact: the
 * wrapper's {@code limit} makes the existing replay code throw
 * {@code IndexOutOfBoundsException} on a bogus module descriptor instead
 * of reading wild memory — the same safety the JNI direct-buffer path has.
 *
 * <h2>Stability note</h2>
 * The field layout of {@code java.nio.Buffer} has been stable since Java 9
 * (and identical in spirit since 1.4). {@link #selfCheck()} verifies the
 * offsets at runtime against a freshly allocated memory block; any
 * mismatch disables the shared-memory transport and falls back to JNI, so
 * an exotic JVM can never ship a broken wrapping path.
 *
 * <p>Package-private, JDK-only (no Minecraft classes): compiled into the
 * bare-javac JNI test harness alongside NativeScriptManager.
 */
final class NativeArenaView {
    private static final sun.misc.Unsafe UNSAFE = unsafe();
    /** Package-private Unsafe access for the FFM upcall adapters and the
     *  PanamaFfmChannel vtable builder — raw-address reads/writes without
     *  a single restricted FFM method. */
    static sun.misc.Unsafe rawAccess() {
        return UNSAFE;
    }
    private static final int BYTE_ARRAY_BASE = UNSAFE.arrayBaseOffset(byte[].class);
    /**
     * Derived from a LIVE instance instead of a hard-coded name: the
     * concrete direct-buffer class moved between packages across JDK
     * releases (sun.nio.ch.DirectByteBuffer up to JDK 18, java.nio.DirectByteBuffer
     * since JDK 19), and {@code allocateDirect} is guaranteed to return it.
     */
    private static final Class<?> DIRECT_BUFFER_CLASS = ByteBuffer.allocateDirect(0).getClass();
    /**
     * Field offsets inside the wrapper instance. {@code address} may be
     * declared either in Buffer or in MappedByteBuffer depending on the
     * JDK — resolved by walking DOWN from the concrete class, so the
     * outermost (effective) declaration wins.
     */
    private static final long ADDRESS_OFFSET = fieldOffset("address");
    private static final long MARK_OFFSET = fieldOffset("mark");
    private static final long POSITION_OFFSET = fieldOffset("position");
    private static final long LIMIT_OFFSET = fieldOffset("limit");
    private static final long CAPACITY_OFFSET = fieldOffset("capacity");

    private NativeArenaView() {}

    private static sun.misc.Unsafe unsafe() {
        try {
            final Field field = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            return (sun.misc.Unsafe) field.get(null);
        } catch (Throwable t) {
            throw new IllegalStateException("Cannot access sun.misc.Unsafe", t);
        }
    }

    /** Most-derived declaration of {@code name} between the concrete class and Buffer. */
    private static long fieldOffset(String name) {
        for (Class<?> c = DIRECT_BUFFER_CLASS; c != null && c != Object.class; c = c.getSuperclass()) {
            try {
                final Field field = c.getDeclaredField(name);
                return UNSAFE.objectFieldOffset(field);
            } catch (NoSuchFieldException ignored) {
                // walk up
            } catch (Throwable t) {
                throw new IllegalStateException("Cannot resolve Buffer field " + name, t);
            }
        }
        throw new IllegalStateException("Buffer field " + name + " not found");
    }

    /**
     * A fresh, zero-capacity view. The returned buffer is NOT valid until
     * {@link #bind} points it at an arena. Kept package-visible: one
     * wrapper is created per arena per module and re-bound each frame.
     */
    static ByteBuffer allocate() {
        try {
            final ByteBuffer view = (ByteBuffer) UNSAFE.allocateInstance(DIRECT_BUFFER_CLASS);
            UNSAFE.putInt(view, MARK_OFFSET, -1);
            UNSAFE.putInt(view, POSITION_OFFSET, 0);
            UNSAFE.putInt(view, LIMIT_OFFSET, 0);
            UNSAFE.putInt(view, CAPACITY_OFFSET, 0);
            UNSAFE.putLong(view, ADDRESS_OFFSET, 0L);
            /* Raw instances have a zeroed bigEndian flag — which would read
               LITTLE-endian on x64 (accidentally native) but fail on a
               big-endian host. Route through the real setter once. */
            view.order(ByteOrder.nativeOrder());
            return view;
        } catch (Throwable t) {
            throw new IllegalStateException("Cannot allocate DirectByteBuffer view", t);
        }
    }

    /**
     * Points an existing wrapper at an arena. Plain stores are enough:
     * the wrapper is confined to the render thread, and every frame is
     * write-then-read by the same thread that calls {@code nativeShmKick}.
     *
     * @param capacity the arena's full capacity (bounds ceiling), or the
     *                 frame length when a pre-ABI-9 module does not report
     *                 a capacity
     */
    static void bind(ByteBuffer view, long address, int limit, int capacity) {
        UNSAFE.putLong(view, ADDRESS_OFFSET, address);
        UNSAFE.putInt(view, CAPACITY_OFFSET, capacity);
        UNSAFE.putInt(view, LIMIT_OFFSET, limit);
        UNSAFE.putInt(view, POSITION_OFFSET, 0);
        UNSAFE.putInt(view, MARK_OFFSET, -1);
    }

    /**
     * Raw address of a direct buffer (0 for heap buffers). Works for every
     * direct buffer — wrappers created here, plain
     * {@code ByteBuffer.allocateDirect} snapshots, or the file-mapped
     * region — because they all store their address in the same field.
     */
    static long addressOf(ByteBuffer buffer) {
        return buffer == null || !buffer.isDirect() ? 0L : UNSAFE.getLong(buffer, ADDRESS_OFFSET);
    }

    /* ------------------------------------------------------------------ */
    /* raw native-memory helpers (FFM upcall marshalling)                  */
    /* ------------------------------------------------------------------ */

    /** Copies {@code length} bytes at raw {@code address} into a fresh array. */
    static byte[] readBytes(long address, int length) {
        final byte[] out = new byte[length];
        if (length > 0 && address != 0) {
            UNSAFE.copyMemory(null, address, out, BYTE_ARRAY_BASE, length);
        }
        return out;
    }

    /** Copies {@code src} to raw {@code address} (bulk Unsafe, no per-byte loop). */
    static void writeBytes(long address, byte[] src) {
        if (address != 0 && src.length > 0) {
            UNSAFE.copyMemory(src, BYTE_ARRAY_BASE, null, address, src.length);
        }
    }

    /** Reads {@code out.length} int32s from raw {@code address}. */
    static void readInts(long address, int[] out) {
        for (int i = 0; i < out.length; i++) out[i] = UNSAFE.getInt(address + 4L * i);
    }

    /** Reads {@code out.length} floats from raw {@code address}. */
    static void readFloats(long address, float[] out) {
        for (int i = 0; i < out.length; i++) out[i] = UNSAFE.getFloat(address + 4L * i);
    }

    /** Writes an int32 at raw {@code address}. */
    static void putInt(long address, int value) {
        UNSAFE.putInt(address, value);
    }

    /** Writes a double at raw {@code address}. */
    static void putDouble(long address, double value) {
        UNSAFE.putDouble(address, value);
    }

    /**
     * One-shot verification against throwaway memory: validates the field
     * offsets AND the byte order AND the acquire/release Unsafe accessors
     * before the shared-memory transport ever relies on them.
     */
    static boolean selfCheck() {
        final long block = UNSAFE.allocateMemory(64);
        try {
            UNSAFE.putLong(block, 0x0102030405060708L);
            /* allocateMemory is NOT zeroed — initialise every word the
               checks below read, or the verification itself reads garbage. */
            UNSAFE.putLong(block + 8, 0x8877665544332211L);
            UNSAFE.putLong(block + 16, 0L);
            final ByteBuffer view = allocate();
            bind(view, block, 64, 64);
            if (view.get(0) != UNSAFE.getByte(block)) return false;
            if (view.getInt(0) != UNSAFE.getInt(block)) return false;
            if (view.getLong(8) != 0x8877665544332211L) return false;
            view.putLong(16, 0x1122334455667788L);
            if (UNSAFE.getLong(block + 16) != 0x1122334455667788L) return false;
            if (view.capacity() != 64 || view.limit() != 64 || view.position() != 0) return false;
            if (view.order() != ByteOrder.nativeOrder()) return false;
            /* Bounds checks must still throw on the poked wrapper. */
            try {
                view.getInt(64);
                return false;
            } catch (IndexOutOfBoundsException expected) {
                return true;
            }
        } catch (Throwable t) {
            JCMLogger.warn("Native arena view self-check failed: {}", t.toString());
            return false;
        } finally {
            UNSAFE.freeMemory(block);
        }
    }
}
