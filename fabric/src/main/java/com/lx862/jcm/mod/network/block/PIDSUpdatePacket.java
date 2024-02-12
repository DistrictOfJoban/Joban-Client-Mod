package com.lx862.jcm.mod.network.block;

import com.lx862.jcm.mod.block.JCMPIDSBlock;
import com.lx862.jcm.mod.block.RVPIDSBlock;
import com.lx862.jcm.mod.block.base.Horizontal2MirroredBlock;
import com.lx862.jcm.mod.block.base.JCMBlock;
import com.lx862.jcm.mod.block.entity.PIDSBlockEntity;
import com.lx862.jcm.mod.data.BlockProperties;
import com.lx862.jcm.mod.util.BlockUtil;
import org.mtr.mapping.holder.*;
import org.mtr.mapping.registry.PacketHandler;
import org.mtr.mapping.tool.PacketBufferReceiver;
import org.mtr.mapping.tool.PacketBufferSender;

public class PIDSUpdatePacket extends PacketHandler {
    private final BlockPos blockPos;
    private final String[] customMessages;
    private final boolean[] rowHidden;
    private final boolean hidePlatformNumber;
    private final String presetId;

    public PIDSUpdatePacket(PacketBufferReceiver packetBufferReceiver) {
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

    public PIDSUpdatePacket(BlockPos blockPos, String[] customMessages, boolean[] rowHidden, boolean hidePlatformNumber, String pidsPreset) {
        this.blockPos = blockPos;
        this.customMessages = customMessages;
        this.rowHidden = rowHidden;
        this.presetId = pidsPreset;
        this.hidePlatformNumber = hidePlatformNumber;
    }

    @Override
    public void write(PacketBufferSender packetBufferSender) {
        packetBufferSender.writeLong(blockPos.asLong());
        packetBufferSender.writeInt(customMessages.length);

        for(String customMessage : customMessages) {
            packetBufferSender.writeString(customMessage);
        }
        for(boolean hidePlatform : rowHidden) {
            packetBufferSender.writeBoolean(hidePlatform);
        }

        packetBufferSender.writeBoolean(hidePlatformNumber);
        packetBufferSender.writeString(presetId);
    }

    @Override
    public void runServer(MinecraftServer minecraftServer, ServerPlayerEntity serverPlayerEntity) {
        World world = serverPlayerEntity.getEntityWorld();
        BlockState state = BlockUtil.getBlockState(world, blockPos);

        if(state == null || !(state.getBlock().data instanceof JCMBlock)) return;

        ((JCMBlock)state.getBlock().data).forEachBlockEntity(state, world, blockPos, be -> {
            if(be.data instanceof PIDSBlockEntity) {
                ((PIDSBlockEntity)be.data).setData(customMessages, rowHidden, hidePlatformNumber, presetId);
            }
        });
    }
}