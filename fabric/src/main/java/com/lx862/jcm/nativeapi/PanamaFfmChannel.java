package com.lx862.jcm.nativeapi;

import com.lx862.jcm.mod.util.JCMLogger;

/**
 * PanamaFfmChannel — the PLANNED Foreign Function &amp; Memory API
 * transport ("Panama FFM"), intentionally unfinished.
 *
 * <h2>Planned design (not implemented yet)</h2>
 * Where the shared-memory transport still crosses the JNI boundary once
 * per frame (the empty-argument {@code nativeShmKick()} trigger), the FFM
 * transport would replace JNI as the INVOCATION mechanism as well:
 *
 * <ol>
 *   <li>{@code SymbolLookup.libraryLookup(modulePath, arena)} for every
 *       script module (no JNI {@code nOpen}/{@code nClose});</li>
 *   <li>{@code Linker.nativeLinker().downcallHandle(...)} for
 *       {@code mtrCreate / mtrRender / mtrDispose / mtrInit} with the
 *       snapshot address passed as a
 *       {@code MemorySegment.ofAddress(...)} argument;</li>
 *   <li>host upcalls become
 *       {@code Linker.nativeLinker().upcallStub(...)} function pointers
 *       handed to the module through the JcmHostServices table — the
 *       bridge layer becomes optional entirely.</li>
 * </ol>
 *
 * Data exchange would still use the shared-memory region for the bulk
 * payload; only the per-frame call flips from JNI to an FFM downcall.
 * Estimated effect: sits between the two finished transports — hence the
 * config description "性能中等，需 JDK 22 以上，未完成".
 *
 * <p>Today this class only (a) knows whether the running JVM could host
 * it and (b) prints the one-line fallback warning; the actual transport
 * degrades to {@link NativeChannel.Transport#SHARED_MEMORY}.
 */
final class PanamaFfmChannel {
    private static boolean warned;

    private PanamaFfmChannel() {
    }

    /** FFM downcalls need the final java.lang.foreign API (JDK 22+). */
    static boolean available() {
        try {
            return Runtime.version().feature() >= 22;
        } catch (Throwable t) {
            return false;
        }
    }

    /** One-line, once-per-session notice that the FFM option is a stub. */
    static synchronized void warnOnce() {
        if (warned) return;
        warned = true;
        JCMLogger.warn("Panama FFM transport is unfinished ({}), using the shared memory transport instead",
                available() ? "scaffold present, downcalls not wired yet" : "requires JDK 22 or newer");
    }
}
