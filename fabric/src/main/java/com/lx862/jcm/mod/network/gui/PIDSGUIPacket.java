package com.lx862.jcm.mod.network.gui;

import com.lx862.jcm.mod.render.gui.screen.PIDSScreen;
import org.mtr.mapping.holder.BlockPos;
import org.mtr.mapping.holder.MinecraftClient;
import org.mtr.mapping.holder.PacketBuffer;
import org.mtr.mapping.holder.Screen;
import org.mtr.mapping.registry.PacketHandler;
import org.mtr.mapping.tool.PacketBufferReceiver;
import org.mtr.mapping.tool.PacketBufferSender;

public class PIDSGUIPacket extends PacketHandler {
    private final BlockPos blockPos;
    private final String[] customMessages;
    private final boolean[] rowHidden;
    private final boolean hidePlatformNumber;
    private final String presetId;

    public PIDSGUIPacket(PacketBufferReceiver packetBufferReceiver) {
        this.blockPos = BlockPos.fromLong(packetBufferReceiver.readLong());
        int rows = packetBufferReceiver.readInt();
        this.customMessages = new String[rows];
        this.rowHidden = new boolean[rows];

        for(int i = 0; i < rows; i++) {
            this.customMessages[i] = packetBufferReceiver.readString();
        }
        for(int i = 0; i < rows; i++) {
            this.rowHidden[i] = packetBufferReceiver.readBoolean();
        }

        this.hidePlatformNumber = packetBufferReceiver.readBoolean();
        this.presetId = packetBufferReceiver.readString();
    }

    public PIDSGUIPacket(BlockPos blockPos, String[] customMessages, boolean[] rowHidden, boolean hidePlatformNumber, String presetId) {
        this.blockPos = blockPos;
        this.customMessages = customMessages;
        this.rowHidden = rowHidden;
        this.hidePlatformNumber = hidePlatformNumber;
        this.presetId = presetId;
    }

    @Override
    public void write(PacketBufferSender packetBufferSender) {
        packetBufferSender.writeLong(blockPos.asLong());
        packetBufferSender.writeInt(customMessages.length);

        for(String customMessage : customMessages) {
            packetBufferSender.writeString(customMessage == null ? "" : customMessage);
        }
        for(boolean hidePlatform : rowHidden) {
            packetBufferSender.writeBoolean(hidePlatform);
        }

        packetBufferSender.writeBoolean(hidePlatformNumber);
        packetBufferSender.writeString(presetId);
    }

    @Override
    public void runClient() {
        MinecraftClient.getInstance().openScreen(new Screen(new PIDSScreen(blockPos, customMessages, rowHidden, hidePlatformNumber, presetId)));
    }
}
