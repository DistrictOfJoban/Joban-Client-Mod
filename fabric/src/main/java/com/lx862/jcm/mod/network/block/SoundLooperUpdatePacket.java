package com.lx862.jcm.mod.network.block;

import com.lx862.jcm.mod.block.base.JCMBlock;
import com.lx862.jcm.mod.block.entity.SoundLooperBlockEntity;
import com.lx862.jcm.mod.util.BlockUtil;
import org.mtr.mapping.holder.*;
import org.mtr.mapping.registry.PacketHandler;
import org.mtr.mapping.tool.PacketBufferReceiver;
import org.mtr.mapping.tool.PacketBufferSender;

public class SoundLooperUpdatePacket extends PacketHandler {
    private final BlockPos blockPos;
    private final BlockPos pos1;
    private final BlockPos pos2;
    private final String soundId;
    private final int soundCategory;
    private final int interval;
    private final float volume;
    private final boolean needRedstone;
    private final boolean limitRange;

    public SoundLooperUpdatePacket(PacketBufferReceiver packetBufferReceiver) {
        this.blockPos = BlockPos.fromLong(packetBufferReceiver.readLong());
        this.pos1 = BlockPos.fromLong(packetBufferReceiver.readLong());
        this.pos2 = BlockPos.fromLong(packetBufferReceiver.readLong());
        this.soundId = packetBufferReceiver.readString();
        this.soundCategory = packetBufferReceiver.readInt();
        this.interval = packetBufferReceiver.readInt();
        this.volume = packetBufferReceiver.readFloat();
        this.needRedstone = packetBufferReceiver.readBoolean();
        this.limitRange = packetBufferReceiver.readBoolean();
    }

    public SoundLooperUpdatePacket(BlockPos blockPos, BlockPos pos1, BlockPos pos2, String soundId, int soundCategory, int interval, float volume, boolean needRedstone, boolean limitRange) {
        this.blockPos = blockPos;
        this.pos1 = pos1;
        this.pos2 = pos2;
        this.soundId = soundId;
        this.soundCategory = soundCategory;
        this.interval = interval;
        this.volume = volume;
        this.needRedstone = needRedstone;
        this.limitRange = limitRange;
    }

    @Override
    public void runServer(MinecraftServer minecraftServer, ServerPlayerEntity serverPlayerEntity) {
        World world = serverPlayerEntity.getEntityWorld();
        BlockState state = BlockUtil.getBlockState(world, blockPos);
        if(state == null || !(state.getBlock().data instanceof JCMBlock)) return;

        ((JCMBlock)state.getBlock().data).forEachBlockEntity(state, world, blockPos, be -> {
            if(be.data instanceof SoundLooperBlockEntity) {
                ((SoundLooperBlockEntity)be.data).setData(soundId, soundCategory, interval, volume, needRedstone, limitRange, pos1, pos2);
            }
        });
    }

    @Override
    public void write(PacketBufferSender packetBufferSender) {
        packetBufferSender.writeLong(blockPos.asLong());
        packetBufferSender.writeLong(pos1.asLong());
        packetBufferSender.writeLong(pos2.asLong());
        packetBufferSender.writeString(soundId);
        packetBufferSender.writeInt(soundCategory);
        packetBufferSender.writeInt(interval);
        packetBufferSender.writeFloat(volume);
        packetBufferSender.writeBoolean(needRedstone);
        packetBufferSender.writeBoolean(limitRange);
    }
}
