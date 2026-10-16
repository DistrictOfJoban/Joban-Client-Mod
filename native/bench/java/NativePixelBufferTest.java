package com.lx862.jcm.nativeapi;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.Random;
import java.lang.management.ManagementFactory;

/** Regression test for the zero-copy native upload staging path. */
public final class NativePixelBufferTest {
    public static void main(String[] args) {
        final int sourceWidth = 5;
        final ByteBuffer source = ByteBuffer.allocateDirect(sourceWidth * 4 * 4)
                .order(ByteOrder.LITTLE_ENDIAN);
        for (int row = 0; row < 4; row++) {
            for (int col = 0; col < sourceWidth; col++) {
                source.putInt((row * sourceWidth + col) * 4, 0xA0000000 | row << 8 | col);
            }
        }
        final int[] destination = new int[6 * 5];
        NativePixelBuffer.copy(source, 0, sourceWidth, 1, 1, destination, 6, 2, 1, 3, 2);
        check(destination[1 * 6 + 2] == 0xA0000101, "top-left source pixel");
        check(destination[1 * 6 + 4] == 0xA0000103, "top-right source pixel");
        check(destination[2 * 6 + 2] == 0xA0000201, "second-row source pixel");
        check(destination[2 * 6 + 4] == 0xA0000203, "second-row right pixel");
        check(destination[0] == 0, "destination outside dirty rectangle");
        randomized();
        benchmark();
    }

    private static void randomized() {
        Random random = new Random(719273);
        for (int test = 0; test < 4096; test++) {
            int sw = 1 + random.nextInt(48), sh = 1 + random.nextInt(32);
            int offset = random.nextInt(13), sx = random.nextInt(sw), sy = random.nextInt(sh);
            int w = 1 + random.nextInt(sw - sx), h = 1 + random.nextInt(sh - sy);
            int dw = w + random.nextInt(20), dh = h + random.nextInt(16);
            int dx = random.nextInt(dw - w + 1), dy = random.nextInt(dh - h + 1);
            byte[] bytes = new byte[offset + sw * sh * 4 + 7]; random.nextBytes(bytes);
            ByteBuffer arena = test % 2 == 0 ? ByteBuffer.allocateDirect(bytes.length) : ByteBuffer.allocate(bytes.length);
            arena.put(bytes).position(1).limit(bytes.length - 7);
            arena = arena.asReadOnlyBuffer().order(ByteOrder.BIG_ENDIAN);
            int pos = arena.position(), limit = arena.limit();
            int[] expected = new int[dw * dh]; Arrays.fill(expected, 0x57125A83);
            int[] actual = expected.clone();
            for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) {
                int at = offset + ((sy + y) * sw + sx + x) * 4;
                expected[(dy + y) * dw + dx + x] = bytes[at] & 255 | (bytes[at+1] & 255) << 8
                        | (bytes[at+2] & 255) << 16 | (bytes[at+3] & 255) << 24;
            }
            NativePixelBuffer.copy(arena, offset, sw, sx, sy, actual, dw, dx, dy, w, h);
            check(Arrays.equals(expected, actual), "randomized ARGB/alpha/stride " + test);
            check(pos == arena.position() && limit == arena.limit() && arena.order() == ByteOrder.BIG_ENDIAN,
                    "caller buffer state preserved");
        }
        ByteBuffer shortBuffer = ByteBuffer.allocateDirect(8);
        int[] sentinel = {1,2,3,4};
        try { NativePixelBuffer.copy(shortBuffer, 0, 2, 0, 0, sentinel, 2, 0, 0, 2, 2); throw new AssertionError("truncation accepted"); }
        catch (IllegalArgumentException expected) { check(Arrays.equals(sentinel, new int[]{1,2,3,4}), "validate before writing"); }
        try { NativePixelBuffer.copy(shortBuffer, Integer.MAX_VALUE, Integer.MAX_VALUE, 0, Integer.MAX_VALUE,
                sentinel, 2, 0, 0, 1, Integer.MAX_VALUE); throw new AssertionError("overflow accepted"); }
        catch (IllegalArgumentException expected) { /* reject before arithmetic overflow or allocation */ }
        System.out.println("PASS: 4096 exact ARGB/alpha/stride/unaligned/read-only/byte-order comparisons; bounds and buffer state");
    }

    private static final int[] LUT = new int[1024];
    static { for(int n=0;n<256;n++){ LUT[n]=n; LUT[256+n]=n<<8; LUT[512+n]=n<<16; LUT[768+n]=n<<24; } }
    private static void legacy(ByteBuffer source, int[] destination) {
        byte[] bytes = new byte[source.capacity()];
        source.duplicate().clear().get(bytes);
        for (int n = 0, at = 0; n < destination.length; n++, at += 4) {
            destination[n] = LUT[768 + (bytes[at+3] & 255)] | LUT[512 + (bytes[at+2] & 255)]
                    | LUT[256 + (bytes[at+1] & 255)] | LUT[bytes[at] & 255];
        }
    }
    private static void benchmark() {
        final int width = 2048, height = 1024, count = 12;
        ByteBuffer source = ByteBuffer.allocateDirect(width * height * 4).order(ByteOrder.LITTLE_ENDIAN);
        for(int n=0;n<width*height;n++) source.putInt(n*4, 0xFF314159 ^ n);
        int[] destination = new int[width * height];
        com.sun.management.ThreadMXBean allocation = (com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean();
        allocation.setThreadAllocatedMemoryEnabled(true);
        for(int mode=0;mode<2;mode++) {
            for(int warm=0;warm<20;warm++) {
                if(mode==0) legacy(source,destination); else NativePixelBuffer.copy(source,0,width,0,0,destination,width,0,0,width,height);
            }
            long[] times = new long[50]; long thread=Thread.currentThread().getId();
            long before=allocation.getThreadAllocatedBytes(thread), collections=gc();
            for(int run=0;run<times.length;run++) {
                long start=System.nanoTime();
                for(int texture=0;texture<count;texture++) {
                    if(mode==0) legacy(source,destination); else NativePixelBuffer.copy(source,0,width,0,0,destination,width,0,0,width,height);
                }
                times[run]=System.nanoTime()-start;
                check(destination[12345]==(0xFF314159 ^ 12345),"benchmark data observed");
            }
            long allocated=allocation.getThreadAllocatedBytes(thread)-before, gcDelta=gc()-collections;
            Arrays.sort(times);
            System.out.printf(java.util.Locale.ROOT,
                    "BENCH %s 12x2048x1024 (96MiB/update) median=%.3fms p95=%.3fms allocated_per_update=%.4fMiB gc_collections=%d updates=%d scope=arena_to_owned_image_only%n",
                    mode==0?"legacy":"bulk",times[25]/1e6,times[47]/1e6,allocated/(50.0*1048576),gcDelta,times.length);
        }
    }
    private static long gc() {
        return ManagementFactory.getGarbageCollectorMXBeans().stream().mapToLong(b->Math.max(0,b.getCollectionCount())).sum();
    }

    private static void check(boolean ok, String message) {
        if (!ok) throw new AssertionError(message);
    }
}
