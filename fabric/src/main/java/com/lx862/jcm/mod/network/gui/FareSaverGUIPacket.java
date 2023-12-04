package com.lx862.jcm.mod.network.gui;

import com.lx862.jcm.mod.render.screen.FareSaverScreen;
import org.mtr.mapping.holder.BlockPos;
import org.mtr.mapping.holder.MinecraftClient;
import org.mtr.mapping.holder.PacketBuffer;
import org.mtr.mapping.holder.Screen;
import org.mtr.mapping.registry.PacketHandler;

public class FareSaverGUIPacket extends PacketHandler {
    private final BlockPos blockPos;
    private final int discount;

    public FareSaverGUIPacket(PacketBuffer packetBuffer) {
        this.blockPos = packetBuffer.readBlockPos();
        this.discount = packetBuffer.readInt();
    }

    public FareSaverGUIPacket(BlockPos blockPos, int discount) {
        this.blockPos = blockPos;
        this.discount = discount;
    }

    @Override
    public void write(PacketBuffer packetBuffer) {
        packetBuffer.writeBlockPos(blockPos);
        packetBuffer.writeVarInt(discount);
    }

    @Override
    public void runClientQueued() {
        MinecraftClient.getInstance().openScreen(new Screen(new FareSaverScreen(blockPos, discount)));
    }
}
