package com.lx862.jcm.mod.network.gui;

import org.mtr.mapping.holder.BlockPos;
import org.mtr.mapping.tool.PacketBufferReceiver;
import org.mtr.mapping.tool.PacketBufferSender;

public class PIDSProjectorGUIPacket extends PIDSGUIPacket {
    private final float x1;
    private final float y1;
    private final float z1;
    private final float rotateX;
    private final float rotateY;
    private final float rotateZ;
    private final float scale;

    public PIDSProjectorGUIPacket(PacketBufferReceiver packetBufferReceiver) {
        super(packetBufferReceiver);
        this.x1 = packetBufferReceiver.readFloat();
        this.y1 = packetBufferReceiver.readFloat();
        this.z1 = packetBufferReceiver.readFloat();
        this.rotateX = packetBufferReceiver.readFloat();
        this.rotateY = packetBufferReceiver.readFloat();
        this.rotateZ = packetBufferReceiver.readFloat();
        this.scale = packetBufferReceiver.readFloat();
    }

    public PIDSProjectorGUIPacket(BlockPos blockPos, String[] customMessages, boolean[] rowHidden, boolean hidePlatformNumber, String presetId, float x1, float y1, float z1, float rotateX, float rotateY, float rotateZ, float scale) {
        super(blockPos, customMessages, rowHidden, hidePlatformNumber, presetId);
        this.x1 = x1;
        this.y1 = y1;
        this.z1 = z1;
        this.rotateX = rotateX;
        this.rotateY = rotateY;
        this.rotateZ = rotateZ;
        this.scale = scale;
    }

    @Override
    public void write(PacketBufferSender packetBufferSender) {
        super.write(packetBufferSender);
        packetBufferSender.writeFloat(x1);
        packetBufferSender.writeFloat(y1);
        packetBufferSender.writeFloat(z1);
        packetBufferSender.writeFloat(rotateX);
        packetBufferSender.writeFloat(rotateY);
        packetBufferSender.writeFloat(rotateZ);
        packetBufferSender.writeFloat(scale);
    }

    @Override
    public void runClient() {
        ClientHelper.openPIDSProjectorGUIScreen(blockPos, customMessages, rowHidden, hidePlatformNumber, presetId, x1, y1, z1, rotateX, rotateY, rotateZ, scale);
    }
}
