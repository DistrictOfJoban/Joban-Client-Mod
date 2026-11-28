package com.lx862.jcm.nativeapi;

import org.mtr.core.data.SimplifiedRoute;
import org.mtr.core.data.SimplifiedRoutePlatform;

import java.lang.reflect.Method;
import java.util.IdentityHashMap;

/**
 * FangSuServiceInfo — soft, reflection-based integration with the FangSu
 * (方速) addon's express-service (快车 / ServiceStop) and hidden-route
 * (Route.getHidden) models, feeding the native (C++) script snapshots'
 * service flag bytes (ABI 16).
 *
 * <p>Mirrors the {@link FangSuStopStatus} pattern: FangSu duck-types
 * MTR's data classes through mixins, so the client-side
 * {@code SimplifiedRoutePlatform} instances already carry the ServiceStop
 * record (serialized as {@code fangsu_service_stop} inside the simplified
 * route platforms that sync to clients). This class is the single place
 * that knows about those APIs — no FangSu class is referenced at compile
 * time, and when the addon is absent every method returns the
 * non-express / non-hidden answer, which is exactly the pre-FangSu
 * behaviour:
 * <ul>
 *   <li>{@code ServiceStop.get(entry)} returns the record
 *       {@code (id, parentRoute, parentStop, express, platformOverride,
 *       statusOverride, bypass)} — only {@code express()} is consumed
 *       here (the stop-status byte of ABI 14 already carries the
 *       SKIPPED/DISABLED side of the same model);</li>
 *   <li>{@code HiddenRoutesClient.getHiddenRouteById(routeId)} returns a
 *       route only present in FangSu's supplementary hidden-route cache —
 *       a route id absent from the MTR main channel but present there is
 *       a hidden route.</li>
 * </ul>
 *
 * <p>Lookups are memoised by data-object identity / route id (the house
 * pattern of {@code FangSuStopStatus}); MTR re-syncs replace the
 * platform instances, so fresh identities naturally invalidate the memo,
 * a size guard drops it wholesale, and a late-loading FangSu clears the
 * memos once. Render-thread callers only; the maps are synchronized
 * anyway because it costs a fraction of the reflective call they guard.
 */
final class FangSuServiceInfo {

    /* MTR_ARRIVAL_FLAG_* bit values (mtr_native.h, ABI 16). */
    static final byte FLAG_EXPRESS = 0x01;
    static final byte FLAG_HIDDEN_ROUTE = 0x02;

    /** Cache size guard — wholesale drop, exactly like FangSuStopStatus. */
    private static final int MAX_CACHE = 2048;

    /** Class-probe retry interval (FangSu may load after the first call). */
    private static final long PROBE_RETRY_NANOS = 1_000_000_000L;

    /** Resolved reflectively; null while FangSu is (thought) absent. */
    private static volatile Method serviceStopExpress;   /* ServiceStop record accessor */
    private static volatile Method serviceStopGet;       /* ServiceStop.get(Object) */
    private static volatile Method hiddenRouteById;      /* HiddenRoutesClient.getHiddenRouteById(long) */
    private static volatile long nextProbeNanos;

    private static final IdentityHashMap<Object, Boolean> EXPRESS_MEMO = new IdentityHashMap<>();
    private static final java.util.concurrent.ConcurrentHashMap<Long, Boolean> HIDDEN_MEMO =
            new java.util.concurrent.ConcurrentHashMap<>();

    private FangSuServiceInfo() {
    }

    /**
     * Whether a route's stop at the given platform runs a FangSu express
     * service (快车 — the arrival overtakes non-express stations).
     *
     * @param route      the {@code SimplifiedRoute} carrying the platform
     *                   list (same object the stop wrappers were built
     *                   from), null when unknown
     * @param platformId the platform id (0 = unresolved)
     * @return true only when FangSu is installed AND the platform's
     *         ServiceStop record exists and is express
     */
    static boolean isExpressService(SimplifiedRoute route, long platformId) {
        if (route == null || platformId == 0) return false;
        final Method get = serviceStopGet;
        if (get == null && !probe()) return false;
        final SimplifiedRoutePlatform platform = platformOf(route, platformId);
        if (platform == null) return false;
        synchronized (EXPRESS_MEMO) {
            final Boolean hit = EXPRESS_MEMO.get(platform);
            if (hit != null) return hit;
        }
        boolean value = false;
        try {
            final Method getter = serviceStopGet != null ? serviceStopGet : serviceStopGet;
            final Method express = serviceStopExpress;
            if (getter != null && express != null) {
                final Object stopRecord = getter.invoke(null, platform);
                if (stopRecord != null) {
                    final Object expressFlag = express.invoke(stopRecord);
                    value = expressFlag instanceof Boolean && (Boolean) expressFlag;
                }
            }
        } catch (Throwable ignored) {
            /* FangSu changed shape — behave as non-express. */
        }
        synchronized (EXPRESS_MEMO) {
            if (EXPRESS_MEMO.size() >= MAX_CACHE) EXPRESS_MEMO.clear();
            EXPRESS_MEMO.put(platform, value);
        }
        return value;
    }

    /**
     * Whether a route id only exists in FangSu's hidden-route client
     * cache (never in the MTR main simplified-route channel). Routes
     * synced through the main channel are never hidden here, so the
     * answer is {@code false} without FangSu, for hidden routes it
     * becomes {@code true} once FangSu's cache has them (the client
     * requests them on demand — a fresh world join may briefly answer
     * false, matching what JS scripts see through FangSu's own wrappers).
     */
    static boolean isHiddenRoute(long routeId) {
        if (routeId == 0) return false;
        final Boolean cached = HIDDEN_MEMO.get(routeId);
        if (cached != null) return cached;
        final Method byId = hiddenRouteById;
        if (byId == null && !probe()) return false;
        final Method method = hiddenRouteById != null ? hiddenRouteById : byId;
        boolean value = false;
        try {
            if (method != null) {
                /* A route the MTR main channel already carries is not
                   "hidden", regardless of FangSu's cache. */
                if (!org.mtr.mod.client.MinecraftClientData.getInstance().simplifiedRouteIdMap.containsKey(routeId)) {
                    final Object hidden = method.invoke(null, routeId);
                    value = hidden != null;
                }
            }
        } catch (Throwable ignored) {
            /* FangSu changed shape — behave as non-hidden. */
        }
        if (HIDDEN_MEMO.size() >= MAX_CACHE) HIDDEN_MEMO.clear();
        HIDDEN_MEMO.put(routeId, value);
        return value;
    }

    /** Linear platform-id match inside the route's platform list. */
    private static SimplifiedRoutePlatform platformOf(SimplifiedRoute route, long platformId) {
        final org.mtr.libraries.it.unimi.dsi.fastutil.objects.ObjectArrayList<SimplifiedRoutePlatform> platforms = route.getPlatforms();
        if (platforms == null || platforms.isEmpty()) return null;
        for (int i = 0; i < platforms.size(); i++) {
            final SimplifiedRoutePlatform platform = platforms.get(i);
            if (platform != null && platform.getPlatformId() == platformId) return platform;
        }
        return null;
    }

    /**
     * Resolves FangSu's ServiceStop / HiddenRoutesClient surfaces once;
     * retried at most once per second so a late load is still picked up.
     * A successful probe after previously cached "absent" answers clears
     * both memos.
     *
     * @return true when both the ServiceStop and the hidden-route
     *         surfaces resolved (either may stay absent on older FangSu
     *         builds — the individual lookups degrade independently)
     */
    private static boolean probe() {
        final long now = System.nanoTime();
        if (now < nextProbeNanos) return false;
        nextProbeNanos = now + PROBE_RETRY_NANOS;
        try {
            final ClassLoader loader = FangSuServiceInfo.class.getClassLoader();
            boolean anyNew = false;
            try {
                final Class<?> serviceStop = Class.forName("com.fangsu.mtr.ServiceStop", false, loader);
                final Method get = serviceStop.getMethod("get", Object.class);
                /* Record accessor: public boolean express(). */
                final Method express = serviceStop.getMethod("express");
                if (serviceStopGet == null || serviceStopExpress == null) anyNew = true;
                serviceStopGet = get;
                serviceStopExpress = express;
            } catch (Throwable ignored) {
                /* Older FangSu without the ServiceStop API — express stays false. */
            }
            try {
                final Class<?> hiddenRoutes = Class.forName("com.fangsu.network.HiddenRoutesClient", false, loader);
                final Method byId = hiddenRoutes.getMethod("getHiddenRouteById", long.class);
                if (hiddenRouteById == null) anyNew = true;
                hiddenRouteById = byId;
            } catch (Throwable ignored) {
                /* Older FangSu without the hidden-route cache — hidden stays false. */
            }
            if (anyNew) {
                /* First success (or shape change): drop anything cached while absent. */
                synchronized (EXPRESS_MEMO) { EXPRESS_MEMO.clear(); }
                HIDDEN_MEMO.clear();
            }
            return serviceStopGet != null || hiddenRouteById != null;
        } catch (Throwable t) {
            return false;
        }
    }
}
