package com.lx862.jcm.mod.network.gui;

import com.lx862.jcm.mod.render.screen.PIDSScreen;
import org.mtr.mapping.holder.BlockPos;
import org.mtr.mapping.holder.MinecraftClient;
import org.mtr.mapping.holder.PacketBuffer;
import org.mtr.mapping.holder.Screen;
import org.mtr.mapping.registry.PacketHandler;

public class PIDSGUIPacket extends PacketHandler {
    private final BlockPos blockPos;
    private final String[] customMessages;
    private final boolean[] rowHidden;
    private final boolean hidePlatformNumber;
    private final String presetId;

    public PIDSGUIPacket(PacketBuffer packetBuffer) {
        this.blockPos = packetBuffer.readBlockPos();
        int rows = packetBuffer.readVarInt();
        this.customMessages = new String[rows];
        this.rowHidden = new boolean[rows];

        for(int i = 0; i < rows; i++) {
            this.customMessages[i] = packetBuffer.readString();
        }
        for(int i = 0; i < rows; i++) {
            this.rowHidden[i] = packetBuffer.readBoolean();
        }

        this.hidePlatformNumber = packetBuffer.readBoolean();
        this.presetId = packetBuffer.readString();
    }

    public PIDSGUIPacket(BlockPos blockPos, String[] customMessages, boolean[] rowHidden, boolean hidePlatformNumber, String presetId) {
        this.blockPos = blockPos;
        this.customMessages = customMessages;
        this.rowHidden = rowHidden;
        this.hidePlatformNumber = hidePlatformNumber;
        this.presetId = presetId;
    }

    @Override
    public void write(PacketBuffer packetBuffer) {
        packetBuffer.writeBlockPos(blockPos);
        packetBuffer.writeVarInt(customMessages.length);

        for(String customMessage : customMessages) {
            packetBuffer.writeString(customMessage == null ? "" : customMessage);
        }
        for(boolean hidePlatform : rowHidden) {
            packetBuffer.writeBoolean(hidePlatform);
        }

        packetBuffer.writeBoolean(hidePlatformNumber);
        packetBuffer.writeString(presetId);
    }

    @Override
    public void runClientQueued() {
        MinecraftClient.getInstance().openScreen(new Screen(new PIDSScreen(blockPos, customMessages, rowHidden, hidePlatformNumber, presetId)));
    }
}
