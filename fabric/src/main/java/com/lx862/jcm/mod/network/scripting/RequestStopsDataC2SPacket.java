package com.lx862.jcm.mod.network.scripting;

import com.lx862.jcm.mixin.modded.mtr.InitAccessorMixin;
import com.lx862.jcm.mixin.modded.tsc.MainAccessorMixin;
import com.lx862.jcm.mixin.modded.tsc.SidingAccessorMixin;
import com.lx862.jcm.mod.registry.Networking;
import com.lx862.jcm.mod.scripting.mtr.vehicle.VehicleDataCache;
import com.lx862.jcm.mod.util.JCMLogger;
import org.mtr.core.Main;
import org.mtr.core.data.*;
import org.mtr.core.simulation.Simulator;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.ObjectArraySet;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.ObjectImmutableList;
import org.mtr.mapping.holder.MinecraftServer;
import org.mtr.mapping.holder.PlayerEntity;
import org.mtr.mapping.holder.ServerPlayerEntity;
import org.mtr.mapping.holder.World;
import org.mtr.mapping.registry.PacketHandler;
import org.mtr.mapping.tool.PacketBufferReceiver;
import org.mtr.mapping.tool.PacketBufferSender;
import org.mtr.mod.Init;

public class RequestStopsDataC2SPacket extends PacketHandler {
    private final long vehicleId;
    private final long sidingId;

    public RequestStopsDataC2SPacket(PacketBufferReceiver packetBufferReceiver) {
        this.vehicleId = packetBufferReceiver.readLong();
        this.sidingId = packetBufferReceiver.readLong();
    }

    public RequestStopsDataC2SPacket(long vehicleId, long sidingId) {
        this.vehicleId = vehicleId;
        this.sidingId = sidingId;
    }

    @Override
    public void write(PacketBufferSender packetBufferSender) {
        packetBufferSender.writeLong(vehicleId);
        packetBufferSender.writeLong(sidingId);
    }

    @Override
    public void runServer(MinecraftServer minecraftServer, ServerPlayerEntity serverPlayerEntity) {
        Main tscInstance = InitAccessorMixin.getMain();
        World playerWorld = serverPlayerEntity.getEntityWorld();
        String tscDimensionId = Init.getWorldId(playerWorld);

        ObjectImmutableList<Simulator> simulators = ((MainAccessorMixin)tscInstance).getSimulators();
        for(Simulator simulator : simulators) {
            if(simulator.dimension.equals(tscDimensionId)) {
                simulator.run(() -> {
                    try {
                        Siding siding = simulator.sidingIdMap.get(sidingId);
                        if(siding != null) {
                            Depot depot = siding.area;
                            if(depot == null) return;
                            
                            ObjectArraySet<Vehicle> vehicles = ((SidingAccessorMixin)(Object)siding).getVehicles();
                            for(Vehicle vehicle : vehicles) {
                                if(vehicle.getId() == vehicleId) {
                                    int routeIdx = 0;
                                    VehicleDataCache.SimplifiedStopsData simplifiedStopsData = new VehicleDataCache.SimplifiedStopsData();

                                    Route belongingRoute = depot.routes.get(routeIdx);
                                    VehicleDataCache.RouteStopsData routeStopsData = new VehicleDataCache.RouteStopsData(belongingRoute.getId());
                                    for(PathData pathData : vehicle.vehicleExtraData.immutablePath) {
                                        if(!pathData.getRail().isPlatform() || pathData.getDwellTime() <= 0) continue;
                                        ObjectArrayList<RoutePlatformData> routePlatforms = belongingRoute.getRoutePlatforms();
                                        int routePlatformIndex = routeStopsData.stops.size();
                                        boolean finalRouteStop = routePlatformIndex == routePlatforms.size()-1;
                                        RoutePlatformData routePlatformData = routePlatforms.get(routePlatformIndex);

                                        long platformId = routePlatformData.getPlatform().getId();
                                        long stationId = routePlatformData.getPlatform().area.getId();
                                        String destination = belongingRoute.getDestination(routePlatformIndex);
                                        double distance = pathData.getEndDistance();

                                        VehicleDataCache.SimplifiedStop stopObject = new VehicleDataCache.SimplifiedStop(destination, stationId, platformId, distance);
                                        routeStopsData.addStop(stopObject);
                                        if(finalRouteStop) {
                                            routeIdx++;
                                            simplifiedStopsData.addRouteStopsData(routeStopsData);
                                            if(routeIdx >= depot.routes.size()) break;
                                            belongingRoute = depot.routes.get(routeIdx);
                                            routeStopsData = new VehicleDataCache.RouteStopsData(belongingRoute.getId());
                                            routeStopsData.addStop(stopObject);
                                        }
                                    }

                                    minecraftServer.submit(() -> {
                                        Networking.sendPacketToClient(PlayerEntity.cast(serverPlayerEntity), new StopsDataS2CPacket(vehicleId, sidingId, simplifiedStopsData));
                                    });
                                    break;
                                }
                                break;
                            }
                        }
                    } catch (Exception e) {
                        JCMLogger.error("RequestStopsDataC2SPacket failed!", e);
                    }
                });
                break;
            }
        }
    }
}
