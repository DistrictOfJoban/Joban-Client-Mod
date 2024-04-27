package com.lx862.jcm.mod.network.gui;

import org.mtr.mapping.holder.BlockPos;
import org.mtr.mapping.registry.PacketHandler;
import org.mtr.mapping.tool.PacketBufferReceiver;
import org.mtr.mapping.tool.PacketBufferSender;

public class FareSaverGUIPacket extends PacketHandler {
    private final BlockPos blockPos;
    private final String currency;
    private final int discount;

    public FareSaverGUIPacket(PacketBufferReceiver packetBufferReceiver) {
        this.blockPos = BlockPos.fromLong(packetBufferReceiver.readLong());
        this.currency = packetBufferReceiver.readString();
        this.discount = packetBufferReceiver.readInt();
    }

    public FareSaverGUIPacket(BlockPos blockPos, String currency, int discount) {
        this.blockPos = blockPos;
        this.currency = currency;
        this.discount = discount;
    }

    @Override
    public void write(PacketBufferSender packetBufferSender) {
        packetBufferSender.writeLong(blockPos.asLong());
        packetBufferSender.writeString(currency);
        packetBufferSender.writeInt(discount);
    }

    @Override
    public void runClient() {
        ClientHelper.openFareSaverGUIScreen(blockPos, currency, discount);
    }
}
