package com.lx862.jcm.mod.network.gui;

import org.mtr.mapping.holder.BlockPos;
import org.mtr.mapping.registry.PacketHandler;
import org.mtr.mapping.tool.PacketBufferReceiver;
import org.mtr.mapping.tool.PacketBufferSender;

public class FareSaverGUIPacket extends PacketHandler {
    private final BlockPos blockPos;
    private final String prefix;
    private final int discount;

    public FareSaverGUIPacket(PacketBufferReceiver packetBufferReceiver) {
        this.blockPos = BlockPos.fromLong(packetBufferReceiver.readLong());
        this.prefix = packetBufferReceiver.readString();
        this.discount = packetBufferReceiver.readInt();
    }

    public FareSaverGUIPacket(BlockPos blockPos, String prefix, int discount) {
        this.blockPos = blockPos;
        this.prefix = prefix;
        this.discount = discount;
    }

    @Override
    public void write(PacketBufferSender packetBufferSender) {
        packetBufferSender.writeLong(blockPos.asLong());
        packetBufferSender.writeString(prefix);
        packetBufferSender.writeInt(discount);
    }

    @Override
    public void runClient() {
        ClientHelper.openFareSaverGUIScreen(blockPos, prefix, discount);
    }
}
