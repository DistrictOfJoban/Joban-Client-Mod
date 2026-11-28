package com.lx862.jcm.nativeapi;

import java.util.concurrent.atomic.AtomicLong;

/**
 * NativePerfCounters — cumulative performance counters for the debug
 * overlay's native section (ABI 16 era "performance panel" extension).
 *
 * <p>Everything here is a monotonically increasing cumulative value; the
 * overlay samples the deltas per rendered HUD frame and derives per-frame
 * / per-second rates. Callers are the NativeHost upcall adapters (host
 * crossings), NativeScriptModule.render (snapshot bytes handed to the
 * native side) and the render drivers (native render micros).</p>
 *
 * <p>Intentionally allocation-free: a handful of AtomicLongs read at HUD
 * frequency cost nothing on the render path.</p>
 */
public final class NativePerfCounters {

    /** Host crossings: every upcall into NativeHost (model acquire, font
     *  rasterization, resource creation, pixel upload, API round trips). */
    static final AtomicLong HOST_CALLS = new AtomicLong();

    /** Snapshot bytes marshalled per render call (the blob handed to
     *  mtrRender), 4 bytes per int field included. */
    static final AtomicLong SNAPSHOT_BYTES = new AtomicLong();

    /** Native render invocations (mtrRender calls issued). */
    static final AtomicLong RENDER_CALLS = new AtomicLong();

    /** Cumulative native render time (the mtrRender call itself,
     *  transport included). */
    static final AtomicLong NATIVE_MICROS = new AtomicLong();

    private NativePerfCounters() {
    }

    /** One host crossing (upcall from C++ into Java). */
    static void hostCall() {
        HOST_CALLS.incrementAndGet();
    }

    static void render(long snapshotBytes, long elapsedMicros) {
        SNAPSHOT_BYTES.addAndGet(snapshotBytes);
        RENDER_CALLS.incrementAndGet();
        NATIVE_MICROS.addAndGet(elapsedMicros);
    }

    public static long hostCalls() { return HOST_CALLS.get(); }
    public static long snapshotBytes() { return SNAPSHOT_BYTES.get(); }
    public static long renderCalls() { return RENDER_CALLS.get(); }
    public static long nativeMicros() { return NATIVE_MICROS.get(); }
}
