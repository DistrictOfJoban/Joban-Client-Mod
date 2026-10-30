package com.lx862.mtrscripting.mod.impl.mtr.vehicle;

import com.lx862.mtrscripting.core.annotation.ApiInternal;
import org.mtr.core.data.RouteType;
import org.mtr.core.data.TransportMode;
import org.mtr.core.tool.Utilities;

import java.lang.reflect.Method;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * MagicRouteTypes — soft, reflection-based integration with the
 * MAGIC MTR Addon (org.justnoone: "MAGIC"/jme) route type overrides.
 *
 * <p>MAGIC extends the {@link TransportMode#TRAIN} selector with display
 * types stored in {@code config/MAGIC/configs/route_types.json}:
 * {@code train_normal}, {@code train_light_rail}, {@code train_high_speed},
 * {@code train_metro}, {@code train_bus}, {@code train_tram}, {@code train_sbahn}.
 * They are plain strings keyed by the route id
 * ({@link Utilities#numberToPaddedHexString(long)}), not new enum values,
 * which is why code that only speaks {@code TransportMode} cannot see them.
 *
 * <p>This class is the single place that knows about MAGIC. It is strictly
 * optional: when the addon is absent, or a route has no override, every
 * method degrades cleanly to the base MTR behaviour. No class from the
 * addon is referenced at compile time, so JCM never hard-depends on MAGIC.
 *
 * <p>Results are memoised per route id with a short TTL: MAGIC's own config
 * map is a {@code ConcurrentHashMap} behind a synchronized getter, but the
 * reflective call plus its boxing still costs more than a map hit, and this
 * is queried per rendered vehicle/station interchange. The TTL keeps type
 * edits made in MAGIC's UI visible within a second while keeping steady-state
 * lookups allocation-free.
 */
@ApiInternal
public final class MagicRouteTypes {

    /** Cache lifetime; MAGIC edits are rare and not latency-critical. */
    private static final long TTL_NANOS = 1_000_000_000L;

    /**
     * Base mode ids cached per enum ordinal — {@link #baseTypeId} runs on the
     * render-thread hot path (once per vehicle and per interchange per
     * frame), and {@code name().toLowerCase()} would allocate a fresh String
     * each call. Indexed by {@link TransportMode#ordinal()}.
     */
    private static final String[] BASE_IDS = buildBaseIds();

    private static String[] buildBaseIds() {
        final TransportMode[] modes = TransportMode.values();
        final String[] ids = new String[modes.length];
        for (int i = 0; i < modes.length; i++) ids[i] = modes[i].name().toLowerCase(Locale.ROOT);
        return ids;
    }

    /** Sentinel stored for "no MAGIC override" so misses are cached too. */
    private static final String NONE = "";

    private static final Map<Long, Cached> CACHE = new ConcurrentHashMap<>();

    /** Resolved once; null for the whole session when MAGIC is absent. */
    private static volatile Method getRouteType;

    /** Method handle liveness is checked at most this often. */
    private static volatile long nextProbeNanos;

    private static final class Cached {
        final String type;
        final long expiresNanos;
        Cached(String type, long expiresNanos) {
            this.type = type;
            this.expiresNanos = expiresNanos;
        }
    }

    private MagicRouteTypes() {
    }

    /** True when the MAGIC addon is present and its config class resolved. */
    public static boolean available() {
        return resolver() != null;
    }

    /**
     * The MAGIC override for one route id, or null when there is none
     * (addon missing, route unknown, or plain TRAIN with no override).
     */
    public static String extendedType(long routeId) {
        final long now = System.nanoTime();
        final Cached hit = CACHE.get(routeId);
        if (hit != null && now < hit.expiresNanos) {
            return hit.type == NONE ? null : hit.type;
        }
        String found = null;
        final Method resolver = resolver();
        if (resolver != null) {
            try {
                final Object value = resolver.invoke(null, key(routeId));
                if (value instanceof String && !((String) value).isEmpty()) found = (String) value;
            } catch (Throwable ignored) {
                /* MAGIC changed its API shape — behave as if absent. */
            }
        }
        CACHE.put(routeId, new Cached(found == null ? NONE : found, now + TTL_NANOS));
        return found;
    }

    /**
     * The canonical transport type id for a route: MAGIC's extended id when
     * present, otherwise the base mode as a lowercase snake_case string
     * ("train", "boat", "cable_car", "airplane"). Never null; unknown modes
     * return "unknown".
     *
     * <p>This is the string surfaced by {@code getTransportType()} on both
     * the JS wrapper and the native (C++) snapshot, so the two sides must
     * agree on it exactly — see native/include/mtr/vehicle.hpp.
     */
    public static String typeId(long routeId, TransportMode mode) {
        final String extended = routeId == 0 ? null : extendedType(routeId);
        if (extended != null) return extended;
        return baseTypeId(mode);
    }

    /**
     * The canonical transport type id for a route, honouring MTR 4's native
     * {@link RouteType} (the "类别" selector on the route editor: NORMAL /
     * LIGHT_RAIL / HIGH_SPEED, synced inside every Route and inside
     * VehicleExtraData for the vehicle's own route).
     *
     * <p>Resolution order mirrors MAGIC's own effective-key logic
     * (EditRouteScreenRouteTypesMixin#getTrainRouteTypeKeyForUi): a
     * route_types.json sidecar override wins when present — MAGIC clears it
     * whenever a native type is picked, so the two never really compete —
     * then a non-NORMAL native RouteType contributes its
     * {@code mode + "_" + routeType} key ("train_light_rail",
     * "train_high_speed", "boat_light_rail", ...: the exact strings MTR's
     * own {@code Route.getRouteTypeKey()} produces and MAGIC reuses for the
     * same types), and the default NORMAL type keeps the plain base mode id
     * ("train") so vanilla-default Train routes stay indistinguishable from
     * untyped ones. Never null.
     */
    public static String typeId(long routeId, TransportMode mode, RouteType routeType) {
        final String extended = routeId == 0 ? null : extendedType(routeId);
        if (extended != null) return extended;
        return nativeTypeKey(mode, routeType);
    }

    /**
     * Same resolution as {@link #typeId(long, TransportMode, RouteType)} but
     * returns null when the route has neither a MAGIC override nor a
     * non-NORMAL native RouteType — the same "nothing to say" contract as
     * {@link #extendedType(long)}, so (color, name)-group merging in
     * {@link InterchangeRouteModes} keeps its old behaviour for plain
     * vanilla routes.
     */
    public static String extendedType(long routeId, TransportMode mode, RouteType routeType) {
        final String extended = routeId == 0 ? null : extendedType(routeId);
        if (extended != null) return extended;
        return routeType != null && routeType != RouteType.NORMAL
                ? nativeTypeKey(mode, routeType) : null;
    }

    /**
     * Canonical id for a native {@link RouteType}: "train_light_rail" /
     * "train_high_speed" / "boat_light_rail" / "boat_high_speed" (the key
     * format MTR's own {@code Route.getRouteTypeKey()} uses) — or the base
     * mode id when the type is null or the default NORMAL.
     */
    public static String nativeTypeKey(TransportMode mode, RouteType routeType) {
        if (mode == null) return "unknown";
        if (routeType != null && routeType != RouteType.NORMAL) {
            return (mode.name() + "_" + routeType.name()).toLowerCase(Locale.ROOT);
        }
        return baseTypeId(mode);
    }

    /** Lowercase base id for a {@link TransportMode}; null-safe. */
    public static String baseTypeId(TransportMode mode) {
        if (mode == null) return "unknown";
        final int ordinal = mode.ordinal();
        return ordinal >= 0 && ordinal < BASE_IDS.length ? BASE_IDS[ordinal] : "unknown";
    }

    /** Route id in MAGIC's normalized key form (uppercase padded hex). */
    private static String key(long routeId) {
        return Utilities.numberToPaddedHexString(routeId).toUpperCase(Locale.ROOT);
    }

    private static Method resolver() {
        final long now = System.nanoTime();
        Method resolver = getRouteType;
        if (resolver != null) return resolver;
        if (now < nextProbeNanos) return null;
        nextProbeNanos = now + TTL_NANOS;
        try {
            final Class<?> config = Class.forName("org.justnoone.jme.config.RouteTypeOverrideConfig");
            resolver = config.getMethod("getRouteType", String.class);
            getRouteType = resolver;
        } catch (Throwable t) {
            getRouteType = null;
        }
        return resolver;
    }
}
