package com.lx862.jcm.nativeapi;

import com.lx862.mtrscripting.core.util.render.ScriptRenderManager;

import java.util.HashMap;
import java.util.Map;

/**
 * NativeDrawRegistry — where the per-car draw calls of a NATIVE script wait
 * between being captured and being replayed.
 *
 * <p>The JS pipeline has the same two-stage split: {@code RenderVehiclesMixin}
 * fills a {@code VehicleScriptInstance.capturedScriptCalls}, and
 * {@code VehicleResourceMixin} replays it while MTR draws that car. Native
 * scripts bypass the whole Rhino instance machinery, so they need their own
 * (much smaller) holder — this class.
 *
 * <p>Keyed by {@code vehicleHexId + '\u0000' + scriptId} so a vehicle that
 * uses several resources/scripts cannot mix their cars up.
 *
 * <p>Thread affinity: both the capture (RenderVehicles.render) and the replay
 * (VehicleResource.queue) happen on the render thread, but assets reload can
 * clear from another thread — hence the synchronized map and the defensive
 * copy in {@link #get}.
 */
public final class NativeDrawRegistry {

    private static final Map<String, Map<Integer, ScriptRenderManager>> ENTRIES = new HashMap<>();

    private NativeDrawRegistry() {
    }

    private static String key(String vehicleHexId, String scriptId) {
        return vehicleHexId + "\u0000" + scriptId;
    }

    /* Cars of one vehicle replay consecutively, so the last hex/script pair
       is memoised instead of concatenating a fresh key string per car per
       frame (10 cars -> 10 concatenations -> 10 young-gen strings). */
    private static String memoHex, memoScript, memoKey;

    private static String memoizedKey(String vehicleHexId, String scriptId) {
        if (memoHex != null && memoHex.equals(vehicleHexId) && memoScript != null && memoScript.equals(scriptId)) {
            return memoKey;
        }
        memoHex = vehicleHexId;
        memoScript = scriptId;
        memoKey = key(vehicleHexId, scriptId);
        return memoKey;
    }

    /**
     * The mutable manager set for one vehicle+script, created on first use.
     * {@code carCount} entries are (re)created so a car that stops being
     * rendered cannot keep stale draw calls alive.
     */
    public static synchronized Map<Integer, ScriptRenderManager> begin(
            String vehicleHexId, String scriptId, int carCount) {
        return begin(key(vehicleHexId, scriptId), carCount);
    }

    /** Variant taking the driver's precomputed {@code hexId + '\u0000' + scriptId}. */
    public static synchronized Map<Integer, ScriptRenderManager> begin(String combinedKey, int carCount) {
        Map<Integer, ScriptRenderManager> managers = ENTRIES.get(combinedKey);
        if (managers == null || managers.size() != carCount) {
            managers = new HashMap<>();
            for (int i = 0; i < carCount; i++) {
                managers.put(i, new ScriptRenderManager());
            }
            ENTRIES.put(combinedKey, managers);
        } else {
            managers.values().forEach(ScriptRenderManager::reset);
        }
        return managers;
    }

    /** Draw calls captured for one car this frame, or null. */
    public static synchronized ScriptRenderManager get(
            String vehicleHexId, String scriptId, int carIndex) {
        final Map<Integer, ScriptRenderManager> managers = ENTRIES.get(memoizedKey(vehicleHexId, scriptId));
        return managers == null ? null : managers.get(carIndex);
    }

    /** True when this vehicle+script has captured anything (skips needless work). */
    public static synchronized boolean has(String vehicleHexId, String scriptId) {
        return ENTRIES.containsKey(key(vehicleHexId, scriptId));
    }

    /** Forget one vehicle+script (script reload, vehicle despawn). */
    public static synchronized void remove(String vehicleHexId, String scriptId) {
        ENTRIES.remove(key(vehicleHexId, scriptId));
        memoHex = memoScript = memoKey = null;
    }

    /** Forget everything (resource reload). */
    public static synchronized void clear() {
        ENTRIES.clear();
        memoHex = memoScript = memoKey = null;
    }
}
