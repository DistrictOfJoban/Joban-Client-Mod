package com.lx862.jcm.mod.network.gui;

import com.lx862.jcm.mod.render.screen.ButterflyLightScreen;
import org.mtr.mapping.holder.BlockPos;
import org.mtr.mapping.holder.MinecraftClient;
import org.mtr.mapping.holder.PacketBuffer;
import org.mtr.mapping.holder.Screen;
import org.mtr.mapping.registry.PacketHandler;

public class ButterflyLightGUIPacket extends PacketHandler {
    private final BlockPos blockPos;
    private final int secondsToBlink;

    public ButterflyLightGUIPacket(PacketBuffer packetBuffer) {
        this.blockPos = packetBuffer.readBlockPos();
        this.secondsToBlink = packetBuffer.readInt();
    }

    public ButterflyLightGUIPacket(BlockPos blockPos, int secondsToBlink) {
        this.blockPos = blockPos;
        this.secondsToBlink = secondsToBlink;
    }

    @Override
    public void write(PacketBuffer packetBuffer) {
        packetBuffer.writeBlockPos(blockPos);
        packetBuffer.writeInt(secondsToBlink);
    }

    @Override
    public void runClientQueued() {
        MinecraftClient.getInstance().openScreen(new Screen(new ButterflyLightScreen(blockPos, secondsToBlink)));
    }
}
