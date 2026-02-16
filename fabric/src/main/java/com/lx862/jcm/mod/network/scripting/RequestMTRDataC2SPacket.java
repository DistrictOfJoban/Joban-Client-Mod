package com.lx862.jcm.mod.network.scripting;

import com.lx862.jcm.mixin.modded.mtr.InitAccessorMixin;
import com.lx862.jcm.mixin.modded.tsc.MainAccessorMixin;
import com.lx862.jcm.mod.registry.Networking;
import com.lx862.jcm.mod.scripting.MTRDatasetHolder;
import org.mtr.core.Main;
import org.mtr.core.data.*;
import org.mtr.core.simulation.Simulator;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.ObjectImmutableList;
import org.mtr.mapping.holder.MinecraftServer;
import org.mtr.mapping.holder.PlayerEntity;
import org.mtr.mapping.holder.ServerPlayerEntity;
import org.mtr.mapping.holder.World;
import org.mtr.mapping.registry.PacketHandler;
import org.mtr.mapping.tool.PacketBufferReceiver;
import org.mtr.mapping.tool.PacketBufferSender;
import org.mtr.mod.Init;

import java.util.ArrayList;
import java.util.List;

public class RequestMTRDataC2SPacket extends PacketHandler {
    private final List<Long> stationIds = new ArrayList<>();
    private final List<Long> sidingIds = new ArrayList<>();
    private final List<Long> routeIds = new ArrayList<>();
    private final List<Long> platformIds = new ArrayList<>();

    public RequestMTRDataC2SPacket(PacketBufferReceiver packetBufferReceiver) {
        int stationLength = packetBufferReceiver.readInt();
        int sidingLength = packetBufferReceiver.readInt();
        int routeLength = packetBufferReceiver.readInt();
        int platformLength = packetBufferReceiver.readInt();
        for(int i = 0; i < stationLength; i++) {
            this.stationIds.add(packetBufferReceiver.readLong());
        }
        for(int i = 0; i < sidingLength; i++) {
            this.sidingIds.add(packetBufferReceiver.readLong());
        }
        for(int i = 0; i < routeLength; i++) {
            this.routeIds.add(packetBufferReceiver.readLong());
        }
        for(int i = 0; i < platformLength; i++) {
            this.platformIds.add(packetBufferReceiver.readLong());
        }
    }

    public RequestMTRDataC2SPacket(List<Long> stationIds, List<Long> sidingIds, List<Long> routeIds, List<Long> platformIds) {
        this.stationIds.addAll(stationIds);
        this.sidingIds.addAll(sidingIds);
        this.routeIds.addAll(routeIds);
        this.platformIds.addAll(platformIds);
    }

    @Override
    public void write(PacketBufferSender packetBufferSender) {
        packetBufferSender.writeInt(this.stationIds.size());
        packetBufferSender.writeInt(this.sidingIds.size());
        packetBufferSender.writeInt(this.routeIds.size());
        packetBufferSender.writeInt(this.platformIds.size());
        stationIds.forEach(packetBufferSender::writeLong);
        sidingIds.forEach(packetBufferSender::writeLong);
        routeIds.forEach(packetBufferSender::writeLong);
        platformIds.forEach(packetBufferSender::writeLong);
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
                    MTRDatasetHolder dataInstance = new MTRDatasetHolder();
                    for(long id : stationIds) {
                        dataInstance.addStation(simulator.stationIdMap.get(id));
                    }
                    for(long id : sidingIds) {
                        dataInstance.addSiding(simulator.sidingIdMap.get(id));
                    }
                    for(long id : routeIds) {
                        ObjectArrayList<SimplifiedRoute> list = new ObjectArrayList<>();
                        SimplifiedRoute.addToList(list, simulator.routeIdMap.get(id));
                        dataInstance.addRoute(list.get(0));
                    }
                    for(long id : platformIds) {
                        dataInstance.addPlatform(simulator.platformIdMap.get(id));
                    }

                    minecraftServer.submit(() -> {
                        Networking.sendPacketToClient(PlayerEntity.cast(serverPlayerEntity), new MTRDataS2CPacket(dataInstance));
                    });
                });
                break;
            }
        }
    }
}
