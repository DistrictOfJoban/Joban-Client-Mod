package com.lx862.mtrscripting.mod.impl.mtr.server;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.lx862.jcm.mixin.mtrscripting.InitAccessorMixin;
import com.lx862.jcm.mixin.mtrscripting.MainAccessorMixin;
import com.lx862.mtrscripting.mod.MTRScriptingMod;
import com.lx862.mtrscripting.mod.network.packet.data.DoorDirectionsS2CPacket;
import org.mtr.core.Main;
import org.mtr.core.data.Data;
import org.mtr.core.data.Platform;
import org.mtr.core.data.Position;
import org.mtr.core.data.Rail;
import org.mtr.core.data.Route;
import org.mtr.core.data.RoutePlatformData;
import org.mtr.core.simulation.Simulator;
import org.mtr.libraries.it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import org.mtr.libraries.it.unimi.dsi.fastutil.longs.LongArrayList;
import org.mtr.libraries.it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import org.mtr.mapping.holder.MinecraftServer;
import org.mtr.mapping.holder.PlayerEntity;
import org.mtr.mapping.holder.ServerPlayerEntity;
import org.mtr.mapping.holder.ServerWorld;
import org.mtr.mapping.holder.World;
import org.mtr.mapping.holder.WorldSavePath;
import org.mtr.mapping.mapper.MinecraftServerHelper;
import org.mtr.mod.Init;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Stream;

/**
 * The server half of the door-opening-direction feature — the "learn once,
 * answer anywhere" memory migrated off the client and onto the server, so a
 * train's next-stop door sides can be answered mid-journey no matter how far
 * away (or chunk-unloaded) that platform is.
 *
 * <h2>Storage</h2>
 * Per dimension, under the MTR data root of the world save:
 * <pre>
 *     &lt;world&gt;/mtr/&lt;dimension&gt;/routes-door-directions/
 *         &lt;2-char hex shard&gt;/&lt;route hex id&gt;.json      one file per route
 *         scan-complete-v2                                 written once the first
 *                                                       full scan has finished
 * </pre>
 * Each route file maps the ids of the platforms the route calls at to the
 * two canonical openable sides (see {@link DoorScan} for the frame in which
 * "left" and "right" are stored).
 *
 * <p>The folder deliberately sits <b>beside</b> MTR's own {@code routes/}
 * folder and mirrors its two-hex-character sharding rather than inside it:
 * MTR's {@code FileLoader} parses every file it finds in {@code routes/} as
 * MessagePack route data, deletes on the spot anything that fails to parse
 * with a {@code MessageTypeException}, and — worse — registers every file
 * it does parse (valid route or not) in its hash bookkeeping, so the next
 * autosave purges whatever does not belong to a live route. A foreign file
 * placed in {@code routes/} or {@code routes/&lt;xx&gt;/} would therefore be
 * silently deleted within thirty seconds. {@code routes-door-directions/}
 * keeps the same per-route layout without being inside that purge's reach.
 *
 * <h2>Lifecycle</h2>
 * <ul>
 *   <li><b>First start on this save</b> (no {@code scan-complete-v2} marker):
 *       every platform that at least one route calls at — and only those,
 *       per requirement — is scanned by walking its platform rail(s) and
 *       testing the blocks beside the track; unloaded chunks are loaded for
 *       the read, exactly the capability a client never had. The work is
 *       spread over server ticks ({@value #SCAN_ITEMS_PER_TICK} platforms
 *       per tick behind a {@value #TICK_TIME_BUDGET_MILLIS} ms guard) so
 *       the one-time scan never stalls a tick, and it begins from a
 *       snapshot taken at least {@value #FULL_SCAN_START_DELAY_MILLIS} ms
 *       after server start so the full scan never runs against a railway
 *       snapshot taken before every route↔platform link has settled.
 *       Results land in per-route files, then the marker is written; later
 *       boots load the files and never rescan the whole save again. The
 *       marker is a <b>v2</b> name: the v1 marker of earlier test builds is
 *       deliberately ignored so every save gets one fresh, complete scan
 *       under the corrected platform-only block test.</li>
 *   <li><b>Runtime block changes</b>: the mixin on
 *       {@code World#setBlockState} reports every change whose old or new
 *       block is a platform block or a PSD/APG door. Positions are
 *       coalesced by chunk and then by platform, and only a handful of
 *       platforms are re-scanned per tick — so WorldEdit/Axiom batch edits
 *       of any size turn into a short trickle of re-scans instead of a lag
 *       spike, and platforms far from the change are never touched.</li>
 *   <li><b>Client queries</b>: {@code getDirectionOfDoorOpening()} asks the
 *       server; the server answers from the stored per-route data,
 *       scheduling a one-route scan on demand if that route predates the
 *       feature or was added after the marker was written.</li>
 * </ul>
 *
 * <h2>Startup robustness</h2>
 * <b>Every early-out is visible.</b> This handler previously depended on
 * MTR's core ({@code org.mtr.mod.Init#main}) already existing when the
 * server-started event fired, and silently did nothing otherwise — the
 * entire feature no-oped without a single log line, which is exactly what
 * field logs from the first test builds showed. Now:
 * <ul>
 *   <li>{@link #onServerStarted} initializes every dimension it can and
 *       <b>logs</b> the outcome; when the core is not ready yet (mod init
 *       order differences across MTR builds), it says so and defers.</li>
 *   <li>{@link #onWorldTick} <b>self-heals</b>: any world that reaches a
 *       tick without its per-dimension state gets initialized on the spot,
 *       so a missed/mis-ordered start hook can never keep the feature
 *       down — the first world tick after MTR's core appears brings the
 *       dimension online (logged).</li>
 *   <li>A world whose dimension has no matching MTR simulator is logged
 *       once with both strings, instead of being skipped silently.</li>
 * </ul>
 *
 * <p>All mutable state is confined to the server thread (the same
 * discipline as the MTR code this complements); railway data is only ever
 * read inside {@code Simulator#run}, the MTR-sanctioned handoff, and the
 * only other thread is a daemon file writer that receives immutable
 * snapshot strings.
 */
public final class DoorDirectionManager {

    /** Sibling of MTR's {@code routes} folder, mirroring its layout. */
    static final String FOLDER_NAME = "routes-door-directions";
    /**
     * Marker file whose presence means "this save has been fully scanned;
     * do not rescan on boot". Versioned: v2 ignores the v1 markers of the
     * first test builds (whose scans ran with an obsolete block test), so
     * every save gets one fresh complete scan after upgrading.
     */
    static final String MARKER_NAME = "scan-complete-v2";
    private static final int FORMAT_VERSION = 1;

    /* Tick budgets: bounded work per tick so neither the first full scan nor
       a WorldEdit burst can ever lag the server. */
    private static final int SCAN_ITEMS_PER_TICK = 2;
    private static final int CHANGED_CHUNKS_PER_TICK = 64;
    private static final int RESCANS_PER_TICK = 4;
    private static final long TICK_TIME_BUDGET_MILLIS = 4;
    private static final int INDEX_REFRESH_TICKS = 600;

    /**
     * The full scan's platform universe is taken from a snapshot at least
     * this long after server start, so it cannot run against railway data
     * that is still linking routes to platforms. Regular index snapshots
     * start immediately; only the full scan waits.
     */
    private static final long FULL_SCAN_START_DELAY_MILLIS = 5_000;

    /* Blocks that can influence door-openability sit within a couple of
       blocks of the rail line; the platform box margin covers the client
       scan's lattice reach (±1 horizontal, ±2 vertical around the strip). */
    private static final int BOX_MARGIN = 3;

    private static final long SAVE_DEBOUNCE_MILLIS = 10_000;
    private static final long CORE_MISSING_WARN_MILLIS = 60_000;

    /** Dimension-string-keyed view, for the client-query path (a player's world arrives as a fresh holder wrapper). */
    private static final Object2ObjectArrayMap<String, WorldState> WORLDS = new Object2ObjectArrayMap<>();
    /** Raw-world-keyed view, for the per-tick and block-change paths: the raw world instance is a per-dimension singleton at runtime, so identity-keying makes those lookups allocation-free. */
    private static final IdentityHashMap<Object, WorldState> WORLDS_BY_RAW = new IdentityHashMap<>();
    private static MinecraftServer server;

    /* Lazy-init diagnostics: every kind of "could not initialize" state is
       reported at least once, never silently. */
    private static long startedAtMillis;
    private static long lastCoreMissingWarnAt;
    private static final ObjectOpenHashSet<String> WARNED_NO_SIMULATOR = new ObjectOpenHashSet<>();
    private static final LongOpenHashSet QUERY_MISS_WARNED = new LongOpenHashSet();

    /* Cached simulator-by-dimension view of the MTR core, rebuilt when the
       core instance changes; null means "core not up yet". */
    private static Object2ObjectOpenHashMap<String, Simulator> simulatorsByDimension;
    private static Main simulatorsForMain;

    /* ==================================================================== */
    /* Lifecycle (mapped event registry callbacks, server thread)           */
    /* ==================================================================== */

    /** Server starting: build per-dimension state, load stored files, schedule the first scan if needed. */
    public static void onServerStarted(MinecraftServer minecraftServer) {
        reset();
        server = minecraftServer;
        startedAtMillis = System.currentTimeMillis();

        final Object2ObjectOpenHashMap<String, Simulator> simulators = simulatorsByDimension();
        if(simulators == null || simulators.isEmpty()) {
            MTRScriptingMod.LOGGER.warn("[MTR Scripting via JCM] Door directions: MTR core not available at server start (event ordering); dimensions will initialize lazily on world ticks");
            return;
        }

        final int[] initialized = {0};
        MinecraftServerHelper.iterateWorlds(minecraftServer, serverWorld -> {
            if(initWorld(minecraftServer, simulators, serverWorld, "server start") != null) {
                initialized[0]++;
            }
        });
        MTRScriptingMod.LOGGER.info("[MTR Scripting via JCM] Door directions: {} dimension(s) initialized at server start", initialized[0]);
    }

    /** Server stopping: flush pending file writes, forget everything. */
    public static void onServerStopping(MinecraftServer minecraftServer) {
        try {
            for(final WorldState state : WORLDS.values()) {
                state.writeDirtyRoutes();
            }
            Writer.flush();
        } finally {
            reset();
        }
    }

    /**
     * World tick (server thread): advance this dimension's pipelines, and
     * self-heal — a world reaching a tick without its state gets
     * initialized right here, so a missed or mis-ordered server-start hook
     * (the core not existing yet when it fired) costs at most a few ticks
     * instead of disabling the feature for the whole session.
     */
    public static void onWorldTick(ServerWorld serverWorld) {
        final WorldState state = WORLDS_BY_RAW.get(serverWorld.data);
        if(state != null) {
            state.tick(state.world);
            return;
        }
        if(server == null) return;
        if(startedAtMillis == 0) {
            /* A fully lazy startup (the server-start hook never running)
               still needs an anchor for the full-scan delay. */
            startedAtMillis = System.currentTimeMillis();
        }
        final Object2ObjectOpenHashMap<String, Simulator> simulators = simulatorsByDimension();
        if(simulators == null || simulators.isEmpty()) {
            final long now = System.currentTimeMillis();
            if(now - lastCoreMissingWarnAt >= CORE_MISSING_WARN_MILLIS) {
                lastCoreMissingWarnAt = now;
                MTRScriptingMod.LOGGER.warn("[MTR Scripting via JCM] Door directions: MTR core still not available ({}); retrying on later world ticks", serverWorld.data.getClass().getName());
            }
            return;
        }
        initWorld(server, simulators, serverWorld, "lazy");
    }

    private static void reset() {
        WORLDS.clear();
        WORLDS_BY_RAW.clear();
        server = null;
        startedAtMillis = 0;
        lastCoreMissingWarnAt = 0;
        WARNED_NO_SIMULATOR.clear();
        QUERY_MISS_WARNED.clear();
        simulatorsByDimension = null;
        simulatorsForMain = null;
    }

    /**
     * The MTR core's simulators keyed by dimension string, cached against
     * the core instance; null while the core does not exist yet (the
     * server-start hook can legitimately run before it does).
     */
    private static Object2ObjectOpenHashMap<String, Simulator> simulatorsByDimension() {
        final Main main = InitAccessorMixin.getMain();
        if(main == null) return null;
        if(simulatorsByDimension == null || simulatorsForMain != main) {
            final Object2ObjectOpenHashMap<String, Simulator> map = new Object2ObjectOpenHashMap<>();
            for(final Simulator simulator : ((MainAccessorMixin)(Object)main).getSimulators()) {
                map.put(simulator.dimension, simulator);
            }
            simulatorsByDimension = map;
            simulatorsForMain = main;
        }
        return simulatorsByDimension;
    }

    /**
     * Brings one server world's per-dimension state online (idempotent per
     * world via the caller's map membership) and reports what it found —
     * the marker state and the stored file count — so the boot log always
     * shows the feature's state for this dimension.
     */
    private static WorldState initWorld(MinecraftServer minecraftServer, Object2ObjectOpenHashMap<String, Simulator> simulators, ServerWorld serverWorld, String context) {
        final String dimension = Init.getWorldId(new World(serverWorld.data));
        final Simulator simulator = simulators.get(dimension);
        if(simulator == null) {
            if(WARNED_NO_SIMULATOR.add(dimension)) {
                MTRScriptingMod.LOGGER.warn("[MTR Scripting via JCM] Door directions: no MTR simulator for dimension \"{}\" ({}); known dimensions: {} — this world's door sides are not tracked", dimension, context, String.join(", ", simulators.keySet()));
            }
            return null;
        }
        final Path mtrRoot = minecraftServer.getSavePath(WorldSavePath.getRootMapped()).resolve("mtr");
        final WorldState state = new WorldState(dimension, simulator, mtrRoot.resolve(dimension), serverWorld);
        WORLDS.put(dimension, state);
        WORLDS_BY_RAW.put(serverWorld.data, state);
        state.loadFromDisk();
        if(state.scanned) {
            MTRScriptingMod.LOGGER.info("[MTR Scripting via JCM] Door directions: dimension {} online ({}): scan marker present, {} stored route file(s) loaded", dimension, context, state.routes.size());
        } else {
            MTRScriptingMod.LOGGER.info("[MTR Scripting via JCM] Door directions: dimension {} online ({}): no scan marker, a full platform scan is due", dimension, context);
        }
        return state;
    }

    /* ==================================================================== */
    /* Block-change entry points (called from the mixin, server thread)     */
    /* ==================================================================== */

    /**
     * A block state change whose old or new block was a platform block or a
     * PSD/APG door (the mixin already filtered), i.e. a change that might
     * alter whether trains can open doors on some side of a nearby platform.
     * The raw world object keys {@link #WORLDS_BY_RAW}; never allocates.
     */
    public static void onDoorBlockChange(Object rawWorld, int x, int y, int z) {
        final WorldState state = WORLDS_BY_RAW.get(rawWorld);
        if(state == null) return;
        state.noteChangedBlock(x, z);
    }

    /** Cheap pre-filter for the mixin: whether any indexed platform area intersects this chunk column. Never allocates. */
    public static boolean chunkMayContainPlatforms(Object rawWorld, int chunkX, int chunkZ) {
        final WorldState state = WORLDS_BY_RAW.get(rawWorld);
        return state != null && state.chunkHasPlatforms(chunkX, chunkZ);
    }

    /* ==================================================================== */
    /* Client query (called via minecraftServer.submit from the C2S packet) */
    /* ==================================================================== */

    /**
     * Answers a client's request for one route's stored door directions.
     * Always sends a response: the stored data when the route has been
     * scanned, otherwise an empty "incomplete" one while a one-route scan
     * is scheduled — a client retry a few seconds later finds the data.
     */
    public static void handleQuery(MinecraftServer minecraftServer, ServerPlayerEntity player, long routeId) {
        if(routeId == 0) {
            send(minecraftServer, player, DoorDirectionsS2CPacket.empty(routeId));
            return;
        }
        final WorldState state = WORLDS.get(Init.getWorldId(player.getEntityWorld()));
        if(state == null) {
            send(minecraftServer, player, DoorDirectionsS2CPacket.empty(routeId));
            return;
        }
        state.requestLazyScan(routeId);
        final WorldState.RouteData data = state.routes.get(routeId);
        if(data != null) {
            final long[] platformIds = data.sides.keySet().toLongArray();
            Arrays.sort(platformIds);
            final boolean[] left = new boolean[platformIds.length];
            final boolean[] right = new boolean[platformIds.length];
            for(int i = 0; i < platformIds.length; i++) {
                final boolean[] sides = data.sides.get(platformIds[i]);
                left[i] = sides[0];
                right[i] = sides[1];
            }
            send(minecraftServer, player, new DoorDirectionsS2CPacket(routeId, true, platformIds, left, right));
        } else {
            if(QUERY_MISS_WARNED.add(routeId)) {
                MTRScriptingMod.LOGGER.info("[MTR Scripting via JCM] Door directions: client asked for route {} which has no stored sides yet; a one-route scan was scheduled", routeId);
            }
            send(minecraftServer, player, DoorDirectionsS2CPacket.empty(routeId));
        }
    }

    private static void send(MinecraftServer minecraftServer, ServerPlayerEntity player, DoorDirectionsS2CPacket packet) {
        minecraftServer.submit(() -> com.lx862.jcm.mod.registry.Networking.sendPacketToClient(PlayerEntity.cast(player), packet));
    }

    /* ==================================================================== */
    /* Per-dimension state                                                   */
    /* ==================================================================== */

    private static final class WorldState {
        final String dimension;
        final Simulator simulator;
        final Path root;
        /** Holder-wrapped world, cached once — it only wraps the raw instance. */
        final World world;

        /** Route id → platform id → canonical sides {@code [left, right]}. */
        final Long2ObjectOpenHashMap<RouteData> routes = new Long2ObjectOpenHashMap<>();

        static final class RouteData {
            final Long2ObjectOpenHashMap<boolean[]> sides = new Long2ObjectOpenHashMap<>();
            boolean dirty;
        }

        boolean scanned;          /* marker present, or full scan finished */
        boolean fullScanActive;   /* full-scan work is queued/running */
        boolean indexRequested;   /* a snapshot handoff to the sim is in flight */
        boolean warnedEmptyFullScan;  /* the "full scan found nothing" WARN prints once per dimension */
        int ticksSinceIndex;
        int fullScanCount;        /* platforms in the running full scan, for the log */

        /* Completion summary of the running full scan: how many platforms
           landed on each side distribution, so one log line shows both that
           the scan really covered the save and whether the results look
           sane (a scan that reports "neither" for nearly every platform is
           a geometry bug, and now it says so loudly). */
        int fullLeftOnly;
        int fullRightOnly;
        int fullBoth;
        int fullNeither;
        long nextSaveAllowedAtMillis;

        /* Pending work, all server-thread confined. */
        final ObjectArrayList<PlatformWork> scanQueue = new ObjectArrayList<>();
        int scanCursor;
        final LongOpenHashSet lazyRoutes = new LongOpenHashSet();
        final LongOpenHashSet changedChunks = new LongOpenHashSet();
        final LongOpenHashSet pendingRescans = new LongOpenHashSet();

        /* Spatial index from the latest simulator snapshot. */
        ObjectArrayList<PlatformSnapshot> platformIndex = new ObjectArrayList<>();
        Long2ObjectOpenHashMap<LongOpenHashSet> chunkToPlatforms = new Long2ObjectOpenHashMap<>();

        WorldState(String dimension, Simulator simulator, Path dimensionRoot, ServerWorld serverWorld) {
            this.dimension = dimension;
            this.simulator = simulator;
            this.root = dimensionRoot.resolve(FOLDER_NAME);
            this.world = new World(serverWorld.data);
        }

        /* ---------------------------------------------------------------- */
        /* Boot                                                              */
        /* ---------------------------------------------------------------- */

        void loadFromDisk() {
            scanned = Files.isRegularFile(root.resolve(MARKER_NAME));
            if(!Files.isDirectory(root)) return;
            try(final Stream<Path> shards = Files.list(root)) {
                for(final Path shard : (Iterable<Path>) shards::iterator) {
                    if(!Files.isDirectory(shard)) continue;
                    try(final Stream<Path> files = Files.list(shard)) {
                        for(final Path file : (Iterable<Path>) files::iterator) {
                            loadRouteFile(file);
                        }
                    } catch(Exception e) {
                        MTRScriptingMod.LOGGER.warn("[MTR Scripting via JCM] Door directions: could not list {} ({})", shard, e.toString());
                    }
                }
            } catch(Exception e) {
                MTRScriptingMod.LOGGER.warn("[MTR Scripting via JCM] Door directions: could not list {} ({})", root, e.toString());
            }
        }

        private void loadRouteFile(Path file) {
            final String name = file.getFileName().toString();
            if(!name.endsWith(".json")) return;
            final long routeId;
            try {
                routeId = Long.parseUnsignedLong(name.substring(0, name.length() - ".json".length()), 16);
            } catch(NumberFormatException ignored) {
                return;
            }
            try(final BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                /* Instance-form JsonParser.parse: MC 1.17.1 bundles a gson
                   older than 2.8.6 without the static parseReader (the same
                   compatibility note as the rest of this repo). */
                final JsonElement rootElement = new JsonParser().parse(reader);
                if(!rootElement.isJsonObject()) return;
                final JsonElement platformsElement = rootElement.getAsJsonObject().get("platforms");
                if(platformsElement == null || !platformsElement.isJsonObject()) return;
                final RouteData data = new RouteData();
                for(final Map.Entry<String, JsonElement> entry : platformsElement.getAsJsonObject().entrySet()) {
                    try {
                        final long platformId = Long.parseLong(entry.getKey());
                        if(!entry.getValue().isJsonObject()) continue;
                        final JsonObject sidesObject = entry.getValue().getAsJsonObject();
                        final JsonElement leftElement = sidesObject.get("left");
                        final JsonElement rightElement = sidesObject.get("right");
                        if(leftElement == null || rightElement == null) continue;
                        data.sides.put(platformId, new boolean[]{leftElement.getAsBoolean(), rightElement.getAsBoolean()});
                    } catch(RuntimeException ignored) {
                        /* Skip a malformed entry, keep the rest. */
                    }
                }
                routes.put(routeId, data);
            } catch(Exception e) {
                MTRScriptingMod.LOGGER.warn("[MTR Scripting via JCM] Door directions: could not read {} ({})", file, e.toString());
            }
        }

        /* ---------------------------------------------------------------- */
        /* Tick                                                              */
        /* ---------------------------------------------------------------- */

        void tick(World world) {
            ticksSinceIndex++;
            /* The full scan's universe must come from a snapshot taken at
               least FULL_SCAN_START_DELAY_MILLIS after server start, so it
               can never freeze a half-linked railway into the marker.
               Index snapshots (for the block-change pre-filter and lazy
               route scans) are not delayed. scanned flips to true only in
               finishScanIfNeeded — i.e. after a full scan really completed —
               so a completed scan can never re-arm here. */
            final boolean canFullScan = !scanned && startedAtMillis > 0
                    && System.currentTimeMillis() - startedAtMillis >= FULL_SCAN_START_DELAY_MILLIS;
            if(!indexRequested
                    && (!lazyRoutes.isEmpty() || platformIndex.isEmpty() || canFullScan || ticksSinceIndex >= INDEX_REFRESH_TICKS)) {
                requestIndex(canFullScan);
            }

            final long deadline = System.currentTimeMillis() + TICK_TIME_BUDGET_MILLIS;
            processScanQueue(world, deadline);
            processChangedChunks(deadline);
            processPendingRescans(world, deadline);
            finishScanIfNeeded();
            writeDirtyRoutesIfDue();
        }

        /* ---------------------------------------------------------------- */
        /* Simulator snapshot (via Simulator#run, the MTR-sanctioned path)   */
        /* ---------------------------------------------------------------- */

        void requestLazyScan(long routeId) {
            if(!routes.containsKey(routeId)) lazyRoutes.add(routeId);
        }

        private void requestIndex(boolean includeFullScan) {
            indexRequested = true;
            ticksSinceIndex = 0;
            final boolean fullScan = includeFullScan;
            final long[] lazyRouteIds = lazyRoutes.isEmpty() ? new long[0] : lazyRoutes.toLongArray();
            lazyRoutes.clear();
            final WorldState self = this;
            simulator.run(() -> snapshotRailways(self, fullScan, lazyRouteIds));
        }

        /**
         * Runs in the simulator's context: reads live railway data and posts
         * an immutable snapshot back to the server thread.
         *
         * <p>The platform→rail association uses the core's own lookup —
         * {@code Data.tryGet(simulator.positionsToRail, platform position1,
         * platform position2)} with an {@code isPlatform()} check, the exact
         * resolution {@code SavedRailBase#isInvalidSavedRail} performs — and
         * NOT any hex id: {@code Rail.getHexId()} is the two-positions hex
         * ({@link org.mtr.core.data.TwoPositionsBase}) while
         * {@code Platform.getHexId()} is the 16-char hex of the platform's
         * numeric id ({@link org.mtr.core.data.NameColorDataBase}) —
         * two unrelated final methods on divergent hierarchy branches that
         * happen to share a name. The first server build keyed rails by
         * positions-hex and looked platforms up by id-hex, so the lookup
         * could never match a single platform on any save, the full scan
         * quietly had zero work, and clients fell back to view-distance
         * scans forever. Platform endpoints are read through the public
         * accessors ({@code getRandomPosition()} returns position1,
         * {@code getOtherPosition(position1)} returns position2), the same
         * trick keeps this on MTR's public API on every MC version.
         */
        private static void snapshotRailways(WorldState state, boolean fullScan, long[] lazyRouteIds) {
            final ObjectArrayList<PlatformSnapshot> platformIndex = new ObjectArrayList<>();
            final ObjectArrayList<PlatformWork> fullScanWork = new ObjectArrayList<>();
            int platformsTotal = 0;
            int platformsWithRoutes = 0;
            for(final Platform platform : state.simulator.platforms) {
                platformsTotal++;
                if(platform.routes.isEmpty()) continue;   /* only platforms a route calls at, per requirement */
                platformsWithRoutes++;
                final Rail rail = platformRail(state.simulator, platform);
                if(rail == null) continue;   /* no platform rail: nothing trains can stop on */
                final PlatformSnapshot snapshot = new PlatformSnapshot(
                        platform.getId(), ObjectArrayList.of(rail), platform.routes.stream().mapToLong(Route::getId).sorted().toArray());
                platformIndex.add(snapshot);
                if(fullScan) fullScanWork.add(new PlatformWork(snapshot));
            }

            /* Lazy per-route scans cover routes the first scan never saw —
               added after the marker, or predating the feature. */
            final ObjectArrayList<PlatformWork> lazyWork = new ObjectArrayList<>();
            final LongOpenHashSet lazyQueued = new LongOpenHashSet();
            final LongArrayList emptyLazyRoutes = new LongArrayList();
            for(final long routeId : lazyRouteIds) {
                final Route route = state.simulator.routeIdMap.get(routeId);
                if(route == null) continue;
                boolean any = false;
                for(final RoutePlatformData routePlatformData : route.getRoutePlatforms()) {
                    final Platform platform = routePlatformData.getPlatform();
                    if(platform == null || platform.routes.isEmpty()) continue;
                    final Rail rail = platformRail(state.simulator, platform);
                    if(rail == null) continue;
                    any = true;
                    if(lazyQueued.add(platform.getId())) {
                        lazyWork.add(new PlatformWork(new PlatformSnapshot(
                                platform.getId(), ObjectArrayList.of(rail), platform.routes.stream().mapToLong(Route::getId).sorted().toArray())));
                    }
                }
                if(!any) {
                    /* Route exists but has no platform a train could stop
                       on: record an empty entry so clients stop asking. */
                    emptyLazyRoutes.add(routeId);
                }
            }

            if(server == null) return;   /* server stopped while snapshotting */
            final MinecraftServer minecraftServer = server;
            minecraftServer.submit(() -> state.acceptIndex(fullScan, platformIndex, fullScanWork, lazyWork, emptyLazyRoutes, platformsTotal, platformsWithRoutes));
        }

        /**
         * The one rail this platform sits on, resolved exactly how the core
         * resolves it ({@code SavedRailBase#isInvalidSavedRail}): the rail
         * registered between the platform's two endpoint positions in the
         * simulator's {@code positionsToRail} cache, provided it is a
         * platform rail. A platform without such a rail has nothing for a
         * train to stop on and is skipped — same as the core's own
         * validity rule.
         */
        private static Rail platformRail(Simulator simulator, Platform platform) {
            final Position position1 = platform.getRandomPosition();
            final Position position2 = platform.getOtherPosition(position1);
            final Rail rail = Data.tryGet(simulator.positionsToRail, position1, position2);
            return rail != null && rail.isPlatform() ? rail : null;
        }

        /** Back on the server thread: install the snapshot and queue the work. */
        private void acceptIndex(boolean fullScan, ObjectArrayList<PlatformSnapshot> platformIndex,
                ObjectArrayList<PlatformWork> fullScanWork, ObjectArrayList<PlatformWork> lazyWork, LongArrayList emptyLazyRoutes,
                int platformsTotal, int platformsWithRoutes) {
            indexRequested = false;
            this.platformIndex = platformIndex;
            this.chunkToPlatforms = new Long2ObjectOpenHashMap<>();
            for(final PlatformSnapshot snapshot : platformIndex) {
                for(final long chunkKey : snapshot.chunkKeys()) {
                    chunkToPlatforms.computeIfAbsent(chunkKey, key -> new LongOpenHashSet()).add(snapshot.platformId);
                }
            }
            for(int i = 0; i < emptyLazyRoutes.size(); i++) {
                final RouteData data = new RouteData();
                data.dirty = true;
                routes.putIfAbsent(emptyLazyRoutes.getLong(i), data);
            }
            for(final PlatformWork work : lazyWork) {
                if(!isQueuedForScan(work.snapshot.platformId)) {
                    scanQueue.add(work);
                }
            }
            if(fullScan && !fullScanWork.isEmpty() && !fullScanActive) {
                fullScanActive = true;
                fullScanCount = fullScanWork.size();
                fullLeftOnly = 0;
                fullRightOnly = 0;
                fullBoth = 0;
                fullNeither = 0;
                scanQueue.clear();
                scanCursor = 0;
                scanQueue.addAll(fullScanWork);
                MTRScriptingMod.LOGGER.info("[MTR Scripting via JCM] Door directions: full platform scan of {} started — {} route-served platform(s) queued; results will land under {}", dimension, fullScanCount, root);
            } else if(fullScan && fullScanWork.isEmpty() && !scanned && !warnedEmptyFullScan) {
                /* The very failure mode that hid the first test build's
                   matching bug: a full scan with zero work prints nothing,
                   writes nothing, and looks exactly like "no feature". Now
                   it says so, with the tallies that tell the two real
                   causes apart (a save with no route-served platforms vs.
                   an association that matched nothing). No marker is
                   written, so a fixed environment rescans on the next
                   boot — an empty result is never frozen as truth. */
                warnedEmptyFullScan = true;
                MTRScriptingMod.LOGGER.warn("[MTR Scripting via JCM] Door directions: full platform scan of {} found no scannable platform — {} platform(s) in the simulator, {} route-served; if this save has platforms that trains stop at, report this line; no scan marker will be written", dimension, platformsTotal, platformsWithRoutes);
            }
        }

        private boolean isQueuedForScan(long platformId) {
            for(int i = scanCursor; i < scanQueue.size(); i++) {
                if(scanQueue.get(i).snapshot.platformId == platformId) return true;
            }
            return false;
        }

        /* ---------------------------------------------------------------- */
        /* Scanning (server thread, world block reads may chunk-load)        */
        /* ---------------------------------------------------------------- */

        private void processScanQueue(World world, long deadline) {
            int processed = 0;
            while(scanCursor < scanQueue.size() && processed < SCAN_ITEMS_PER_TICK && System.currentTimeMillis() < deadline) {
                final PlatformWork work = scanQueue.get(scanCursor);
                scanCursor++;
                processed++;
                final boolean[] sides = applyPlatformSides(world, work.snapshot);
                if(fullScanActive) tallySides(sides);
            }
            if(scanCursor >= scanQueue.size()) {
                scanQueue.clear();
                scanCursor = 0;
            }
        }

        private void tallySides(boolean[] sides) {
            if(sides[0] && sides[1]) fullBoth++;
            else if(sides[0]) fullLeftOnly++;
            else if(sides[1]) fullRightOnly++;
            else fullNeither++;
        }

        private void finishScanIfNeeded() {
            if(fullScanActive && scanQueue.isEmpty()) {
                fullScanActive = false;
                /* scanned == true is what stops canFullScan from re-arming
                   every tick after the scan completes: without it the
                   completed scan re-queued itself forever, one full pass
                   every few seconds, re-writing every route file each
                   cycle. */
                scanned = true;
                /* Data files first, marker second: the queue is FIFO, so the
                   marker can never precede its data. */
                writeDirtyRoutes();
                final JsonObject marker = new JsonObject();
                marker.addProperty("version", FORMAT_VERSION);
                marker.addProperty("dimension", dimension);
                marker.addProperty("platforms", fullScanCount);
                marker.addProperty("scannedAt", System.currentTimeMillis());
                Writer.enqueue(root.resolve(MARKER_NAME), marker.toString());
                MTRScriptingMod.LOGGER.info("[MTR Scripting via JCM] Door directions: full scan of {} complete — {} route-served platform(s): left-only {}, right-only {}, both {}, neither {}; {} route file(s) under {}", dimension, fullScanCount, fullLeftOnly, fullRightOnly, fullBoth, fullNeither, routes.size(), root);
            }
        }

        /** Scans a platform's rails now, unions the sides, and stores them into every route serving the platform. */
        private boolean[] applyPlatformSides(World world, PlatformSnapshot snapshot) {
            boolean left = false;
            boolean right = false;
            for(final Rail rail : snapshot.rails) {
                final boolean[] sides = DoorScan.scanRailSides(world, rail);
                left |= sides[0];
                right |= sides[1];
                if(left && right) break;
            }
            final boolean[] result = new boolean[]{left, right};
            storeSides(snapshot, result);
            return result;
        }

        private void storeSides(PlatformSnapshot snapshot, boolean[] sides) {
            for(final long routeId : snapshot.routeIds) {
                final RouteData data = routes.computeIfAbsent(routeId, key -> new RouteData());
                final boolean[] existing = data.sides.get(snapshot.platformId);
                if(existing == null || existing[0] != sides[0] || existing[1] != sides[1]) {
                    data.sides.put(snapshot.platformId, new boolean[]{sides[0], sides[1]});
                    data.dirty = true;
                }
            }
        }

        /* ---------------------------------------------------------------- */
        /* Runtime block changes                                             */
        /* ---------------------------------------------------------------- */

        void noteChangedBlock(int x, int z) {
            changedChunks.add(chunkKey(x >> 4, z >> 4));
        }

        boolean chunkHasPlatforms(int chunkX, int chunkZ) {
            return chunkToPlatforms.containsKey(chunkKey(chunkX, chunkZ));
        }

        private void processChangedChunks(long deadline) {
            if(changedChunks.isEmpty()) return;
            int drained = 0;
            final LongOpenHashSet iteratorSet = changedChunks;
            final var iterator = iteratorSet.iterator();
            while(iterator.hasNext() && drained < CHANGED_CHUNKS_PER_TICK && System.currentTimeMillis() < deadline) {
                final long chunkKey = iterator.nextLong();
                iterator.remove();
                drained++;
                final LongOpenHashSet platformIds = chunkToPlatforms.get(chunkKey);
                if(platformIds != null) pendingRescans.addAll(platformIds);
            }
        }

        private void processPendingRescans(World world, long deadline) {
            int processed = 0;
            final var iterator = pendingRescans.iterator();
            while(iterator.hasNext() && processed < RESCANS_PER_TICK && System.currentTimeMillis() < deadline) {
                final long platformId = iterator.nextLong();
                iterator.remove();
                processed++;
                for(final PlatformSnapshot snapshot : platformIndex) {
                    if(snapshot.platformId == platformId) {
                        applyPlatformSides(world, snapshot);
                        break;
                    }
                }
            }
        }

        /* ---------------------------------------------------------------- */
        /* Persistence                                                       */
        /* ---------------------------------------------------------------- */

        private void writeDirtyRoutesIfDue() {
            if(System.currentTimeMillis() >= nextSaveAllowedAtMillis) {
                writeDirtyRoutes();
            }
        }

        /** Snapshots every dirty route's JSON now (server thread) and hands the strings to the writer thread. */
        void writeDirtyRoutes() {
            nextSaveAllowedAtMillis = System.currentTimeMillis() + SAVE_DEBOUNCE_MILLIS;
            ObjectArrayList<Object[]> pending = null;
            for(final Long2ObjectOpenHashMap.Entry<RouteData> entry : routes.long2ObjectEntrySet()) {
                final RouteData data = entry.getValue();
                if(!data.dirty) continue;
                data.dirty = false;
                if(pending == null) pending = new ObjectArrayList<>();
                final long routeId = entry.getLongKey();
                final JsonObject rootJson = new JsonObject();
                rootJson.addProperty("version", FORMAT_VERSION);
                rootJson.addProperty("routeId", Long.toString(routeId));
                final JsonObject platformsJson = new JsonObject();
                for(final Long2ObjectOpenHashMap.Entry<boolean[]> platformEntry : data.sides.long2ObjectEntrySet()) {
                    final JsonObject sidesJson = new JsonObject();
                    sidesJson.addProperty("left", platformEntry.getValue()[0]);
                    sidesJson.addProperty("right", platformEntry.getValue()[1]);
                    platformsJson.add(Long.toString(platformEntry.getLongKey()), sidesJson);
                }
                rootJson.add("platforms", platformsJson);
                pending.add(new Object[]{routeIdPath(routeId), rootJson.toString()});
            }
            if(pending == null) return;
            for(final Object[] routeFile : pending) {
                Writer.enqueue((Path) routeFile[0], (String) routeFile[1]);
            }
        }

        /** File path mirroring MTR's routes sharding: last two hex characters of the route's hex id. */
        private Path routeIdPath(long routeId) {
            final Route route = simulator.routeIdMap.get(routeId);
            /* Same format as SerializedDataBaseWithId#getHexId (16-char,
               zero-padded, uppercase) even when the sim no longer knows the
               route, so a file never splits across two names. */
            final String hexId = route == null
                    ? String.format("%16s", Long.toHexString(routeId)).replace(' ', '0').toUpperCase(Locale.ENGLISH)
                    : route.getHexId();
            final String shard = hexId.length() <= 2 ? hexId : hexId.substring(hexId.length() - 2);
            return root.resolve(shard).resolve(hexId + ".json");
        }
    }

    /* ==================================================================== */
    /* Snapshots                                                            */
    /* ==================================================================== */

    /**
     * Immutable snapshot of one route-served platform: the rail references
     * (their geometry fields are final), the ids of every route calling at
     * it, and the block box — rail bounds plus the change-detection margin —
     * within which a block edit can affect this platform's door sides.
     */
    private static final class PlatformSnapshot {
        final long platformId;
        final ObjectArrayList<Rail> rails;
        final long[] routeIds;
        final int minX;
        final int minY;
        final int minZ;
        final int maxX;
        final int maxY;
        final int maxZ;

        PlatformSnapshot(long platformId, ObjectArrayList<Rail> rails, long[] routeIds) {
            this.platformId = platformId;
            this.rails = rails;
            this.routeIds = routeIds;
            int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
            int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
            for(final Rail rail : rails) {
                minX = Math.min(minX, (int) rail.railMath.minX - BOX_MARGIN);
                minY = Math.min(minY, (int) rail.railMath.minY - BOX_MARGIN);
                minZ = Math.min(minZ, (int) rail.railMath.minZ - BOX_MARGIN);
                maxX = Math.max(maxX, (int) rail.railMath.maxX + BOX_MARGIN);
                maxY = Math.max(maxY, (int) rail.railMath.maxY + BOX_MARGIN);
                maxZ = Math.max(maxZ, (int) rail.railMath.maxZ + BOX_MARGIN);
            }
            this.minX = minX;
            this.minY = minY;
            this.minZ = minZ;
            this.maxX = maxX;
            this.maxY = maxY;
            this.maxZ = maxZ;
        }

        /** Chunk-column keys covering the platform's block box (including the client scan's lattice reach). */
        LongOpenHashSet chunkKeys() {
            final LongOpenHashSet keys = new LongOpenHashSet();
            for(int x = minX >> 4; x <= maxX >> 4; x++) {
                for(int z = minZ >> 4; z <= maxZ >> 4; z++) {
                    keys.add(chunkKey(x, z));
                }
            }
            return keys;
        }
    }

    /** One platform awaiting a scan. */
    private static final class PlatformWork {
        final PlatformSnapshot snapshot;

        PlatformWork(PlatformSnapshot snapshot) {
            this.snapshot = snapshot;
        }
    }

    /* ==================================================================== */
    /* File writer (single daemon thread, immutable jobs only)              */
    /* ==================================================================== */

    private static long chunkKey(int chunkX, int chunkZ) {
        return ((long) chunkX << 32) | (chunkZ & 0xFFFFFFFFL);
    }

    private static final class Writer {
        private static final LinkedBlockingQueue<Object> QUEUE = new LinkedBlockingQueue<>();
        private static final Thread THREAD = new Thread(Writer::run, "JCM door-direction writer");
        private static final Object FLUSH_TOKEN = new Object();
        private static boolean flushDone;

        static {
            THREAD.setDaemon(true);
            THREAD.start();
        }

        static void enqueue(Path file, String content) {
            QUEUE.add(new Write(file, content));
        }

        /** Blocks until everything queued before this call has been written (server stopping). */
        static void flush() {
            try {
                QUEUE.put(FLUSH_TOKEN);
                synchronized(FLUSH_TOKEN) {
                    /* Wait in bounded steps: a wedged writer must never
                       hang the shutdown loop for more than a few seconds. */
                    for(int i = 0; i < 5 && !flushDone; i++) {
                        FLUSH_TOKEN.wait(2000);
                    }
                    flushDone = false;
                }
            } catch(InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        private static void run() {
            while(true) {
                try {
                    final Object job = QUEUE.take();
                    if(job == FLUSH_TOKEN) {
                        synchronized(FLUSH_TOKEN) {
                            flushDone = true;
                            FLUSH_TOKEN.notifyAll();
                        }
                        continue;
                    }
                    write((Write) job);
                } catch(InterruptedException e) {
                    return;
                } catch(RuntimeException e) {
                    MTRScriptingMod.LOGGER.warn("[MTR Scripting via JCM] Door directions: writer failed ({})", e.toString());
                }
            }
        }

        /** tmp + atomic move, the same durability approach the client-side memory used. */
        private static void write(Write write) {
            try {
                Files.createDirectories(write.file.getParent());
                final Path tmp = write.file.resolveSibling(write.file.getFileName() + ".tmp");
                try(final BufferedWriter writer = Files.newBufferedWriter(tmp, StandardCharsets.UTF_8)) {
                    writer.write(write.content);
                }
                try {
                    Files.move(tmp, write.file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
                } catch(IOException atomicUnsupported) {
                    Files.move(tmp, write.file, StandardCopyOption.REPLACE_EXISTING);
                }
            } catch(IOException | RuntimeException e) {
                MTRScriptingMod.LOGGER.warn("[MTR Scripting via JCM] Door directions: could not write {} ({})", write.file, e.toString());
            }
        }

        private static final class Write {
            final Path file;
            final String content;

            Write(Path file, String content) {
                this.file = file;
                this.content = content;
            }
        }
    }
}
