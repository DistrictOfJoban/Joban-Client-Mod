package com.lx862.jcm.nativeapi;

import com.lx862.jcm.mod.scripting.pids.PIDSWrapper;
import com.lx862.mtrscripting.mod.impl.mtr.pids.ArrivalWrapper;
import com.lx862.mtrscripting.mod.impl.mtr.eyecandy.EyecandyBlockEntityWrapper;
import com.lx862.mtrscripting.mod.impl.mtr.lift.LiftWrapper;
import com.lx862.mtrscripting.core.util.ScriptVector3f;
import org.mtr.mapping.holder.MinecraftClient;

import java.nio.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** POD snapshots for all non-vehicle script kinds; offsets are blob-relative. */
public final class NativeContentSnapshot {
    private final ByteBuffer buffer;
    private final Map<String, int[]> strings = new HashMap<>();
    private NativeContentSnapshot(ByteBuffer buffer, int header) {
        this.buffer = buffer;
        buffer.clear().order(ByteOrder.nativeOrder());
        for (int i = 0; i < header; i++) buffer.put((byte)0);
    }
    private int reserve(int size, int alignment) {
        int pos = (buffer.position() + alignment - 1) & -alignment;
        if (size < 0 || (long)pos + size > buffer.capacity()) throw new BufferOverflowException();
        buffer.position(pos + size);
        return pos;
    }
    private void string(int at, String text) {
        if (text == null) { buffer.putInt(at, -1); buffer.putInt(at + 4, 0); return; }
        int[] ref = strings.get(text);
        if (ref == null) {
            byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
            ref = new int[]{buffer.position(), bytes.length};
            buffer.put(bytes); strings.put(text, ref);
        }
        buffer.putInt(at, ref[0]); buffer.putInt(at + 4, ref[1]);
    }
    private static long day() {
        return MinecraftClient.getInstance().getWorldMapped() == null ? 0 : com.lx862.mtrscripting.core.integration.MinecraftClientWrapper.worldDayTime();
    }

    /** ABI 15: the client world's weather state, the same answer the JS
     *  wrappers' getWeather() returns — 0 clear, 1 rain, 2 thunder.
     *  Always 0 ("clear") when no client world is loaded, matching the
     *  former zero padding byte for byte. */
    static byte weatherCode() {
        if (com.lx862.mtrscripting.core.integration.MinecraftClientWrapper.worldIsThundering()) return 2;
        if (com.lx862.mtrscripting.core.integration.MinecraftClientWrapper.worldIsRaining()) return 1;
        return 0;
    }

    /** Client {@code SimplifiedRoute} by id — the same map
     *  {@code PSDWrapper.routes()} reads, and the object each route's stop
     *  list was built from, so the FangSu status lookup (ABI 14) matches the
     *  exact platforms the stop wrappers came from. Null when not synced. */
    private static org.mtr.core.data.SimplifiedRoute simplifiedOf(long routeId) {
        return org.mtr.mod.client.MinecraftClientData.getInstance().simplifiedRouteIdMap.get(routeId);
    }

    /** ABI 16: the FangSu service flag byte for one arrival (route id +
     *  platform id pair). Express needs the platform's ServiceStop record
     *  on the client-side simplified route; hidden-route needs FangSu's
     *  supplementary cache. Both bits 0 without FangSu. */
    static byte arrivalFlags(long routeId, long platformId) {
        byte flags = 0;
        final org.mtr.core.data.SimplifiedRoute route = simplifiedOf(routeId);
        if (FangSuServiceInfo.isExpressService(route, platformId)) flags |= FangSuServiceInfo.FLAG_EXPRESS;
        if (FangSuServiceInfo.isHiddenRoute(routeId)) flags |= FangSuServiceInfo.FLAG_HIDDEN_ROUTE;
        return flags;
    }

    /** ABI 16: the FangSu service flag byte for one PSD route strip. */
    private static byte routeFlags(long routeId, long platformId) {
        return arrivalFlags(routeId, platformId);
    }

    private ByteBuffer finish() { buffer.flip(); return buffer; }

    public static ByteBuffer pids(ByteBuffer buffer, PIDSWrapper pids) {
        NativeContentSnapshot out = new NativeContentSnapshot(buffer,104);
        ScriptVector3f pos = pids.blockPos();
        buffer.putLong(0,(long)pos.x()); buffer.putLong(8,(long)pos.y()); buffer.putLong(16,(long)pos.z());
        buffer.putLong(24,pids.station() == null ? 0 : pids.station().getId());
        buffer.putLong(32,System.currentTimeMillis()); buffer.putLong(40,day());
        buffer.putInt(48,pids.width); buffer.putInt(52,pids.height); buffer.putInt(56,pids.rows);
        buffer.put(61,(byte)(pids.isKeyBlock()?1:0)); buffer.put(62,(byte)(pids.isPlatformNumberHidden()?1:0));
        /* ABI 15: the weather byte — the header's former _pad0 (offset 63). */
        buffer.put(63,weatherCode());
        int rows=0; for (int i=0;i<Math.min(pids.rows,32);i++) if(pids.isRowHidden(i)) rows |= 1 << i;
        buffer.putInt(72,rows);
        List<ArrivalWrapper> arrivals = new ArrayList<>(); pids.arrivals().forEach(arrivals::add);
        int arrivalOffset=out.reserve(arrivals.size()*96,8);
        buffer.putInt(64,arrivals.size()); buffer.putInt(68,arrivalOffset);
        int messages=out.reserve(pids.rows*8,4);
        buffer.putInt(76,pids.rows); buffer.putInt(80,messages);
        int platforms=out.reserve(pids.getTargetPlatformIds().size()*8,8);
        buffer.putInt(84,pids.getTargetPlatformIds().size()); buffer.putInt(88,platforms);
        for(int i=0;i<pids.getTargetPlatformIds().size();i++) buffer.putLong(platforms+i*8,pids.getTargetPlatformIds().getLong(i));
        int pool=buffer.position(); buffer.putInt(92,pool);
        for (int i=0;i<arrivals.size();i++) {
            ArrivalWrapper arrival=arrivals.get(i); int at=arrivalOffset+i*96;
            buffer.putLong(at,arrival.arrivalTime()); buffer.putLong(at+8,arrival.departureTime()); buffer.putLong(at+16,arrival.deviation());
            buffer.putLong(at+24,arrival.departureIndex()); buffer.putLong(at+32,arrival.routeId()); buffer.putLong(at+40,arrival.platformId());
            buffer.putInt(at+48,arrival.routeColor()); buffer.putInt(at+52,arrival.carCount());
            buffer.put(at+56,(byte)(arrival.realtime()?1:0)); buffer.put(at+57,(byte)(arrival.terminating()?1:0));
            buffer.put(at+58,(byte)arrival.circularState().ordinal());
            /* ABI 16: FangSu service flags (former padding at offset 59) —
               express / hidden-route. Both bits 0 without FangSu. */
            buffer.put(at+59,arrivalFlags(arrival.routeId(),arrival.platformId()));
            out.string(at+60,arrival.routeNumber()); out.string(at+68,arrival.routeName());
            out.string(at+76,arrival.destination()); out.string(at+84,arrival.platformName());
        }
        for(int i=0;i<pids.rows;i++) out.string(messages+i*8,pids.getCustomMessage(i));
        buffer.putInt(96,buffer.position()-pool);
        return out.finish();
    }

    /** JcmPsdSnapshot (ABI 12) — layout pinned by the static_asserts in
     *  mtr_native.h. Header 128 (136 since ABI 15, see below), JcmPsdDoor 32,
     *  JcmPsdArrival 104 (JcmArrival + door refs), JcmPsdRoute 48,
     *  stops/interchanges/exits reuse the vehicle JcmStop layout. ABI 13
     *  adds the station pools: JcmPsdStation 96 at snapshot offset 124
     *  (formerly padding), JcmPsdStationInterchange 32,
     *  JcmPsdStationConnectingGroup 16.
     *  ABI 14 fills the JcmStop padding bytes 82/83 with the FangSu
     *  暂缓开通 / RouteStopStatus data (FangSuStopStatus) — unlike the
     *  vehicle path, PSD route-strip stops are NOT filtered by FangSu, so
     *  these carry live values and are the primary consumer of the API.
     *  ABI 15 grows the header 128 -> 136 and puts the weather byte at
     *  offset 128 — the struct had no spare byte left. Every pool the
     *  writer places after the header moves with it, but all pool offsets
     *  are blob-relative and host-computed, so ABI 12-14 modules (which
     *  read only offsets < 128) are unaffected. */
    public static ByteBuffer psd(ByteBuffer buffer, com.lx862.jcm.mod.scripting.psd.PSDWrapper psd) {
        final com.lx862.jcm.mod.scripting.psd.PSDWallScanner.WallRun wallRun = psd.wallRun();
        final List<com.lx862.jcm.mod.scripting.psd.PSDWrapper.PSDDoorWrapper> doors = psd.doors();
        final List<com.lx862.jcm.mod.scripting.psd.PSDWrapper.PSDArrivalWrapper> arrivals = psd.arrivalList();
        final List<com.lx862.jcm.mod.scripting.psd.PSDWrapper.PSDRouteWrapper> routes = psd.routes();
        final com.lx862.jcm.mod.scripting.psd.PSDWrapper.PSDStationWrapper station = psd.stop();
        final List<List<com.lx862.jcm.mod.scripting.psd.PSDWrapper.PSDStopWrapper>> routeStops = new ArrayList<>(routes.size());
        for (com.lx862.jcm.mod.scripting.psd.PSDWrapper.PSDRouteWrapper route : routes) {
            routeStops.add(route.stops());
        }

        final NativeContentSnapshot out = new NativeContentSnapshot(buffer, 136);
        final org.mtr.core.data.Platform platform = psd.platformData();

        /* Header (absolute puts; pools are reserved below). */
        buffer.putLong(0,(long)psd.blockPos().x()); buffer.putLong(8,(long)psd.blockPos().y()); buffer.putLong(16,(long)psd.blockPos().z());
        buffer.putLong(24, platform == null ? 0 : platform.getId());
        buffer.putLong(32, platform == null || platform.area == null ? 0 : platform.area.getId());
        buffer.putLong(40, System.currentTimeMillis());
        buffer.putLong(48, day());
        buffer.putDouble(56, wallRun.doorValue);
        buffer.putInt(64, psd.type == com.lx862.jcm.mod.scripting.psd.PSDWrapper.PSDType.TOP ? 1 : 0);
        buffer.putInt(68, psd.width); buffer.putInt(72, psd.height);
        /* ABI 15: the weather byte at offset 128 — the struct's only free
           slot required growing the header 128 -> 136 (see the method
           javadoc above for the ABI 12-14 compatibility argument). */
        buffer.put(128, weatherCode());

        /* ---- reserve every struct pool (all offsets blob-relative) ---- */
        final int doorOffset = out.reserve(doors.size() * 32, 8);
        buffer.putInt(76, doors.size()); buffer.putInt(80, doorOffset);

        final int arrivalOffset = out.reserve(arrivals.size() * 104, 8);
        buffer.putInt(84, arrivals.size()); buffer.putInt(88, arrivalOffset);
        final int[] arrivalDoorOffsets = new int[arrivals.size()];
        for (int i = 0; i < arrivals.size(); i++) {
            final List<Integer> numbers = arrivals.get(i).doorNumbers();
            arrivalDoorOffsets[i] = numbers.isEmpty() ? 0 : out.reserve(numbers.size() * 4, 4);
        }

        final int routeOffset = out.reserve(routes.size() * 48, 8);
        buffer.putInt(92, routes.size()); buffer.putInt(96, routeOffset);
        final int[] stopOffsets = new int[routes.size()];
        final List<List<Integer>> interchangeOffsetsPerRoute = new ArrayList<>(routes.size());
        final List<List<Integer>> exitOffsetsPerRoute = new ArrayList<>(routes.size());
        for (int r = 0; r < routes.size(); r++) {
            final List<com.lx862.jcm.mod.scripting.psd.PSDWrapper.PSDStopWrapper> stops = routeStops.get(r);
            if (stops.isEmpty()) {
                stopOffsets[r] = 0;
            } else {
                stopOffsets[r] = out.reserve(stops.size() * 88, 8);
            }
            final List<Integer> icOffsets = new ArrayList<>();
            final List<Integer> exitOffsets = new ArrayList<>();
            for (com.lx862.jcm.mod.scripting.psd.PSDWrapper.PSDStopWrapper stop : stops) {
                if (stop.interchanges().isEmpty()) {
                    icOffsets.add(0);
                } else {
                    icOffsets.add(out.reserve(stop.interchanges().size() * 24, 4));
                }
                if (stop.exits().isEmpty()) {
                    exitOffsets.add(0);
                } else {
                    exitOffsets.add(out.reserve(stop.exits().size() * 16, 4));
                }
            }
            interchangeOffsetsPerRoute.add(icOffsets);
            exitOffsetsPerRoute.add(exitOffsets);
        }

        /* ---- station pools (ABI 13; -1 at offset 124 when absent) ---- */
        final List<com.lx862.jcm.mod.scripting.psd.PSDWrapper.PSDInterchangeWrapper> stationInterchanges;
        final List<Map.Entry<String, List<com.lx862.jcm.mod.scripting.psd.PSDWrapper.PSDInterchangeWrapper>>> connectingGroups;
        final List<Integer> connectingRouteOffsets;
        final int stationOffset, stationInterchangeOffset, connectingOffset, connectedStationOffset, stationExitOffset, stationPlatformOffset;
        final List<com.lx862.jcm.mod.scripting.psd.PSDWrapper.PSDExitWrapper> stationExits;
        final List<Integer> stationExitDestinationOffsets;
        if (station == null) {
            stationInterchanges = null;
            connectingGroups = null;
            connectingRouteOffsets = null;
            stationExits = null;
            stationExitDestinationOffsets = null;
            stationOffset = -1;
            stationInterchangeOffset = 0;
            connectingOffset = 0;
            connectedStationOffset = 0;
            stationExitOffset = 0;
            stationPlatformOffset = 0;
            buffer.putInt(124, -1);
        } else {
            stationInterchanges = station.interchanges();
            connectingGroups = new ArrayList<>(station.connectingInterchanges().entrySet());
            connectingRouteOffsets = new ArrayList<>(connectingGroups.size());
            for (final Map.Entry<String, List<com.lx862.jcm.mod.scripting.psd.PSDWrapper.PSDInterchangeWrapper>> group : connectingGroups) {
                connectingRouteOffsets.add(group.getValue().isEmpty() ? 0 : out.reserve(group.getValue().size() * 32, 8));
            }
            stationExits = station.exits();
            stationExitDestinationOffsets = new ArrayList<>(stationExits.size());
            for (final com.lx862.jcm.mod.scripting.psd.PSDWrapper.PSDExitWrapper exit : stationExits) {
                stationExitDestinationOffsets.add(exit.destinations().isEmpty() ? 0 : out.reserve(exit.destinations().size() * 8, 4));
            }
            stationOffset = out.reserve(96, 8);
            buffer.putInt(124, stationOffset);
            stationInterchangeOffset = stationInterchanges.isEmpty() ? 0 : out.reserve(stationInterchanges.size() * 32, 8);
            connectingOffset = connectingGroups.isEmpty() ? 0 : out.reserve(connectingGroups.size() * 16, 4);
            connectedStationOffset = station.connectedStations().isEmpty() ? 0 : out.reserve(station.connectedStations().size() * 8, 4);
            stationExitOffset = stationExits.isEmpty() ? 0 : out.reserve(stationExits.size() * 16, 4);
            stationPlatformOffset = station.platforms().isEmpty() ? 0 : out.reserve(station.platforms().size() * 8, 4);
        }

        /* ---- string pool starts here ---- */
        final int pool = buffer.position();
        buffer.putInt(116, pool);
        out.string(100, platform == null ? "" : platform.getName());
        out.string(108, platform == null ? "" : platform.getStationName());

        /* doors */
        for (int i = 0; i < doors.size(); i++) {
            final com.lx862.jcm.mod.scripting.psd.PSDWrapper.PSDDoorWrapper door = doors.get(i);
            final int p = doorOffset + i * 32;
            final org.mtr.mapping.holder.BlockPos pos = door.pos().rawBlockPos();
            buffer.putLong(p, pos.getX()); buffer.putLong(p + 8, pos.getY()); buffer.putLong(p + 16, pos.getZ());
            buffer.putInt(p + 24, door.number());
            buffer.put(p + 28, (byte) (door.isOpen() ? 1 : 0));
            buffer.put(p + 29, (byte) (door.isLocked() ? 1 : 0));
            buffer.put(p + 30, (byte) 0); buffer.put(p + 31, (byte) 0);
        }

        /* arrivals: the 96-byte JcmArrival part mirrors pids(), plus doors */
        for (int i = 0; i < arrivals.size(); i++) {
            final com.lx862.jcm.mod.scripting.psd.PSDWrapper.PSDArrivalWrapper arrival = arrivals.get(i);
            final int p = arrivalOffset + i * 104;
            buffer.putLong(p, arrival.arrivalTime()); buffer.putLong(p + 8, arrival.departureTime()); buffer.putLong(p + 16, arrival.deviation());
            buffer.putLong(p + 24, arrival.departureIndex()); buffer.putLong(p + 32, arrival.routeId()); buffer.putLong(p + 40, arrival.platformId());
            buffer.putInt(p + 48, arrival.routeColor()); buffer.putInt(p + 52, arrival.carCount());
            buffer.put(p + 56, (byte) (arrival.realtime() ? 1 : 0)); buffer.put(p + 57, (byte) (arrival.terminating() ? 1 : 0));
            buffer.put(p + 58, (byte) arrival.circularState().ordinal());
            /* ABI 16: FangSu service flags (former padding at offset 59). */
            buffer.put(p + 59, arrivalFlags(arrival.routeId(), arrival.platformId()));
            out.string(p + 60, arrival.routeNumber()); out.string(p + 68, arrival.routeName());
            out.string(p + 76, arrival.destination()); out.string(p + 84, arrival.platformName());
            final List<Integer> numbers = arrival.doorNumbers();
            buffer.putInt(p + 96, numbers.size());
            buffer.putInt(p + 100, numbers.isEmpty() ? 0 : arrivalDoorOffsets[i]);
            for (int j = 0; j < numbers.size(); j++) {
                buffer.putInt(arrivalDoorOffsets[i] + j * 4, numbers.get(j));
            }
        }

        /* routes + stops */
        for (int r = 0; r < routes.size(); r++) {
            final com.lx862.jcm.mod.scripting.psd.PSDWrapper.PSDRouteWrapper route = routes.get(r);
            final int p = routeOffset + r * 48;
            buffer.putLong(p, route.id());
            buffer.putInt(p + 8, route.color() | 0xFF000000);
            buffer.put(p + 12, (byte) route.circularState().ordinal());
            /* ABI 16: FangSu service flags for this route at the PSD's
               platform (former padding at offset 13) — express (the route
               runs a ServiceStop here) / hidden route. */
            buffer.put(p + 13, routeFlags(route.id(), platform == null ? 0 : platform.getId()));
            buffer.put(p + 14, (byte) 0); buffer.put(p + 15, (byte) 0);
            out.string(p + 16, route.name());
            out.string(p + 24, route.routeNumber());
            out.string(p + 32, route.getTransportType());
            final List<com.lx862.jcm.mod.scripting.psd.PSDWrapper.PSDStopWrapper> stops = routeStops.get(r);
            buffer.putInt(p + 40, stops.size());
            buffer.putInt(p + 44, stops.isEmpty() ? 0 : stopOffsets[r]);

            for (int s = 0; s < stops.size(); s++) {
                final com.lx862.jcm.mod.scripting.psd.PSDWrapper.PSDStopWrapper stop = stops.get(s);
                final int q = stopOffsets[r] + s * 88;
                final org.mtr.core.data.Station stopStation = stop.station();
                final org.mtr.core.data.Platform stopPlatform = stop.platform();
                buffer.putLong(q, route.id());
                buffer.putLong(q + 8, stopStation == null ? 0 : stopStation.getId());
                buffer.putLong(q + 16, stopPlatform == null ? 0 : stopPlatform.getId());
                buffer.putDouble(q + 24, -1);
                buffer.putDouble(q + 32, stopPlatform == null ? -1 : stopPlatform.getDwellTime());
                out.string(q + 40, stop.name());
                out.string(q + 48, stop.destinationName == null ? "" : stop.destinationName);
                final String custom = stop.customDestination();
                if (custom == null) {
                    buffer.putInt(q + 56, -1); buffer.putInt(q + 60, 0);
                } else {
                    out.string(q + 56, custom);
                }
                final List<com.lx862.jcm.mod.scripting.psd.PSDWrapper.PSDInterchangeWrapper> interchanges = stop.interchanges();
                buffer.putInt(q + 64, interchanges.size());
                buffer.putInt(q + 68, interchanges.isEmpty() ? 0 : interchangeOffsetsPerRoute.get(r).get(s));
                final List<com.lx862.jcm.mod.scripting.psd.PSDWrapper.PSDExitWrapper> exits = stop.exits();
                buffer.putInt(q + 72, exits.size());
                buffer.putInt(q + 76, exits.isEmpty() ? 0 : exitOffsetsPerRoute.get(r).get(s));
                buffer.put(q + 80, (byte) route.circularState().ordinal());
                buffer.put(q + 81, (byte) 0);
                /* ABI 14: FangSu 暂缓开通 / RouteStopStatus. The stop list is
                   built from the client's SimplifiedRoute platforms, so the
                   status resolves through the same route's platform list. */
                buffer.put(q + 82, FangSuStopStatus.stopStatus(
                        simplifiedOf(route.id()), stopPlatform == null ? 0 : stopPlatform.getId(), stopStation));
                buffer.put(q + 83, (byte) (FangSuStopStatus.stationDeferred(stopStation) ? 1 : 0));

                for (int j = 0; j < interchanges.size(); j++) {
                    final com.lx862.jcm.mod.scripting.psd.PSDWrapper.PSDInterchangeWrapper interchange = interchanges.get(j);
                    final int ic = interchangeOffsetsPerRoute.get(r).get(s) + j * 24;
                    buffer.putInt(ic, interchange.color());
                    out.string(ic + 4, interchange.name());
                    buffer.putInt(ic + 12, interchange.transportMode() == null ? -1 : interchange.transportMode().ordinal());
                    out.string(ic + 16, interchange.getTransportType());
                }

                for (int j = 0; j < exits.size(); j++) {
                    final com.lx862.jcm.mod.scripting.psd.PSDWrapper.PSDExitWrapper exit = exits.get(j);
                    final int ex = exitOffsetsPerRoute.get(r).get(s) + j * 16;
                    out.string(ex, exit.name());
                    buffer.putInt(ex + 8, exit.destinations().size());
                    /* The str-ref array is appended into the pool stream,
                       4-aligned — same as the vehicle writer. */
                    final int strRefBase = exit.destinations().isEmpty()
                            ? 0 : out.reserve(exit.destinations().size() * 8, 4);
                    buffer.putInt(ex + 12, strRefBase);
                    for (int k = 0; k < exit.destinations().size(); k++) {
                        out.string(strRefBase + k * 8, exit.destinations().get(k));
                    }
                }
            }
        }

        /* station (ABI 13) — the complete info of psd.stop() */
        if (station != null) {
            buffer.putLong(stationOffset, station.id());
            buffer.putInt(stationOffset + 8, station.color());
            buffer.putInt(stationOffset + 12, 0);
            buffer.putLong(stationOffset + 16, station.zone1());
            buffer.putLong(stationOffset + 24, station.zone2());
            buffer.putLong(stationOffset + 32, station.zone3());
            out.string(stationOffset + 40, station.name());
            out.string(stationOffset + 48, station.colorHex());
            buffer.putInt(stationOffset + 56, stationInterchanges.size());
            buffer.putInt(stationOffset + 60, stationInterchangeOffset);
            for (int i = 0; i < stationInterchanges.size(); i++) {
                final com.lx862.jcm.mod.scripting.psd.PSDWrapper.PSDInterchangeWrapper interchange = stationInterchanges.get(i);
                final int ic = stationInterchangeOffset + i * 32;
                buffer.putLong(ic, interchange.routeId());
                buffer.putInt(ic + 8, interchange.color());
                buffer.putInt(ic + 12, interchange.transportMode() == null ? -1 : interchange.transportMode().ordinal());
                out.string(ic + 16, interchange.name());
                out.string(ic + 24, interchange.getTransportType());
            }
            buffer.putInt(stationOffset + 64, connectingGroups.size());
            buffer.putInt(stationOffset + 68, connectingOffset);
            for (int i = 0; i < connectingGroups.size(); i++) {
                final Map.Entry<String, List<com.lx862.jcm.mod.scripting.psd.PSDWrapper.PSDInterchangeWrapper>> group = connectingGroups.get(i);
                final int cg = connectingOffset + i * 16;
                out.string(cg, group.getKey());
                buffer.putInt(cg + 8, group.getValue().size());
                buffer.putInt(cg + 12, connectingRouteOffsets.get(i));
                for (int j = 0; j < group.getValue().size(); j++) {
                    final com.lx862.jcm.mod.scripting.psd.PSDWrapper.PSDInterchangeWrapper interchange = group.getValue().get(j);
                    final int ic = connectingRouteOffsets.get(i) + j * 32;
                    buffer.putLong(ic, interchange.routeId());
                    buffer.putInt(ic + 8, interchange.color());
                    buffer.putInt(ic + 12, interchange.transportMode() == null ? -1 : interchange.transportMode().ordinal());
                    out.string(ic + 16, interchange.name());
                    out.string(ic + 24, interchange.getTransportType());
                }
            }
            buffer.putInt(stationOffset + 72, station.connectedStations().size());
            buffer.putInt(stationOffset + 76, connectedStationOffset);
            for (int i = 0; i < station.connectedStations().size(); i++) {
                out.string(connectedStationOffset + i * 8, station.connectedStations().get(i));
            }
            buffer.putInt(stationOffset + 80, stationExits.size());
            buffer.putInt(stationOffset + 84, stationExitOffset);
            for (int i = 0; i < stationExits.size(); i++) {
                final com.lx862.jcm.mod.scripting.psd.PSDWrapper.PSDExitWrapper exit = stationExits.get(i);
                final int ex = stationExitOffset + i * 16;
                out.string(ex, exit.name());
                buffer.putInt(ex + 8, exit.destinations().size());
                buffer.putInt(ex + 12, stationExitDestinationOffsets.get(i));
                for (int j = 0; j < exit.destinations().size(); j++) {
                    out.string(stationExitDestinationOffsets.get(i) + j * 8, exit.destinations().get(j));
                }
            }
            buffer.putInt(stationOffset + 88, station.platformCount());
            buffer.putInt(stationOffset + 92, stationPlatformOffset);
            for (int i = 0; i < station.platformCount(); i++) {
                out.string(stationPlatformOffset + i * 8, station.platforms().get(i));
            }
        }

        buffer.putInt(120, buffer.position() - pool);
        return out.finish();
    }

    public static ByteBuffer eyecandy(ByteBuffer buffer, EyecandyBlockEntityWrapper block, boolean used) {
        NativeContentSnapshot out = new NativeContentSnapshot(buffer,88);
        ScriptVector3f pos=block.blockPos();
        buffer.putLong(0,(long)pos.x()); buffer.putLong(8,(long)pos.y()); buffer.putLong(16,(long)pos.z());
        buffer.putLong(24,System.currentTimeMillis()); buffer.putLong(32,day());
        buffer.putFloat(40,block.getTranslateX()); buffer.putFloat(44,block.getTranslateY()); buffer.putFloat(48,block.getTranslateZ());
        buffer.putFloat(52,block.getRotateX()); buffer.putFloat(56,block.getRotateY()); buffer.putFloat(60,block.getRotateZ());
        buffer.put(64,(byte)(block.getFullBrightness()?1:0)); buffer.put(65,(byte)block.facing().ordinal());
        buffer.put(66,(byte)(block.isCrosshairTarget()?1:0)); buffer.putInt(68,block.redstoneLevel());
        /* ABI 15: the weather byte — the header's former _pad0 (offset 67). */
        buffer.put(67,weatherCode());
        buffer.putInt(80,used?1:0); out.string(72,block.getModelId());
        return out.finish();
    }

    public static ByteBuffer lift(ByteBuffer buffer, LiftWrapper lift) {
        NativeContentSnapshot out = new NativeContentSnapshot(buffer,136);
        buffer.putLong(0,lift.getId()); buffer.putLong(8,System.currentTimeMillis()); buffer.putLong(16,day());
        ScriptVector3f pos=lift.getRawPos(), offset=lift.getOffset();
        buffer.putDouble(24,pos.x()); buffer.putDouble(32,pos.y()); buffer.putDouble(40,pos.z());
        buffer.putDouble(48,offset.x()); buffer.putDouble(56,offset.y()); buffer.putDouble(64,offset.z());
        buffer.putDouble(72,lift.getWidth()); buffer.putDouble(80,lift.getHeight()); buffer.putDouble(88,lift.getDepth());
        buffer.putDouble(96,lift.getAngleDegrees()); buffer.putDouble(104,lift.getAngleRadians()); buffer.putDouble(112,lift.getDoorValue());
        buffer.putInt(120,lift.getDirection()); buffer.put(124,(byte)(lift.shouldRender()?1:0));
        buffer.put(125,(byte)(lift.isDoubleSided()?1:0)); buffer.put(126,(byte)(lift.isClientPlayerRiding()?1:0));
        /* ABI 15: the weather byte — the header's former _pad0 (offset 127). */
        buffer.put(127,weatherCode());
        List<LiftWrapper.Floor> floors=lift.getFloors(); int floorOffset=out.reserve(floors.size()*48,8);
        buffer.putInt(128,floors.size()); buffer.putInt(132,floorOffset);
        for(int i=0;i<floors.size();i++) {
            LiftWrapper.Floor floor=floors.get(i); int at=floorOffset+i*48;
            buffer.putInt(at,floor.getIndex()); buffer.put(at+4,(byte)(floor.isCurrentFloor()?1:0)); buffer.put(at+5,(byte)(floor.isTargetFloor()?1:0));
            ScriptVector3f floorPos=floor.getPos(); buffer.putDouble(at+8,floorPos.x()); buffer.putDouble(at+16,floorPos.y()); buffer.putDouble(at+24,floorPos.z());
            out.string(at+32,floor.getNumber()); out.string(at+40,floor.getDescription());
        }
        return out.finish();
    }

    /* ------------------------------------------------------------------ */
    /* ABI 16: the three block-script snapshots (level crossing / fare    */
    /* machine / announcement). Layouts pinned by the static_asserts in   */
    /* mtr_native.h; every *_offset is blob-relative, strings share the   */
    /* string pool like everywhere else.                                  */
    /* ------------------------------------------------------------------ */

    /** One approaching train at a level crossing (JcmCrossingApproach). */
    public static final class CrossingApproachData {
        public long vehicleId;
        public long routeId;
        public long arrivalMillis;      /* 0 = unknown */
        public double distanceBlocks = -1;
        public double speedKmh;
        public double secondsToArrival = -1;
        public int carCount;
        public String destination = "";
    }

    /** JcmLevelCrossingSnapshot payload (header 72 bytes). */
    public static final class CrossingData {
        public org.mtr.mapping.holder.BlockPos pos;
        public double trackYawDegrees = -1;
        public boolean warning;
        public boolean barrierTarget;   /* true = closed (lowered) */
        public boolean fangsuPresent;
        public final java.util.List<CrossingApproachData> approaches = new java.util.ArrayList<>();
    }

    /** JcmFareMachineSnapshot payload (header 96 bytes). */
    public static final class FareData {
        public org.mtr.mapping.holder.BlockPos pos;
        public double doorValue;         /* 0 open .. 1 closed */
        public int fareType;             /* 0 zone, 1 flat */
        public int fareValue;
        public int playerBalance = -1;
        public boolean open;
        public boolean exitSide;
        public int facing;               /* MC Direction ordinal */
        public int machineType;          /* 0 barrier, 1 ticket machine */
        public boolean fangsuPresent;
        public String stationName = "";
        public final java.util.List<ArrivalLite> arrivals = new java.util.ArrayList<>();
    }

    /** Compact arrival for the fare machine pool (96-byte JcmArrival). */
    public static final class ArrivalLite {
        public long arrivalTime, departureTime;
        public long routeId, platformId;
        public int routeColor;
        public int carCount;
        public boolean realtime, terminating;
        public String routeNumber = "", routeName = "", destination = "", platformName = "";

        public ArrivalLite(long arrivalTime, long departureTime, long routeId, long platformId,
                           int routeColor, int carCount, boolean realtime, boolean terminating,
                           String routeNumber, String routeName, String destination, String platformName) {
            this.arrivalTime = arrivalTime; this.departureTime = departureTime;
            this.routeId = routeId; this.platformId = platformId;
            this.routeColor = routeColor; this.carCount = carCount;
            this.realtime = realtime; this.terminating = terminating;
            this.routeNumber = routeNumber; this.routeName = routeName;
            this.destination = destination; this.platformName = platformName;
        }
    }

    /** One announcement arrival entry (JcmAnnounceArrival, 56 bytes). */
    public static final class AnnounceArrivalData {
        public long arrivalMillis, departureMillis;
        public long routeId, platformId;
        public int carCount;
        public boolean realtime, terminating;
        public String destination = "", routeName = "";

        public AnnounceArrivalData(long arrivalMillis, long departureMillis, long routeId, long platformId,
                                   int carCount, boolean realtime, boolean terminating,
                                   String destination, String routeName) {
            this.arrivalMillis = arrivalMillis; this.departureMillis = departureMillis;
            this.routeId = routeId; this.platformId = platformId;
            this.carCount = carCount; this.realtime = realtime; this.terminating = terminating;
            this.destination = destination; this.routeName = routeName;
        }
    }

    /** JcmAnnouncementSnapshot payload (header 88 bytes). */
    public static final class SpeakerData {
        public org.mtr.mapping.holder.BlockPos pos;
        public int leadSeconds = 30;
        public boolean allStops;
        public String template = "", variant = "";
        public boolean fangsuPresent;
        public final java.util.List<long[]> bindings = new java.util.ArrayList<>(); /* {routeId, platformId} */
        public final java.util.List<AnnounceArrivalData> arrivals = new java.util.ArrayList<>();
    }

    /** JcmLevelCrossingSnapshot (ABI 16) — 72-byte header, 64-byte
     *  JcmCrossingApproach pool, shared string pool. */
    public static ByteBuffer levelCrossing(ByteBuffer buffer, CrossingData data) {
        final NativeContentSnapshot out = new NativeContentSnapshot(buffer, 72);
        buffer.putLong(0, data.pos.getX()); buffer.putLong(8, data.pos.getY()); buffer.putLong(16, data.pos.getZ());
        buffer.putLong(24, System.currentTimeMillis()); buffer.putLong(32, day());
        buffer.putDouble(40, data.trackYawDegrees);
        final int approachOffset = out.reserve(data.approaches.size() * 64, 8);
        buffer.putInt(48, data.approaches.size()); buffer.putInt(52, approachOffset);
        final int pool = buffer.position();
        buffer.putInt(56, pool); buffer.putInt(60, 0);
        buffer.put(64, (byte) (data.warning ? 1 : 0));
        buffer.put(65, (byte) (data.barrierTarget ? 1 : 0));
        buffer.put(66, weatherCode());
        buffer.put(67, (byte) (data.fangsuPresent ? 1 : 0));
        for (int i = 0; i < data.approaches.size(); i++) {
            final CrossingApproachData ap = data.approaches.get(i);
            final int at = approachOffset + i * 64;
            buffer.putLong(at, ap.vehicleId); buffer.putLong(at + 8, ap.routeId);
            buffer.putLong(at + 16, ap.arrivalMillis);
            buffer.putDouble(at + 24, ap.distanceBlocks);
            buffer.putDouble(at + 32, ap.speedKmh);
            buffer.putDouble(at + 40, ap.secondsToArrival);
            buffer.putInt(at + 48, ap.carCount);
        }
        for (int i = 0; i < data.approaches.size(); i++) {
            final CrossingApproachData ap = data.approaches.get(i);
            final int at = approachOffset + i * 64;
            out.string(at + 52, ap.destination);
            buffer.putInt(at + 60, 0);
        }
        buffer.putInt(60, buffer.position() - pool);
        return out.finish();
    }

    /** JcmFareMachineSnapshot (ABI 16) — 96-byte header, JcmArrival pool.
     *  Byte fields: open/exit/facing/machine/weather/fangsu at 84..89,
     *  station_name at 68/72, string pool at 76/80. */
    public static ByteBuffer fareMachine(ByteBuffer buffer, FareData data) {
        final NativeContentSnapshot out = new NativeContentSnapshot(buffer, 96);
        buffer.putLong(0, data.pos.getX()); buffer.putLong(8, data.pos.getY()); buffer.putLong(16, data.pos.getZ());
        buffer.putLong(24, System.currentTimeMillis()); buffer.putLong(32, day());
        buffer.putDouble(40, data.doorValue);
        buffer.putInt(48, data.fareType); buffer.putInt(52, data.fareValue);
        buffer.putInt(56, data.playerBalance);
        final int arrivalOffset = out.reserve(data.arrivals.size() * 96, 8);
        buffer.putInt(60, data.arrivals.size()); buffer.putInt(64, arrivalOffset);
        final int pool = buffer.position();
        buffer.putInt(76, pool);
        buffer.put(84, (byte) (data.open ? 1 : 0));
        buffer.put(85, (byte) (data.exitSide ? 1 : 0));
        buffer.put(86, (byte) data.facing);
        buffer.put(87, (byte) data.machineType);
        buffer.put(88, weatherCode());
        buffer.put(89, (byte) (data.fangsuPresent ? 1 : 0));
        for (int i = 0; i < data.arrivals.size(); i++) {
            final ArrivalLite a = data.arrivals.get(i);
            final int at = arrivalOffset + i * 96;
            buffer.putLong(at, a.arrivalTime); buffer.putLong(at + 8, a.departureTime);
            buffer.putLong(at + 16, 0); buffer.putLong(at + 24, 0);
            buffer.putLong(at + 32, a.routeId); buffer.putLong(at + 40, a.platformId);
            buffer.putInt(at + 48, a.routeColor); buffer.putInt(at + 52, a.carCount);
            buffer.put(at + 56, (byte) (a.realtime ? 1 : 0));
            buffer.put(at + 57, (byte) (a.terminating ? 1 : 0));
            buffer.put(at + 58, (byte) 0);
            buffer.put(at + 59, arrivalFlags(a.routeId, a.platformId));
            out.string(at + 60, a.routeNumber); out.string(at + 68, a.routeName);
            out.string(at + 76, a.destination); out.string(at + 84, a.platformName);
        }
        out.string(68, data.stationName);
        buffer.putInt(80, buffer.position() - pool);
        return out.finish();
    }

    /** JcmAnnouncementSnapshot (ABI 16) — 88-byte header, int64[2]
     *  binding pool, JcmAnnounceArrival pool. template at 60/64, variant
     *  at 68/72, string pool at 76/80, byte fields at 84..87. */
    public static ByteBuffer announcement(ByteBuffer buffer, SpeakerData data) {
        final NativeContentSnapshot out = new NativeContentSnapshot(buffer, 88);
        buffer.putLong(0, data.pos.getX()); buffer.putLong(8, data.pos.getY()); buffer.putLong(16, data.pos.getZ());
        buffer.putLong(24, System.currentTimeMillis()); buffer.putLong(32, day());
        buffer.putInt(40, data.leadSeconds);
        final int bindingOffset = out.reserve(data.bindings.size() * 16, 8);
        buffer.putInt(44, data.bindings.size()); buffer.putInt(48, bindingOffset);
        final int arrivalOffset = out.reserve(data.arrivals.size() * 56, 8);
        buffer.putInt(52, data.arrivals.size()); buffer.putInt(56, arrivalOffset);
        final int pool = buffer.position();
        buffer.putInt(76, pool);
        buffer.put(84, (byte) (data.allStops ? 1 : 0));
        buffer.put(85, weatherCode());
        buffer.put(86, (byte) (data.fangsuPresent ? 1 : 0));
        for (int i = 0; i < data.bindings.size(); i++) {
            final long[] pair = data.bindings.get(i);
            buffer.putLong(bindingOffset + i * 16, pair[0]);
            buffer.putLong(bindingOffset + i * 16 + 8, pair[1]);
        }
        for (int i = 0; i < data.arrivals.size(); i++) {
            final AnnounceArrivalData a = data.arrivals.get(i);
            final int at = arrivalOffset + i * 56;
            buffer.putLong(at, a.arrivalMillis); buffer.putLong(at + 8, a.departureMillis);
            buffer.putLong(at + 16, a.routeId); buffer.putLong(at + 24, a.platformId);
            buffer.putInt(at + 32, a.carCount);
            buffer.put(at + 36, (byte) (a.realtime ? 1 : 0));
            buffer.put(at + 37, (byte) (a.terminating ? 1 : 0));
            buffer.put(at + 38, arrivalFlags(a.routeId, a.platformId));
            buffer.put(at + 39, (byte) 0);
        }
        out.string(60, data.template);
        out.string(68, data.variant);
        for (int i = 0; i < data.arrivals.size(); i++) {
            final AnnounceArrivalData a = data.arrivals.get(i);
            final int at = arrivalOffset + i * 56;
            out.string(at + 40, a.destination);
            out.string(at + 48, a.routeName);
        }
        buffer.putInt(80, buffer.position() - pool);
        return out.finish();
    }
}
