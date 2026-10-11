package com.lx862.jcm.nativeapi;

import com.lx862.jcm.mod.util.JCMLogger;
import com.lx862.mtrscripting.core.util.render.ScriptRenderManager;
import com.lx862.mtrscripting.mod.impl.mtr.vehicle.VehicleScriptContext;
import com.lx862.mtrscripting.mod.impl.mtr.vehicle.VehicleWrapper;
import com.lx862.mtrscripting.mod.resource.MtrScriptingResourceManager;
import com.lx862.mtrscripting.mod.resource.VehicleResourceProvider;
import org.mtr.mapping.holder.Direction;
import org.mtr.mapping.holder.MinecraftClient;
import org.mtr.mapping.holder.World;
import org.mtr.mod.data.VehicleExtension;
import org.mtr.mod.render.StoredMatrixTransformations;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * NativeVehicleDriver — one frame of a NATIVE vehicle script.
 *
 * <p>Mirrors what {@code RenderVehiclesMixin} does for the JS path:
 * <ol>
 *   <li>build a {@link VehicleWrapper} for this vehicle (per frame, exactly
 *       like the JS path — MTR's stops cache inside it makes that cheap);</li>
 *   <li>marshal it once into the snapshot blob ({@link NativeSnapshot});</li>
 *   <li>run every C++ module registered for the script id
 *       ({@link NativeScriptManager#renderVehicle});</li>
 *   <li>translate the returned draw-call records into
 *       {@link ScriptRenderManager} calls parked in {@link NativeDrawRegistry},
 *       so {@code VehicleResourceMixin} can replay them per car — the same
 *       capture-then-replay split the JS pipeline uses.</li>
 * </ol>
 *
 * <p>Called from the render thread. The state map is concurrent only because a
 * resource reload can clear it from another thread.
 *
 * <p><b>Sound records are ignored on purpose.</b> The JS pipeline plays them
 * through {@code VehicleScriptCallsHolder.carSoundManagers} and
 * {@code VehicleResourceMixin}; native scripts would need the same per-car
 * base-position plumbing, and neither the WR2-A03 port nor any other ported
 * script emits sounds today. Adding it later means one more record kind in
 * {@link #applyFrame}.
 */
public final class NativeVehicleDriver {

    /** Per vehicle+script state that must outlive a frame. */
    private static final class State {
        final ByteBuffer snapshot = ByteBuffer
                .allocateDirect(NativeSnapshot.CAPACITY)
                .order(ByteOrder.nativeOrder());
        final String instanceKey;
        /** First snapshot-build failure, so it is logged once and not per frame. */
        String reportedSnapshotFailure;

        State(String vehicleHexId, String scriptId) {
            this.instanceKey = vehicleHexId + "/" + scriptId;
        }
    }

    private static final Map<String, State> STATES = new ConcurrentHashMap<>();

    /* JcmRecordHeader */
    private static final int HDR_SIZE = 8;
    private static final int KIND_MODEL = 3;
    private static final int KIND_TEXTURE_UPLOAD = 6;

    private NativeVehicleDriver() {
    }

    /**
     * One-shot trace, so a "nothing is drawn" report can be answered from the
     * log instead of guessed at. Every early return in the render path routes
     * through here; each distinct reason is reported once per reload.
     */
    private static final java.util.Set<String> TRACED =
            java.util.concurrent.ConcurrentHashMap.newKeySet();

    private static void traceOnce(String reason, Object... detail) {
        if (TRACED.add(reason)) {
            JCMLogger.info("[native vehicle] {} — {}", reason, format(detail));
        }
    }

    private static String format(Object... detail) {
        final StringBuilder sb = new StringBuilder();
        for (Object o : detail) {
            if (sb.length() > 0) sb.append(' ');
            sb.append(o);
        }
        return sb.toString();
    }

    private static String key(String vehicleHexId, String scriptId) {
        return vehicleHexId + "\u0000" + scriptId;
    }

    /**
     * Renders every native script this vehicle uses. Safe to call for any
     * vehicle: it returns immediately when no C++ module is loaded for the
     * vehicle's script id.
     *
     * @return number of modules that produced a frame (0 when idle).
     */
    public static int render(VehicleExtension vehicle) {
        if (vehicle == null) return 0;
        final int carCount = vehicle.vehicleExtraData.immutableVehicleCars.size();
        if (carCount <= 0) {
            traceOnce("vehicle has no cars", vehicle.getHexId());
            return 0;
        }

        final List<String> ids = scriptIdsOf(vehicle);
        // Most vehicles in a city use only JS or no script. Avoid logging each
        // of those as a missing native module; it hides real native failures.
        ids.removeIf(id -> !NativeScriptManager.isLoaded(id));
        if (ids.isEmpty()) return 0;
        traceOnce("vehicle " + vehicle.getHexId(), "cars=" + carCount, "scriptIds=" + ids,
                "nativeModules=" + NativeScriptManager.getModules().size());

        int rendered = 0;
        for (String scriptId : ids) {
            /* was this id registered as a native (language=cpp) script? */
            if (NativeScriptManager.getModules(scriptId).isEmpty()) {
                traceOnce("no native module registered for id '" + scriptId + "'",
                        "loaded=" + NativeScriptManager.getModules().size(),
                        "skipped=" + NativeScriptManager.getSkippedScripts());
                continue;
            }
            rendered += renderOne(vehicle, scriptId, carCount);
        }
        return rendered;
    }

    private static List<String> scriptIdsOf(VehicleExtension vehicle) {
        final List<String> ids = new ArrayList<>(2);
        final int carCount = vehicle.vehicleExtraData.immutableVehicleCars.size();
        for (int i = 0; i < carCount; i++) {
            final String vehicleId = vehicle.vehicleExtraData.immutableVehicleCars.get(i).getVehicleId();
            final String scriptId = MtrScriptingResourceManager.vehicle.getVehicleScriptEntryId(vehicleId);
            if (scriptId == null || ids.contains(scriptId)) continue;
            ids.add(scriptId);
        }
        return ids;
    }

    private static int renderOne(VehicleExtension vehicle, String scriptId, int carCount) {
        final VehicleResourceProvider.VehicleScriptConfiguration config =
                MtrScriptingResourceManager.vehicle.getVehicleScript(scriptId);
        if (config == null) {
            traceOnce("no VehicleScriptConfiguration for '" + scriptId + "'",
                    "(the native entry never reached vehicleScripts)");
            return 0;
        }
        traceOnce("rendering '" + scriptId + "'", "dataFetchMode=" + config.dataFetchMode());

        final VehicleScriptContext.DataFetchMode fetchMode = config.dataFetchMode();
        final State state = STATES.computeIfAbsent(
                key(vehicle.getHexId(), scriptId), k -> new State(vehicle.getHexId(), scriptId));

        final VehicleWrapper wrapper;
        try {
            wrapper = new VehicleWrapper(fetchMode, vehicle);
        } catch (Throwable t) {
            /* Full trace on purpose: t.toString() alone pointed at an inlined
               getter and made the failing line impossible to find. */
            JCMLogger.error("Native vehicle script failed to build its route data: {}", t.toString());
            JCMLogger.error("  stack:", t);
            return 0;
        }
        /* Same contract as the JS path: a MANDATORY script does not draw until
           the full stop list arrived (its header would show an empty route). */
        if (fetchMode == VehicleScriptContext.DataFetchMode.MANDATORY
                && !wrapper.isStopsDataFullyFetched()) {
            traceOnce("'" + scriptId + "' waiting for the full stop list", "(MANDATORY)");
            return 0;
        }

        /* Build the snapshot FIRST, and if it fails, do NOT call into native
           code at all.

           This is not defensive padding: the JNI bridge hands the module
           GetDirectBufferAddress with GetDirectBufferCapacity, i.e. the WHOLE
           256 KB block, regardless of how much the marshaller actually wrote.
           `ByteBuffer.putX(index, ...)` also does not advance position, so a
           half-written buffer has position 0 while its header is garbage.
           Passing that to mtrRender gives the script a bogus car_count and
           stop_count, and it walks off the end of the blob — which is how a
           route-data NullPointerException turned into a native access
           violation inside memcpy. Skipping the call keeps the previous frame's
           textures on screen instead. */
        try {
            NativeSnapshot.build(state.snapshot, wrapper);
        } catch (Throwable t) {
            if (state.reportedSnapshotFailure == null) {
                state.reportedSnapshotFailure = t.toString();
                JCMLogger.error("Native vehicle script {} could not build a snapshot from the "
                        + "route data (native render skipped): {}", scriptId, t.toString());
                JCMLogger.error("  stack:", t);
            }
            return 0;
        }

        final List<NativeScriptManager.NativeFrame> frames;
        try {
            frames = NativeScriptManager.renderVehicle(state.instanceKey, scriptId, state.snapshot);
        } catch (Throwable t) {
            JCMLogger.error("Native vehicle script {} threw while rendering: {}", scriptId, t.toString());
            JCMLogger.error("  stack:", t);
            return 0;
        }
        if (frames.isEmpty()) {
            traceOnce("'" + scriptId + "' produced no frame",
                    "(both modules returned null — see the bridge log above)");
            return 0;
        }

        final Map<Integer, ScriptRenderManager> managers =
                NativeDrawRegistry.begin(vehicle.getHexId(), scriptId, carCount);

        int produced = 0;
        for (NativeScriptManager.NativeFrame frame : frames) {
            if (applyFrame(frame, managers)) produced++;
        }
        traceOnce("'" + scriptId + "' frame applied", "modules=" + frames.size(),
                "withDrawCalls=" + produced, "carCount=" + carCount);
        return produced;
    }

    /* ------------------------------------------------------------------ */
    /* Frame records -> ScriptRenderManager calls                          */
    /*                                                                     */
    /* The record stream is the variable-size JcmDraw* array from           */
    /* mtr_native.h: walk it with pos += header.record_size. Every payload   */
    /* offset below is relative to the RECORD start (not the arena).         */
    /* ------------------------------------------------------------------ */

    private static boolean applyFrame(NativeScriptManager.NativeFrame frame,
                                      Map<Integer, ScriptRenderManager> managers) {
        final ByteBuffer records = frame.records;
        if (records == null) return false;
        NativeHost.get().setActiveInstance(frame.instanceKey);
        records.order(ByteOrder.nativeOrder());

        boolean any = false;
        int pos = 0;
        final int limit = records.limit();
        for (int i = 0; i < frame.recordCount; i++) {
            if (pos + HDR_SIZE > limit) break;
            final int kind = records.get(pos) & 0xFF;
            final int size = records.getInt(pos + 4);
            if (size < HDR_SIZE || pos + size > limit) break;   /* malformed: stop */

            switch (kind) {
                case KIND_MODEL: {
                    /* JcmDrawModel: car(8) bogie(9) pad(10..11)
                       model_handle(12) pose_offset(16) pose_count(20) */
                    final int car = records.get(pos + 8) & 0xFF;
                    final int modelHandle = records.getInt(pos + 12);
                    final ScriptRenderManager manager = managers.get(car);
                    if (manager != null && NativeHost.get().model(modelHandle) != null) {
                        manager.draw(NativeModelDrawCall.create(modelHandle));
                        any = true;
                    }
                    break;
                }
                case KIND_TEXTURE_UPLOAD: {
                    /* JcmDrawTextureUpload: texture_handle(8) width(12) height(16)
                       dirty_x(20) dirty_y(24) dirty_w(28) dirty_h(32)
                       pixel_data_offset(40, int64) pixel_data_len(48, int64) */
                    final int textureHandle = records.getInt(pos + 8);
                    final int dx = records.getInt(pos + 20);
                    final int dy = records.getInt(pos + 24);
                    final int dw = records.getInt(pos + 28);
                    final int dh = records.getInt(pos + 32);
                    final long pixelOffset = records.getLong(pos + 40);
                    final long pixelLen = records.getLong(pos + 48);
                    final ByteBuffer pixels = frame.pixelArena;
                    if (pixels != null && dw > 0 && dh > 0 && pixelLen > 0
                            && pixelOffset >= 0 && pixelOffset + pixelLen <= pixels.limit()) {
                        final byte[] bytes = new byte[(int) pixelLen];
                        final ByteBuffer src = pixels.duplicate();
                        src.position((int) pixelOffset);
                        src.get(bytes, 0, (int) pixelLen);
                        NativeHost.get().uploadPixels(textureHandle, dx, dy, dw, dh, bytes);
                    }
                    break;
                }
                default:
                    /* text / texture quads / outline shapes / sounds: the
                       vehicle LCD ports emit none of these. See the class note
                       about sound records. */
                    break;
            }
            pos += size;
        }
        return any;
    }

    /* ------------------------------------------------------------------ */
    /* Replay (called from VehicleResourceMixin)                           */
    /* ------------------------------------------------------------------ */

    /** Replays the captured calls for one car; no-op when nothing native ran. */
    public static void invokeCar(VehicleExtension vehicle, String scriptId, int carNumber,
                                 StoredMatrixTransformations storedMatrixTransformations,
                                 int light) {
        if (vehicle == null || scriptId == null) return;
        if (carNumber < 0) return;
        final ScriptRenderManager manager =
                NativeDrawRegistry.get(vehicle.getHexId(), scriptId, carNumber);
        if (manager == null) return;
        final World world = World.cast(MinecraftClient.getInstance().getWorldMapped());
        if (world == null) return;
        traceOnce("replaying '" + scriptId + "' car " + carNumber, vehicle.getHexId());
        final StoredMatrixTransformations transform = storedMatrixTransformations.copy();
        transform.add(gh -> gh.translate(0, -1, 0));
        manager.invoke(world, transform, Direction.NORTH, light);
    }

    /* ------------------------------------------------------------------ */
    /* Lifetime                                                            */
    /* ------------------------------------------------------------------ */

    /** Forget one vehicle+script (script instance dropped, vehicle gone). */
    public static void forget(String vehicleHexId, String scriptId) {
        STATES.remove(key(vehicleHexId, scriptId));
        NativeDrawRegistry.remove(vehicleHexId, scriptId);
    }

    /** Drop every per-vehicle state (resource reload). */
    public static void reset() {
        STATES.clear();
        NativeDrawRegistry.clear();
        /* let the next session report its early-exit reasons again */
        TRACED.clear();
    }
}
