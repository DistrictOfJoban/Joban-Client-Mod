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
    private ByteBuffer finish() { buffer.flip(); return buffer; }

    public static ByteBuffer pids(ByteBuffer buffer, PIDSWrapper pids) {
        NativeContentSnapshot out = new NativeContentSnapshot(buffer,104);
        ScriptVector3f pos = pids.blockPos();
        buffer.putLong(0,(long)pos.x()); buffer.putLong(8,(long)pos.y()); buffer.putLong(16,(long)pos.z());
        buffer.putLong(24,pids.station() == null ? 0 : pids.station().getId());
        buffer.putLong(32,System.currentTimeMillis()); buffer.putLong(40,day());
        buffer.putInt(48,pids.width); buffer.putInt(52,pids.height); buffer.putInt(56,pids.rows);
        buffer.put(61,(byte)(pids.isKeyBlock()?1:0)); buffer.put(62,(byte)(pids.isPlatformNumberHidden()?1:0));
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
            out.string(at+60,arrival.routeNumber()); out.string(at+68,arrival.routeName());
            out.string(at+76,arrival.destination()); out.string(at+84,arrival.platformName());
        }
        for(int i=0;i<pids.rows;i++) out.string(messages+i*8,pids.getCustomMessage(i));
        buffer.putInt(96,buffer.position()-pool);
        return out.finish();
    }

    /** JcmPsdSnapshot (ABI 12) — layout pinned by the static_asserts in
     *  mtr_native.h. Header 128, JcmPsdDoor 32, JcmPsdArrival 104
     *  (JcmArrival + door refs), JcmPsdRoute 48, stops/interchanges/exits
     *  reuse the vehicle JcmStop layout. ABI 13 adds the station pools:
     *  JcmPsdStation 96 at snapshot offset 124 (formerly padding),
     *  JcmPsdStationInterchange 32, JcmPsdStationConnectingGroup 16. */
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

        final NativeContentSnapshot out = new NativeContentSnapshot(buffer, 128);
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
            buffer.put(p + 13, (byte) 0); buffer.put(p + 14, (byte) 0); buffer.put(p + 15, (byte) 0);
            out.string(p + 16, route.name());
            out.string(p + 24, route.routeNumber());
            out.string(p + 32, route.getTransportType());
            final List<com.lx862.jcm.mod.scripting.psd.PSDWrapper.PSDStopWrapper> stops = routeStops.get(r);
            buffer.putInt(p + 40, stops.size());
            buffer.putInt(p + 44, stops.isEmpty() ? 0 : stopOffsets[r]);

            for (int s = 0; s < stops.size(); s++) {
                final com.lx862.jcm.mod.scripting.psd.PSDWrapper.PSDStopWrapper stop = stops.get(s);
                final int q = stopOffsets[r] + s * 88;
                final org.mtr.core.data.Station station = stop.station();
                final org.mtr.core.data.Platform stopPlatform = stop.platform();
                buffer.putLong(q, route.id());
                buffer.putLong(q + 8, station == null ? 0 : station.getId());
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
                buffer.put(q + 81, (byte) 0); buffer.put(q + 82, (byte) 0); buffer.put(q + 83, (byte) 0);

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
                    buffer.position((buffer.position() + 3) & ~3);
                    final int strRefBase = exit.destinations().isEmpty() ? 0 : buffer.position();
                    buffer.position(buffer.position() + exit.destinations().size() * 8);
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
}
