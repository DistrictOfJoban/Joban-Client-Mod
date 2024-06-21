package com.lx862.jcm.mod.network.gui;

import org.mtr.mapping.holder.BlockPos;
import org.mtr.mapping.registry.PacketHandler;
import org.mtr.mapping.tool.PacketBufferReceiver;
import org.mtr.mapping.tool.PacketBufferSender;

public class SoundLooperGUIPacket extends PacketHandler {
    private final BlockPos blockPos;
    private final BlockPos corner1;
    private final BlockPos corner2;
    private final String soundId;
    private final int soundCategory;
    private final int interval;
    private final float soundVolume;
    private final boolean needRedstone;
    private final boolean limitRange;

    public SoundLooperGUIPacket(PacketBufferReceiver packetBufferReceiver) {
        this.blockPos = BlockPos.fromLong(packetBufferReceiver.readLong());
        this.corner1 = BlockPos.fromLong(packetBufferReceiver.readLong());
        this.corner2 = BlockPos.fromLong(packetBufferReceiver.readLong());
        this.soundId = packetBufferReceiver.readString();
        this.soundCategory = packetBufferReceiver.readInt();
        this.interval = packetBufferReceiver.readInt();
        this.soundVolume = packetBufferReceiver.readFloat();
        this.needRedstone = packetBufferReceiver.readBoolean();
        this.limitRange = packetBufferReceiver.readBoolean();
    }

    public SoundLooperGUIPacket(BlockPos blockPos, BlockPos corner1, BlockPos corner2, String soundId, int soundCategory, int interval, float soundVolume, boolean needRedstone, boolean limitRange) {
        this.blockPos = blockPos;
        this.corner1 = corner1;
        this.corner2 = corner2;
        this.soundId = soundId;
        this.soundCategory = soundCategory;
        this.interval = interval;
        this.soundVolume = soundVolume;
        this.needRedstone = needRedstone;
        this.limitRange = limitRange;
    }

    @Override
    public void runClient() {
        ClientHelper.openSoundLooperGUIScreen(blockPos, corner1, corner2, soundId, soundCategory, soundVolume, interval, needRedstone, limitRange);
    }

    @Override
    public void write(PacketBufferSender packetBufferSender) {
        packetBufferSender.writeLong(blockPos.asLong());
        packetBufferSender.writeLong(corner1.asLong());
        packetBufferSender.writeLong(corner2.asLong());
        packetBufferSender.writeString(soundId);
        packetBufferSender.writeInt(soundCategory);
        packetBufferSender.writeInt(interval);
        packetBufferSender.writeFloat(soundVolume);
        packetBufferSender.writeBoolean(needRedstone);
        packetBufferSender.writeBoolean(limitRange);
    }
}
