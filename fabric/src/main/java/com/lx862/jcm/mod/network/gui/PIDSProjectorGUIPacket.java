package com.lx862.jcm.mod.network.gui;

import org.mtr.mapping.holder.BlockPos;
import org.mtr.mapping.tool.PacketBufferReceiver;
import org.mtr.mapping.tool.PacketBufferSender;

public class PIDSProjectorGUIPacket extends PIDSGUIPacket {
    private final double x1;
    private final double y1;
    private final double z1;
    private final double rotateX;
    private final double rotateY;
    private final double rotateZ;
    private final double scale;

    public PIDSProjectorGUIPacket(PacketBufferReceiver packetBufferReceiver) {
        super(packetBufferReceiver);
        this.x1 = packetBufferReceiver.readDouble();
        this.y1 = packetBufferReceiver.readDouble();
        this.z1 = packetBufferReceiver.readDouble();
        this.rotateX = packetBufferReceiver.readDouble();
        this.rotateY = packetBufferReceiver.readDouble();
        this.rotateZ = packetBufferReceiver.readDouble();
        this.scale = packetBufferReceiver.readDouble();
    }

    public PIDSProjectorGUIPacket(BlockPos blockPos, String[] customMessages, boolean[] rowHidden, boolean hidePlatformNumber, String presetId, double x1, double y1, double z1, double rotateX, double rotateY, double rotateZ, double scale) {
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
        packetBufferSender.writeDouble(x1);
        packetBufferSender.writeDouble(y1);
        packetBufferSender.writeDouble(z1);
        packetBufferSender.writeDouble(rotateX);
        packetBufferSender.writeDouble(rotateY);
        packetBufferSender.writeDouble(rotateZ);
        packetBufferSender.writeDouble(scale);
    }

    @Override
    public void runClient() {
        ClientHelper.openPIDSProjectorGUIScreen(blockPos, customMessages, rowHidden, hidePlatformNumber, presetId, x1, y1, z1, rotateX, rotateY, rotateZ, scale);
    }
}
