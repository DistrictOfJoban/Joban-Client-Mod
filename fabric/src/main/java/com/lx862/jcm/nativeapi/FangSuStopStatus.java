package com.lx862.jcm.nativeapi;

import org.mtr.core.data.SimplifiedRoute;
import org.mtr.core.data.SimplifiedRoutePlatform;
import org.mtr.core.data.Station;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.ObjectArrayList;

import java.lang.reflect.Method;
import java.util.IdentityHashMap;

/**
 * FangSuStopStatus — soft, reflection-based integration with the FangSu
 * (方速) addon's 暂缓开通 (deferred station opening) / RouteStopStatus model,
 * feeding the native (C++) script snapshots (ABI 14).
 *
 * <p>FangSu duck-types MTR's own data classes through mixins:
 * <ul>
 *   <li>{@code Station} implements {@code com.fangsu.mtr.StationOpening}
 *       ({@code fangsu$isDeferred()}) — the station-level 暂缓开通 flag,
 *       serialized as {@code fangsu_station_deferred} in the station data
 *       that syncs to clients;</li>
 *   <li>{@code SimplifiedRoutePlatform} (and the full {@code RoutePlatformData})
 *       implement {@code com.fangsu.mtr.RouteStopStatus$Holder}
 *       ({@code fangsu$getStopStatus()} → DISABLED / SKIPPED / NORMAL),
 *       serialized as {@code fangsu_stop_status} inside the simplified route
 *       platforms that sync to clients.</li>
 * </ul>
 *
 * <p>This class is the single place that knows about FangSu. It is strictly
 * optional: no FangSu class is referenced at compile time, and when the addon
 * is absent (or its internals change shape) every method returns the
 * non-deferred answer — {@link #STOP_NORMAL} / {@code false} — which is
 * exactly the pre-FangSu behaviour. FangSu handles the JavaScript script
 * surface itself via its own mixins into the mtrscripting data pipeline;
 * this bridge exists so the C++ snapshots carry the same information.
 *
 * <p>Semantics mirror FangSu's {@code StationOpening.displayStatus}:
 * a deferred station overrides any per-platform status to DISABLED
 * (trains still take the configured 越行 bypass track per {@code pathStatus},
 * which is a server-side pathing concern — displays report DISABLED).
 *
 * <p>Lookups are memoised by data-object identity (the house pattern of
 * {@code NativeSnapshot.ExitCache}): MTR re-syncs replace the Station /
 * SimplifiedRoutePlatform instances, so fresh identities naturally
 * invalidate the memo, a size guard drops it wholesale instead of growing
 * unbounded, and a late-loading FangSu (probe succeeded after cached
 * misses) clears both maps once. The class probe itself retries on a short
 * interval, same as {@code MagicRouteTypes}. Render-thread callers only;
 * the maps are synchronized anyway because it costs a fraction of the
 * reflective call they guard.
 */
final class FangSuStopStatus {

    /* JcmStop.stop_status byte values (mtr_native.h, ABI 14). */
    static final byte STOP_NORMAL = 0;
    static final byte STOP_DISABLED = 1;
    static final byte STOP_SKIPPED = 2;

    /** Cache size guard — wholesale drop, exactly like ExitCache. */
    private static final int MAX_CACHE = 2048;

    /** Class-probe retry interval (FangSu may load after the first call). */
    private static final long PROBE_RETRY_NANOS = 1_000_000_000L;

    /** Resolved reflectively; null while FangSu is (thought) absent. */
    private static volatile Method stationIsDeferred;   /* StationOpening#fangsu$isDeferred()Z */
    private static volatile Method platformGetStatus;   /* RouteStopStatus$Holder#fangsu$getStopStatus() */
    private static volatile long nextProbeNanos;

    private static final IdentityHashMap<Object, Byte> PLATFORM_STATUS = new IdentityHashMap<>();
    private static final IdentityHashMap<Object, Byte> STATION_DEFERRED = new IdentityHashMap<>();

    private FangSuStopStatus() {
    }

    /**
     * The display status of one stop — FangSu's
     * {@code StationOpening.displayStatus(stop, station)}: a deferred
     * station reports DISABLED regardless of the platform's own status,
     * otherwise the platform's RouteStopStatus.
     *
     * @param route      the stop's {@code SimplifiedRoute}, null when unknown
     * @param platformId the stop's platform id (0 = unresolved)
     * @param station    the stop's {@code Station}, null when unknown
     * @return 0 normal, 1 disabled, 2 skipped — always 0 without FangSu
     */
    static byte stopStatus(SimplifiedRoute route, long platformId, Station station) {
        /* Fast fail when FangSu is absent (probe is rate-limited to 1/s). */
        if (stationIsDeferred == null && platformGetStatus == null && !probe()) return STOP_NORMAL;
        if (stationDeferred(station)) return STOP_DISABLED;
        if (route == null || platformId == 0) return STOP_NORMAL;
        final SimplifiedRoutePlatform platform = platformOf(route, platformId);
        return platform == null ? STOP_NORMAL : platformStatus(platform);
    }

    /**
     * The station-level 暂缓开通 (deferred opening) flag.
     *
     * @return true when FangSu marks this station deferred; always false
     * without FangSu (or for a null station)
     */
    static boolean stationDeferred(Station station) {
        if (station == null) return false;
        final Method isDeferred = stationIsDeferred;
        if (isDeferred == null && !probe()) return false;
        final Method method = isDeferred != null ? isDeferred : stationIsDeferred;
        if (method == null) return false;
        if (!method.getDeclaringClass().isInstance(station)) return false; /* mixin not applied */
        synchronized (STATION_DEFERRED) {
            final Byte hit = STATION_DEFERRED.get(station);
            if (hit != null) return hit != STOP_NORMAL;
        }
        byte value = 0;
        try {
            final Object result = method.invoke(station);
            if (result instanceof Boolean && (Boolean) result) value = 1;
        } catch (Throwable ignored) {
            /* FangSu changed shape — behave as if not deferred. */
        }
        synchronized (STATION_DEFERRED) {
            if (STATION_DEFERRED.size() >= MAX_CACHE) STATION_DEFERRED.clear();
            STATION_DEFERRED.put(station, value);
        }
        return value != 0;
    }

    /**
     * The per-platform RouteStopStatus, identity-memoised on the platform
     * object (MTR re-syncs hand out fresh instances, so the memo
     * self-invalidates).
     */
    private static byte platformStatus(SimplifiedRoutePlatform platform) {
        final Method getStatus = platformGetStatus;
        if (getStatus == null && !probe()) return STOP_NORMAL;
        final Method method = getStatus != null ? getStatus : platformGetStatus;
        if (method == null) return STOP_NORMAL;
        if (!method.getDeclaringClass().isInstance(platform)) return STOP_NORMAL; /* mixin not applied */
        synchronized (PLATFORM_STATUS) {
            final Byte hit = PLATFORM_STATUS.get(platform);
            if (hit != null) return hit;
        }
        byte value = STOP_NORMAL;
        try {
            final Object result = method.invoke(platform);
            if (result instanceof Enum) {
                final String name = ((Enum<?>) result).name();
                if ("DISABLED".equals(name)) value = STOP_DISABLED;
                else if ("SKIPPED".equals(name)) value = STOP_SKIPPED;
            }
        } catch (Throwable ignored) {
            /* FangSu changed shape — behave as normal. */
        }
        synchronized (PLATFORM_STATUS) {
            if (PLATFORM_STATUS.size() >= MAX_CACHE) PLATFORM_STATUS.clear();
            PLATFORM_STATUS.put(platform, value);
        }
        return value;
    }

    /** Linear platform-id match inside the route's platform list. */
    private static SimplifiedRoutePlatform platformOf(SimplifiedRoute route, long platformId) {
        final ObjectArrayList<SimplifiedRoutePlatform> platforms = route.getPlatforms();
        if (platforms == null || platforms.isEmpty()) return null;
        for (int i = 0; i < platforms.size(); i++) {
            final SimplifiedRoutePlatform platform = platforms.get(i);
            if (platform != null && platform.getPlatformId() == platformId) return platform;
        }
        return null;
    }

    /**
     * Resolves FangSu's duck interfaces once; retried at most once per
     * second so a late load is still picked up. A successful probe after
     * previously cached "absent" answers clears both memos.
     *
     * @return true when both methods resolved
     */
    private static boolean probe() {
        final long now = System.nanoTime();
        if (now < nextProbeNanos) return false;
        nextProbeNanos = now + PROBE_RETRY_NANOS;
        try {
            final ClassLoader loader = FangSuStopStatus.class.getClassLoader();
            final Class<?> stationOpening = Class.forName("com.fangsu.mtr.StationOpening", false, loader);
            final Class<?> statusHolder = Class.forName("com.fangsu.mtr.RouteStopStatus$Holder", false, loader);
            final Method isDeferred = stationOpening.getMethod("fangsu$isDeferred");
            final Method getStatus = statusHolder.getMethod("fangsu$getStopStatus");
            if (stationIsDeferred == null || platformGetStatus == null) {
                /* First success (or shape change): drop anything cached while absent. */
                synchronized (PLATFORM_STATUS) { PLATFORM_STATUS.clear(); }
                synchronized (STATION_DEFERRED) { STATION_DEFERRED.clear(); }
            }
            stationIsDeferred = isDeferred;
            platformGetStatus = getStatus;
            return true;
        } catch (Throwable t) {
            return false;
        }
    }
}
