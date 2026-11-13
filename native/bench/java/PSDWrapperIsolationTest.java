package com.lx862.jcm.test;

import com.lx862.jcm.mod.scripting.psd.*;
import com.lx862.jcm.mod.scripting.psd.PSDWrapper.*;
import com.lx862.mtrscripting.mod.impl.mtr.pids.*;
import org.mtr.core.data.*;
import org.mtr.core.operation.ArrivalResponse;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.mtr.mapping.holder.BlockPos;
import org.mtr.mod.client.MinecraftClientData;
import org.mtr.mod.data.ArrivalsCacheClient;
import java.util.*;

public final class PSDWrapperIsolationTest {
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static void checkStation(PSDStationWrapper station) {
        check(station.exits().size() == 1 && station.exits().get(0).destinations().equals(Arrays.asList("South", "North")), "station exit list leaked between wrappers");
        check(station.connectingInterchanges().get("Remote").size() == 1, "nested connecting route list leaked");
        check(station.interchanges().size() == 2, "station interchange list leaked");
        check(station.connectedStations().equals(Collections.singletonList("Remote")), "connected station names leaked");
        check(station.platforms().equals(Arrays.asList("2", "10")), "platform list lost numeric ordering or was exposed by cache");
    }

    private static void checkRoute(PSDRouteWrapper route) {
        check(route.stops().size() == 1, "route stop list leaked between wrappers");
        final PSDStopWrapper stop = route.stops().get(0);
        check(stop.exits().get(0).destinations().equals(Arrays.asList("South", "North")), "route cached exit destinations were mutated");
        check(stop.connectingInterchanges().get("Remote").size() == 1, "route cached nested interchange list was mutated");
        check(stop.interchanges().size() == 1, "route interchange list leaked between wrappers");
    }

    public static void main(String[] args) {
        PSDStationWrapper.resetCache();
        PSDRouteStopCache.reset();
        final Station station = new Station(1, "Central");
        final Station remote = new Station(2, "Remote");
        final Route own = new Route(10, "Line 1"), other = new Route(20, "Line 2");
        station.interchanges.put(station, Collections.singletonMap(1, Arrays.asList(own, other)));
        station.interchanges.put(remote, Collections.singletonMap(2, Collections.singletonList(other)));
        station.exits.add(new StationExit("A", Arrays.asList("South", "North")));
        station.connectedStations.add(remote);
        final Platform platform = new Platform(100, "10", station);
        platform.routes.add(own);
        station.savedRails.add(platform);
        station.savedRails.add(new Platform(101, "2", station));
        final MinecraftClientData data = MinecraftClientData.getInstance();
        data.stationIdMap.put(1L, station);
        data.platformIdMap.put(100L, platform);
        final SimplifiedRoute simplified = new SimplifiedRoute();
        simplified.platforms.add(new SimplifiedRoutePlatform(1, 100, "Central", "Terminus"));
        data.simplifiedRouteIdMap.put(own.getId(), simplified);
        data.routeIdMap.put(own.getId(), own);

        final PSDStationWrapper firstStation = PSDStationWrapper.of(station), secondStation = PSDStationWrapper.of(station);
        firstStation.exits().get(0).destinations().sort(Comparator.naturalOrder());
        firstStation.exits().get(0).destinations().clear();
        firstStation.exits().clear();
        firstStation.connectingInterchanges().get("Remote").clear();
        firstStation.connectingInterchanges().clear();
        firstStation.interchanges().clear();
        firstStation.connectedStations().clear();
        firstStation.platforms().clear();
        checkStation(secondStation);
        checkStation(PSDStationWrapper.of(station));

        final PSDRouteWrapper firstRoute = new PSDRouteWrapper(own, simplified), secondRoute = new PSDRouteWrapper(own, simplified);
        firstRoute.stops().get(0).exits().get(0).destinations().sort(Comparator.naturalOrder());
        firstRoute.stops().get(0).exits().get(0).destinations().clear();
        firstRoute.stops().get(0).exits().clear();
        firstRoute.stops().get(0).connectingInterchanges().get("Remote").clear();
        firstRoute.stops().get(0).connectingInterchanges().clear();
        firstRoute.stops().get(0).interchanges().clear();
        firstRoute.stops().clear();
        checkRoute(secondRoute);
        checkRoute(new PSDRouteWrapper(own, simplified));

        final long now = System.currentTimeMillis();
        final ObjectArrayList<ArrivalResponse> arrivals = ArrivalsCacheClient.INSTANCE.responses;
        arrivals.add(new ArrivalResponse(now + 60_000, now + 120_000, own.getId()));
        arrivals.add(new ArrivalResponse(now - 60_000, now + 60_000, own.getId()));
        arrivals.add(new ArrivalResponse(now - 120_000, now - 60_000, own.getId()));
        final PSDWallScanner.WallRun run = new PSDWallScanner.WallRun();
        run.doorValue = 1;
        final PSDWrapper psd = new PSDWrapper(PSDType.GLASS, new BlockPos(0, 0, 0), 128, 64, platform, run, DataFetchMode.ALL);
        check(psd.arrivals().get(-1) == null && psd.arrivals().get(3) == null, "PSD arrival boundary must return null");
        check(!psd.arrivals().get(0).isDoorOpen(), "future train must not inherit the dwelling train's open doors");
        check(psd.arrivals().get(1).isDoorOpen(), "dwelling train should expose actual open doors");
        check(!psd.arrivals().get(2).isDoorOpen(), "departed train must not inherit another train's open doors");
        final ArrivalsWrapper pids = new ArrivalsWrapper(arrivals);
        check(pids.get(-1) == null && pids.get(3) == null, "PIDS arrival boundary must return null");
        check(pids.get(0).route() == simplified, "arrival route must use the simplified route map");
        System.out.println("PSD wrapper regression: station/route deep-copy isolation, nested list mutation, arrival bounds and train-specific door state passed");
    }
}
