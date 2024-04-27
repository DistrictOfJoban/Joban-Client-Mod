package com.lx862.jcm.mod.network.gui;

import com.lx862.jcm.mod.network.JCMPacketHandlerHelper;
import org.mtr.mapping.holder.BlockPos;
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
        JCMPacketHandlerHelper.readArray(packetBufferReceiver, i -> this.customMessages[i] = packetBufferReceiver.readString());
        JCMPacketHandlerHelper.readArray(packetBufferReceiver, i -> this.rowHidden[i] = packetBufferReceiver.readBoolean());
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
        JCMPacketHandlerHelper.writeArray(packetBufferSender, customMessages, i -> {
            String str = customMessages[i] == null ? "" : customMessages[i];
            packetBufferSender.writeString(str);
        });
        JCMPacketHandlerHelper.writeArray(packetBufferSender, rowHidden, i -> packetBufferSender.writeBoolean(rowHidden[i]));
        packetBufferSender.writeBoolean(hidePlatformNumber);
        packetBufferSender.writeString(presetId);
    }

    @Override
    public void runClient() {
        ClientHelper.openPIDSGUIScreen(blockPos, customMessages, rowHidden, hidePlatformNumber, presetId);
    }
}
