package com.lx862.jcm.mod.network.scripting;

import com.lx862.jcm.mod.scripting.mtr.vehicle.VehicleStopsDataCache;
import org.mtr.mapping.registry.PacketHandler;
import org.mtr.mapping.tool.PacketBufferReceiver;
import org.mtr.mapping.tool.PacketBufferSender;

public class FullStopsDataS2CPacket extends PacketHandler {
    private final long vehicleId;
    private final VehicleStopsDataCache.SimplifiedStopsData simplifiedStopsData;

    public FullStopsDataS2CPacket(PacketBufferReceiver packetBufferReceiver) {
        this.vehicleId = packetBufferReceiver.readLong();
        long routeAmount = packetBufferReceiver.readInt();

        this.simplifiedStopsData = new VehicleStopsDataCache.SimplifiedStopsData();
        for(int i = 0; i < routeAmount; i++) {
            long routeId = packetBufferReceiver.readLong();
            VehicleStopsDataCache.RouteStopsData routeStopsData = new VehicleStopsDataCache.RouteStopsData(routeId);
            int routeStopAmount = packetBufferReceiver.readInt();

            for(int j = 0; j < routeStopAmount; j++) {
                long stationId = packetBufferReceiver.readLong();
                long platformId = packetBufferReceiver.readLong();
                double distance = packetBufferReceiver.readDouble();
                String destination = packetBufferReceiver.readString();
                VehicleStopsDataCache.SimplifiedStop simplifiedStop = new VehicleStopsDataCache.SimplifiedStop(destination, stationId, platformId, distance);
                routeStopsData.addStop(simplifiedStop);
            }
            this.simplifiedStopsData.addRouteStopsData(routeStopsData);
        }
    }

    public FullStopsDataS2CPacket(long vehicleId, VehicleStopsDataCache.SimplifiedStopsData simplifiedStopsData) {
        this.vehicleId = vehicleId;
        this.simplifiedStopsData = simplifiedStopsData;
    }

    @Override
    public void write(PacketBufferSender packetBufferSender) {
        packetBufferSender.writeLong(vehicleId);
        packetBufferSender.writeInt(simplifiedStopsData.routeStopsData.size());

        for(VehicleStopsDataCache.RouteStopsData routeStopsData : simplifiedStopsData.routeStopsData) {
            packetBufferSender.writeLong(routeStopsData.routeId);
            packetBufferSender.writeInt(routeStopsData.stops.size());

            for(VehicleStopsDataCache.SimplifiedStop stop : routeStopsData.stops) {
                packetBufferSender.writeLong(stop.stationId);
                packetBufferSender.writeLong(stop.platformId);
                packetBufferSender.writeDouble(stop.distance);
                packetBufferSender.writeString(stop.destination);
            }
        }
    }

    @Override
    public void runClient() {
        System.out.println(simplifiedStopsData.toString()); // debug
        VehicleStopsDataCache.putCache(vehicleId, simplifiedStopsData);

        // TODO Part 2: Collect route, station, siding ids and fetch them
    }
}
