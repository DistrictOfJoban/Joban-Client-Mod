package com.lx862.jcm.nativeapi;

import com.lx862.jcm.mod.util.JCMLogger;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

/**
 * PanamaFfmUpcalls — the Java side of the bridge's JNI-free host vtable
 * ({@code JcmFfmHostVTable}, see {@code native/include/mtr/ffm_host.h}).
 *
 * <h2>What this is</h2>
 * Every static method here is the target of ONE
 * {@code Linker.upcallStub} created by {@link PanamaFfmChannel}: the C
 * signatures in the vtable arrive as raw addresses ({@code JAVA_LONG}
 * descriptors — register-identical to pointer arguments on every
 * supported platform ABI) plus primitives, and each adapter converts
 * between those raw addresses and the ordinary Java objects
 * ({@link NativeHost}, {@link NativeApiSession}) the JNIEnv-based path
 * used to reach through cached jmethodIDs. The bridge calls a table
 * entry; the stub frames it back into Java; the adapter runs; control
 * returns — the same shape as a JNI Call*Method, minus the JNIEnv, the
 * local-ref frames and the reflection machinery.
 *
 * <h2>Hard rules</h2>
 * <ul>
 *   <li><b>No exception may escape an upcall.</b> The native caller is
 *       inside a C++ frame that cannot unwind Java exceptions — every
 *       body is wrapped and degrades to the same fallback the JNI host
 *       path used, logging the first few distinct failures only.</li>
 *   <li><b>Thread-confined reuse.</b> Upcalls run on whichever JVM thread
 *       made the downcall (render thread, background api worker) — and
 *       different modules render on different executors — so every
 *       reusable buffer/wrapper is {@link ThreadLocal}.</li>
 *   <li><b>Lifetime.</b> Output parameters (pixels, metrics, bounds,
 *       handles, reply length) are written synchronously before returning;
 *       the invoke_api reply rides a per-thread reused buffer whose
 *       address stays valid until the next invoke_api entry on that
 *       thread — exactly the contract {@code ffm_host.h} documents and
 *       the bridge's copy-out honors.</li>
 * </ul>
 *
 * <p>Package-private, JDK-only (no Minecraft classes), and compiled into
 * the bare-javac JNI test harness together with the rest of the channel
 * classes: the harness's Fixture/NativeApiSession stubs let the same
 * adapters run end-to-end on every CI platform.
 */
final class PanamaFfmUpcalls {

    private PanamaFfmUpcalls() {
    }

    /* ------------------------------------------------------------------ */
    /* per-thread reusable marshalling surfaces                            */
    /* ------------------------------------------------------------------ */

    /** Growable reply buffer for invoke_api (grow-once-on-demand). */
    private static final ThreadLocal<ByteBuffer> REPLY = new ThreadLocal<>();
    /** Reused direct-buffer wrapper for the rasterize/draw pixel window. */
    private static final ThreadLocal<ByteBuffer> PIXEL_VIEW = new ThreadLocal<>();

    private static ByteBuffer replyBuffer(int required) {
        ByteBuffer buffer = REPLY.get();
        if (buffer == null || buffer.capacity() < required) {
            final int capacity = Math.max(1024, Integer.highestOneBit(Math.max(required, 1) * 2 - 1) << 1);
            buffer = ByteBuffer.allocateDirect(capacity).order(ByteOrder.nativeOrder());
            REPLY.set(buffer);
        }
        return buffer;
    }

    private static ByteBuffer pixelView(long address, int byteLength) {
        ByteBuffer view = PIXEL_VIEW.get();
        if (view == null) {
            view = NativeArenaView.allocate();
            PIXEL_VIEW.set(view);
        }
        NativeArenaView.bind(view, address, byteLength, byteLength);
        return view;
    }

    /* error logging: first few distinct failures only, never a flood */
    private static void degrade(String site, Throwable t) {
        final String key = site + ':' + t.getClass().getSimpleName();
        if (RECENT.add(key)) {
            JCMLogger.warn("Panama FFM host upcall degraded at {} ({}); further {} suppressed this session",
                    site, t.toString(), t.getClass().getSimpleName());
        }
    }

    private static final java.util.Set<String> RECENT =
            java.util.Collections.newSetFromMap(new java.util.concurrent.ConcurrentHashMap<>());

    /* ------------------------------------------------------------------ */
    /* vtable entries (exact primitive signatures — see ffm_host.h)        */
    /* ------------------------------------------------------------------ */

    /** acquire_model(path, len) -> host model handle (or -1). */
    public static long cbAcquireModel(long pathAddress, int pathLength) {
        try {
            final String path = new String(NativeArenaView.readBytes(pathAddress, pathLength),
                    StandardCharsets.UTF_8);
            final int handle = NativeHost.get().acquireModel(path);
            return handle < 0 ? -1 : handle;
        } catch (Throwable t) {
            degrade("acquire_model", t);
            return -1;
        }
    }

    /** release_model(handle). */
    public static void cbReleaseModel(long hostHandle) {
        try {
            NativeHost.get().releaseModel((int) hostHandle);
        } catch (Throwable t) {
            degrade("release_model", t);
        }
    }

    /** rasterize_text(utf8, len, x, y, max_w, r, g, b, pixels, w, h) -> glyphs (or -1). */
    public static int cbRasterizeText(long utf8Address, int length, int x, int y, int maxWidth,
                                       int red, int green, int blue,
                                       long pixelsAddress, int outWidth, int outHeight) {
        try {
            if (pixelsAddress == 0 || outWidth <= 0 || outHeight <= 0 || length <= 0) return -1;
            final byte[] utf8 = NativeArenaView.readBytes(utf8Address, length);
            final ByteBuffer pixels = pixelView(pixelsAddress, outWidth * outHeight * 4);
            return NativeHost.get().rasterizeText(utf8, x, y, maxWidth, red, green, blue,
                    pixels, outWidth, outHeight);
        } catch (Throwable t) {
            degrade("rasterize_text", t);
            return -1;
        }
    }

    /** measure_font_text(font, font_len, utf8, len, size, sx, sy, out_metrics) -> 0/-1. */
    public static int cbMeasureFontText(long fontAddress, int fontLength,
                                        long utf8Address, int length,
                                        double size, double sx, double sy,
                                        long outMetricsAddress) {
        try {
            if (outMetricsAddress == 0 || length < 0) return -1;
            final byte[] font = NativeArenaView.readBytes(fontAddress, fontLength);
            final byte[] utf8 = NativeArenaView.readBytes(utf8Address, length);
            final double[] metrics = NativeHost.get().measureFontText(font, utf8, size, sx, sy);
            if (metrics == null || metrics.length < 4) return -1;
            for (int i = 0; i < 4; i++) NativeArenaView.putDouble(outMetricsAddress + 8L * i, metrics[i]);
            return 0;
        } catch (Throwable t) {
            degrade("measure_font_text", t);
            return -1;
        }
    }

    /** draw_font_text(...) -> 0/-1, bounds written to out_bounds. */
    public static int cbDrawFontText(long fontAddress, int fontLength,
                                     long utf8Address, int length,
                                     double size, double x, double baseline, double sx, double sy,
                                     double compression, long argb,
                                     int clipX0, int clipY0, int clipX1, int clipY1,
                                     long pixelsAddress, int width, int height,
                                     long outBoundsAddress) {
        try {
            if (pixelsAddress == 0 || width <= 0 || height <= 0 || outBoundsAddress == 0 || length < 0) return -1;
            final byte[] font = NativeArenaView.readBytes(fontAddress, fontLength);
            final byte[] utf8 = NativeArenaView.readBytes(utf8Address, length);
            final ByteBuffer pixels = pixelView(pixelsAddress, width * height * 4);
            final int[] bounds = NativeHost.get().drawFontText(font, utf8, size, x, baseline, sx, sy,
                    compression, (int) argb, clipX0, clipY0, clipX1, clipY1, pixels, width, height);
            if (bounds == null || bounds.length < 4) return -1;
            for (int i = 0; i < 4; i++) NativeArenaView.putInt(outBoundsAddress + 4L * i, bounds[i]);
            return 0;
        } catch (Throwable t) {
            degrade("draw_font_text", t);
            return -1;
        }
    }

    /** create_resources(tex/quad counts, raw arrays, out_handles) -> 0/-1. */
    public static int cbCreateResources(int textureCount, int quadCount,
                                        long widthsAddress, long heightsAddress,
                                        long verticesAddress, long uvAddress,
                                        long slotsAddress, long stagesAddress,
                                        long outHandlesAddress) {
        try {
            if (textureCount < 0 || quadCount < 0 || outHandlesAddress == 0) return -1;
            final int[] widths = new int[textureCount];
            final int[] heights = new int[textureCount];
            NativeArenaView.readInts(widthsAddress, widths);
            NativeArenaView.readInts(heightsAddress, heights);
            final float[] vertices = new float[quadCount * 12];
            final float[] uv = new float[quadCount * 8];
            NativeArenaView.readFloats(verticesAddress, vertices);
            NativeArenaView.readFloats(uvAddress, uv);
            final int[] slots = new int[quadCount];
            final int[] stages = new int[quadCount];
            NativeArenaView.readInts(slotsAddress, slots);
            NativeArenaView.readInts(stagesAddress, stages);
            final int[] handles = NativeHost.get().createResources(widths, heights,
                    vertices, uv, slots, stages);
            if (handles == null || handles.length != textureCount + quadCount) return -1;
            for (int i = 0; i < handles.length; i++) {
                NativeArenaView.putInt(outHandlesAddress + 4L * i, handles[i]);
            }
            return 0;
        } catch (Throwable t) {
            degrade("create_resources", t);
            return -1;
        }
    }

    /**
     * invoke_api(session_key, key_len, request, request_len, out_len) ->
     * reply address (0 = failure). The reply lives in this thread's reused
     * buffer and stays valid until the next invoke_api entry on the same
     * thread; the bridge copies it out synchronously before returning.
     */
    public static long cbInvokeApi(long keyAddress, int keyLength,
                                   long requestAddress, int requestLength,
                                   long outLengthAddress) {
        try {
            if (requestLength < 0 || outLengthAddress == 0) return 0;
            final byte[] key = NativeArenaView.readBytes(keyAddress, keyLength);
            final byte[] request = NativeArenaView.readBytes(requestAddress, requestLength);
            final byte[] reply = NativeApiSession.invoke(key, request);
            final int length = reply == null ? 0 : reply.length;
            final ByteBuffer buffer = replyBuffer(length);
            if (length > 0) NativeArenaView.writeBytes(NativeArenaView.addressOf(buffer), reply);
            /* Volatile form: the length slot lives on the C++ poller thread's
               stack; the store-release orders the reply-byte copy above
               before it, so a length observed through RSP always implies
               its full reply is visible (paired with the full fence the
               ShmPoller spins before publishing the SVC answer). */
            NativeArenaView.putIntVolatile(outLengthAddress, length);
            return NativeArenaView.addressOf(buffer);
        } catch (Throwable t) {
            degrade("invoke_api", t);
            return 0;
        }
    }
}
