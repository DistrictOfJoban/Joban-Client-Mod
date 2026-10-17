package com.lx862.mtrscripting.mod.impl.mtr.vehicle;

import com.lx862.mtrscripting.core.annotation.ApiInternal;
import org.mtr.core.data.TransportMode;

import java.util.HashMap;
import java.util.Map;

/**
 * Joins simplified interchange names to the original routes' transport modes
 * and — when the MAGIC MTR Addon is installed — their extended route types.
 *
 * <p>A (color, simplified name) group may contain several actual routes.
 * Each contributes its {@link TransportMode} and, optionally, a MAGIC type
 * string ("train_metro", ...). A field is only reported when every route in
 * the group agrees; a mixed group keeps the field unknown, exactly like the
 * enum-only behaviour before MAGIC support:
 *
 * <ul>
 *   <li>mixed modes (train + boat) → mode unknown, type unknown;</li>
 *   <li>mixed MAGIC types (train_metro + train_tram) → mode TRAIN (all agree),
 *       type falls back to the mode's base id so callers still get a truthy,
 *       if coarser, answer;</li>
 *   <li>MAGIC absent or no overrides → mode as before, type = base id.</li>
 * </ul>
 */
@ApiInternal
final class InterchangeRouteModes {
    private final Map<String, Entry> modes = new HashMap<>();

    /** Resolved per (color, name) group: agreed mode + agreed MAGIC type. */
    static final class Entry {
        /** Agreed enum mode, or null when the group is mixed/missing. */
        final TransportMode mode;
        /** Agreed MAGIC id, null when mixed or MAGIC has nothing to say. */
        final String extendedType;
        /** First known originating route id (0 when unknown). */
        final long routeId;

        Entry(TransportMode mode, String extendedType) {
            this(mode, extendedType, 0);
        }

        Entry(TransportMode mode, String extendedType, long routeId) {
            this.mode = mode;
            this.extendedType = extendedType;
            this.routeId = routeId;
        }

        /** Canonical type id: MAGIC id, else base mode id, else "unknown". */
        String typeId() {
            if (extendedType != null) return extendedType;
            return MagicRouteTypes.baseTypeId(mode);
        }
    }

    private static String key(int color, String name) {
        // SimplifiedRoute.addInterchangeRoutes removes the first || suffix.
        // Keep whitespace and single | language separators exactly as MTR does.
        final int suffix = name.indexOf("||");
        return color + "\u0000" + (suffix < 0 ? name : name.substring(0, suffix));
    }

    void add(int color, String name, TransportMode mode) {
        add(color, name, mode, null, 0);
    }

    void add(int color, String name, TransportMode mode, String extendedType) {
        add(color, name, mode, extendedType, 0);
    }

    /**
     * Contribute one route's mode, extended type and route id to its
     * (color, name) group. Mode/type may be null (older server data omits
     * modes; MAGIC may have no override) — null never erases a value the
     * group already agreed on.
     */
    void add(int color, String name, TransportMode mode, String extendedType, long routeId) {
        if (name == null) return;
        final String key = key(color, name);
        final Entry existing = modes.get(key);
        if (existing == null) {
            modes.put(key, new Entry(mode, extendedType, routeId));
            return;
        }
        final TransportMode mergedMode = (existing.mode == mode || mode == null)
                ? existing.mode : null;
        final String mergedType;
        if (existing.extendedType == null) mergedType = extendedType;
        else if (extendedType == null || extendedType.equals(existing.extendedType)) mergedType = existing.extendedType;
        else mergedType = null;
        final long mergedRouteId = existing.routeId != 0 ? existing.routeId : routeId;
        // Only install the merged entry when something actually changed —
        // HashMap.put on an existing key would re-allocate the node.
        if (mergedMode != existing.mode || mergedType != existing.extendedType
                || mergedRouteId != existing.routeId) {
            modes.put(key, new Entry(mergedMode, mergedType, mergedRouteId));
        }
        // A mixed-mode name/color group has no single truthful transport mode.
        // Retain UNKNOWN rather than assuming that all interchange routes are trains.
    }

    /** Agreed enum mode for the group, or null when mixed/missing. */
    TransportMode get(int color, String name) {
        final Entry entry = getEntry(color, name);
        return entry == null ? null : entry.mode;
    }

    /** Full entry (mode + extended type) or null when unknown. */
    Entry getEntry(int color, String name) {
        return name == null ? null : modes.get(key(color, name));
    }
}
