package com.lx862.mtrscripting.mod.network.packet.data;

import com.lx862.jcm.mixin.mtrscripting.InitAccessorMixin;
import com.lx862.jcm.mixin.mtrscripting.MainAccessorMixin;
import com.lx862.jcm.mixin.mtrscripting.SidingAccessorMixin;
import com.lx862.jcm.mod.config.JCMServerConfig;
import com.lx862.jcm.mod.registry.Networking;
import com.lx862.mtrscripting.mod.MTRScriptingMod;
import com.lx862.mtrscripting.mod.impl.mtr.vehicle.VehicleDataCache;
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
        if(JCMServerConfig.INSTANCE.disableScriptStopsFetching.value()) {
            if(JCMServerConfig.INSTANCE.debugMode.value()) {
                MTRScriptingMod.LOGGER.info("[MTR Scripting via JCM] Refusing to provide stop data for {} as it is disabled in server config.", serverPlayerEntity.getGameProfile().getName());
            }
            return;
        }

        Main tscInstance = InitAccessorMixin.getMain();
        if(tscInstance == null) return; // Simulator may not be initialized during server startup.
        World playerWorld = serverPlayerEntity.getEntityWorld();
        String tscDimensionId = Init.getWorldId(playerWorld);

        ObjectImmutableList<Simulator> simulators = ((MainAccessorMixin)tscInstance).getSimulators();
        for(Simulator simulator : simulators) {
            if(simulator.dimension.equals(tscDimensionId)) {
                simulator.run(() -> {
                    boolean debugLogging = JCMServerConfig.INSTANCE.debugMode.value();
                    if(debugLogging) {
                        MTRScriptingMod.LOGGER.info("[MTR Scripting via JCM] RequestStopsDataC2SPacket: Start compiling stops data in " + tscDimensionId);
                    }

                    try {
                        Siding siding = simulator.sidingIdMap.get(sidingId);
                        if(siding != null) {
                            Depot depot = siding.area;
                            if(depot == null) {
                                if(debugLogging) {
                                    MTRScriptingMod.LOGGER.warn("[MTR Scripting via JCM] RequestStopsDataC2SPacket: Failed to find depot from siding!");
                                }
                                return;
                            }

                            boolean vehicleFound = false;
                            ObjectArraySet<Vehicle> vehicles = ((SidingAccessorMixin)(Object)siding).getVehicles();
                            for(Vehicle vehicle : vehicles) {
                                if(vehicle.getId() == vehicleId) {
                                    vehicleFound = true;
                                    VehicleDataCache.SimplifiedStopsData simplifiedStopsData = new VehicleDataCache.SimplifiedStopsData();
                                    if(!depot.routes.isEmpty()) {
                                        // Snapshot each route's platform sequence once. Keeping original
                                        // indices is essential: destinations are indexed before deduplication.
                                        ObjectArrayList<ObjectArrayList<RoutePlatformData>> routePlatforms = new ObjectArrayList<>();
                                        long[][] platformIds = new long[depot.routes.size()][];
                                        for(int routeIndex = 0; routeIndex < depot.routes.size(); routeIndex++) {
                                            ObjectArrayList<RoutePlatformData> platforms = depot.routes.get(routeIndex).getRoutePlatforms();
                                            routePlatforms.add(platforms);
                                            platformIds[routeIndex] = new long[platforms.size()];
                                            for(int platformIndex = 0; platformIndex < platforms.size(); platformIndex++) {
                                                platformIds[routeIndex][platformIndex] = platforms.get(platformIndex).getPlatform().getId();
                                            }
                                        }
                                        RouteStopCursor cursor = new RouteStopCursor(platformIds);
                                        VehicleDataCache.RouteStopsData routeStopsData = null;
                                        ObjectImmutableList<PathData> vehiclePath = vehicle.vehicleExtraData.immutablePath;

                                        for(int i = 0; i < vehiclePath.size() && cursor.hasNext(); i++) {
                                            PathData currentPath = vehiclePath.get(i);
                                            if(!currentPath.getRail().isPlatform() || currentPath.getDwellTime() <= 0) continue;
                                            double distance = currentPath.getEndDistance();
                                            boolean turnbackPlatform = (i+1 < vehiclePath.size()) && vehiclePath.get(i+1).isOppositeRail(currentPath);

                                            // One dwell can finish a route and start the next. Rebuild
                                            // route-specific destinations instead of copying the old stop.
                                            while(cursor.hasNext()) {
                                                int routeIndex = cursor.routeIndex();
                                                int platformIndex = cursor.platformIndex();
                                                Route belongingRoute = depot.routes.get(routeIndex);
                                                if(routeStopsData == null) {
                                                    routeStopsData = new VehicleDataCache.RouteStopsData(belongingRoute.getId());
                                                }
                                                RoutePlatformData routePlatformData = routePlatforms.get(routeIndex).get(platformIndex);
                                                long platformId = routePlatformData.getPlatform().getId();
                                                long stationId = routePlatformData.getPlatform().area == null ? 0 : routePlatformData.getPlatform().area.getId();
                                                String destination = belongingRoute.getDestination(platformIndex);
                                                String customDestination = routePlatformData.getCustomDestination();
                                                routeStopsData.addStop(new VehicleDataCache.SimplifiedStop(destination, customDestination, stationId, platformId, distance, turnbackPlatform));

                                                boolean sharedBoundary = cursor.advance();
                                                if(cursor.routeIndex() != routeIndex) {
                                                    simplifiedStopsData.addRouteStopsData(routeStopsData);
                                                    routeStopsData = null;
                                                }
                                                if(!sharedBoundary) break;
                                            }
                                        }
                                    }
                                    
                                    if(debugLogging) {
                                        MTRScriptingMod.LOGGER.info("[MTR Scripting via JCM] RequestStopsDataC2SPacket: Submitting Response Packet");
                                    }
                                    minecraftServer.submit(() -> {
                                        Networking.sendPacketToClient(PlayerEntity.cast(serverPlayerEntity), new StopsDataS2CPacket(vehicleId, sidingId, simplifiedStopsData));
                                        if(debugLogging) {
                                            MTRScriptingMod.LOGGER.info("[MTR Scripting via JCM] RequestStopsDataC2SPacket: Request Packet Sent to {}, Goodbye!", serverPlayerEntity.getGameProfile().getName());
                                        }
                                    });
                                    break;
                                }
                            }
                            if(!vehicleFound) {
                                MTRScriptingMod.LOGGER.warn("[MTR Scripting via JCM] RequestStopsDataC2SPacket: Failed to find vehicle from siding!");
                            }
                        } else {
                            MTRScriptingMod.LOGGER.warn("[MTR Scripting via JCM] RequestStopsDataC2SPacket: Failed to find requested siding!");
                        }
                    } catch (Exception e) {
                        MTRScriptingMod.LOGGER.error("RequestStopsDataC2SPacket failed!", e);
                    }
                });
                break;
            }
        }
    }

}
