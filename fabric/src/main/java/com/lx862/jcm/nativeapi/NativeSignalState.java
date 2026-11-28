package com.lx862.jcm.nativeapi;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * NativeSignalState — the signal-state extension point feeding
 * {@code JcmVehicleSnapshot.signal_state} (ABI 16).
 *
 * <p>Signal mods (mtr-nte bridges, FangSu-style addons, or a small glue
 * mod) push the per-vehicle signal state observed ahead of a train; the
 * vehicle snapshot marshaller reads it back for the C++ scripts
 * (PIDS "train held at signal" notices, headlight logic — see
 * {@code mtr::signal_state}). This is the deliberate soft-dependency
 * shape: JCM has no compile-time knowledge of any signal mod, mods have
 * no compile-time knowledge of JCM beyond this one public class, and
 * with nothing pushing states every vehicle reports 0 ("unknown") —
 * byte-identical to the pre-ABI-16 padding.
 *
 * <h2>State values (mirror {@code mtr::signal_state} in vehicle.hpp)</h2>
 * <ul>
 *   <li>{@code 0} unknown (no provider / not probed / expired)</li>
 *   <li>{@code 1} clear (proceed)</li>
 *   <li>{@code 2} caution</li>
 *   <li>{@code 3} danger (stop)</li>
 * </ul>
 *
 * <h2>Integration contract for provider mods</h2>
 * <p>Call {@link #set(long, int)} (reflectively or directly — the class
 * is on the classpath whenever JCM is) from your client tick for every
 * tracked vehicle. Values expire after {@link #TIMEOUT_MILLIS} so a
 * removed signal block or a stopped provider degrades to "unknown"
 * instead of pinning a stale signal aspect. The map is capped; a mass
 * push beyond the cap drops the OLDEST entries wholesale, which is the
 * same guard shape the memo caches in {@code FangSuStopStatus} use.
 */
public final class NativeSignalState {

    /** Signal aspects (values mirror mtr_native.h / mtr::signal_state). */
    public static final byte UNKNOWN = 0;
    public static final byte CLEAR = 1;
    public static final byte CAUTION = 2;
    public static final byte DANGER = 3;

    /** Pushed states older than this read back as UNKNOWN. */
    static final long TIMEOUT_MILLIS = 750;

    /** Guard against a runaway provider flooding the map. */
    private static final int MAX_VEHICLES = 4096;

    private static final class Entry {
        final byte state;
        final long at;
        Entry(byte state, long at) { this.state = state; this.at = at; }
    }

    private static final Map<Long, Entry> STATES = new ConcurrentHashMap<>();

    private NativeSignalState() {
    }

    /**
     * Push (or refresh) the signal state ahead of one vehicle. Called by
     * provider mods on the client thread; cheap enough for every tracked
     * vehicle every tick.
     *
     * @param vehicleId the MTR vehicle id (matches JcmVehicleSnapshot.vehicle_id)
     * @param state     0 unknown .. 3 danger; out-of-range values are clamped
     *                  to UNKNOWN
     */
    public static void set(long vehicleId, int state) {
        if (vehicleId == 0) return;
        if (state < 0 || state > 3) state = UNKNOWN;
        if (state == UNKNOWN) {
            STATES.remove(vehicleId);
            return;
        }
        if (STATES.size() >= MAX_VEHICLES) STATES.clear();
        STATES.put(vehicleId, new Entry((byte) state, System.currentTimeMillis()));
    }

    /**
     * The signal state for a vehicle, or UNKNOWN when no provider pushed
     * a (still fresh) value. Called by the snapshot marshaller.
     */
    static byte get(long vehicleId) {
        final Entry entry = STATES.get(vehicleId);
        if (entry == null) return UNKNOWN;
        if (System.currentTimeMillis() - entry.at > TIMEOUT_MILLIS) {
            STATES.remove(vehicleId);
            return UNKNOWN;
        }
        return entry.state;
    }

    /** Drops everything (native runtime reload / world switch). */
    public static void reset() {
        STATES.clear();
    }
}
