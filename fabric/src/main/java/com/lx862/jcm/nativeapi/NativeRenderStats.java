package com.lx862.jcm.nativeapi;

import com.lx862.jcm.mod.util.JCMLogger;
import java.lang.management.ManagementFactory;
import java.lang.management.GarbageCollectorMXBean;
import java.util.Locale;

/** Passive render-thread phase timings. No script decisions depend on them. */
final class NativeRenderStats {
    private long lastLog = System.nanoTime();
    private long calls, uploads, bytes, totalNs, maxTotal, maxSetup, maxNative, maxReplay;
    private long gcCount = gc(false), gcMillis = gc(true);

    void upload(long length) { uploads++; bytes += length; }

    void frame(String script, long begin, long nativeBegin, long replayBegin) {
        final long end = System.nanoTime();
        calls++;
        totalNs += end - begin;
        maxTotal = Math.max(maxTotal, end - begin);
        maxSetup = Math.max(maxSetup, nativeBegin - begin);
        maxNative = Math.max(maxNative, replayBegin - nativeBegin);
        maxReplay = Math.max(maxReplay, end - replayBegin);
        if (end - lastLog < 60_000_000_000L) return;
        final long nextCount = gc(false), nextMillis = gc(true);
        JCMLogger.info(String.format(Locale.ROOT,
                "[native perf] script=%s interval=%.1fs calls=%d avg_total=%.3fms max_total=%.3fms "
                + "max_setup=%.3fms max_native=%.3fms max_replay=%.3fms uploads=%d upload=%.2fMiB "
                + "jvm_gc_count_delta=%d jvm_gc_ms_delta=%d scope=render_thread_cpu_and_driver_submission",
                script, (end - lastLog) / 1e9, calls, totalNs / (double) calls / 1e6,
                maxTotal / 1e6, maxSetup / 1e6, maxNative / 1e6, maxReplay / 1e6,
                uploads, bytes / 1048576.0, Math.max(0, nextCount - gcCount), Math.max(0, nextMillis - gcMillis)));
        lastLog = end;
        gcCount = nextCount; gcMillis = nextMillis;
        calls = uploads = bytes = totalNs = maxTotal = maxSetup = maxNative = maxReplay = 0;
    }

    private static long gc(boolean time) {
        long total = 0;
        for (GarbageCollectorMXBean bean : ManagementFactory.getGarbageCollectorMXBeans()) {
            total += Math.max(0, time ? bean.getCollectionTime() : bean.getCollectionCount());
        }
        return total;
    }
}
