package com.lx862.mtrscripting.mod.network.packet.psd;

import com.lx862.mtrscripting.mod.impl.mtr.server.PSDServerBindings;
import org.mtr.mapping.holder.MinecraftServer;
import org.mtr.mapping.holder.ServerPlayerEntity;
import org.mtr.mapping.registry.PacketHandler;
import org.mtr.mapping.tool.PacketBufferReceiver;
import org.mtr.mapping.tool.PacketBufferSender;

/**
 * Client → server: "bind (or, with an empty script id, unbind) a PSD script
 * on this block" — sent by the PSD Script Editor's selection screen. The
 * server validates that the sender holds the editor item and that the block
 * really is a PSD glass/top block of the matching surface, stores the
 * binding in the dimension's {@code psd-scripts/bindings.json} and broadcasts
 * the updated list to every player in the dimension.
 */
public class PSDScriptBindC2SPacket extends PacketHandler {
    private final int x;
    private final int y;
    private final int z;
    private final String scriptId;
    private final boolean pidsPreset;
    private final String type;

    public PSDScriptBindC2SPacket(PacketBufferReceiver packetBufferReceiver) {
        this.x = packetBufferReceiver.readInt();
        this.y = packetBufferReceiver.readInt();
        this.z = packetBufferReceiver.readInt();
        this.scriptId = packetBufferReceiver.readString();
        this.pidsPreset = packetBufferReceiver.readBoolean();
        this.type = packetBufferReceiver.readString();
    }

    public PSDScriptBindC2SPacket(org.mtr.mapping.holder.BlockPos pos, String scriptId, boolean pidsPreset, String type) {
        this.x = (int) pos.getX();
        this.y = (int) pos.getY();
        this.z = (int) pos.getZ();
        this.scriptId = scriptId;
        this.pidsPreset = pidsPreset;
        this.type = type;
    }

    @Override
    public void write(PacketBufferSender packetBufferSender) {
        packetBufferSender.writeInt(x);
        packetBufferSender.writeInt(y);
        packetBufferSender.writeInt(z);
        packetBufferSender.writeString(scriptId);
        packetBufferSender.writeBoolean(pidsPreset);
        packetBufferSender.writeString(type);
    }

    @Override
    public void runServer(MinecraftServer minecraftServer, ServerPlayerEntity serverPlayerEntity) {
        minecraftServer.submit(() -> PSDServerBindings.handleBind(minecraftServer, serverPlayerEntity, x, y, z, scriptId, pidsPreset, type));
    }
}
