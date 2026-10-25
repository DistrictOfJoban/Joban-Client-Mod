package com.lx862.mtrscripting.mod.network.packet.data;

import com.lx862.mtrscripting.mod.impl.mtr.server.DoorDirectionManager;
import org.mtr.mapping.holder.MinecraftServer;
import org.mtr.mapping.holder.ServerPlayerEntity;
import org.mtr.mapping.registry.PacketHandler;
import org.mtr.mapping.tool.PacketBufferReceiver;
import org.mtr.mapping.tool.PacketBufferSender;

/**
 * Client → server: "give me the stored door-opening directions of this
 * route" — the request behind {@code vehicle.getDirectionOfDoorOpening()}
 * while a train runs the mainline and its next stop is too far away for the
 * client's own world scan to see anything.
 *
 * <p>The server reads the answer out of the per-route files it maintains
 * (see {@link DoorDirectionManager}) and replies with a
 * {@link DoorDirectionsS2CPacket}; when the route has not been scanned yet
 * it schedules a one-route scan and replies empty-but-incomplete, which
 * the client treats as "retry in a few seconds".
 */
public class RequestDoorDirectionsC2SPacket extends PacketHandler {
    private final long routeId;

    public RequestDoorDirectionsC2SPacket(PacketBufferReceiver packetBufferReceiver) {
        this.routeId = packetBufferReceiver.readLong();
    }

    public RequestDoorDirectionsC2SPacket(long routeId) {
        this.routeId = routeId;
    }

    @Override
    public void write(PacketBufferSender packetBufferSender) {
        packetBufferSender.writeLong(routeId);
    }

    @Override
    public void runServer(MinecraftServer minecraftServer, ServerPlayerEntity serverPlayerEntity) {
        /* The manager is server-thread confined; hop over from the network
           thread, exactly like the other request packets in this package. */
        minecraftServer.submit(() -> DoorDirectionManager.handleQuery(minecraftServer, serverPlayerEntity, routeId));
    }
}
