package com.lx862.mtrscripting.mod.network.packet;

import com.lx862.mtrscripting.mod.impl.mtr.eyecandy.config.JCMBlockEyecandyExtra;
import com.lx862.jcm.mod.network.PacketValidator;
import com.lx862.jcm.mod.util.BlockUtil;
import com.lx862.jcm.mod.util.JCMLogger;
import org.mtr.mapping.holder.*;
import org.mtr.mapping.registry.PacketHandler;
import org.mtr.mapping.tool.PacketBufferReceiver;
import org.mtr.mapping.tool.PacketBufferSender;
import org.mtr.mod.block.BlockEyeCandy;

import java.util.HashMap;
import java.util.Map;

public class EyecandyCustomConfigUpdateC2SPacket extends PacketHandler {
    private final Map<String, String> entries;
    private final BlockPos blockPos;
    private final String modelId;

    public EyecandyCustomConfigUpdateC2SPacket(PacketBufferReceiver packetBufferReceiver) {
        this.entries = new HashMap<>();
        this.modelId = packetBufferReceiver.readString();
        this.blockPos = BlockPos.fromLong(packetBufferReceiver.readLong());
        int size = packetBufferReceiver.readInt();

        for(int i = 0; i < size; i++) {
            String key = packetBufferReceiver.readString();
            String value = packetBufferReceiver.readString();
            entries.put(key, value);
        }
    }

    public EyecandyCustomConfigUpdateC2SPacket(String modelId, BlockPos blockPos, Map<String, String> entries) {
        this.modelId = modelId;
        this.blockPos = blockPos;
        this.entries = new HashMap<>(entries);
    }

    @Override
    public void write(PacketBufferSender packetBufferSender) {
        packetBufferSender.writeString(modelId);
        packetBufferSender.writeLong(blockPos.asLong());

        int size = entries.size();
        packetBufferSender.writeInt(size);

        for(Map.Entry<String, String> entry : entries.entrySet()) {
            packetBufferSender.writeString(entry.getKey());
            packetBufferSender.writeString(entry.getValue());
        }
    }

    @Override
    public void runServer(MinecraftServer minecraftServer, ServerPlayerEntity serverPlayerEntity) {
        World world = serverPlayerEntity.getEntityWorld();
        BlockEntity be = BlockUtil.getBlockEntityOrNull(world, blockPos);
        if(be == null || !(be.data instanceof BlockEyeCandy.BlockEntity)) return;

        if(!PacketValidator.canConfigureBlock(serverPlayerEntity)) {
            JCMLogger.warn("Player {} attempted to write Eyecandy custom config at {} {} {} without permission.", serverPlayerEntity.getGameProfile().getName(), blockPos.getX(), blockPos.getY(), blockPos.getZ());
            return;
        }

        BlockEyeCandy.BlockEntity eyecandyBE = (BlockEyeCandy.BlockEntity)be.data;
        ((JCMBlockEyecandyExtra)eyecandyBE).jsblock$updateCustomConfig(this.entries);
    }
}
