package com.lx862.jcm.nativeapi;

import com.lx862.mtrscripting.mod.impl.mtr.vehicle.VehicleWrapper;
import org.mtr.core.data.Route;
import org.mtr.core.data.Siding;
import org.mtr.core.data.SimplifiedRoute;
import org.mtr.core.data.StationExit;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.ObjectArrayList;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * NativeSnapshot — marshals a {@link VehicleWrapper} into the flat
 * {@code JcmVehicleSnapshot} POD that native vehicle scripts read.
 *
 * <p>Java twin of the ABI's "VehicleSnapshotBuilder": the hundreds of
 * reflective getter calls the JS path makes per frame collapse into ONE
 * sequential write here, and the script then reads plain memory.
 *
 * <p><b>The byte layout must match {@code native/include/mtr/mtr_native.h}
 * (ABI 6).</b> Offsets are written through named constants rather than a
 * chain of relative {@code put} calls, so an accidental reorder becomes a
 * name error instead of silent corruption. {@link #selfCheck()} validates
 * the struct sizes at load time.
 *
 * <p>Strings live in ONE shared UTF-8 pool at the end of the blob; every
 * {@code *_offset} is a byte offset from the START of the blob
 * (ABI-critical, see the header's OFFSET CONVENTION note).
 */
public final class NativeSnapshot {

    /** Generous worst case: 8 cars, ~40 stops, exits and interchanges. */
    public static final int CAPACITY = 256 * 1024;

    /* ---- JcmVehicleSnapshot ---- */
    private static final int SZ_VEHICLE = 184;
    private static final int OFF_VEHICLE_ID = 0;
    private static final int OFF_SIDING_ID = 8;
    private static final int OFF_THIS_ROUTE_ID = 16;
    private static final int OFF_DEPARTURE_INDEX = 24;
    private static final int OFF_CAR_COUNT = 32;
    private static final int OFF_TRANSPORT_MODE = 36;
    private static final int OFF_SPEED_KMH = 40;
    private static final int OFF_SPEED_MS = 48;
    private static final int OFF_RAIL_PROGRESS = 56;
    private static final int OFF_DOOR_VALUE = 64;
    private static final int OFF_NOTCH_LEVEL = 72;
    private static final int OFF_FLAGS = 76;
    private static final int OFF_TOTAL_DWELL = 88;
    private static final int OFF_ELAPSED_DWELL = 96;
    private static final int OFF_GAME_TIME = 104;
    private static final int OFF_IN_GAME_TIME = 112;
    private static final int OFF_CAR_OFFSET = 120;
    private static final int OFF_STOP_COUNT = 124;
    private static final int OFF_STOP_OFFSET = 128;
    private static final int OFF_THIS_ROUTE_STOP_COUNT = 132;
    private static final int OFF_THIS_ROUTE_STOP_OFFSET = 136;
    private static final int OFF_NEXT_ROUTE_STOP_COUNT = 140;
    private static final int OFF_NEXT_ROUTE_STOP_OFFSET = 144;
    private static final int OFF_NEXT_STOP_INDEX = 148;
    private static final int OFF_ROUTE_NAME_OFFSET = 152;
    private static final int OFF_ROUTE_NAME_LEN = 156;
    private static final int OFF_ROUTE_COLOR = 160;
    private static final int OFF_CIRCULAR_STATE = 164;
    private static final int OFF_SIDING_NAME_OFFSET = 168;
    private static final int OFF_SIDING_NAME_LEN = 172;
    private static final int OFF_STRING_POOL_OFFSET = 176;
    private static final int OFF_STRING_POOL_LEN = 180;

    /* ---- JcmCar ---- */
    private static final int SZ_CAR = 20;
    private static final int CAR_LENGTH = 0;
    private static final int CAR_WIDTH = 4;
    private static final int CAR_LEFT_DOOR = 8;
    private static final int CAR_RIGHT_DOOR = 9;
    private static final int CAR_RENDERED = 10;
    private static final int CAR_TYPE_OFFSET = 12;
    private static final int CAR_TYPE_LEN = 16;

    /* ---- JcmStop ---- */
    private static final int SZ_STOP = 88;
    private static final int STOP_ROUTE_ID = 0;
    private static final int STOP_STATION_ID = 8;
    private static final int STOP_PLATFORM_ID = 16;
    private static final int STOP_DISTANCE = 24;
    private static final int STOP_DWELL = 32;
    private static final int STOP_NAME_OFFSET = 40;
    private static final int STOP_NAME_LEN = 44;
    private static final int STOP_DEST_OFFSET = 48;
    private static final int STOP_DEST_LEN = 52;
    private static final int STOP_CUSTOM_OFFSET = 56;
    private static final int STOP_CUSTOM_LEN = 60;
    private static final int STOP_IC_COUNT = 64;
    private static final int STOP_IC_OFFSET = 68;
    private static final int STOP_EXIT_COUNT = 72;
    private static final int STOP_EXIT_OFFSET = 76;
    private static final int STOP_ROUTE_CIRCULAR = 80;
    private static final int STOP_SWITCHOVER = 81;

    /* ---- JcmInterchange / JcmExit / JcmStrRef ---- */
    private static final int SZ_INTERCHANGE = 16;
    private static final int SZ_EXIT = 16;
    private static final int SZ_STR_REF = 8;

    private NativeSnapshot() {
    }

    /** Cheap ABI sanity probe; a mismatch means the jar and the .dll disagree. */
    public static boolean selfCheck() {
        return SZ_VEHICLE == 184 && SZ_CAR == 20 && SZ_STOP == 88
                && SZ_INTERCHANGE == 16 && SZ_EXIT == 16 && SZ_STR_REF == 8;
    }

    /* ------------------------------------------------------------------ */

    /**
     * Flattens {@code v} into {@code out}. The buffer is cleared and left
     * FLIPPED (position 0, limit = end of blob) — hand it straight to JNI.
     */
    public static ByteBuffer build(ByteBuffer out, VehicleWrapper v) { return build(out, v, 8); }

    public static ByteBuffer build(ByteBuffer out, VehicleWrapper v, int abi) {
        final int interchangeSize = abi >= 8 ? 16 : 12;
        final List<VehicleWrapper.Stop> allStops = new ArrayList<>(v.getStops());
        final List<VehicleWrapper.Stop> thisRouteStops = new ArrayList<>(v.getThisRouteStops());
        final List<VehicleWrapper.Stop> nextRouteStops = new ArrayList<>(v.getNextRouteStops());
        final int allStopCount = allStops.size();
        // Current and next routes need their own contiguous ranges. The
        // current route is not necessarily the prefix of a multi-route trip.
        allStops.addAll(thisRouteStops);
        allStops.addAll(nextRouteStops);
        final int carCount = v.getCarCount();

        out.clear();
        out.order(ByteOrder.nativeOrder());
        out.position(0);

        /* ---- reserve the header ---- */
        out.position(SZ_VEHICLE);

        /* ---- cars ---- */
        final int carOffset = out.position();
        for (int i = 0; i < carCount; i++) {
            final int p = carOffset + i * SZ_CAR;
            out.putFloat(p + CAR_LENGTH, (float) v.getLength(i));
            out.putFloat(p + CAR_WIDTH, (float) v.getWidth(i));
            out.put(p + CAR_LEFT_DOOR, (byte) (v.doorLeftOpen[i] ? 1 : 0));
            out.put(p + CAR_RIGHT_DOOR, (byte) (v.doorRightOpen[i] ? 1 : 0));
            out.put(p + CAR_RENDERED, (byte) (v.isCarRendered(i) ? 1 : 0));
            out.putInt(p + CAR_TYPE_OFFSET, 0);
            out.putInt(p + CAR_TYPE_LEN, 0);
        }
        out.position(carOffset + carCount * SZ_CAR);

        /* ---- interchanges (JcmInterchange[] pool, one block per stop) ---- */
        final int[] stopIcOffset = new int[allStops.size()];
        for (int s = 0; s < allStops.size(); s++) {
            final List<VehicleWrapper.Stop.RouteInterchange> ics = allStops.get(s).routeInterchanges;
            if (ics == null || ics.isEmpty()) continue;
            stopIcOffset[s] = out.position();
            for (int j = 0; j < ics.size(); j++) {
                final int p = stopIcOffset[s] + j * interchangeSize;
                out.putInt(p, ics.get(j).color);
                out.putInt(p + 4, 0);      /* name offset, patched in the pool pass */
                out.putInt(p + 8, 0);
                if (abi >= 8) out.putInt(p + 12, ics.get(j).getTransportMode() == null ? -1 : ics.get(j).getTransportMode().ordinal());
            }
            out.position(stopIcOffset[s] + ics.size() * interchangeSize);
        }

        /* ---- exits (JcmExit[] pool; destinations are JcmStrRef[]) ---- */
        final List<List<ExitData>> stopExits = new ArrayList<>(allStops.size());
        final int[] stopExitOffset = new int[allStops.size()];
        final ExitCache exitCache = new ExitCache();
        for (int s = 0; s < allStops.size(); s++) {
            final List<ExitData> exits = exitCache.get(allStops.get(s));
            stopExits.add(exits);
            if (exits.isEmpty()) continue;
            stopExitOffset[s] = out.position();
            for (int j = 0; j < exits.size(); j++) {
                final int p = stopExitOffset[s] + j * SZ_EXIT;
                out.putInt(p, 0);                              /* name offset */
                out.putInt(p + 4, 0);                          /* name length */
                out.putInt(p + 8, exits.get(j).destinations.size());
                out.putInt(p + 12, 0);                         /* str-ref offset */
            }
            out.position(stopExitOffset[s] + exits.size() * SZ_EXIT);
        }

        /* ---- stops (reserve; filled after the pool is known) ---- */
        out.position((out.position() + 7) & ~7); // JcmStop contains int64/double
        final int stopOffset = out.position();
        out.position(stopOffset + allStops.size() * SZ_STOP);

        /* ---- string pool ---- */
        final int poolStart = out.position();
        final Pool pool = new Pool(out);

        final long[] stopName = new long[allStops.size()];
        final long[] stopDest = new long[allStops.size()];
        final long[] stopCustom = new long[allStops.size()];
        for (int s = 0; s < allStops.size(); s++) {
            final VehicleWrapper.Stop stop = allStops.get(s);
            stopName[s] = pool.add(stop.name);
            stopDest[s] = pool.add(stop.destinationName);
            stopCustom[s] = pool.add(stop.customDestination);
        }
        /* interchange names — patch the reserved blocks */
        for (int s = 0; s < allStops.size(); s++) {
            final List<VehicleWrapper.Stop.RouteInterchange> ics = allStops.get(s).routeInterchanges;
            if (ics == null || ics.isEmpty()) continue;
            for (int j = 0; j < ics.size(); j++) {
                final long ref = pool.add(ics.get(j).name);
                final int p = stopIcOffset[s] + j * interchangeSize;
                out.putInt(p + 4, Pool.offset(ref));
                out.putInt(p + 8, Pool.length(ref));
            }
        }
        /* exit names + destination str-ref arrays */
        for (int s = 0; s < allStops.size(); s++) {
            final List<ExitData> exits = stopExits.get(s);
            if (exits.isEmpty()) continue;
            for (int j = 0; j < exits.size(); j++) {
                final ExitData ex = exits.get(j);
                final int p = stopExitOffset[s] + j * SZ_EXIT;
                final long nameRef = pool.add(ex.name);
                out.putInt(p, Pool.offset(nameRef));
                out.putInt(p + 4, Pool.length(nameRef));

                out.position((out.position() + 3) & ~3);
                final int refOffset = out.position();
                out.position(refOffset + ex.destinations.size() * SZ_STR_REF);
                for (int k = 0; k < ex.destinations.size(); k++) {
                    final long dref = pool.add(ex.destinations.get(k));
                    final int q = refOffset + k * SZ_STR_REF;
                    out.putInt(q, Pool.offset(dref));
                    out.putInt(q + 4, Pool.length(dref));
                }
                out.putInt(p + 12, refOffset);
            }
        }
        /* car vehicle-type ids (the car array sits before the pool) */
        for (int i = 0; i < carCount; i++) {
            final long ref = pool.add(v.getVehicleId(i));
            final int p = carOffset + i * SZ_CAR;
            out.putInt(p + CAR_TYPE_OFFSET, Pool.offset(ref));
            out.putInt(p + CAR_TYPE_LEN, Pool.length(ref));
        }
        /* route + siding names */
        final SimplifiedRoute route = thisRouteStops.isEmpty() ? null : thisRouteStops.get(0).route;
        final long routeNameRef = pool.add(route == null ? "" : route.getName());
        final Siding siding = v.getSiding();
        final long sidingRef = pool.add(siding == null ? "" : siding.getName());
        final int poolEnd = out.position();

        /* ---- header ---- */
        out.putLong(OFF_VEHICLE_ID, v.getId());
        out.putLong(OFF_SIDING_ID, siding == null ? 0 : siding.getId());
        out.putLong(OFF_THIS_ROUTE_ID, route == null ? 0 : route.getId());
        out.putLong(OFF_DEPARTURE_INDEX, v.getDepartureIndex());
        out.putInt(OFF_CAR_COUNT, carCount);
        out.putInt(OFF_TRANSPORT_MODE, transportModeOrdinal(v));
        out.putDouble(OFF_SPEED_KMH, v.getSpeedKmh());
        out.putDouble(OFF_SPEED_MS, v.getSpeedMs());
        out.putDouble(OFF_RAIL_PROGRESS, v.getRailProgress());
        out.putDouble(OFF_DOOR_VALUE, v.getDoorValue());
        out.putInt(OFF_NOTCH_LEVEL, v.getNotchLevel());
        out.put(OFF_FLAGS, (byte) (v.isReversed() ? 1 : 0));
        out.put(OFF_FLAGS + 1, (byte) (v.isOnRoute() ? 1 : 0));
        out.put(OFF_FLAGS + 2, (byte) (v.isDoorOpening() ? 1 : 0));
        out.put(OFF_FLAGS + 3, (byte) (v.isCurrentlyManual() ? 1 : 0));
        out.put(OFF_FLAGS + 4, (byte) (v.isManualAllowed() ? 1 : 0));
        out.put(OFF_FLAGS + 5, (byte) (v.isClientPlayerRiding() ? 1 : 0));
        out.put(OFF_FLAGS + 6, (byte) (v.isRendered() ? 1 : 0));
        out.put(OFF_FLAGS + 7, (byte) 0);
        out.putDouble(OFF_TOTAL_DWELL, v.getTotalDwellTime());
        out.putDouble(OFF_ELAPSED_DWELL, v.getElapsedDwellTime());
        out.putLong(OFF_GAME_TIME, System.currentTimeMillis());
        out.putLong(OFF_IN_GAME_TIME, 0);
        out.putInt(OFF_CAR_OFFSET, carOffset);
        out.putInt(OFF_STOP_COUNT, allStopCount);
        out.putInt(OFF_STOP_OFFSET, stopOffset);
        out.putInt(OFF_THIS_ROUTE_STOP_COUNT, thisRouteStops.size());
        out.putInt(OFF_THIS_ROUTE_STOP_OFFSET, stopOffset + allStopCount * SZ_STOP);
        out.putInt(OFF_NEXT_ROUTE_STOP_COUNT, nextRouteStops.size());
        out.putInt(OFF_NEXT_ROUTE_STOP_OFFSET,
                stopOffset + (allStopCount + thisRouteStops.size()) * SZ_STOP);
        out.putInt(OFF_NEXT_STOP_INDEX, v.getNextStopIndex(thisRouteStops));
        out.putInt(OFF_ROUTE_NAME_OFFSET, Pool.offset(routeNameRef));
        out.putInt(OFF_ROUTE_NAME_LEN, Pool.length(routeNameRef));
        out.putInt(OFF_ROUTE_COLOR, route == null ? 0 : (route.getColor() | 0xFF000000));
        out.put(OFF_CIRCULAR_STATE, (byte) circularOrdinal(route));
        out.putInt(OFF_SIDING_NAME_OFFSET, Pool.offset(sidingRef));
        out.putInt(OFF_SIDING_NAME_LEN, Pool.length(sidingRef));
        out.putInt(OFF_STRING_POOL_OFFSET, poolStart);
        out.putInt(OFF_STRING_POOL_LEN, poolEnd - poolStart);

        /* ---- stops ---- */
        for (int s = 0; s < allStops.size(); s++) {
            final VehicleWrapper.Stop stop = allStops.get(s);
            final int p = stopOffset + s * SZ_STOP;
            out.putLong(p + STOP_ROUTE_ID, stop.route == null ? 0 : stop.route.getId());
            out.putLong(p + STOP_STATION_ID, stop.station == null ? 0 : stop.station.getId());
            out.putLong(p + STOP_PLATFORM_ID, stop.platform == null ? 0 : stop.platform.getId());
            out.putDouble(p + STOP_DISTANCE, stop.distance);
            out.putDouble(p + STOP_DWELL, stop.dwellTimeMillis);
            out.putInt(p + STOP_NAME_OFFSET, Pool.offset(stopName[s]));
            out.putInt(p + STOP_NAME_LEN, Pool.length(stopName[s]));
            out.putInt(p + STOP_DEST_OFFSET, Pool.offset(stopDest[s]));
            out.putInt(p + STOP_DEST_LEN, Pool.length(stopDest[s]));
            final boolean hasCustom = stop.customDestination != null && !stop.customDestination.isEmpty();
            out.putInt(p + STOP_CUSTOM_OFFSET, hasCustom ? Pool.offset(stopCustom[s]) : -1);
            out.putInt(p + STOP_CUSTOM_LEN, hasCustom ? Pool.length(stopCustom[s]) : 0);
            final int icCount = stop.routeInterchanges == null ? 0 : stop.routeInterchanges.size();
            out.putInt(p + STOP_IC_COUNT, icCount);
            out.putInt(p + STOP_IC_OFFSET, icCount == 0 ? 0 : stopIcOffset[s]);
            final List<ExitData> exits = stopExits.get(s);
            out.putInt(p + STOP_EXIT_COUNT, exits.size());
            out.putInt(p + STOP_EXIT_OFFSET, exits.isEmpty() ? 0 : stopExitOffset[s]);
            /* v5: use this stop's route rather than the current route. */
            out.put(p + STOP_ROUTE_CIRCULAR, (byte) circularOrdinal(stop.route));
            out.put(p + STOP_SWITCHOVER, (byte) (stop.isRouteSwitchoverStop ? 1 : 0));
            out.put(p + STOP_SWITCHOVER + 1, (byte) 0);
            out.put(p + STOP_SWITCHOVER + 2, (byte) 0);
        }

        out.position(poolEnd);
        out.flip();
        return out;
    }

    /* ------------------------------------------------------------------ */

    /**
     * Dense UTF-8 string pool with a duplicate cache (names repeat a lot).
     *
     * <p>{@link #add} returns a packed {@code (offset << 32) | length} in a
     * {@code long} instead of an {@code int[2]}. This method runs a few hundred
     * times per frame — once per stop name, destination, interchange, exit and
     * exit destination — and the old two-element array made every one of those
     * a heap allocation, i.e. pure young-generation garbage on the render
     * thread. {@link #offset}/{@link #length} unpack it with no allocation.
     */
    private static final class Pool {
        private final ByteBuffer buf;
        private final Map<String, long[]> cache = new HashMap<>();

        Pool(ByteBuffer buf) {
            this.buf = buf;
        }

        /** @return packed (offset, length); 0L for null/empty (reads back as "") */
        long add(String s) {
            if (s == null || s.isEmpty()) return 0L;
            final long[] hit = cache.get(s);
            if (hit != null) return hit[0];
            final byte[] bytes = s.getBytes(StandardCharsets.UTF_8);
            final int off = buf.position();
            buf.put(bytes);
            final long packed = pack(off, bytes.length);
            cache.put(s, new long[] { packed });
            return packed;
        }

        static long pack(int offset, int length) {
            return ((long) offset << 32) | (length & 0xFFFFFFFFL);
        }

        static int offset(long packed) {
            return (int) (packed >>> 32);
        }

        static int length(long packed) {
            return (int) packed;
        }
    }

    private static int circularOrdinal(SimplifiedRoute route) {
        if (route == null) return 0;
        try {
            final Route.CircularState state = route.getCircularState();
            return state == null ? 0 : state.ordinal();
        } catch (Throwable t) {
            return 0;
        }
    }

    private static int transportModeOrdinal(VehicleWrapper v) {
        try {
            return v.getTransportMode() == null ? 0 : v.getTransportMode().ordinal();
        } catch (Throwable t) {
            return 0;
        }
    }

    /** Station exits (JS: station.getExits()), flattened. */
    private static final class ExitData {
        final String name;
        final List<String> destinations = new ArrayList<>();

        ExitData(String name) {
            this.name = name;
        }
    }

    private static List<ExitData> collectExits(VehicleWrapper.Stop stop) {
        final List<ExitData> out = new ArrayList<>();
        if (stop == null || stop.station == null) return out;
        final ObjectArrayList<StationExit> exits = stop.station.getExits();
        if (exits == null || exits.isEmpty()) return out;
        for (StationExit ex : exits) {
            final ObjectArrayList<String> dests = ex.getDestinations();
            if (dests == null || dests.isEmpty()) continue;   /* same guard as the JS */
            final ExitData data = new ExitData(ex.getName());
            data.destinations.addAll(dests);
            out.add(data);
        }
        return out;
    }

    /**
     * {@link #collectExits} memoised for one build pass.
     *
     * <p>The snapshot repeats the same {@code Stop} objects across the
     * all-stops / this-route / next-route ranges, so without this the exits of a
     * shared station were flattened (and their destination lists copied) up to
     * three times per frame. The memo is per call, so a reload that changes
     * station data is never served stale — the next frame rebuilds it.
     */
    private static final class ExitCache {
        private final java.util.IdentityHashMap<VehicleWrapper.Stop, List<ExitData>> byStop =
                new java.util.IdentityHashMap<>();

        List<ExitData> get(VehicleWrapper.Stop stop) {
            final List<ExitData> hit = byStop.get(stop);
            if (hit != null) return hit;
            final List<ExitData> exits = collectExits(stop);
            byStop.put(stop, exits);
            return exits;
        }
    }
}
