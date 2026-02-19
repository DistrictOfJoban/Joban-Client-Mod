package com.lx862.jcm.mod.network.scripting;

import com.lx862.jcm.mod.JCMClient;
import com.lx862.jcm.mod.registry.Networking;
import com.lx862.jcm.mod.scripting.mtr.vehicle.VehicleDataCache;
import com.lx862.jcm.mod.util.JCMLogger;
import org.mtr.mapping.registry.PacketHandler;
import org.mtr.mapping.tool.PacketBufferReceiver;
import org.mtr.mapping.tool.PacketBufferSender;

import java.util.ArrayList;
import java.util.List;

public class StopsDataS2CPacket extends PacketHandler {
    private final long vehicleId;
    private final long sidingId;
    private final VehicleDataCache.SimplifiedStopsData simplifiedStopsData;

    public StopsDataS2CPacket(PacketBufferReceiver packetBufferReceiver) {
        this.vehicleId = packetBufferReceiver.readLong();
        VehicleDataCache.stopsDataByteCounter += 8;
        this.sidingId = packetBufferReceiver.readLong();
        VehicleDataCache.stopsDataByteCounter += 8;
        long routeAmount = packetBufferReceiver.readInt();
        VehicleDataCache.stopsDataByteCounter += 4;

        this.simplifiedStopsData = new VehicleDataCache.SimplifiedStopsData();
        for(int i = 0; i < routeAmount; i++) {
            long routeId = packetBufferReceiver.readLong();
            VehicleDataCache.stopsDataByteCounter += 8;
            VehicleDataCache.RouteStopsData routeStopsData = new VehicleDataCache.RouteStopsData(routeId);
            int routeStopAmount = packetBufferReceiver.readInt();
            VehicleDataCache.stopsDataByteCounter += 4;

            for(int j = 0; j < routeStopAmount; j++) {
                long stationId = packetBufferReceiver.readLong();
                long platformId = packetBufferReceiver.readLong();
                double distance = packetBufferReceiver.readDouble();
                VehicleDataCache.stopsDataByteCounter += (8*3);
                String destination = packetBufferReceiver.readString();
                VehicleDataCache.stopsDataByteCounter += 4; // int to store string length
                VehicleDataCache.stopsDataByteCounter += destination.length() * 2L; // 2 byte chars
                VehicleDataCache.SimplifiedStop simplifiedStop = new VehicleDataCache.SimplifiedStop(destination, stationId, platformId, distance);
                routeStopsData.addStop(simplifiedStop);
            }
            this.simplifiedStopsData.addRouteStopsData(routeStopsData);
        }
    }

    public StopsDataS2CPacket(long vehicleId, long sidingId, VehicleDataCache.SimplifiedStopsData simplifiedStopsData) {
        this.vehicleId = vehicleId;
        this.sidingId = sidingId;
        this.simplifiedStopsData = simplifiedStopsData;
    }

    @Override
    public void write(PacketBufferSender packetBufferSender) {
        packetBufferSender.writeLong(vehicleId);
        packetBufferSender.writeLong(sidingId);
        packetBufferSender.writeInt(simplifiedStopsData.routeStopsData.size());

        for(VehicleDataCache.RouteStopsData routeStopsData : simplifiedStopsData.routeStopsData) {
            packetBufferSender.writeLong(routeStopsData.routeId);
            packetBufferSender.writeInt(routeStopsData.stops.size());

            for(VehicleDataCache.SimplifiedStop stop : routeStopsData.stops) {
                packetBufferSender.writeLong(stop.stationId);
                packetBufferSender.writeLong(stop.platformId);
                packetBufferSender.writeDouble(stop.distance);
                packetBufferSender.writeString(stop.destination);
            }
        }
    }

    @Override
    public void runClient() {
        if(JCMClient.getConfig().debug) JCMLogger.info("Received Vehicle Stops Data:\n" + simplifiedStopsData.toString());

        VehicleDataCache.putStopsDataCache(vehicleId, simplifiedStopsData);

        /* Second round of data fetching: Collect detailed information of station, route, platform and sidings */
        List<Long> stationIds = new ArrayList<>();
        List<Long> routeIds = new ArrayList<>();
        List<Long> platformIds = new ArrayList<>();
        List<Long> sidingIds = new ArrayList<>();

        for(VehicleDataCache.RouteStopsData routeStopsData : simplifiedStopsData.routeStopsData) {
            if(!routeIds.contains(routeStopsData.routeId)) routeIds.add(routeStopsData.routeId);
            for(VehicleDataCache.SimplifiedStop stop : routeStopsData.stops) {
                if(!stationIds.contains(stop.stationId)) stationIds.add(stop.stationId);
                if(!platformIds.contains(stop.platformId)) platformIds.add(stop.platformId);
            }
        }
        sidingIds.add(this.sidingId);

        // Remove already fetched data
        stationIds.removeIf(e -> VehicleDataCache.mtrData.stations.stream().anyMatch(obj -> e == obj.getId()));
        routeIds.removeIf(e -> VehicleDataCache.mtrData.routes.stream().anyMatch(obj -> e == obj.getId()));
        platformIds.removeIf(e -> VehicleDataCache.mtrData.platforms.stream().anyMatch(obj -> e == obj.getId()));
        sidingIds.removeIf(e -> VehicleDataCache.mtrData.sidings.stream().anyMatch(obj -> e == obj.getId()));

        // Don't bother if we don't need any data
        if(stationIds.size() + sidingIds.size() + routeIds.size() + platformIds.size() == 0) return;

        Networking.sendPacketToServer(new RequestMTRDataC2SPacket(stationIds, sidingIds, routeIds, platformIds));
    }
}
