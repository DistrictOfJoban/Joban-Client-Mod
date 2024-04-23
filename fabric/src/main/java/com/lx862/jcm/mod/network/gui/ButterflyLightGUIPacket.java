package com.lx862.jcm.mod.network.gui;

import com.lx862.jcm.mod.render.gui.screen.ButterflyLightScreen;
import org.mtr.mapping.holder.BlockPos;
import org.mtr.mapping.holder.MinecraftClient;
import org.mtr.mapping.holder.Screen;
import org.mtr.mapping.registry.PacketHandler;
import org.mtr.mapping.tool.PacketBufferReceiver;
import org.mtr.mapping.tool.PacketBufferSender;

public class ButterflyLightGUIPacket extends PacketHandler {
    private final BlockPos blockPos;
    private final int secondsToBlink;

    public ButterflyLightGUIPacket(PacketBufferReceiver packetBufferReceiver) {
        this.blockPos = BlockPos.fromLong(packetBufferReceiver.readLong());
        this.secondsToBlink = packetBufferReceiver.readInt();
    }

    public ButterflyLightGUIPacket(BlockPos blockPos, int secondsToBlink) {
        this.blockPos = blockPos;
        this.secondsToBlink = secondsToBlink;
    }

    @Override
    public void write(PacketBufferSender packetBufferSender) {
        packetBufferSender.writeLong(blockPos.asLong());
        packetBufferSender.writeInt(secondsToBlink);
    }

    @Override
    public void runClient() {
        ClientHelper.openButterflyLightScreen(blockPos, secondsToBlink);
    }
}
