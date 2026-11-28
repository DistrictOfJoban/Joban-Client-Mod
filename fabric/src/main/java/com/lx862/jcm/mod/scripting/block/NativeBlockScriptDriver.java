package com.lx862.jcm.mod.scripting.block;

import com.lx862.jcm.mod.scripting.pids.PIDSScriptContext;
import com.lx862.jcm.mod.util.JCMLogger;
import com.lx862.jcm.nativeapi.NativeContentSnapshot;
import com.lx862.jcm.nativeapi.NativeScriptManager;
import com.lx862.mtrscripting.core.util.ScriptVector3f;
import org.mtr.core.data.Platform;
import org.mtr.core.operation.ArrivalResponse;
import org.mtr.libraries.it.unimi.dsi.fastutil.longs.LongImmutableList;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.mtr.mapping.holder.BlockPos;
import org.mtr.mapping.holder.ClientWorld;
import org.mtr.mapping.holder.Direction;
import org.mtr.mapping.holder.MinecraftClient;
import org.mtr.mapping.holder.World;
import org.mtr.mod.client.MinecraftClientData;
import org.mtr.mod.data.ArrivalsCacheClient;
import org.mtr.mod.data.VehicleExtension;
import org.mtr.mod.render.PositionAndRotation;
import org.mtr.mod.render.StoredMatrixTransformations;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * NativeBlockScriptDriver — drives the three ABI 16 block-script kinds
 * (level crossing / fare machine / announcement) over the
 * {@link NativeBlockScriptBindings} table.
 *
 * <h2>Frame discipline</h2>
 * <p>{@link #frame()} runs once per world render frame from the same
 * RenderVehicles.render injection the vehicle scripts use (render
 * thread, shadow-pass guarded). The DRIVE half — FangSu probing, vehicle
 * scans, arrival lookups, native render — executes at most every
 * {@code DRIVE_INTERVAL} frames (~12 Hz at 60 fps: the data feeding
 * barrier logic and announcements changes at human scale, and reflective
 * probes per frame would be waste); the REPLAY half — handing the
 * captured draw calls to MTR's render queues — runs EVERY frame, so the
 * drawn models never flicker between drives. The capture/replay split
 * is the same discipline NativeContentDriver's PIDS/PSD paths use: the
 * render manager holds the last captured calls until the next drive
 * replaces them.</p>
 *
 * <h2>Data sources</h2>
 * <ul>
 *   <li>Crossing approaches: one scan of the client vehicle table
 *       (position, speed, route id, car count);</li>
 *   <li>Speaker / fare-machine arrivals: MTR's
 *       {@code ArrivalsCacheClient} for the bound / nearest platform ids,
 *       offset-corrected exactly like the PIDS wrappers;</li>
 *   <li>Fare machine barrier state: FangSu's extraConfig via
 *       {@link FangSuBlockProbe}; the wing animation value is derived
 *       from the open flag's edges with FangSu's timing constants
 *       (200 ms open, 500+200 ms close).</li>
 * </ul>
 */
public final class NativeBlockScriptDriver {

    /** Drive at most every N frames (~12 Hz at 60 fps). */
    private static final int DRIVE_INTERVAL = 5;

    /** Horizontal radius a vehicle enters the crossing's approach pool. */
    private static final double APPROACH_RADIUS = 64;
    /** Inside this radius (or this few seconds to arrival) the warning
     * state latches and the barrier target lowers. */
    private static final double WARNING_RADIUS = 32;
    private static final double WARNING_SECONDS = 20;
    private static final double BARRIER_RADIUS = 16;
    private static final double BARRIER_SECONDS = 8;

    /** Barrier wing animation constants, mirroring FangSu's
     * BlockEntityTicketBarrier.whenRendering. */
    private static final long DOOR_OPEN_ANIM_MS = 200;
    private static final long DOOR_CLOSE_DELAY_MS = 500;
    private static final long DOOR_CLOSE_ANIM_MS = 200;

    private static final class State {
        final NativeBlockScriptBindings.Binding binding;
        final Map<Long, PIDSScriptContext> contexts = new HashMap<>();
        java.nio.ByteBuffer snapshot = com.lx862.jcm.nativeapi.NativeChannel.newSnapshotBuffer(8 * 1024);
        long touched = System.nanoTime();
        long errorUntil;
        /* Barrier wing animation bookkeeping. */
        boolean wasOpen;
        long openEdgeMillis;
        long closeEdgeMillis;
        boolean edgesSeen;
        State(NativeBlockScriptBindings.Binding binding) {
            this.binding = binding;
        }
    }

    private static final Map<String, State> STATES = new HashMap<>();
    private static int frameCounter;

    private NativeBlockScriptDriver() {
    }

    /* ------------------------------------------------------------------ */
    /* Frame entry (render thread, once per frame)                        */
    /* ------------------------------------------------------------------ */

    public static void frame() {
        final List<NativeBlockScriptBindings.Binding> bindings = NativeBlockScriptBindings.all();
        if (bindings.isEmpty()) {
            if (!STATES.isEmpty()) resetStates();
            return;
        }
        final ClientWorld clientWorld = MinecraftClient.getInstance().getWorldMapped();
        if (clientWorld == null) {
            if (!STATES.isEmpty()) resetStates();
            return;
        }
        final World world = World.cast(clientWorld);
        final long now = System.currentTimeMillis();
        final int frame = frameCounter++;

        /* REPLAY every frame — captured draw calls go back onto MTR's
           render queues; between drives the previous capture is re-fed
           (see the class javadoc). */
        for (State state : STATES.values()) {
            state.touched = System.nanoTime();
            replay(world, state);
        }

        /* DRIVE at the reduced cadence. */
        if (frame % DRIVE_INTERVAL != 0) return;

        for (NativeBlockScriptBindings.Binding binding : bindings) {
            if (!NativeScriptManager.isLoaded(binding.scriptId())) continue;
            final int kind = kindOf(binding.kind());
            if (kind < 0) continue;
            drive(binding, kind, now);
        }
        sweep();
    }

    /* ------------------------------------------------------------------ */
    /* Drive half                                                          */
    /* ------------------------------------------------------------------ */

    private static void drive(NativeBlockScriptBindings.Binding binding, int kind, long now) {
        final String key = keyOf(binding);
        State existing = STATES.get(key);
        if (existing == null) {
            existing = new State(binding);
            STATES.put(key, existing);
        }
        final State state = existing;
        state.touched = System.nanoTime();
        if (now < state.errorUntil) return;
        try {
            final Function<java.nio.ByteBuffer, java.nio.ByteBuffer> marshal;
            final Object wrapper;
            switch (kind) {
                case NativeScriptManager.MTR_RESOURCE_LEVEL_CROSSING -> {
                    final NativeContentSnapshot.CrossingData data = buildCrossing(binding.pos(), now);
                    wrapper = data;
                    marshal = b -> NativeContentSnapshot.levelCrossing(b, data);
                }
                case NativeScriptManager.MTR_RESOURCE_FARE_MACHINE -> {
                    final NativeContentSnapshot.FareData data = buildFareMachine(state, binding.pos(), now);
                    wrapper = data;
                    marshal = b -> NativeContentSnapshot.fareMachine(b, data);
                }
                case NativeScriptManager.MTR_RESOURCE_ANNOUNCEMENT -> {
                    final NativeContentSnapshot.SpeakerData data = buildSpeaker(binding.pos());
                    wrapper = data;
                    marshal = b -> NativeContentSnapshot.announcement(b, data);
                }
                default -> {
                    return;
                }
            }
            while (true) {
                try {
                    marshal.apply(state.snapshot);
                    break;
                } catch (java.nio.BufferOverflowException | IndexOutOfBoundsException ex) {
                    if (state.snapshot.capacity() >= 512 * 1024) throw ex;
                    state.snapshot = com.lx862.jcm.nativeapi.NativeChannel.growSnapshotBuffer(state.snapshot);
                }
            }
            NativeScriptManager.render(key, binding.scriptId(), state.snapshot, kind,
                    module -> state.contexts.computeIfAbsent(module.getNativeHandle(), h -> new PIDSScriptContext(binding.scriptId())),
                    wrapper, (module, ctx) -> {
                    }).forEach(nativeFrame -> {
                final PIDSScriptContext context = state.contexts.get(parseTrailingLong(nativeFrame.instanceKey));
                if (context != null) {
                    com.lx862.jcm.nativeapi.NativeContentDriver.apply(nativeFrame, context);
                }
            });
        } catch (Throwable ex) {
            state.errorUntil = now + 4000;
            JCMLogger.error("Native {} script {} failed", binding.kind(), binding.scriptId(), ex);
        }
    }

    /* ------------------------------------------------------------------ */
    /* Replay half                                                         */
    /* ------------------------------------------------------------------ */

    private static void replay(World world, State state) {
        final BlockPos pos = state.binding.pos();
        final int facing = FangSuBlockProbe.facingOf(pos);
        final Direction direction = Direction.values()[facing];
        final StoredMatrixTransformations transform = new StoredMatrixTransformations(
                0.5 + pos.getX(), pos.getY(), 0.5 + pos.getZ());
        transform.add(graphicsHolder -> graphicsHolder.rotateYDegrees(-direction.asRotation()));
        final ScriptVector3f soundPos = new ScriptVector3f(pos);
        for (PIDSScriptContext context : state.contexts.values()) {
            context.getRenderManager().invoke(world, transform, direction,
                    org.mtr.mapping.mapper.GraphicsHolder.getDefaultLight());
            context.getSoundManager().invoke(world, soundPos);
        }
    }

    /* ------------------------------------------------------------------ */
    /* Snapshot builders                                                   */
    /* ------------------------------------------------------------------ */

    private static NativeContentSnapshot.CrossingData buildCrossing(BlockPos pos, long now) {
        final NativeContentSnapshot.CrossingData data = new NativeContentSnapshot.CrossingData();
        data.pos = pos;
        data.fangsuPresent = FangSuBlockProbe.present();
        data.trackYawDegrees = FangSuBlockProbe.crossingTrackYaw(pos);
        for (VehicleExtension vehicle : MinecraftClientData.getInstance().vehicles) {
            /* Head position: first car's first bogie position, the same
               source VehicleWrapper flattens into PositionAndRotation. */
            final ObjectArrayList<org.mtr.libraries.it.unimi.dsi.fastutil.objects.ObjectObjectImmutablePair<org.mtr.core.data.VehicleCar, ObjectArrayList<org.mtr.libraries.it.unimi.dsi.fastutil.objects.ObjectObjectImmutablePair<org.mtr.core.tool.Vector, org.mtr.core.tool.Vector>>>>
                    smoothed = vehicle.getSmoothedVehicleCarsAndPositions(0);
            if (smoothed == null || smoothed.isEmpty()) continue;
            final ObjectArrayList<org.mtr.libraries.it.unimi.dsi.fastutil.objects.ObjectObjectImmutablePair<org.mtr.core.tool.Vector, org.mtr.core.tool.Vector>> firstCar = smoothed.get(0).right();
            if (firstCar == null || firstCar.isEmpty()) continue;
            final org.mtr.core.tool.Vector head = firstCar.get(0).left();
            if (head == null) continue;
            final double dx = head.x - (pos.getX() + 0.5);
            final double dz = head.z - (pos.getZ() + 0.5);
            final double distance = Math.sqrt(dx * dx + dz * dz);
            if (distance > APPROACH_RADIUS) continue;
            final double speedMs = Math.abs(vehicle.getSpeed()) * 1000;
            final NativeContentSnapshot.CrossingApproachData approach = new NativeContentSnapshot.CrossingApproachData();
            approach.vehicleId = vehicle.getId();
            approach.routeId = vehicle.vehicleExtraData.getThisRouteId();
            approach.carCount = vehicle.vehicleExtraData.immutableVehicleCars.size();
            approach.distanceBlocks = distance;
            approach.speedKmh = speedMs * 3.6;
            if (speedMs > 0.01) {
                approach.secondsToArrival = distance / speedMs;
                approach.arrivalMillis = now + (long) (approach.secondsToArrival * 1000);
            }
            data.approaches.add(approach);
            if (distance <= WARNING_RADIUS
                    || (approach.secondsToArrival >= 0 && approach.secondsToArrival <= WARNING_SECONDS)) {
                data.warning = true;
            }
            if (distance <= BARRIER_RADIUS
                    || (approach.secondsToArrival >= 0 && approach.secondsToArrival <= BARRIER_SECONDS)) {
                data.barrierTarget = true;
            }
        }
        return data;
    }

    private static NativeContentSnapshot.FareData buildFareMachine(State state, BlockPos pos, long now) {
        final NativeContentSnapshot.FareData data = new NativeContentSnapshot.FareData();
        data.pos = pos;
        data.fangsuPresent = FangSuBlockProbe.present();
        data.facing = FangSuBlockProbe.facingOf(pos);
        final FangSuBlockProbe.BarrierState barrier = FangSuBlockProbe.barrierState(pos);
        data.open = barrier.open;
        data.exitSide = barrier.exitSide;
        data.fareType = barrier.fareType;
        data.fareValue = barrier.fareValue;
        data.machineType = 0;
        /* Wing animation: track the open flag's edges and replay FangSu's
           timing (open over 200 ms after the edge; close 500 ms after the
           edge over 200 ms). door_value 0 = open, 1 = closed. */
        if (barrier.open != state.wasOpen || !state.edgesSeen) {
            state.wasOpen = barrier.open;
            state.edgesSeen = true;
            if (barrier.open) {
                state.openEdgeMillis = now;
            } else {
                state.closeEdgeMillis = now;
            }
        }
        if (barrier.open) {
            final long since = now - state.openEdgeMillis;
            data.doorValue = clamp01(1 - since / (double) DOOR_OPEN_ANIM_MS);
        } else {
            final long since = now - state.closeEdgeMillis;
            data.doorValue = clamp01((since - DOOR_CLOSE_DELAY_MS) / (double) DOOR_CLOSE_ANIM_MS);
        }
        data.playerBalance = playerBalance();
        /* Nearest platform arrivals — congestion / next-train content. */
        final List<Platform> platforms = new ArrayList<>();
        org.mtr.mod.InitClient.findClosePlatform(pos, 5, platforms::add);
        if (!platforms.isEmpty()) {
            final Platform platform = platforms.get(0);
            data.stationName = platform.getStationName() == null ? "" : platform.getStationName();
            final ObjectArrayList<ArrivalResponse> arrivals = ArrivalsCacheClient.INSTANCE
                    .requestArrivals(LongImmutableList.of(platform.getId()));
            appendArrivals(data.arrivals, arrivals, platform.getId(), 8);
        }
        return data;
    }

    private static NativeContentSnapshot.SpeakerData buildSpeaker(BlockPos pos) {
        final NativeContentSnapshot.SpeakerData data = new NativeContentSnapshot.SpeakerData();
        data.pos = pos;
        data.fangsuPresent = FangSuBlockProbe.present();
        final FangSuBlockProbe.SpeakerState speaker = FangSuBlockProbe.speakerState(pos);
        data.leadSeconds = speaker.leadSeconds;
        data.allStops = speaker.allStops;
        data.template = speaker.template;
        data.variant = speaker.variant;
        data.bindings.addAll(speaker.bindings);
        if (speaker.bindings.isEmpty()) return data;
        /* Arrivals for every bound platform. */
        final List<Long> ids = new ArrayList<>();
        for (long[] pair : speaker.bindings) {
            if (pair[1] != 0 && !ids.contains(pair[1])) ids.add(pair[1]);
        }
        final long[] platformIds = new long[ids.size()];
        for (int i = 0; i < ids.size(); i++) platformIds[i] = ids.get(i);
        final ObjectArrayList<ArrivalResponse> arrivals = ArrivalsCacheClient.INSTANCE
                .requestArrivals(LongImmutableList.of(platformIds));
        final long offset = ArrivalsCacheClient.INSTANCE.getMillisOffset();
        for (int i = 0; i < Math.min(arrivals.size(), 16); i++) {
            final ArrivalResponse response = arrivals.get(i);
            data.arrivals.add(new NativeContentSnapshot.AnnounceArrivalData(
                    response.getArrival() - offset,
                    response.getDeparture() - offset,
                    response.getRouteId(),
                    response.getPlatformId(),
                    response.getCarCount(),
                    Boolean.TRUE.equals(response.getRealtime()),
                    response.getIsTerminating(),
                    response.getDestination() == null ? "" : response.getDestination(),
                    response.getRouteName() == null ? "" : response.getRouteName()));
        }
        return data;
    }

    private static void appendArrivals(List<NativeContentSnapshot.ArrivalLite> out,
                                       ObjectArrayList<ArrivalResponse> arrivals, long platformId, int limit) {
        final long offset = ArrivalsCacheClient.INSTANCE.getMillisOffset();
        for (int i = 0; i < Math.min(arrivals.size(), limit); i++) {
            final ArrivalResponse response = arrivals.get(i);
            out.add(new NativeContentSnapshot.ArrivalLite(
                    response.getArrival() - offset,
                    response.getDeparture() - offset,
                    response.getRouteId(),
                    platformId,
                    response.getRouteColor() | 0xFF000000,
                    response.getCarCount(),
                    Boolean.TRUE.equals(response.getRealtime()),
                    response.getIsTerminating(),
                    response.getRouteNumber() == null ? "" : response.getRouteNumber(),
                    response.getRouteName() == null ? "" : response.getRouteName(),
                    response.getDestination() == null ? "" : response.getDestination(),
                    ""));
        }
    }

    /**
     * The client player's mtr_balance scoreboard value, -1 when unknown
     * (no player / no objective / API mismatch).
     *
     * <p>Reflection-only on purpose: the source set is shared with the
     * Forge build (mojmap), where the yarn class names do not exist. The
     * lookups try both mapping dialects — world accessor {@code getWorld}
     * (yarn) vs {@code level} (mojmap), objective lookup
     * {@code getNullableObjective} (yarn 1.20.4) vs {@code getObjective}
     * (mojmap), score creation {@code getOrCreateScore} (yarn) vs
     * {@code getOrCreatePlayerScore} (mojmap), and the value read
     * {@code getScore} / {@code get} / {@code value}. The player doubles
     * as the score holder on both mappings (Entity implements it), and a
     * missing entry reads 0 — the value the objective would render, with
     * the client scoreboard rebuilt from the server's anyway.</p>
     */
    private static int playerBalance() {
        try {
            final org.mtr.mapping.holder.ClientPlayerEntity player = MinecraftClient.getInstance().getPlayerMapped();
            if (player == null) return -1;
            final Object rawPlayer = player.data;
            final Object world = invokeFirst(rawPlayer, new String[]{"getWorld", "level"});
            if (world == null) return -1;
            final Object scoreboard = world.getClass().getMethod("getScoreboard").invoke(world);
            if (scoreboard == null) return -1;
            final Object objective = invokeStringFirst(scoreboard, new String[]{"getNullableObjective", "getObjective"}, "mtr_balance");
            if (objective == null) return -1;
            final Object score = invokeFirst(scoreboard, new String[]{"getOrCreateScore", "getOrCreatePlayerScore"}, rawPlayer, objective);
            if (score == null) return -1;
            final Object value = invokeFirst(score, new String[]{"getScore", "get", "value"});
            return value instanceof Integer ? (Integer) value : -1;
        } catch (Throwable t) {
            return -1;
        }
    }

    /** Invokes the first resolvable zero-arg method of the given names. */
    private static Object invokeFirst(Object target, String[] methods) {
        for (String method : methods) {
            try {
                return target.getClass().getMethod(method).invoke(target);
            } catch (NoSuchMethodException next) {
                /* try the other mapping dialect */
            } catch (Throwable t) {
                return null;
            }
        }
        return null;
    }

    /** Invokes the first resolvable single-String-argument method. */
    private static Object invokeStringFirst(Object target, String[] methods, String argument) {
        for (String method : methods) {
            try {
                return target.getClass().getMethod(method, String.class).invoke(target, argument);
            } catch (NoSuchMethodException next) {
                /* try the other mapping dialect */
            } catch (Throwable t) {
                return null;
            }
        }
        return null;
    }

    /** Invokes the first resolvable method of the given names with the
     *  given arguments (types derived from the runtime values). */
    private static Object invokeFirst(Object target, String[] methods, Object... args) {
        final Class<?>[] types = new Class<?>[args.length];
        for (int i = 0; i < args.length; i++) types[i] = args[i].getClass();
        for (String method : methods) {
            try {
                /* The declared parameter types may be supertypes of the
                   runtime argument classes (ScoreHolder, Property, ...) —
                   walk the class chain for a match on each argument. */
                outer:
                for (java.lang.reflect.Method candidate : target.getClass().getMethods()) {
                    if (!candidate.getName().equals(method)) continue;
                    final Class<?>[] params = candidate.getParameterTypes();
                    if (params.length != types.length) continue;
                    for (int i = 0; i < params.length; i++) {
                        if (!params[i].isAssignableFrom(types[i])) continue outer;
                    }
                    return candidate.invoke(target, args);
                }
            } catch (Throwable t) {
                /* try the next name */
            }
        }
        return null;
    }

    private static double clamp01(double v) {
        return v < 0 ? 0 : Math.min(v, 1);
    }

    /* ------------------------------------------------------------------ */

    private static int kindOf(String kind) {
        return switch (kind) {
            case NativeBlockScriptBindings.KIND_LEVEL_CROSSING -> NativeScriptManager.MTR_RESOURCE_LEVEL_CROSSING;
            case NativeBlockScriptBindings.KIND_FARE_MACHINE -> NativeScriptManager.MTR_RESOURCE_FARE_MACHINE;
            case NativeBlockScriptBindings.KIND_ANNOUNCEMENT -> NativeScriptManager.MTR_RESOURCE_ANNOUNCEMENT;
            default -> -1;
        };
    }

    private static void sweep() {
        final long now = System.nanoTime();
        final List<State> expired = new ArrayList<>();
        for (State state : STATES.values()) {
            if (now - state.touched > NativeScriptManager.IDLE_DISPOSE_NANOS) expired.add(state);
        }
        for (State state : expired) {
            final String key = keyOf(state.binding);
            if (STATES.remove(key) != null) {
                NativeScriptManager.disposeInstance(key, state.binding.scriptId(), state.snapshot);
                com.lx862.jcm.nativeapi.NativeChannel.releaseSnapshotBuffer(state.snapshot);
            }
        }
    }

    private static String keyOf(NativeBlockScriptBindings.Binding binding) {
        return binding.kind() + "/" + binding.scriptId()
                + "/" + binding.pos().getX() + "/" + binding.pos().getY() + "/" + binding.pos().getZ();
    }

    private static void resetStates() {
        for (State state : STATES.values()) {
            NativeScriptManager.disposeInstance(keyOf(state.binding), state.binding.scriptId(), state.snapshot);
            com.lx862.jcm.nativeapi.NativeChannel.releaseSnapshotBuffer(state.snapshot);
        }
        STATES.clear();
    }

    /** Decimal number at the end of a ".../module/<handle>" key. */
    private static long parseTrailingLong(String s) {
        final int end = s.length();
        int start = end;
        while (start > 0) {
            final char c = s.charAt(start - 1);
            if (c >= '0' && c <= '9') start--;
            else break;
        }
        if (start == end) return -1;
        try {
            return Long.parseLong(s, start, end, 10);
        } catch (NumberFormatException ignored) {
            return -1;
        }
    }
}
