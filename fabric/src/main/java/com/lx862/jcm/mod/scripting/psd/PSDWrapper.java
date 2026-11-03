package com.lx862.jcm.mod.scripting.psd;

import com.lx862.mtrscripting.core.annotation.ApiInternal;
import com.lx862.mtrscripting.core.annotation.ValueNullable;
import com.lx862.mtrscripting.core.util.ScriptVector3f;
import com.lx862.mtrscripting.mod.impl.mtr.pids.ArrivalWrapper;
import com.lx862.mtrscripting.mod.impl.mtr.vehicle.MagicRouteTypes;
import org.mtr.core.data.Platform;
import org.mtr.core.data.Route;
import org.mtr.core.data.SimplifiedRoute;
import org.mtr.core.data.SimplifiedRoutePlatform;
import org.mtr.core.data.Station;
import org.mtr.core.data.StationExit;
import org.mtr.core.data.TransportMode;
import org.mtr.core.operation.ArrivalResponse;
import org.mtr.core.operation.CarDetails;
import org.mtr.libraries.it.unimi.dsi.fastutil.longs.LongImmutableList;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.mtr.mapping.holder.BlockPos;
import org.mtr.mod.data.ArrivalsCacheClient;
import org.mtr.mod.client.MinecraftClientData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * The script wrapper for a platform screen door glass or top-panel binding —
 * the {@code psd} argument of {@code create(ctx, state, psd)} /
 * {@code render(ctx, state, psd)} in {@code psdScripts} entries.
 *
 * <h2>Drawing</h2>
 * Identical to PIDS scripts: draw with {@code Text} / {@code Texture} /
 * {@code Rect} into the canvas ({@link #width()} × {@link #height()} PIDS
 * pixels), the origin is the top-left corner of the visible surface.
 *
 * <h2>Data</h2>
 * <ul>
 *   <li>{@link #platform()} — the platform this PSD serves (number/name, station);</li>
 *   <li>{@link #routes()} — every route calling at the platform, each with its
 *       full stop list (station name, destination, custom destination,
 *       interchange routes with color and transport type, station exits).
 *       Only marshalled when the entry declares {@code "dataFetchMode": "ALL"}
 *       (or MANDATORY); with SKIP each route carries identity only;</li>
 *   <li>{@link #arrivals()} — upcoming trains with destination, route, car
 *       count, arrival countdown, and the <b>door unit numbers that will
 *       open</b> for that train ({@link PSDArrivalWrapper#doorNumbers()});</li>
 *   <li>{@link #doors()} / {@link #doorValue()} — the wall's door units with
 *       their live open/locked state.</li>
 * </ul>
 */
public class PSDWrapper {

    /** The binding surface of a psdScripts entry ("glass" or "top"). */
    public enum PSDType {
        GLASS("glass"), TOP("top");

        public final String id;
        PSDType(String id) {
            this.id = id;
        }
    }

    /** Same semantics as the vehicle category: SKIP keeps route identity only,
        ALL (and MANDATORY) marshals the full per-route stop lists. */
    public enum DataFetchMode {
        SKIP, MANDATORY, ALL
    }

    public final PSDType type;
    public final int width;
    public final int height;

    private final BlockPos blockPos;
    private final @ValueNullable Platform platform;
    private final PSDWallScanner.WallRun wallRun;
    private final DataFetchMode dataFetchMode;
    private final PSDArrivalsWrapper arrivalsWrapper;
    private final List<PSDRouteWrapper> routes;

    @ApiInternal
    public PSDWrapper(PSDType type, BlockPos blockPos, int width, int height, @ValueNullable Platform platform, PSDWallScanner.WallRun wallRun, DataFetchMode dataFetchMode) {
        this.type = type;
        this.blockPos = blockPos;
        this.width = width;
        this.height = height;
        this.platform = platform;
        this.wallRun = wallRun;
        this.dataFetchMode = dataFetchMode;

        final LongImmutableList targetPlatformIds = platform == null ? new LongImmutableList(new long[0]) : new LongImmutableList(new long[]{platform.getId()});
        final ObjectArrayList<ArrivalResponse> responses = ArrivalsCacheClient.INSTANCE.requestArrivals(targetPlatformIds);
        this.arrivalsWrapper = new PSDArrivalsWrapper(responses, this);
        this.routes = buildRoutes();
    }

    /* ---------------- Own identity ---------------- */

    /** The binding surface: "glass" or "top". */
    public String type() {
        return type.id;
    }

    public boolean isGlass() {
        return type == PSDType.GLASS;
    }

    public boolean isTop() {
        return type == PSDType.TOP;
    }

    /** Canvas width in PIDS pixels. */
    public int width() {
        return width;
    }

    /** Canvas height in PIDS pixels. */
    public int height() {
        return height;
    }

    public ScriptVector3f blockPos() {
        return new ScriptVector3f(blockPos);
    }

    /* ---------------- Platform ---------------- */

    /** The platform this PSD wall serves, or null when none is in range. */
    public @ValueNullable PSDPlatformWrapper platform() {
        return platform == null ? null : new PSDPlatformWrapper(platform);
    }

    /* ---------------- Routes ---------------- */

    /** All routes calling at this PSD's platform (identity always; stops only with dataFetchMode ALL/MANDATORY). */
    public List<PSDRouteWrapper> routes() {
        return routes;
    }

    private List<PSDRouteWrapper> buildRoutes() {
        final List<PSDRouteWrapper> result = new ArrayList<>();
        if (platform == null) return result;
        final boolean includeStops = dataFetchMode == DataFetchMode.ALL || dataFetchMode == DataFetchMode.MANDATORY;
        for (Route route : platform.routes) {
            result.add(new PSDRouteWrapper(route, includeStops ? MinecraftClientData.getInstance().simplifiedRouteIdMap.get(route.getId()) : null));
        }
        return result;
    }

    /* ---------------- Doors ---------------- */

    /** Door units of this wall run, numbered 1..N along the wall. */
    public List<PSDDoorWrapper> doors() {
        final List<PSDDoorWrapper> result = new ArrayList<>(wallRun.doors.size());
        for (PSDWallScanner.DoorUnit unit : wallRun.doors) {
            result.add(new PSDDoorWrapper(unit));
        }
        return result;
    }

    public int doorCount() {
        return wallRun.doors.size();
    }

    public @ValueNullable PSDDoorWrapper door(int number) {
        for (PSDWallScanner.DoorUnit unit : wallRun.doors) {
            if (unit.number == number) return new PSDDoorWrapper(unit);
        }
        return null;
    }

    /** Current door open amount of the wall, 0 (closed) to 1 (fully open). */
    public double doorValue() {
        return wallRun.doorValue;
    }

    /** Whether any door unit of this wall is currently open. */
    public boolean isDoorOpen() {
        return wallRun.doorValue > 0;
    }

    /* ---------------- Arrivals ---------------- */

    public PSDArrivalsWrapper arrivals() {
        return arrivalsWrapper;
    }

    @ApiInternal
    public PSDWallScanner.WallRun wallRun() {
        return wallRun;
    }

    @ApiInternal
    @ValueNullable
    public Platform platformData() {
        return platform;
    }

    /** Raw arrival list for the native snapshot writer. */
    @ApiInternal
    public List<PSDArrivalWrapper> arrivalList() {
        final List<PSDArrivalWrapper> result = new ArrayList<>(arrivalsWrapper.size());
        arrivalsWrapper.forEach(result::add);
        return result;
    }

    /* ================================================================ */

    /** Platform info: number/name and the station it belongs to. */
    public static class PSDPlatformWrapper {
        private final Platform platform;

        @ApiInternal
        public PSDPlatformWrapper(Platform platform) {
            this.platform = platform;
        }

        public long id() {
            return platform.getId();
        }

        /** The platform number/name (e.g. "1") — the same string PIDS scripts see as {@code arrival.platformName()}. */
        public String name() {
            return platform.getName();
        }

        /** Alias of {@link #name()} — the platform number ("1", "2", ...). */
        public String number() {
            return platform.getName();
        }

        public String stationName() {
            return platform.getStationName();
        }

        public @ValueNullable Station station() {
            return platform.area == null ? null : platform.area;
        }

        /** Dwell time at this platform, in milliseconds. */
        public long dwellTimeMillis() {
            return platform.getDwellTime();
        }
    }

    /** One route calling at the platform, with its full stop list (dataFetchMode ALL/MANDATORY). */
    public static class PSDRouteWrapper {
        public final Route route;
        private final @ValueNullable SimplifiedRoute simplifiedRoute;

        @ApiInternal
        public PSDRouteWrapper(Route route, @ValueNullable SimplifiedRoute simplifiedRoute) {
            this.route = route;
            this.simplifiedRoute = simplifiedRoute;
        }

        public long id() {
            return route.getId();
        }

        public String name() {
            return route.getName();
        }

        public String routeNumber() {
            return route.getRouteNumber();
        }

        public int color() {
            return route.getColor();
        }

        public Route.CircularState circularState() {
            return route.getCircularState();
        }

        public TransportMode transportMode() {
            return route.getTransportMode();
        }

        /** Canonical transport type id ("train", "train_metro", ...) — same ids as vehicle scripts. */
        public String getTransportType() {
            return MagicRouteTypes.extendedType(route.getId(), route.getTransportMode(), route.getRouteType());
        }

        /** The route's stops in order. Empty unless the entry uses dataFetchMode ALL/MANDATORY.
            Memoized per route id (see PSDRouteStopCache) — the render path asks
            for the same graphs every frame and building them walks the client's
            per-station interchange maps. */
        public List<PSDStopWrapper> stops() {
            return PSDRouteStopCache.stopsOf(route, simplifiedRoute);
        }
    }

    /** One stop of a route: station info with interchange routes and exits. */
    public static class PSDStopWrapper {
        public final String name;
        public final String destinationName;
        private final @ValueNullable String customDestination;
        private final List<PSDInterchangeWrapper> interchanges = new ArrayList<>();
        private final Map<String, List<PSDInterchangeWrapper>> connectingInterchanges = new HashMap<>();
        private final List<PSDExitWrapper> exits = new ArrayList<>();
        private final @ValueNullable Station station;
        private final @ValueNullable Platform platform;

        @ApiInternal
        public PSDStopWrapper(Route route, SimplifiedRoutePlatform routePlatform, String terminusDestination) {
            this.name = routePlatform.getStationName();
            this.destinationName = routePlatform.getDestination();
            this.customDestination = destinationName != null && !destinationName.isEmpty() && !destinationName.equals(terminusDestination) ? destinationName : null;
            this.station = MinecraftClientData.getInstance().stationIdMap.get(routePlatform.getStationId());
            this.platform = MinecraftClientData.getInstance().platformIdMap.get(routePlatform.getPlatformId());

            if (station != null) {
                // Interchanges within the same station (other routes at any of its platforms).
                station.getInterchangeStationToColorToRoutesMap(true).forEach((interchangeStation, colorToRoutes) -> {
                    if (interchangeStation.getId() == station.getId()) {
                        colorToRoutes.forEach((color, routes) -> routes.forEach(interchangeRoute -> {
                            if (interchangeRoute.getId() != route.getId()) {
                                interchanges.add(new PSDInterchangeWrapper(
                                        interchangeRoute.getId(), color, interchangeRoute.getName(),
                                        interchangeRoute.getTransportMode(),
                                        MagicRouteTypes.extendedType(interchangeRoute.getId(), interchangeRoute.getTransportMode(), interchangeRoute.getRouteType())));
                            }
                        }));
                    }
                });
                // Routes of connecting (walkable) interchange stations.
                station.getInterchangeStationToColorToRoutesMap(true).forEach((interchangeStation, colorToRoutes) -> {
                    if (interchangeStation.getId() != station.getId()) {
                        colorToRoutes.forEach((color, routes) -> routes.forEach(interchangeRoute -> {
                            connectingInterchanges.computeIfAbsent(interchangeStation.getName(), k -> new ArrayList<>()).add(new PSDInterchangeWrapper(
                                    interchangeRoute.getId(), color, interchangeRoute.getName(),
                                    interchangeRoute.getTransportMode(),
                                    MagicRouteTypes.extendedType(interchangeRoute.getId(), interchangeRoute.getTransportMode(), interchangeRoute.getRouteType())));
                        }));
                    }
                });
                for (StationExit exit : station.getExits()) {
                    exits.add(new PSDExitWrapper(exit.getName(), new ArrayList<>(exit.getDestinations())));
                }
            }
            if (platform != null) {
                platform.routes.forEach(platformRoute -> {
                    if (platformRoute.getId() != route.getId() && (station == null || platformRoute.getColor() != route.getColor() || !platformRoute.getName().equals(route.getName()))) {
                        final boolean alreadyListed = interchanges.stream().anyMatch(e -> e.routeId() == platformRoute.getId());
                        if (!alreadyListed) {
                            interchanges.add(new PSDInterchangeWrapper(
                                    platformRoute.getId(), platformRoute.getColor(), platformRoute.getName(),
                                    platformRoute.getTransportMode(),
                                    MagicRouteTypes.extendedType(platformRoute.getId(), platformRoute.getTransportMode(), platformRoute.getRouteType())));
                        }
                    }
                });
            }
        }

        public String name() {
            return name;
        }

        /** This stop's custom destination on the route, or null when it uses the default. */
        public @ValueNullable String customDestination() {
            return customDestination;
        }

        /** The destination shown for this stop on this route (custom destination or the route terminus). */
        public String destination() {
            return destinationName == null || destinationName.isEmpty() ? name : destinationName;
        }

        public @ValueNullable Station station() {
            return station;
        }

        public @ValueNullable Platform platform() {
            return platform;
        }

        /** Interchange routes at this station (excluding the stop's own route). */
        public List<PSDInterchangeWrapper> interchanges() {
            return interchanges;
        }

        /** Interchange routes of connected stations, keyed by the connecting station name. */
        public Map<String, List<PSDInterchangeWrapper>> connectingInterchanges() {
            return connectingInterchanges;
        }

        /** Station exits (name/letter plus destination landmarks). */
        public List<PSDExitWrapper> exits() {
            return exits;
        }
    }

    /** One interchange route of a stop (color, name, transport type). */
    public static class PSDInterchangeWrapper {
        private final long routeId;
        private final int color;
        private final String name;
        private final TransportMode transportMode;
        private final String transportType;

        @ApiInternal
        public PSDInterchangeWrapper(long routeId, int color, String name, TransportMode transportMode, String transportType) {
            this.routeId = routeId;
            this.color = color;
            this.name = name;
            this.transportMode = transportMode;
            this.transportType = transportType;
        }

        public long routeId() {
            return routeId;
        }

        public int color() {
            return color;
        }

        public String name() {
            return name;
        }

        public TransportMode transportMode() {
            return transportMode;
        }

        public String getTransportType() {
            return transportType;
        }
    }

    /** One station exit: name/letter (e.g. "A") plus destination landmarks. */
    public static class PSDExitWrapper {
        public final String name;
        public final List<String> destinations;

        @ApiInternal
        public PSDExitWrapper(String name, List<String> destinations) {
            this.name = name;
            this.destinations = destinations;
        }

        public String name() {
            return name;
        }

        public List<String> destinations() {
            return destinations;
        }
    }

    /** One door unit of the wall, with its live state. */
    public static class PSDDoorWrapper {
        private final PSDWallScanner.DoorUnit unit;

        @ApiInternal
        public PSDDoorWrapper(PSDWallScanner.DoorUnit unit) {
            this.unit = unit;
        }

        /** 1-based door number along the wall. */
        public int number() {
            return unit.number;
        }

        public boolean isOpen() {
            return unit.open;
        }

        public boolean isLocked() {
            return unit.locked;
        }

        public ScriptVector3f pos() {
            return new ScriptVector3f(unit.pos);
        }
    }

    /* ================================================================ */

    /** Upcoming arrivals at this PSD's platform. */
    public static class PSDArrivalsWrapper {
        private final ObjectArrayList<PSDArrivalWrapper> arrivals;

        @ApiInternal
        public PSDArrivalsWrapper(ObjectArrayList<ArrivalResponse> arrivalsResponse, PSDWrapper psd) {
            this.arrivals = new ObjectArrayList<>();
            for (ArrivalResponse arrivalResponse : arrivalsResponse) {
                this.arrivals.add(new PSDArrivalWrapper(arrivalResponse, psd));
            }
        }

        public int size() {
            return arrivals.size();
        }

        public @ValueNullable PSDArrivalWrapper get(int i) {
            return i >= arrivals.size() ? null : arrivals.get(i);
        }

        public void forEach(Consumer<PSDArrivalWrapper> consumer) {
            for (PSDArrivalWrapper arrival : arrivals) {
                consumer.accept(arrival);
            }
        }

        public boolean mixedCarLength() {
            int car = -1;
            for (PSDArrivalWrapper arrival : arrivals) {
                if (car == -1) car = arrival.carCount();
                if (car != arrival.carCount()) return true;
            }
            return false;
        }
    }

    /**
     * One upcoming train: everything PIDS arrivals expose (destination, route,
     * car count, countdown, realtime, terminating, circular state) plus the
     * PSD-specific door information.
     */
    public static class PSDArrivalWrapper extends ArrivalWrapper {
        private final PSDWrapper psd;
        private final List<Integer> doorNumbers;

        @ApiInternal
        public PSDArrivalWrapper(ArrivalResponse arrivalResponse, PSDWrapper psd) {
            super(arrivalResponse);
            this.psd = psd;

            final Platform platform = psd.platformData();
            Route route = null;
            if (platform != null) {
                for (Route platformRoute : platform.routes) {
                    if (platformRoute.getId() == routeId()) {
                        route = platformRoute;
                        break;
                    }
                }
            }
            this.doorNumbers = PSDWallScanner.predictDoorNumbers(
                    psd.wallRun(), arrived(), cars(), carCount(), platform, route, currentlyOpenDoorNumbers());
        }

        /** The door unit numbers (1-based, see {@link PSDWrapper#doors()}) that will open for this train. */
        public List<Integer> doorNumbers() {
            return doorNumbers;
        }

        /**
         * Whether this arrival's doors are open right now — true during the
         * dwell while any door unit of the wall reports an open value.
         */
        public boolean isDoorOpen() {
            return psd.isDoorOpen();
        }

        /** Remaining milliseconds until arrival (negative once arrived). */
        public long arrivalCountdown() {
            return arrivalTime() - System.currentTimeMillis();
        }

        private List<Integer> currentlyOpenDoorNumbers() {
            final List<Integer> open = new ArrayList<>();
            for (PSDWallScanner.DoorUnit unit : psd.wallRun().doors) {
                if (unit.open) open.add(unit.number);
            }
            return open;
        }
    }
}
