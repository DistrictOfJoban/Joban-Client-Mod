package com.lx862.jcm.mod.scripting.psd;

import com.lx862.jcm.mod.scripting.psd.PSDWrapper.PSDRouteWrapper;
import com.lx862.jcm.mod.scripting.psd.PSDWrapper.PSDStopWrapper;
import org.mtr.core.data.Route;
import org.mtr.core.data.SimplifiedRoute;
import org.mtr.core.data.SimplifiedRoutePlatform;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.ObjectArrayList;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Memo for the per-route stop graphs of {@code psd.routes().get(i).stops()} —
 * the same steady-state problem the vehicle wrapper solved with its
 * LIMITED_CACHE: building one route's stop list walks the client's station
 * interchange maps and exit lists for every stop, and the PSD render path
 * (JS wrapper + native snapshot) asks for the SAME routes every frame.
 *
 * <p>Entries are keyed by route id, validated against a cheap signature
 * (platform count + first/last station names, so route edits invalidate) and
 * expire after {@value #TTL_NANOS} ns — route data is client-side static, so
 * the TTL only exists to eventually drop stale entries of deleted routes.
 * The cache is bounded and cleared when the world changes (see
 * {@link #reset()}).</p>
 */
public final class PSDRouteStopCache {

    private static final long TTL_NANOS = 1_000_000_000L;
    private static final int MAX_ENTRIES = 256;

    private static final Map<Long, Entry> CACHE = new HashMap<>();

    private PSDRouteStopCache() {
    }

    private static final class Entry {
        long signature;
        long builtAtNanos;
        List<PSDStopWrapper> stops;
    }

    public static void reset() {
        CACHE.clear();
    }

    /** The route's stops, memoized per route id. {@code simplified == null}
     *  is the dataFetchMode SKIP case — no stop data, nothing to cache. */
    static List<PSDStopWrapper> stopsOf(Route route, SimplifiedRoute simplified) {
        if (simplified == null) return new ArrayList<>();
        final ObjectArrayList<SimplifiedRoutePlatform> platforms = simplified.getPlatforms();
        final long signature = signatureOf(platforms);
        final long now = System.nanoTime();

        final Entry entry = CACHE.get(route.getId());
        if (entry != null && entry.signature == signature && now - entry.builtAtNanos < TTL_NANOS) {
            return entry.stops;
        }

        final String terminusDestination = platforms.isEmpty() ? "" : platforms.get(platforms.size() - 1).getDestination();
        final List<PSDStopWrapper> stops = new ArrayList<>(platforms.size());
        for (SimplifiedRoutePlatform routePlatform : platforms) {
            stops.add(new PSDWrapper.PSDStopWrapper(route, routePlatform, terminusDestination));
        }

        final Entry newEntry = new Entry();
        newEntry.signature = signature;
        newEntry.builtAtNanos = now;
        newEntry.stops = stops;
        if (CACHE.size() >= MAX_ENTRIES) {
            // Cheap pressure valve: drop the lot rather than the LRU bookkeeping —
            // the next frame rebuilds at most MAX_ENTRIES lists.
            CACHE.clear();
        }
        CACHE.put(route.getId(), newEntry);
        return stops;
    }

    private static long signatureOf(ObjectArrayList<SimplifiedRoutePlatform> platforms) {
        if (platforms.isEmpty()) return 0;
        long hash = platforms.size();
        hash = hash * 1000003 + platforms.get(0).getStationName().hashCode();
        hash = hash * 1000003 + platforms.get(platforms.size() - 1).getStationName().hashCode();
        return hash;
    }
}
