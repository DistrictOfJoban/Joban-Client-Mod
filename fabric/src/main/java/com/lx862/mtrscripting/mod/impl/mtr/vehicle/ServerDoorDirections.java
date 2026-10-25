package com.lx862.mtrscripting.mod.impl.mtr.vehicle;

import com.lx862.jcm.mod.registry.Networking;
import com.lx862.mtrscripting.mod.network.packet.data.RequestDoorDirectionsC2SPacket;
import org.mtr.libraries.it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;

import java.util.HashMap;
import java.util.Map;

/**
 * Client-side cache of the server's per-route door-opening directions —
 * the successor of the old on-disk {@code DoorDirectionStore}, whose role
 * in answering mid-journey predictions is now played by the server (see
 * {@code DoorDirectionManager} on the other side of
 * {@link RequestDoorDirectionsC2SPacket}).
 *
 * <p>The prediction path in {@link VehicleWrapper} consults this cache
 * before attempting its own world scan, so a train on the mainline gets
 * its next stop's door sides even when that platform's chunks are far
 * outside view distance — and even on the very first approach, since the
 * server scans the whole save once, not per-visit.
 *
 * <p>Caching and retry policy: a complete answer is authoritative until
 * refreshed; an incomplete one (route not scanned yet) is retried after
 * {@value #INCOMPLETE_RETRY_MILLIS} ms; every request is rate-limited so a
 * server without this feature — which never answers — only sees a trickle
 * of packets while the client silently falls back to its own scan.
 */
public final class ServerDoorDirections {

    /* Retry/refresh policy (milliseconds). */
    private static final long UNANSWERED_BACKOFF_MILLIS = 10_000;
    private static final long INCOMPLETE_RETRY_MILLIS = 5_000;
    private static final long MISSING_PLATFORM_RETRY_MILLIS = 30_000;
    private static final long COMPLETE_REFRESH_MILLIS = 60_000;

    /* All accessed from the main client thread only (packet handlers and
       vehicle rendering both run there), same as VehicleDataCache. */
    private static final Map<Long, Answer> ANSWERS = new HashMap<>();
    private static final Map<Long, Long> LAST_REQUEST_AT = new HashMap<>();

    private ServerDoorDirections() {
    }

    /** One route's server answer: canonical per-platform sides. */
    private static final class Answer {
        final long fetchedAtMillis;
        final boolean complete;
        final Long2ObjectOpenHashMap<boolean[]> sides;

        Answer(long fetchedAtMillis, boolean complete, Long2ObjectOpenHashMap<boolean[]> sides) {
            this.fetchedAtMillis = fetchedAtMillis;
            this.complete = complete;
            this.sides = sides;
        }
    }

    /**
     * The remembered canonical sides for a platform on the current route, or
     * null when the server has not answered for this platform yet. On a hit
     * against a complete answer that has gone stale, schedules a refresh so
     * server-side block edits eventually reach long-running clients too.
     */
    public static boolean[] sidesFor(long routeId, long platformId) {
        final Answer answer = ANSWERS.get(routeId);
        if(answer == null) return null;
        final boolean[] sides = answer.sides.get(platformId);
        if(sides == null) return null;
        if(answer.complete && System.currentTimeMillis() - answer.fetchedAtMillis >= COMPLETE_REFRESH_MILLIS) {
            /* Complete but stale: schedule one refresh per refresh interval —
               server-side block edits eventually reach long-running clients. */
            requestIfAllowed(routeId, COMPLETE_REFRESH_MILLIS);
        }
        return sides;
    }

    /**
     * To be called whenever {@link #sidesFor} missed: asks the server for
     * the route unless a recent request is already in flight or the retry
     * interval for the current state has not elapsed.
     */
    public static void requestFor(long routeId) {
        final Answer answer = ANSWERS.get(routeId);
        final long now = System.currentTimeMillis();
        if(answer == null) {
            /* Nothing ever came back: back off hard so a JCM-less or
               older-JCM server is not spammed while the client falls back
               to its own scan. */
            requestIfAllowed(routeId, UNANSWERED_BACKOFF_MILLIS);
            return;
        }
        if(!answer.complete) {
            if(now - answer.fetchedAtMillis >= INCOMPLETE_RETRY_MILLIS) {
                requestIfAllowed(routeId, INCOMPLETE_RETRY_MILLIS);
            }
            return;
        }
        /* Complete but the asked platform is not in it (route edited after
         * the scan): retry on a slower cadence. */
        if(now - answer.fetchedAtMillis >= MISSING_PLATFORM_RETRY_MILLIS) {
            requestIfAllowed(routeId, MISSING_PLATFORM_RETRY_MILLIS);
        }
    }

    /**
     * Sends the request unless the rate limit says otherwise. When nothing
     * has ever come back for this route, the limit backs off to
     * {@value #UNANSWERED_BACKOFF_MILLIS} ms — a JCM-less (or older-JCM)
     * server never answers, and the fallback path must not spam it.
     */
    private static void requestIfAllowed(long routeId, long minGapMillis) {
        if(routeId == 0) return;
        final long now = System.currentTimeMillis();
        final Long last = LAST_REQUEST_AT.get(routeId);
        if(last != null && now - last < minGapMillis) return;
        LAST_REQUEST_AT.put(routeId, now);
        Networking.sendPacketToServer(new RequestDoorDirectionsC2SPacket(routeId));
    }

    /** Applies a server answer (called from the S2C packet handler, main client thread). */
    public static void accept(long routeId, boolean complete, long[] platformIds, boolean[] left, boolean[] right) {
        final Long2ObjectOpenHashMap<boolean[]> sides = new Long2ObjectOpenHashMap<>(platformIds.length);
        for(int i = 0; i < platformIds.length; i++) {
            sides.put(platformIds[i], new boolean[]{left[i], right[i]});
        }
        ANSWERS.put(routeId, new Answer(System.currentTimeMillis(), complete, sides));
    }

    /** Drops all cached answers (leaving the world / joining a server). */
    public static void reset() {
        ANSWERS.clear();
        LAST_REQUEST_AT.clear();
    }
}
