package com.lx862.mtrscripting.mod.network.packet.psd;

import com.lx862.jcm.mod.scripting.psd.PSDBinding;
import com.lx862.jcm.mod.scripting.psd.PSDClientBindings;
import com.lx862.jcm.mod.scripting.psd.PSDWrapper;
import org.mtr.mapping.registry.PacketHandler;
import org.mtr.mapping.tool.PacketBufferReceiver;
import org.mtr.mapping.tool.PacketBufferSender;

import java.util.ArrayList;
import java.util.List;

/**
 * Server → client: the full PSD script binding list of one dimension — the
 * answer to {@link RequestPSDScriptsC2SPacket}, and the broadcast after any
 * player binds or unbinds a PSD script (sent to every player currently in
 * that dimension). The client drops answers that do not match its current
 * dimension.
 */
public class PSDScriptsS2CPacket extends PacketHandler {
    private final String dimension;
    private final List<PSDBinding> bindings;

    public PSDScriptsS2CPacket(PacketBufferReceiver packetBufferReceiver) {
        this.dimension = packetBufferReceiver.readString();
        final int size = packetBufferReceiver.readInt();
        // MTR's PacketBufferSender fragments large logical messages itself.
        // Do not cap legitimate whole-dimension snapshots by a transport-frame
        // count. Grow only as entries are decoded, not from an untrusted size.
        if (size < 0) throw new IllegalArgumentException("Invalid PSD binding count: " + size);
        this.bindings = new ArrayList<>(Math.min(size, 1024));
        for (int i = 0; i < size; i++) {
            final int x = packetBufferReceiver.readInt();
            final int y = packetBufferReceiver.readInt();
            final int z = packetBufferReceiver.readInt();
            final String scriptId = packetBufferReceiver.readString();
            final boolean pidsPreset = packetBufferReceiver.readBoolean();
            final String type = packetBufferReceiver.readString();
            this.bindings.add(new PSDBinding(new org.mtr.mapping.holder.BlockPos(x, y, z), scriptId, pidsPreset, PSDWrapper.PSDType.valueOf(type)));
        }
    }

    public PSDScriptsS2CPacket(String dimension, List<PSDBinding> bindings) {
        this.dimension = dimension;
        this.bindings = bindings;
    }

    @Override
    public void write(PacketBufferSender packetBufferSender) {
        packetBufferSender.writeString(dimension);
        packetBufferSender.writeInt(bindings.size());
        for (PSDBinding binding : bindings) {
            packetBufferSender.writeInt((int) binding.pos.getX());
            packetBufferSender.writeInt((int) binding.pos.getY());
            packetBufferSender.writeInt((int) binding.pos.getZ());
            packetBufferSender.writeString(binding.scriptId);
            packetBufferSender.writeBoolean(binding.pidsPreset);
            packetBufferSender.writeString(binding.type.name());
        }
    }

    @Override
    public void runClient() {
        PSDClientBindings.acceptAll(dimension, bindings);
    }
}
