package com.lx862.jcm.mod.network.gui;

import com.lx862.jcm.mod.network.JCMPacketHandlerHelper;
import org.mtr.mapping.holder.BlockPos;
import org.mtr.mapping.registry.PacketHandler;
import org.mtr.mapping.tool.PacketBufferReceiver;
import org.mtr.mapping.tool.PacketBufferSender;

public class PIDSProjectorGUIPacket extends PIDSGUIPacket {
    private final float x1;
    private final float y1;
    private final float z1;
    private final float scale;

    public PIDSProjectorGUIPacket(PacketBufferReceiver packetBufferReceiver) {
        super(packetBufferReceiver);
        this.x1 = packetBufferReceiver.readFloat();
        this.y1 = packetBufferReceiver.readFloat();
        this.z1 = packetBufferReceiver.readFloat();
        this.scale = packetBufferReceiver.readFloat();
    }

    public PIDSProjectorGUIPacket(BlockPos blockPos, String[] customMessages, boolean[] rowHidden, boolean hidePlatformNumber, String presetId, float x1, float y1, float z1, float scale) {
        super(blockPos, customMessages, rowHidden, hidePlatformNumber, presetId);
        this.x1 = x1;
        this.y1 = y1;
        this.z1 = z1;
        this.scale = scale;
    }

    @Override
    public void write(PacketBufferSender packetBufferSender) {
        super.write(packetBufferSender);
        packetBufferSender.writeFloat(x1);
        packetBufferSender.writeFloat(y1);
        packetBufferSender.writeFloat(z1);
        packetBufferSender.writeFloat(scale);
    }

    @Override
    public void runClient() {
        ClientHelper.openPIDSGUIScreen(blockPos, customMessages, rowHidden, hidePlatformNumber, presetId, x1, y1, z1, scale);
    }
}
