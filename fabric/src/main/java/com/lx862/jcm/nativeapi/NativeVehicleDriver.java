package com.lx862.jcm.nativeapi;

import com.lx862.jcm.mod.util.JCMLogger;
import com.lx862.mtrscripting.core.util.render.ScriptRenderManager;
import com.lx862.mtrscripting.mod.impl.mtr.vehicle.VehicleScriptContext;
import com.lx862.mtrscripting.mod.impl.mtr.vehicle.VehicleWrapper;
import com.lx862.mtrscripting.mod.resource.MtrScriptingResourceManager;
import com.lx862.mtrscripting.mod.resource.VehicleResourceProvider;
import org.mtr.core.data.VehicleCar;
import org.mtr.core.tool.Vector;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.ObjectObjectImmutablePair;
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
 * <p>Models and sounds replay through the same per-car and per-bogie
 * contexts used by JavaScript scripts.
 */
public final class NativeVehicleDriver {

    /** Per vehicle+script state that must outlive a frame. */
    private static final class State {
        final NativeScriptManager.VehicleSnapshotBuffers snapshots =
                new NativeScriptManager.VehicleSnapshotBuffers();
        final String instanceKey;
        /** Precomputed "hexId\u0000scriptId" for the draw registry. */
        final String drawKey;
        final Map<Long, VehicleScriptContext> contexts = new java.util.LinkedHashMap<>();
        final NativeRenderStats perf = new NativeRenderStats();
        int[] cars = new int[0];
        /** Reused per-frame car-index buffer; sized on first use, never per frame. */
        int[] carScratch = EMPTY_CARS;
        /** Backoff after a persistent failure (wrapper build, module render or
         *  record replay): the script is skipped until this timestamp passes,
         * mirroring NativeContentDriver's errorUntil — a throwing script must
         * not re-log its stack trace every frame. */
        long errorUntilNanos;
        long touched = System.nanoTime();

        State(String vehicleHexId, String scriptId) {
            this.instanceKey = vehicleHexId + "/" + scriptId;
            this.drawKey = vehicleHexId + "\u0000" + scriptId;
        }
    }

    /** Per-frame car sound positions shared across a vehicle's scripts.
     * Keyed by hex id, stamped each frame; entries whose vehicle stopped
     * rendering for 5s are swept by tick() together with the states. */
    private static final class CarPositions {
        final float[] xyz = new float[3];
        int count;
        long stamp;
        /** Frame sequence number that last filled this entry. */
        long frame;
    }

    private static final Map<String, CarPositions> CAR_POSITIONS =
            new ConcurrentHashMap<>();

    /** Frame marker: RenderVehiclesMixin.executeScript bumps this once per
     * rendered frame so both the native and the JS script path compute the
     * car positions exactly once per vehicle per frame (time-based guards
     * misfire at 60fps, where consecutive frames are ~16ms apart). */
    private static final java.util.concurrent.atomic.AtomicLong FRAME_SEQ =
            new java.util.concurrent.atomic.AtomicLong();

    /** Call once per rendered frame before vehicles iterate. */
    public static void beginFrame() {
        FRAME_SEQ.incrementAndGet();
    }

    private static final Map<String, State> STATES = new ConcurrentHashMap<>();

    /**
     * Vehicle -> script-id memo. The car -> script-id mapping only changes
     * when the resource pack does, but resolving it used to allocate an
     * ArrayList plus a removeIf lambda per vehicle per frame. Entries compare
     * the car vehicle-id sequence and rebuild on any change; reset() drops
     * everything on reload.
     */
    private static final Map<String, ScriptIds> SCRIPT_IDS = new ConcurrentHashMap<>();

    private static final class ScriptIds {
        final String[] carVehicleIds;
        final List<String> ids;

        ScriptIds(String[] carVehicleIds, List<String> ids) {
            this.carVehicleIds = carVehicleIds;
            this.ids = ids;
        }

        boolean matches(VehicleExtension vehicle, int carCount) {
            if (carVehicleIds.length != carCount) return false;
            for (int i = 0; i < carCount; i++) {
                final String current = vehicle.vehicleExtraData.immutableVehicleCars.get(i).getVehicleId();
                final String memo = carVehicleIds[i];
                if (memo != current && (memo == null || !memo.equals(current))) return false;
            }
            return true;
        }
    }

    /**
     * Vehicles skipped so far because their envelope is outside the camera
     * frustum. Counted so the saving is visible in the log rather than assumed;
     * the per-60s native perf line reports the delta.
     */
    private static final java.util.concurrent.atomic.AtomicLong CULLED =
            new java.util.concurrent.atomic.AtomicLong();
    private static long culledAtLastReport = 0;
    private static long culledLastLog = System.nanoTime();

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
     *
     * <p>Reasons are CONSTANT strings on purpose: call sites guard the call
     * with {@code !TRACED.contains(CONSTANT)} so neither the reason concat, the
     * varargs array nor expensive detail args (getModules(), toString of the
     * skipped map) are built on frames that would not log anyway — that used
     * to allocate for every scripted vehicle, every car, every frame.
     */
    private static final java.util.Set<String> TRACED =
            java.util.concurrent.ConcurrentHashMap.newKeySet();

    private static void traceOnce(String reason, Object... detail) {
        if (TRACED.add(reason)) {
            JCMLogger.info("[native vehicle] {} — {}", reason, format(detail));
        }
    }

    /* Trace reasons — constants, see traceOnce javadoc. */
    private static final String TRACE_NO_CARS = "vehicle has no cars";
    private static final String TRACE_VEHICLE = "vehicle (first native frame)";
    private static final String TRACE_NO_CONFIG = "no VehicleScriptConfiguration (first occurrence)";
    private static final String TRACE_RENDERING = "rendering (first frame)";
    private static final String TRACE_WAITING_STOPS = "waiting for the full stop list (first occurrence)";
    private static final String TRACE_NO_FRAME = "produced no frame (first occurrence)";
    private static final String TRACE_FRAME_APPLIED = "frame applied (first frame)";
    private static final String TRACE_REPLAYING = "replaying (first frame)";
    private static final String TRACE_BAD_RECORD = "malformed native record (first occurrence)";

    private static final long FAILURE_BACKOFF_NANOS = 4_000_000_000L;

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

    private static final int[] EMPTY_CARS = new int[0];

    /** Same-content car-index comparison that does not allocate a copy per frame. */
    private static boolean sameCars(int[] cached, int[] scratch, int count) {
        if (cached.length != count) return false;
        for (int i = 0; i < count; i++) {
            if (cached[i] != scratch[i]) return false;
        }
        return true;
    }

    /**
     * Bytes allocated by the current thread so far, or -1 when the JVM does not
     * expose it.
     *
     * <p>Used only for the perf line: counting what this path allocates is the
     * only way to attribute GC pressure to it, and the counter costs nothing per
     * frame beyond one call.
     */
    private static long allocatedBytes() {
        final com.sun.management.ThreadMXBean mx = ALLOC_MX;
        if (mx == null) return -1L;
        try {
            return mx.getCurrentThreadAllocatedBytes();
        } catch (Throwable t) {
            return -1L;
        }
    }

    /** Allocation attributed to one render call; 0 when unsupported. */
    private static long allocDelta(long begin) {
        if (begin < 0L) return 0L;
        final long end = allocatedBytes();
        return end < begin ? 0L : end - begin;
    }

    private static final com.sun.management.ThreadMXBean ALLOC_MX = allocMx();

    private static com.sun.management.ThreadMXBean allocMx() {
        try {
            final java.lang.management.ThreadMXBean mx =
                    java.lang.management.ManagementFactory.getThreadMXBean();
            if (mx instanceof com.sun.management.ThreadMXBean) {
                final com.sun.management.ThreadMXBean sun = (com.sun.management.ThreadMXBean) mx;
                if (sun.isThreadAllocatedMemorySupported()) {
                    sun.setThreadAllocatedMemoryEnabled(true);
                    return sun;
                }
            }
        } catch (Throwable ignored) {
            /* not a HotSpot-style JVM; allocation reporting stays disabled */
        }
        return null;
    }

    /**
     * Logs how many vehicles the frustum cull skipped, at most once a minute.
     *
     * <p>Reported separately from {@link NativeRenderStats} because it counts
     * vehicles that never reached a script, so they cannot show up in the
     * per-script perf line.
     */
    private static void reportCulled() {
        final long now = System.nanoTime();
        if (now - culledLastLog < 60_000_000_000L) return;
        culledLastLog = now;
        final long total = CULLED.get();
        final long delta = total - culledAtLastReport;
        culledAtLastReport = total;
        if (delta > 0) {
            JCMLogger.info("[native cull] vehicle-script runs skipped by the camera frustum "
                    + "in the last 60s: {} (total {})", delta, total);
        }
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
        reportCulled();
        final int carCount = vehicle.vehicleExtraData.immutableVehicleCars.size();
        if (carCount <= 0) {
            if (!TRACED.contains(TRACE_NO_CARS)) traceOnce(TRACE_NO_CARS, vehicle.getHexId());
            return 0;
        }

        /* Script-id memo FIRST: the frustum box (MTR's whole car-position
           tree) used to be built for every vehicle in the world before this
           check, so script-less trains — the vast majority — paid the full
           culling cost for a render() that would return 0 anyway. */
        final List<String> ids = scriptIdsOf(vehicle);
        // Most vehicles in a city use only JS or no script. Avoid logging each
        // of those as a missing native module; it hides real native failures.
        // Plain contains-loop first: ArrayList.removeIf allocates an iterator
        // plus a BitSet on EVERY call, even when nothing is removed (the memo
        // is already filtered, so the remove is a guaranteed no-op per frame).
        boolean allLoaded = true;
        for (int i = 0; i < ids.size(); i++) {
            if (!NativeScriptManager.isLoaded(ids.get(i))) { allLoaded = false; break; }
        }
        if (!allLoaded) ids.removeIf(id -> !NativeScriptManager.isLoaded(id));
        if (ids.isEmpty()) return 0;

        /* Frustum cull before anything expensive: a train outside the camera
           view would otherwise pay a full snapshot marshal plus a whole-canvas
           repaint and texture upload per car, for pixels that are never drawn.
           The frustum comes from the same Fabric hook MTR caches its own
           block-entity culling frustum from, so this matches MTR's visibility
           decision. It fails open (see VehicleCulling). */
        if (!VehicleCulling.shouldRender(vehicle)) {
            CULLED.incrementAndGet();
            return 0;
        }
        if (!TRACED.contains(TRACE_VEHICLE)) {
            traceOnce(TRACE_VEHICLE, "id=" + vehicle.getHexId(), "cars=" + carCount,
                    "scriptIds=" + ids);
        }

        /* One car-position tree per vehicle per frame: invokeCar() used to
           rebuild MTR's whole getVehicleCarsAndPositions() tree per car
           (a 12-car train ≈ 1500 full tree builds per second) just to place
           car sounds. Cache the per-car average bogie position here. */
        updateCarPositions(vehicle, carCount);

        int rendered = 0;
        for (String scriptId : ids) {
            /* isLoaded() was checked above; NativeScriptManager.renderVehicle
               itself returns an empty frame list when the id lost its modules
               (resource reload), so no per-frame module-list copy is needed
               here — getModules(scriptId) allocated a synchronized copy per
               script per vehicle per frame just for an emptiness check. */
            rendered += renderOne(vehicle, scriptId, carCount);
        }
        return rendered;
    }

    /** Average bogie position per car, once per frame per vehicle.
     * Public: the JS replay path (VehicleResourceMixin) reads the same
     * positions so neither side rebuilds MTR's car-position tree per car. */
    public static void computeCarPositions(VehicleExtension vehicle) {
        final int carCount = vehicle.vehicleExtraData.immutableVehicleCars.size();
        if (carCount <= 0) return;
        updateCarPositions(vehicle, carCount);
    }

    /** Cached per-car sound position for this frame, or null when the vehicle
     *  was not scripted this frame (sound replay is skipped then). */
    public static com.lx862.mtrscripting.core.util.ScriptVector3f carSoundPosition(String vehicleHexId, int carNumber) {
        final CarPositions pos = CAR_POSITIONS.get(vehicleHexId);
        if (pos == null || carNumber < 0 || carNumber >= pos.count) return null;
        return new com.lx862.mtrscripting.core.util.ScriptVector3f(
                pos.xyz[carNumber * 3], pos.xyz[carNumber * 3 + 1], pos.xyz[carNumber * 3 + 2]);
    }

    private static void updateCarPositions(VehicleExtension vehicle, int carCount) {
        /* Same-frame guard: render() (native) and executeScript (JS) both call
         * this per vehicle; the second call within the SAME frame is a no-op. */
        final long frame = FRAME_SEQ.get();
        final CarPositions existing = CAR_POSITIONS.get(vehicle.getHexId());
        if (existing != null && existing.frame == frame) return;
        CarPositions pos = CAR_POSITIONS.computeIfAbsent(vehicle.getHexId(), k -> new CarPositions());
        if (pos.xyz.length < carCount * 3) pos.xyz = new float[Math.max(carCount * 3, 12)];
        final ObjectArrayList<ObjectObjectImmutablePair<VehicleCar, ObjectArrayList<ObjectObjectImmutablePair<Vector, Vector>>>> cars =
                vehicle.getVehicleCarsAndPositions();
        for (int car = 0; car < carCount && car < cars.size(); car++) {
            final ObjectArrayList<ObjectObjectImmutablePair<Vector, Vector>> positions = cars.get(car).right();
            double x = 0, y = 0, z = 0;
            int count = 0;
            if (positions != null) {
                for (int p = 0; p < positions.size(); p++) {
                    final ObjectObjectImmutablePair<Vector, Vector> pair = positions.get(p);
                    if (pair == null) continue;
                    final Vector left = pair.left();
                    final Vector right = pair.right();
                    if (left != null) { x += left.x(); y += left.y(); z += left.z(); count++; }
                    if (right != null) { x += right.x(); y += right.y(); z += right.z(); count++; }
                }
            }
            pos.xyz[car * 3] = count == 0 ? 0 : (float) (x / count);
            pos.xyz[car * 3 + 1] = count == 0 ? 0 : (float) (y / count);
            pos.xyz[car * 3 + 2] = count == 0 ? 0 : (float) (z / count);
        }
        pos.count = carCount;
        pos.stamp = System.nanoTime();
        pos.frame = frame;
    }

    private static List<String> scriptIdsOf(VehicleExtension vehicle) {
        final int carCount = vehicle.vehicleExtraData.immutableVehicleCars.size();
        final ScriptIds memo = SCRIPT_IDS.get(vehicle.getHexId());
        if (memo != null && memo.matches(vehicle, carCount)) return memo.ids;

        final String[] carVehicleIds = new String[carCount];
        final List<String> ids = new ArrayList<>(2);
        for (int i = 0; i < carCount; i++) {
            final String vehicleId = carVehicleIds[i] =
                    vehicle.vehicleExtraData.immutableVehicleCars.get(i).getVehicleId();
            final String scriptId = MtrScriptingResourceManager.vehicle.getVehicleScriptEntryId(vehicleId);
            if (scriptId == null || ids.contains(scriptId)) continue;
            ids.add(scriptId);
        }
        /* Most vehicles in a city use only JS or no script. Avoid logging each
           of those as a missing native module; it hides real native failures. */
        ids.removeIf(id -> !NativeScriptManager.isLoaded(id));
        SCRIPT_IDS.put(vehicle.getHexId(), new ScriptIds(carVehicleIds, ids));
        return ids;
    }

    private static int renderOne(VehicleExtension vehicle, String scriptId, int carCount) {
        final long perfBegin = System.nanoTime();
        final long allocBegin = allocatedBytes();
        final VehicleResourceProvider.VehicleScriptConfiguration config =
                MtrScriptingResourceManager.vehicle.getVehicleScript(scriptId);
        if (config == null) {
            if (!TRACED.contains(TRACE_NO_CONFIG)) {
                traceOnce(TRACE_NO_CONFIG, "id=" + scriptId,
                        "(the native entry never reached vehicleScripts)");
            }
            return 0;
        }
        if (!TRACED.contains(TRACE_RENDERING)) {
            traceOnce(TRACE_RENDERING, "id=" + scriptId, "dataFetchMode=" + config.dataFetchMode());
        }

        final VehicleScriptContext.DataFetchMode fetchMode = config.dataFetchMode();
        final State state = STATES.computeIfAbsent(
                key(vehicle.getHexId(), scriptId), k -> new State(vehicle.getHexId(), scriptId));

        state.touched = System.nanoTime();
        /* Backoff after a failure: the wrapper build, module render and replay
           paths below used to log a full stack trace every frame for a
           persistent error (a >256KB route overflow, a throwing module), which
           is both a log-spam and a performance death spiral. Skip the script
           until the window passes, like NativeContentDriver. */
        if (state.touched < state.errorUntilNanos) return 0;
        /* Plain loop: IntStream.range().filter().toArray() allocated a stream,
           a lambda and a builder on the render thread every single frame. */
        if (state.carScratch.length < carCount) state.carScratch = new int[carCount];
        int ownCarCount = 0;
        final int[] ownCars = state.carScratch;
        for (int i = 0; i < carCount; i++) {
            if (scriptId.equals(MtrScriptingResourceManager.vehicle.getVehicleScriptEntryId(
                    vehicle.vehicleExtraData.immutableVehicleCars.get(i).getVehicleId()))) {
                ownCars[ownCarCount++] = i;
            }
        }
        if (!sameCars(state.cars, ownCars, ownCarCount)) {
            if (state.cars.length != 0) NativeScriptManager.disposeVehicleInstance(state.instanceKey, scriptId, state.snapshots);
            state.contexts.clear();
            state.cars = java.util.Arrays.copyOf(ownCars, ownCarCount);
        }
        final VehicleWrapper wrapper;
        try {
            wrapper = new com.lx862.mtrscripting.mod.impl.mtr.vehicle.NTETrainWrapper(fetchMode, vehicle);
        } catch (Throwable t) {
            /* Full trace on purpose: t.toString() alone pointed at an inlined
               getter and made the failing line impossible to find. */
            JCMLogger.error("Native vehicle script failed to build its route data: {}", t.toString());
            JCMLogger.error("  stack:", t);
            state.errorUntilNanos = state.touched + FAILURE_BACKOFF_NANOS;
            return 0;
        }
        /* Same contract as the JS path: a MANDATORY script does not draw until
           the full stop list arrived (its header would show an empty route). */
        if (fetchMode == VehicleScriptContext.DataFetchMode.MANDATORY
                && !wrapper.isStopsDataFullyFetched()) {
            if (!TRACED.contains(TRACE_WAITING_STOPS)) {
                traceOnce(TRACE_WAITING_STOPS, "id=" + scriptId, "(MANDATORY)");
            }
            return 0;
        }

        /* Snapshot marshalling (per ABI group) and module render happen inside
           NativeScriptManager.renderVehicle; a marshal failure skips the native
           call instead of handing mtrRender a half-written buffer, and the
           driver's reusable Builder keeps a steady frame allocation-free. */
        final List<NativeScriptManager.NativeFrame> frames;
        final long nativeBegin = System.nanoTime();
        try {
            frames = NativeScriptManager.renderVehicle(state.instanceKey, scriptId, state.snapshots, wrapper,
                    module -> { VehicleScriptContext ctx = state.contexts.computeIfAbsent(module.getNativeHandle(), h -> new VehicleScriptContext(vehicle, scriptId, state.cars)); ctx.resetForNextRun(); return ctx; });
        } catch (Throwable t) {
            JCMLogger.error("Native vehicle script {} threw while rendering: {}", scriptId, t.toString());
            JCMLogger.error("  stack:", t);
            state.errorUntilNanos = state.touched + FAILURE_BACKOFF_NANOS;
            return 0;
        }
        if (frames.isEmpty()) {
            if (!TRACED.contains(TRACE_NO_FRAME)) {
                traceOnce(TRACE_NO_FRAME, "id=" + scriptId,
                        "(both modules returned null — see the bridge log above)");
            }
            return 0;
        }

        final long replayBegin = System.nanoTime();
        final Map<Integer, ScriptRenderManager> managers =
                NativeDrawRegistry.begin(state.drawKey, carCount);

        int produced = 0;
        try {
            for (NativeScriptManager.NativeFrame frame : frames) {
                if (applyFrame(frame, managers, state.contexts.get(frame.moduleHandle), wrapper.isClientPlayerRiding(), state.perf)) produced++;
            }
        } catch (Throwable t) {
            /* A malformed record (or a throwing draw call) must not kill the
               render thread — NativeContentDriver.run already had exactly this
               guard with a backoff; the vehicle path did not. */
            JCMLogger.error("Native vehicle script {} threw while replaying records: {}", scriptId, t.toString());
            JCMLogger.error("  stack:", t);
            state.errorUntilNanos = state.touched + FAILURE_BACKOFF_NANOS;
            NativeDrawRegistry.begin(state.drawKey, carCount);
        }
        state.perf.frame(scriptId, perfBegin, nativeBegin, replayBegin, allocDelta(allocBegin));
        if (!TRACED.contains(TRACE_FRAME_APPLIED)) {
            traceOnce(TRACE_FRAME_APPLIED, "id=" + scriptId, "modules=" + frames.size(),
                    "withDrawCalls=" + produced, "carCount=" + carCount);
        }
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
                                      Map<Integer, ScriptRenderManager> managers, VehicleScriptContext context, boolean riding,
                                      NativeRenderStats perf) {
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
            int minimum = kind==3 ? 24 : (kind==4||kind==5) ? 40 : kind==6 ? 56 : 8;
            if (size < minimum) {
                /* Malformed module output: stop walking this frame instead of
                   throwing out of the replay path (which used to escape the
                   only try/catch in renderOne and kill the render thread). */
                if (!TRACED.contains(TRACE_BAD_RECORD)) {
                    traceOnce(TRACE_BAD_RECORD, "kind=" + kind, "size=" + size, "minimum=" + minimum);
                }
                break;
            }

            switch (kind) {
                case KIND_MODEL: {
                    /* JcmDrawModel: car(8) bogie(9) pad(10..11)
                       model_handle(12) pose_offset(16) pose_count(20) */
                    final int car = records.get(pos + 8) & 0xFF;
                    final int modelHandle = records.getInt(pos + 12);
                    final int bogie=records.get(pos+9)&255;
                    final ScriptRenderManager manager = bogie==255 ? managers.get(car) : context.getCarBogieRenderManager(car,bogie);
                    if (manager != null && NativeHost.get().model(modelHandle) != null) {
                        NativeModelDrawCall call = NativeModelDrawCall.create(modelHandle);
                        call.pose(frame.matrixArena, records.getInt(pos+16), records.getInt(pos+20));
                        manager.draw(call);
                        any = true;
                    }
                    break;
                }
                case 4:
                case 5: {
                    int offset=records.getInt(pos+32), length=records.getInt(pos+36);
                    if(offset<0||length<0||(long)offset+length>frame.stringArena.limit()) {
                        if (!TRACED.contains(TRACE_BAD_RECORD)) {
                            traceOnce(TRACE_BAD_RECORD, "sound ref", "offset=" + offset, "length=" + length);
                        }
                        break;
                    }
                    byte[] utf8=new byte[length]; ByteBuffer text=frame.stringArena.duplicate(); text.position(offset); text.get(utf8);
                    org.mtr.mapping.holder.Identifier sound=new org.mtr.mapping.holder.Identifier(new String(utf8,java.nio.charset.StandardCharsets.UTF_8));
                    if(kind==5) { if(riding) context.playAnnSound(sound,records.getFloat(pos+24),records.getFloat(pos+28)); }
                    else context.playCarSound(sound,records.get(pos+8)&255,records.getFloat(pos+12),records.getFloat(pos+16),records.getFloat(pos+20),records.getFloat(pos+24),records.getFloat(pos+28));
                    any=true;
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
                            && pixelOffset >= 0 && pixelLen <= pixels.limit() - pixelOffset && pixelLen <= Integer.MAX_VALUE) {
                        NativeHost.get().uploadPixels(textureHandle, dx, dy, dw, dh, pixels, pixelOffset, pixelLen);
                        perf.upload(pixelLen);
                    }
                    break;
                }
                default:
                    /* Remaining records belong to other script types. */
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
        if (!TRACED.contains(TRACE_REPLAYING)) traceOnce(TRACE_REPLAYING, "id=" + vehicle.getHexId(), "car=" + carNumber);
        final StoredMatrixTransformations transform = storedMatrixTransformations.copy();
        transform.add(gh -> gh.translate(0, -1, 0));
        manager.invoke(world, transform, Direction.NORTH, light);
        State state=STATES.get(key(vehicle.getHexId(),scriptId));
        if(state!=null) {
            /* Per-car average bogie position, computed once per frame by
               render(): this used to rebuild MTR's whole car-position tree
               (getVehicleCarsAndPositions()) per car per frame — an N-car
               train paid 2N+1 full tree builds per frame across culling and
               replay. */
            final CarPositions pos = CAR_POSITIONS.get(vehicle.getHexId());
            final com.lx862.mtrscripting.core.util.ScriptVector3f soundPos =
                    pos != null && carNumber < pos.count
                            ? new com.lx862.mtrscripting.core.util.ScriptVector3f(
                                    pos.xyz[carNumber * 3], pos.xyz[carNumber * 3 + 1], pos.xyz[carNumber * 3 + 2])
                            : null;
            for(VehicleScriptContext ctx:state.contexts.values()) {
                var render=ctx.getCarRenderManager(carNumber); if(render!=null) render.invoke(world,transform.copy(),Direction.NORTH,light);
                var sound=ctx.getCarSoundManager(carNumber); if(sound!=null && soundPos != null) sound.invoke(world,soundPos);
            }
        }
    }

    /* ------------------------------------------------------------------ */
    /* Lifetime                                                            */
    /* ------------------------------------------------------------------ */

    /** Forget one vehicle+script (script instance dropped, vehicle gone). */
    public static void forget(String vehicleHexId, String scriptId) {
        State state=STATES.remove(key(vehicleHexId, scriptId));
        if(state!=null) NativeScriptManager.disposeVehicleInstance(state.instanceKey,scriptId,state.snapshots);
        NativeDrawRegistry.remove(vehicleHexId, scriptId);
        /* The SCRIPT_IDS memo is only ever consumed together with a state;
           leaving it keyed by hex id forever leaked one entry (plus its car
           id array and script id list) for every train that ever spawned. */
        if (STATES.get(key(vehicleHexId, scriptId)) == null) SCRIPT_IDS.remove(vehicleHexId);
    }

    /** Drop every per-vehicle state (resource reload). */
    public static void tick() {
        long now=System.nanoTime();
        for(Map.Entry<String,State> entry:STATES.entrySet()) if(now-entry.getValue().touched>5_000_000_000L) {
            String[] identity=entry.getKey().split("\\u0000",2);
            if(identity.length==2) forget(identity[0],identity[1]);
        }
        for(Map.Entry<String,CarPositions> entry:CAR_POSITIONS.entrySet()) {
            if(now-entry.getValue().stamp>5_000_000_000L) CAR_POSITIONS.remove(entry.getKey());
        }
    }
    public static void invokeBogie(VehicleExtension vehicle,String scriptId,int car,int bogie,StoredMatrixTransformations transforms,int light) {
        State state=STATES.get(key(vehicle.getHexId(),scriptId)); if(state==null) return;
        World world=World.cast(MinecraftClient.getInstance().getWorldMapped()); if(world==null) return;
        for(VehicleScriptContext ctx:state.contexts.values()) {
            var manager=ctx.getCarBogieRenderManager(car,bogie); if(manager!=null) manager.invoke(world,transforms.copy(),Direction.NORTH,light);
        }
    }
    public static void reset() {
        STATES.clear();
        SCRIPT_IDS.clear();
        NativeDrawRegistry.clear();
        /* let the next session report its early-exit reasons again */
        TRACED.clear();
    }
}
