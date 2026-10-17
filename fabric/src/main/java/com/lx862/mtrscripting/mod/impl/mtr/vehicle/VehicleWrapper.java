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
}
