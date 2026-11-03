package com.lx862.mtrscripting.mod.network.packet.psd;

import com.lx862.mtrscripting.mod.impl.mtr.server.PSDServerBindings;
import org.mtr.mapping.holder.MinecraftServer;
import org.mtr.mapping.holder.ServerPlayerEntity;
import org.mtr.mapping.registry.PacketHandler;
import org.mtr.mapping.tool.PacketBufferReceiver;
import org.mtr.mapping.tool.PacketBufferSender;

/**
 * Client → server: "send me every PSD script binding of this dimension" —
 * sent on join and on dimension switch. A server without this feature never
 * answers and the client stops asking (see {@code PSDClientBindings}).
 */
public class RequestPSDScriptsC2SPacket extends PacketHandler {
    private final String dimension;

    public RequestPSDScriptsC2SPacket(PacketBufferReceiver packetBufferReceiver) {
        this.dimension = packetBufferReceiver.readString();
    }

    public RequestPSDScriptsC2SPacket(String dimension) {
        this.dimension = dimension;
    }

    @Override
    public void write(PacketBufferSender packetBufferSender) {
        packetBufferSender.writeString(dimension);
    }

    @Override
    public void runServer(MinecraftServer minecraftServer, ServerPlayerEntity serverPlayerEntity) {
        /* Same thread hop as the other request packets in this package: the
           manager is server-thread confined. */
        minecraftServer.submit(() -> PSDServerBindings.handleQuery(minecraftServer, serverPlayerEntity, dimension));
    }
}
