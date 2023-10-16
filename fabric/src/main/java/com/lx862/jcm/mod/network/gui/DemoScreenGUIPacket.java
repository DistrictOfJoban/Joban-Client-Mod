package com.lx862.jcm.mod.network.gui;

import com.lx862.jcm.mod.gui.DemoScreen;
import org.mtr.mapping.holder.MinecraftClient;
import org.mtr.mapping.holder.PacketBuffer;
import org.mtr.mapping.holder.Screen;
import org.mtr.mapping.registry.PacketHandler;

public class DemoScreenGUIPacket extends PacketHandler {

    public DemoScreenGUIPacket(PacketBuffer packetBuffer) {
    }

    public DemoScreenGUIPacket() {
    }

    @Override
    public void write(PacketBuffer packetBuffer) {
    }

    @Override
    public void runClientQueued() {
        MinecraftClient.getInstance().openScreen(new Screen(new DemoScreen()));
    }
}
