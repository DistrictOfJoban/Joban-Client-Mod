package com.lx862.mtrscripting.mod.impl.mtr.vehicle;

import com.lx862.jcm.mixin.mtrscripting.VehicleSchemaMixin;
import com.lx862.jcm.mixin.mtrscripting.VehicleAccessorMixin;
import com.lx862.jcm.mod.util.MTRUtil;
import com.lx862.mtrscripting.core.annotation.ApiInternal;
import com.lx862.mtrscripting.core.annotation.ValueNullable;
import com.lx862.mtrscripting.core.util.ScriptVector3f;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import org.mtr.core.data.*;
import org.mtr.core.tool.Utilities;
import org.mtr.core.tool.Vector;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.mtr.mapping.holder.Box;
import org.mtr.mod.client.MinecraftClientData;
import org.mtr.mod.client.VehicleRidingMovement;
import org.mtr.mod.data.VehicleExtension;
import org.mtr.mod.render.PositionAndRotation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class VehicleWrapper {
    private static final int FULL_NOTCH_LEVEL = 5;

    /* Direction-of-door-opening vocabulary, shared verbatim between JS,
       the C++ snapshot (mtr::door_direction / mtr::DoorDirection) and
       the abi-11 snapshot byte (0..3). */
    public static final String DOOR_DIRECTION_NOT = "not";
    public static final String DOOR_DIRECTION_LEFT = "left";
    public static final String DOOR_DIRECTION_RIGHT = "right";
    public static final String DOOR_DIRECTION_ALL = "all";

    /* At-platform boundary tolerance: when dwelling, railProgress sits
       exactly on the platform segment's endDistance (the stopping point). */
    private static final double AT_PLATFORM_TOLERANCE = 0.5;

    /**
     * Memo for {@link #getDirectionOfDoorOpening()} — the world-block scans
     * it runs are far more expensive than a getter should be on a hot frame
     * path (every native vehicle snapshot calls it every frame). One entry
     * per vehicle; the (kind, key) pair invalidates on the at-platform ⇄
     * predicting transition and on a platform change, and the TTL bounds
     * world-block staleness only — block edits beside a platform are rare
     * and the LCD-style consumers switch screens on a several-hundred-ms
     * scale anyway. Same access pattern and limits as the limited-stops
     * cache in {@link StopsData#LIMITED_CACHE}.
     */
    private static final Map<Long, CachedDoorDirection> DOOR_DIRECTION_CACHE = new HashMap<>();
    private static final long DOOR_DIRECTION_TTL_NANOS = 250_000_000L;
    private static final int DOOR_DIRECTION_CACHE_MAX = 256;

    private static final class CachedDoorDirection {
        long keyKind;      /* 0 = dwelling at platform, 1 = predicting platform keyId */
        long keyId;        /* platform id while predicting, else 0 */
        long builtAtNanos;
        String value = DOOR_DIRECTION_NOT;
    }

    protected final VehicleExtension vehicleExtension;
    protected final StopsData stopsData;
    protected final ObjectArrayList<PositionAndRotation> posAndRotations;
    public final boolean[] doorLeftOpen;
    public final boolean[] doorRightOpen;
    private final boolean anyVehicleCarVisible;

    public VehicleWrapper(VehicleScriptContext.DataFetchMode dataFetchMode, VehicleExtension vehicleExtension) {
        this.vehicleExtension = vehicleExtension;
        this.doorLeftOpen = new boolean[getCarCount()];
        this.doorRightOpen = new boolean[getCarCount()];
        boolean anyVehicleCarVisible = false;
        for(boolean rayTracingVisible : vehicleExtension.persistentVehicleData.rayTracing) {
            if(rayTracingVisible) {
                anyVehicleCarVisible = true;
                break;
            }
        }
        this.anyVehicleCarVisible = anyVehicleCarVisible;
        posAndRotations = vehicleExtension.getSmoothedVehicleCarsAndPositions(0).stream()
                .map(vehicleCarAndPosition -> {
                    final ObjectArrayList<PositionAndRotation> bogiePositions = vehicleCarAndPosition.right()
                            .stream()
                            .map(bogiePositionPair -> new PositionAndRotation(bogiePositionPair.left(), bogiePositionPair.right(), true))
                            .collect(Collectors.toCollection(ObjectArrayList::new));
                    return new PositionAndRotation(bogiePositions, vehicleCarAndPosition.left(), vehicleExtension.getTransportMode().hasPitchAscending || vehicleExtension.getTransportMode().hasPitchDescending);
                })
                .collect(Collectors.toCollection(ObjectArrayList::new));

        for(int i = 0; i < getCarCount(); i++) {
            PositionAndRotation posAndRotation = posAndRotations.get(i);
            double halfWidth = getWidth(i) / 2;
            double halfLength = getLength(i) / 2;

            this.doorLeftOpen[i] = getDoorValue() > 0 && MTRUtil.canOpenDoors(new Box(-halfWidth - 0.1, -1, -halfLength, -halfWidth, 1, halfLength), posAndRotation);
            this.doorRightOpen[i] = getDoorValue() > 0 && MTRUtil.canOpenDoors(new Box(halfWidth, -1, -halfLength, halfWidth + 0.1, 1, halfLength), posAndRotation);
        }

        this.stopsData = StopsData.constructData(dataFetchMode, vehicleExtension);
    }

    public List<Stop> getStops() {
        return stopsData.allStops;
    }

    public List<Stop> getThisRouteStops() {
        int nextStopIdx = getNextStopIndex(getStops());
        if(nextStopIdx >= getStops().size()) {
            nextStopIdx = getStops().size()-1;
        }
        return getRouteStops(nextStopIdx);
    }

    public List<Stop> getNextRouteStops() {
        int nextStopIdx = getNextStopIndex(getStops());
        long thisRouteId = getThisRouteId();
        int nextRouteIndex = stopsData.routeToRun.indexOf(thisRouteId)+1;
        if(nextRouteIndex >= stopsData.routeToRun.size()) return List.of();

        long nextRouteId = stopsData.routeToRun.getLong(nextRouteIndex);
        int nextRouteStartStopIdx = -1;
        for(int i = nextStopIdx; i < stopsData.allStops.size(); i++) {
            /* Stop.route is @ValueNullable — a train that has no route yet (or a
               stop MTR has not resolved) yields null here. Dereferencing it
               threw an NPE that aborted the whole render frame. */
            final SimplifiedRoute stopRoute = stopsData.allStops.get(i).route;
            if(stopRoute != null && stopRoute.getId() == nextRouteId) {
                nextRouteStartStopIdx = i;
            }
        }

        if(nextRouteStartStopIdx == -1) return List.of();
        return getRouteStops(nextRouteStartStopIdx);
    }

    public int getNextStopIndex(List<Stop> stops) {
        return getNextStopIndex(stops, 0.5);
    }

    public int getNextStopIndex(List<Stop> stops, double overrunTolerance) {
        return findNextStopIndex(overrunTolerance, getRailProgress(), stops);
    }

    public boolean isStopsDataFetched() {
        return stopsData.isFullData;
    }

    public boolean isStopsDataFullyFetched() {
        if(!isStopsDataFetched()) return false;

        for(Stop stop : stopsData.allStops) {
            if(stop.platform == null) return false; // Use platform as a measure, since that will never be null if fetched.
        }
        return true;
    }

    protected long getThisRouteId() {
        long routeId = vehicleExtension.vehicleExtraData.getThisRouteId();
        if(isStopsDataFetched()) {
            int nextStopIndex = getNextStopIndex(getStops(), 0);
            if(nextStopIndex < getStops().size()) {
                SimplifiedRoute route = getStops().get(nextStopIndex).route;
                if(route != null) {
                    routeId = route.getId();
                }
            }
        }
        return routeId;
    }

    protected List<Stop> getRouteStops(int stopIndex) {
        List<Stop> stops = stopsData.stopIdxToRouteStops.get(stopIndex);
        return stops == null ? List.of() : stops;
    }

    protected int findNextStopIndex(double overrunTolerance, double currentRailProgress, List<Stop> stops) {
        boolean distanceAvailable = !stops.isEmpty() && stops.get(0).distance >= 0;

        if(distanceAvailable) {
            int stopIdx = 0;

            for(Stop stop : stops) {
                if(currentRailProgress > stop.distance + overrunTolerance) stopIdx++;
                else break;
            }
            return stopIdx;
        } else {
            long nextPlatId = vehicleExtension.vehicleExtraData.getThisPlatformId();
            Stop nextStop = stops.stream().filter(e -> e.platform != null && e.platform.getId() == nextPlatId).findFirst().orElse(null);
            if(nextStop != null) {
                int idx = stops.indexOf(nextStop);
                if(idx != -1) return idx;
            }
            return stops.size();
        }
    }

    @ApiInternal
    public static class StopsData {
        public final ObjectArrayList<Stop> allStops;
        public final ObjectArrayList<Stop> allStopsNextRoute;
        public final Int2ObjectOpenHashMap<List<Stop>> stopIdxToRouteStops;
        public final LongArrayList routeToRun;
        /* Non-final: the client cache swaps the siding object in when MTR
           replaces it in sidingIdMap, without rebuilding the whole graph. */
        public Siding siding;
        public final boolean isFullData;

        public StopsData(Siding siding, boolean isFullData) {
            this.isFullData = isFullData;
            this.allStops = new ObjectArrayList<>();
            this.allStopsNextRoute = new ObjectArrayList<>();
            this.routeToRun = new LongArrayList();
            this.stopIdxToRouteStops = new Int2ObjectOpenHashMap<>();
            this.siding = siding;
        }

        /**
         * Memo for the limited (non-full) stops path, keyed by vehicle id and
         * fetch mode. The limited path rebuilds its whole Stop graph —
         * including MTR's per-station interchange maps — on every frame when
         * full data has not arrived (or SKIP mode never requests it). The
         * rebuild is bounded to once per second per vehicle, and to
         * immediately when the route signature or dataset revision changes.
         */
        private static final Map<Long, Limited> LIMITED_CACHE = new HashMap<>();
        private static final long LIMITED_TTL_NANOS = 1_000_000_000L;
        private static final int LIMITED_MAX = 256;

        private static final class Limited {
            VehicleScriptContext.DataFetchMode mode;
            long signature;
            long revision;
            long builtAtNanos;
            StopsData data;
        }

        public static StopsData constructData(VehicleScriptContext.DataFetchMode dataFetchMode, VehicleExtension vehicle) {
            if(dataFetchMode != VehicleScriptContext.DataFetchMode.SKIP) {
                VehicleDataCache.requestVehicleStopsData(vehicle.getId(), vehicle.vehicleExtraData.getSidingId());
            }

            StopsData fullStopsData = VehicleDataCache.buildVehicleStopsData(vehicle);
            if(dataFetchMode != VehicleScriptContext.DataFetchMode.SKIP && fullStopsData != null) {
                return fullStopsData;
            }

            final long vehicleId = vehicle.getId();
            final long key = vehicleId | (dataFetchMode == VehicleScriptContext.DataFetchMode.SKIP ? 1L << 62 : 0L);
            final long signature = vehicle.vehicleExtraData.getPreviousRouteId() * 31
                    + vehicle.vehicleExtraData.getThisRouteId() * 7
                    + vehicle.vehicleExtraData.getNextRouteId();
            final long revision = VehicleDataCache.mtrData.getRevision();
            final long now = System.nanoTime();
            final Limited cached = LIMITED_CACHE.get(key);
            if(cached != null && cached.data != null && cached.mode == dataFetchMode
                    && cached.signature == signature && cached.revision == revision
                    && now - cached.builtAtNanos < LIMITED_TTL_NANOS) {
                return cached.data;
            }

            if(LIMITED_CACHE.size() >= LIMITED_MAX) LIMITED_CACHE.clear();

            Siding siding = MinecraftClientData.getInstance().sidingIdMap.get(vehicle.vehicleExtraData.getSidingId());
            StopsData limitedStopsData = new StopsData(siding, false);
            buildLimitedStopsData(limitedStopsData, vehicle);

            Limited entry = cached != null ? cached : new Limited();
            entry.mode = dataFetchMode;
            entry.signature = signature;
            entry.revision = revision;
            entry.builtAtNanos = now;
            entry.data = limitedStopsData;
            LIMITED_CACHE.put(key, entry);
            return limitedStopsData;
        }

        private static StopsData buildLimitedStopsData(StopsData stopsData, VehicleExtension vehicleExtension) {
            List<SimplifiedRoute> allRoutes = new ArrayList<>();
            SimplifiedRoute lastRoute = MinecraftClientData.getInstance().simplifiedRouteIdMap.get(vehicleExtension.vehicleExtraData.getPreviousRouteId());
            if (lastRoute != null) allRoutes.add(lastRoute);

            SimplifiedRoute thisRoute = MinecraftClientData.getInstance().simplifiedRouteIdMap.get(vehicleExtension.vehicleExtraData.getThisRouteId());
            if (thisRoute != null) allRoutes.add(thisRoute);

            SimplifiedRoute nextRoute = MinecraftClientData.getInstance().simplifiedRouteIdMap.get(vehicleExtension.vehicleExtraData.getNextRouteId());
            if (nextRoute != null) allRoutes.add(nextRoute);

            int stopIdx = 0;
            long lastPlatformId = 0;
            for(SimplifiedRoute route : allRoutes) {
                stopsData.routeToRun.add(route.getId());

                List<SimplifiedRoutePlatform> routePlatforms = route.getPlatforms();
                List<Stop> routeStops = new ArrayList<>();
                Station destinationStation = MinecraftClientData.getInstance().stationIdMap.get(routePlatforms.get(routePlatforms.size()-1).getStationId());

                for(SimplifiedRoutePlatform routePlatform : routePlatforms) {
                    String destinationName = routePlatform.getDestination();
                    Station station = MinecraftClientData.getInstance().stationIdMap.get(routePlatform.getStationId());
                    Platform platform = MinecraftClientData.getInstance().platformIdMap.get(routePlatform.getPlatformId());

                    Stop thisStop = new Stop(route, station, platform, routePlatform.getStationName(), destinationName, "", -1, false);
                    thisStop.destinationStation = destinationStation;
                    InterchangeRouteModes interchangeModes = new InterchangeRouteModes();
                    if (station != null) {
                        station.getInterchangeStationToColorToRoutesMap(true).forEach((s, interchanges) -> {
                            if (s.getId() == station.getId()) interchanges.forEach((color, routes) -> routes.forEach(r ->
                                        interchangeModes.add(color, r.getName(), r.getTransportMode(),
                                                MagicRouteTypes.extendedType(r.getId()), r.getId())));
                        });
                    }
                    if (platform != null) platform.routes.forEach(r ->
                            interchangeModes.add(r.getColor(), r.getName(), r.getTransportMode(),
                                    MagicRouteTypes.extendedType(r.getId()), r.getId()));
                    // In-station interchange
                    routePlatform.forEach((color, routes) -> {
                        routes.forEach(routeName -> {
                            final InterchangeRouteModes.Entry entry = interchangeModes.getEntry(color, routeName);
                            thisStop.routeInterchanges.add(new Stop.RouteInterchange(
                                    entry == null ? 0 : entry.routeId, color, routeName,
                                    entry == null ? null : entry.mode, entry == null ? null : entry.typeId()));
                        });
                    });
                    // Connecting station interchange
                    if(station != null) {
                        station.getInterchangeStationToColorToRoutesMap(true).forEach((connectingStation, interchanges) -> {
                            if (connectingStation.getId() != station.getId()) {
                                interchanges.forEach((color, routes) -> routes.forEach(interchangeRoute ->
                                        thisStop.connectingInterchanges.computeIfAbsent(connectingStation.getName(), k -> new ObjectArrayList<>())
                                                .add(new Stop.RouteInterchange(interchangeRoute.getId(), color, interchangeRoute.getName(),
                                                        interchangeRoute.getTransportMode(), null))));
                            }
                        });
                    }

                    boolean shouldAppendStop = false;
                    if(routePlatform.getPlatformId() == lastPlatformId) { // Duplicated platform, likely double-added stop from route changeover.
                        Stop prevStop = stopsData.allStops.get(stopsData.allStops.size()-1);
                        prevStop.roundUpRoute = thisStop;
                        prevStop.reverseAtPlatform = true;
                        prevStop.isRouteSwitchoverStop = true;
                    } else {
                        shouldAppendStop = true;
                        stopsData.allStops.add(thisStop);
                    }
                    routeStops.add(thisStop);
                    stopsData.stopIdxToRouteStops.put(stopIdx, routeStops);
                    lastPlatformId = routePlatform.getPlatformId();
                    if(shouldAppendStop) stopIdx++;
                }
            }
            return stopsData;
        }
    }

    public static class Stop {
        public @ValueNullable SimplifiedRoute route;
        public @ValueNullable Station station;
        public String name;
        public @ValueNullable Platform platform;
        public String destinationName;
        public @ValueNullable String customDestination;
        public List<RouteInterchange> routeInterchanges;
        public Map<String, List<RouteInterchange>> connectingInterchanges;
        public long dwellTimeMillis;
        public double distance;
        public Stop roundUpRoute;
        public boolean isRouteSwitchoverStop;
        @Deprecated
        @ValueNullable
        public Station destinationStation; // Manually obtain the Station of the last stop instead
        @Deprecated
        public long dwellTime; // Use dwellTimeMs instead
        @Deprecated
        public boolean reverseAtPlatform; // Identical to routeSwitchover
        public boolean turnbackPlatform;

        @ApiInternal
        public Stop(SimplifiedRoute route, Station station, Platform platform,
                    String name, String destinationName, String customDestination, double distance, boolean turnbackPlatform) {
            this.route = route;
            this.station = station;
            this.platform = platform;
            this.dwellTime = platform == null ? -1 : platform.getDwellTime() / 500;
            this.dwellTimeMillis = platform == null ? -1 : platform.getDwellTime();
            this.routeInterchanges = new ArrayList<>();
            this.connectingInterchanges = new HashMap<>();
            this.name = name;
            this.destinationName = destinationName;
            this.customDestination = customDestination.isEmpty() ? null : customDestination;
            this.distance = distance;
            this.roundUpRoute = this;
            this.turnbackPlatform = turnbackPlatform;
        }

        public static class RouteInterchange {
            public final int color;
            public final String name;
            private final TransportMode transportMode;
            /** Canonical type id (MAGIC extension or base mode id); never null. */
            private final String transportType;
            /** Originating route id, 0 when the server data omits it. */
            private final long routeId;

            @ApiInternal
            public RouteInterchange(int color, String name) {
                this(0, color, name, null, null);
            }

            @ApiInternal
            public RouteInterchange(int color, String name, TransportMode transportMode) {
                this(0, color, name, transportMode, null);
            }

            @ApiInternal
            public RouteInterchange(int color, String name, TransportMode transportMode, String transportType) {
                this(0, color, name, transportMode, transportType);
            }

            /**
             * @param routeId        originating route id (0 = unknown); enables a
             *                        client-side MAGIC lookup when {@code transportType}
             *                        is null and the route id is known
             * @param transportMode  the actual interchange mode, or null when older
             *                        server data omits it
             * @param transportType   pre-resolved canonical type id, or null to resolve
             *                        it from {@code routeId} (MAGIC) / {@code transportMode}
             */
            @ApiInternal
            public RouteInterchange(long routeId, int color, String name,
                                    TransportMode transportMode, String transportType) {
                this.routeId = routeId;
                this.color = color;
                this.name = name;
                this.transportMode = transportMode;
                this.transportType = transportType != null ? transportType
                        : routeId != 0 ? MagicRouteTypes.typeId(routeId, transportMode)
                        : MagicRouteTypes.baseTypeId(transportMode);
            }

            /** The actual interchange mode, or null when older server data omits it. */
            public @ValueNullable TransportMode getTransportMode() {
                return transportMode;
            }

            /**
             * The interchange's transport type as a canonical id: a MAGIC MTR
             * extension ("train_metro", "train_light_rail", ...) when the addon
             * assigns one, otherwise the base mode id ("train", "boat",
             * "cable_car", "airplane"). Never null; "unknown" only when the mode
             * itself is unknown (older server data).
             */
            public String getTransportType() {
                return transportType;
            }

            /** The originating route id, or 0 when the server data omits it. */
            public long getRouteId() {
                return routeId;
            }
        }
    }

    /* Start getters */
    public boolean isRendered() {
        return anyVehicleCarVisible;
    }
    public boolean isCarRendered(int... cars) {
        for(int car : cars) {
            if(vehicleExtension.persistentVehicleData.rayTracing[car]) return true;
        }
        return false;
    }
    public boolean isClientPlayerRiding() {
        return VehicleRidingMovement.isRiding(vehicleExtension.getId());
    }
    public VehicleExtension getMtrVehicle() {
        return this.vehicleExtension;
    }
    public long getId() {
        return this.vehicleExtension.getId();
    }
    public @ValueNullable Siding getSiding() {
        return this.stopsData.siding;
    }
    public @ValueNullable String getVehicleId(int carIndex) {
        if(carIndex >= vehicleExtension.vehicleExtraData.immutableVehicleCars.size()) return null;
        return vehicleExtension.vehicleExtraData.immutableVehicleCars.get(carIndex).getVehicleId();
    }
    public double getLength(int carIndex) {
        return vehicleExtension.vehicleExtraData.immutableVehicleCars.get(carIndex).getLength();
    }
    public double getWidth(int carIndex) {
        return vehicleExtension.vehicleExtraData.immutableVehicleCars.get(carIndex).getWidth();
    }
    public TransportMode getTransportMode() {
        return vehicleExtension.getTransportMode();
    }
    /**
     * The vehicle's transport type as a canonical id: the MAGIC MTR Addon's
     * extended type for the vehicle's current route ("train_metro",
     * "train_high_speed", ...) when one is assigned, otherwise the base mode
     * id ("train", "boat", "cable_car", "airplane"). Never null.
     *
     * <p>Native (C++) snapshots surface the exact same string through
     * {@code mtr::Train::getTransportType()} (ABI 9), so JS and C++ scripts
     * agree on the vocabulary. {@link #getTransportMode()} keeps returning the
     * plain enum for scripts that only care about the four base modes.
     */
    public String getTransportType() {
        return MagicRouteTypes.typeId(getThisRouteId(), getTransportMode());
    }
    public int getCarCount() {
        return vehicleExtension.vehicleExtraData.immutableVehicleCars.size();
    }
    public double getServiceAcceleration() {
        return vehicleExtension.vehicleExtraData.getAcceleration();
    }
    public double getServiceDeceleration() {
        return vehicleExtension.vehicleExtraData.getDeceleration();
    }
    public double getMaxManualSpeed() {
        return vehicleExtension.vehicleExtraData.getMaxManualSpeed();
    }
    public boolean isManualAllowed() {
        return vehicleExtension.vehicleExtraData.getIsManualAllowed();
    }
    public int getManualToAutomaticTime() {
        Siding siding = getSiding();
        return siding == null ? -1 : siding.getManualToAutomaticTime();
    }
    public List<PathData> getPathData() {
        return vehicleExtension.vehicleExtraData.immutablePath;
    }
    public double getRailProgress() {
        return ((VehicleSchemaMixin)vehicleExtension).getRailProgress();
    }
    public double getRailProgress(int car) {
        double progress = getRailProgress();
        for(int i = 0; i < Math.min(vehicleExtension.vehicleExtraData.immutableVehicleCars.size(), car); i++) {
            progress -= vehicleExtension.vehicleExtraData.immutableVehicleCars.get(i).getLength();
        }
        return progress;
    }
    public int getRailIndex(double railProgress, boolean roundDown) {
        return Utilities.getIndexFromConditionalList(vehicleExtension.vehicleExtraData.immutablePath, railProgress - (roundDown ? 1.0F : 0));
    }
    public double getRailSpeed(int pathIndex) {
        return vehicleExtension.vehicleExtraData.immutablePath.get(pathIndex).getSpeedLimitKilometersPerHour() / 3.6 / 20;
    }
    public int getNotchLevel() {
        return vehicleExtension.vehicleExtraData.getPowerLevel();
    }
    public long getDepartureIndex() {
        return vehicleExtension.getDepartureIndex();
    }
    public double getNotchPosition() {
        return (double)getNotchLevel() / FULL_NOTCH_LEVEL;
    }
    public double getSpeedMs() {
        return vehicleExtension.getSpeed() * 1000;
    }
    public double getSpeedKmh() {
        return getSpeedMs() * 3.6;
    }
    public double getDoorValue() {
        return vehicleExtension.persistentVehicleData.getDoorValue();
    }
    public boolean isCurrentlyManual() {
        return vehicleExtension.vehicleExtraData.getIsCurrentlyManual();
    }
    public boolean isReversed() {
        return vehicleExtension.getReversed();
    }
    public boolean isOnRoute() {
        return vehicleExtension.getIsOnRoute();
    }
    public boolean isDoorOpening() {
        return vehicleExtension.persistentVehicleData.getAdjustedDoorMultiplier(vehicleExtension.vehicleExtraData) > 0;
    }
    public long getTotalDwellTime() {
        int railIndex = getRailIndex(getRailProgress(), true);
        if(railIndex < vehicleExtension.vehicleExtraData.immutablePath.size()) {
            PathData path = vehicleExtension.vehicleExtraData.immutablePath.get(railIndex);
            if(path != null) {
                return path.getDwellTime();
            }
        }
        return -1;
    }
    /** NOTE: Elapsed Dwell Time is not counted on the client-side, it purely relies on the synced value from the server, which isn't real-time. */
    public long getElapsedDwellTime() {
        return ((VehicleAccessorMixin)vehicleExtension).getElapsedDwellTime();
    }

    public boolean isDoorOpenable(int car, ScriptVector3f relativeCarPos1, ScriptVector3f relativeCarPos2) {
        if(car >= posAndRotations.size()) return false;
        PositionAndRotation positionAndRotation = posAndRotations.get(car);
        return MTRUtil.canOpenDoors(new Box(relativeCarPos1.rawVector3d(), relativeCarPos2.rawVector3d()), positionAndRotation);
    }

    public boolean isDoorOpenable(int car, ScriptVector3f relativeCarPos) {
        return isDoorOpenable(car, relativeCarPos, relativeCarPos);
    }

    public boolean isDoorOpen(int car, ScriptVector3f relativeCarPos1, ScriptVector3f relativeCarPos2) {
        return getDoorValue() > 0 && isDoorOpenable(car, relativeCarPos1, relativeCarPos2);
    }

    public boolean isDoorOpen(int car, ScriptVector3f relativeCarPos) {
        return getDoorValue() > 0 && isDoorOpenable(car, relativeCarPos);
    }

    /* ==================================================================== */
    /* Direction of door opening (JCM extension)                            */
    /* ==================================================================== */

    /**
     * Reports which side(s) of this vehicle the doors can open on, as one of
     * {@link #DOOR_DIRECTION_NOT "not"}, {@link #DOOR_DIRECTION_LEFT "left"},
     * {@link #DOOR_DIRECTION_RIGHT "right"} or {@link #DOOR_DIRECTION_ALL "all"}.
     *
     * <p>Semantics (JCM extension over MTR's per-doorway
     * {@code RenderVehicleHelper#canOpenDoors} world scan):
     * <ul>
     *   <li>While dwelling at a platform, the answer is the same regardless of
     *       the current door phase — doors not yet open, partially open,
     *       closing, or already closed all report the value the fully-open
     *       state has: the sides where platform blocks (or unlocked
     *       PSD/APG doors) sit beside the car door planes.</li>
     *   <li>While running on the mainline, the <b>next stop's</b> opening
     *       sides are predicted by laying the vehicle out at the stopping
     *       point on its own path and running the same world scan there
     *       (MTR itself would report nothing in this state).</li>
     *   <li>As soon as a single car on one side finds an openable platform
     *       block, that whole side counts as openable (per-side aggregation,
     *       not per-doorway).</li>
     *   <li>Left/right are relative to the vehicle <b>head</b> (bogie-1 end),
     *       unaffected by travel direction or reversing: per-car boxes are
     *       evaluated in each car's local frame, where -X is the head's left
     *       (MTR model convention, in-game verified).</li>
     * </ul>
     *
     * <p>This is purely informational — it never influences which doors MTR
     * actually opens; MTR keeps owning that.
     */
    public String getDirectionOfDoorOpening() {
        /* Doors are currently opening/open/closing: the per-car arrays built
           in the constructor already hold the exact world-checked per-side
           answer for this very frame — reuse them for free. The aggregation
           below then satisfies the at-platform contract regardless of how far
           the doors have actually traveled. */
        if(getDoorValue() > 0) {
            boolean left = false, right = false;
            for(int i = 0; i < doorLeftOpen.length; i++) {
                if(doorLeftOpen[i]) left = true;
                if(doorRightOpen[i]) right = true;
            }
            return doorDirection(left, right);
        }
        if(!isOnRoute()) return DOOR_DIRECTION_NOT;
        return computeCachedDoorDirection();
    }

    private static String doorDirection(boolean left, boolean right) {
        if(left) return right ? DOOR_DIRECTION_ALL : DOOR_DIRECTION_LEFT;
        return right ? DOOR_DIRECTION_RIGHT : DOOR_DIRECTION_NOT;
    }

    /** Drops a vehicle's cached door-direction entry (dispose hook). */
    @ApiInternal
    public static void clearDoorDirectionCache(long vehicleId) {
        DOOR_DIRECTION_CACHE.remove(vehicleId);
    }

    /** Cached half of {@link #getDirectionOfDoorOpening()} (the world-scan paths). */
    private String computeCachedDoorDirection() {
        final boolean atPlatform = isStoppedAtPlatform();
        final Platform nextPlatform = atPlatform ? null : nextStopPlatform();
        final long keyKind = atPlatform ? 0 : 1;
        final long keyId = atPlatform || nextPlatform == null ? 0 : nextPlatform.getId();

        final long now = System.nanoTime();
        final Map<Long, CachedDoorDirection> cache = DOOR_DIRECTION_CACHE;
        CachedDoorDirection cached = cache.get(getId());
        if(cached != null && cached.keyKind == keyKind && cached.keyId == keyId
                && now - cached.builtAtNanos < DOOR_DIRECTION_TTL_NANOS) {
            return cached.value;
        }

        final String value;
        if(atPlatform) {
            value = currentPlatformDoorDirection();
        } else if(nextPlatform == null) {
            value = DOOR_DIRECTION_NOT;
        } else {
            value = predictPlatformDoorDirection(nextPlatform);
        }

        if(cache.size() >= DOOR_DIRECTION_CACHE_MAX) cache.clear();
        if(cached == null) {
            cached = new CachedDoorDirection();
            cache.put(getId(), cached);
        }
        cached.keyKind = keyKind;
        cached.keyId = keyId;
        cached.builtAtNanos = now;
        cached.value = value;
        return value;
    }

    /**
     * Dwelling at a platform: the vehicle is stopped exactly on a platform
     * segment's stopping point (its front-most railProgress equals the
     * segment endDistance), doors may be in any phase. Checked on the
     * vehicle's own path data, so it works even before stop distances have
     * been requested from the server.
     */
    private boolean isStoppedAtPlatform() {
        if(Math.abs(getSpeedMs()) > 1e-4) return false;
        final List<PathData> path = getPathData();
        if(path.isEmpty()) return false;
        final double railProgress = getRailProgress();
        final int index = Utilities.getIndexFromConditionalList(path, railProgress);
        /* At the stop, railProgress == startDistance of the segment AFTER
           the platform rail == endDistance of the platform rail itself; check
           both boundary conventions. */
        for(int offset = -1; offset <= 0; offset++) {
            final PathData pathData = Utilities.getElement(path, index + offset);
            if(pathData == null) continue;
            if(pathData.getSavedRailBaseId() == 0 || pathData.getDwellTime() <= 0) continue;
            if(Math.abs(pathData.getEndDistance() - railProgress) <= AT_PLATFORM_TOLERANCE) return true;
        }
        return false;
    }

    /** The platform object of the next stop, or null when unknown. */
    private Platform nextStopPlatform() {
        final List<Stop> stops = getStops();
        final int nextStopIndex = getNextStopIndex(stops);
        if(nextStopIndex >= stops.size()) return null;
        return stops.get(nextStopIndex).platform;
    }

    /**
     * Per-side world scan around the vehicles's current car poses — the same
     * boxes the constructor uses for {@code doorLeftOpen/doorRightOpen}, but
     * NOT gated on the door value, so the answer equals the fully-open state.
     */
    private String currentPlatformDoorDirection() {
        boolean left = false, right = false;
        for(int i = 0; i < getCarCount() && !(left && right); i++) {
            final PositionAndRotation positionAndRotation = posAndRotations.get(i);
            final double halfWidth = getWidth(i) / 2;
            final double halfLength = getLength(i) / 2;
            if(!left) {
                left = MTRUtil.canOpenDoors(new Box(-halfWidth - 0.1, -1, -halfLength, -halfWidth, 1, halfLength), positionAndRotation);
            }
            if(!right) {
                right = MTRUtil.canOpenDoors(new Box(halfWidth, -1, -halfLength, halfWidth + 0.1, 1, halfLength), positionAndRotation);
            }
        }
        return doorDirection(left, right);
    }

    /**
     * Prediction for a platform the vehicle has not reached yet: find that
     * platform's rail segment on the vehicle's own path, lay the cars out
     * exactly where they will rest at that stop (mirroring MTR's
     * {@code Vehicle.getVehicleCarsAndPositions} math against the stopping
     * point), then run the standard per-side door scans at those poses.
     * Falls back to "not" when the platform is off-path or its geometry is
     * not resolvable on the client.
     */
    private String predictPlatformDoorDirection(Platform platform) {
        PathData platformPath = null;
        final List<PathData> path = getPathData();
        final int currentIndex = Utilities.getIndexFromConditionalList(path, getRailProgress());
        /* Prefer the first matching segment at/after the current position so
           circular paths resolve the upcoming visit, not a previous one. */
        for(int i = Math.max(0, currentIndex); i < path.size(); i++) {
            final PathData pathData = path.get(i);
            if(pathData.getSavedRailBaseId() == platform.getId() && pathData.getDwellTime() > 0) {
                platformPath = pathData;
                break;
            }
        }
        if(platformPath == null) {
            for(PathData pathData : path) {
                if(pathData.getSavedRailBaseId() == platform.getId() && pathData.getDwellTime() > 0) {
                    platformPath = pathData;
                    break;
                }
            }
        }
        if(platformPath == null) return DOOR_DIRECTION_NOT;

        final double stopDistance = platformPath.getEndDistance();
        final boolean reversed = isReversed();
        final List<VehicleCar> vehicleCars = vehicleExtension.vehicleExtraData.immutableVehicleCars;
        final double totalVehicleLength = vehicleExtension.vehicleExtraData.getTotalVehicleLength();
        final double lowerBound = stopDistance - totalVehicleLength;
        final boolean hasPitch = vehicleExtension.getTransportMode().hasPitchAscending || vehicleExtension.getTransportMode().hasPitchDescending;

        boolean left = false, right = false;
        double checkRailProgress = stopDistance - (reversed ? totalVehicleLength : 0);
        for(int i = 0; i < vehicleCars.size() && !(left && right); i++) {
            final VehicleCar vehicleCar = vehicleCars.get(i);
            final double halfLength = vehicleCar.getLength() / 2;
            /* The two joggled samples per bogie mirror MTR's
               Vehicle.getBogiePositions clamping, which keeps bogies inside
               the stopped vehicle's bounds. MTR additionally steps by the
               package-private coupling padding before each car; skipping it
               drifts each car by a sub-block along the track only — the side
               boxes span the full car length, so side detection is unaffected
               (same approximation as getRailProgress(int) below). */
            final ObjectArrayList<PositionAndRotation> bogiePositions = new ObjectArrayList<>();
            if(!addBogiePosition(bogiePositions, checkRailProgress + (reversed ? 1 : -1) * (halfLength + vehicleCar.getBogie1Position()), lowerBound, stopDistance, reversed, hasPitch)) {
                return DOOR_DIRECTION_NOT;
            }
            if(!vehicleCar.hasOneBogie) {
                if(!addBogiePosition(bogiePositions, checkRailProgress + (reversed ? 1 : -1) * (halfLength + vehicleCar.getBogie2Position()), lowerBound, stopDistance, reversed, hasPitch)) {
                    return DOOR_DIRECTION_NOT;
                }
            }

            final PositionAndRotation positionAndRotation = new PositionAndRotation(bogiePositions, vehicleCar, hasPitch);
            final double halfWidth = vehicleCar.getWidth() / 2;
            if(!left) {
                left = MTRUtil.canOpenDoors(new Box(-halfWidth - 0.1, -1, -halfLength, -halfWidth, 1, halfLength), positionAndRotation);
            }
            if(!right) {
                right = MTRUtil.canOpenDoors(new Box(halfWidth, -1, -halfLength, halfWidth + 0.1, 1, halfLength), positionAndRotation);
            }
            checkRailProgress += (reversed ? 1 : -1) * vehicleCar.getTotalLength(true, false);
        }
        return doorDirection(left, right);
    }

    /**
     * Samples one bogie of the predicted stop layout as a
     * {@code PositionAndRotation(Vector, Vector, boolean)} — the two joggled
     * points MTR derives per bogie — and appends it to {@code out}.
     *
     * @return false when the rail geometry for the progress window could not
     *         be resolved on the client (caller gives up with "not")
     */
    private boolean addBogiePosition(ObjectArrayList<PositionAndRotation> out, double value, double lowerBound, double upperBound, boolean reversed, boolean hasPitch) {
        final double clampedValue = Utilities.clampSafe(value, lowerBound, upperBound);
        final double clamp = Utilities.clampSafe(Math.min(Math.abs(clampedValue - lowerBound), Math.abs(clampedValue - upperBound)), 0.1, 1);
        final double value1 = Utilities.clampSafe(clampedValue + (reversed ? -clamp : clamp), lowerBound, upperBound - 0.001);
        final double value2 = Utilities.clampSafe(clampedValue - (reversed ? -clamp : clamp), lowerBound, upperBound - 0.001);
        final Vector position1 = positionAt(value1);
        final Vector position2 = positionAt(value2);
        if(position1 == null || position2 == null) return false;
        out.add(new PositionAndRotation(position1, position2, hasPitch));
        return true;
    }

    /** World-space rail point at a whole-path progress; null when unresolvable. */
    private Vector positionAt(double railProgress) {
        final List<PathData> path = getPathData();
        if(path.isEmpty()) return null;
        final PathData pathData = Utilities.getElement(path, Utilities.getIndexFromConditionalList(path, railProgress));
        if(pathData == null) return null;
        final Vehicle.PositionAndTiltAngle positionAndTiltAngle = pathData.getPositionAndTiltAngle(MinecraftClientData.getInstance(), railProgress - pathData.getStartDistance());
        return positionAndTiltAngle == null ? null : positionAndTiltAngle.position();
    }
}
