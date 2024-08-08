package com.lx862.jcm.mod.network.block;

import com.lx862.jcm.mod.block.base.JCMBlock;
import com.lx862.jcm.mod.block.entity.PIDSBlockEntity;
import com.lx862.jcm.mod.network.JCMPacketHandlerHelper;
import com.lx862.jcm.mod.util.BlockUtil;
import org.mtr.libraries.it.unimi.dsi.fastutil.longs.LongAVLTreeSet;
import org.mtr.mapping.holder.*;
import org.mtr.mapping.registry.PacketHandler;
import org.mtr.mapping.tool.PacketBufferReceiver;
import org.mtr.mapping.tool.PacketBufferSender;

public class PIDSUpdatePacket extends PacketHandler {
    protected final BlockPos blockPos;
    protected final LongAVLTreeSet filteredPlatforms;
    protected final String[] customMessages;
    protected final boolean[] rowHidden;
    protected final boolean hidePlatformNumber;
    protected final String presetId;

    public PIDSUpdatePacket(PacketBufferReceiver packetBufferReceiver) {
        this.blockPos = BlockPos.fromLong(packetBufferReceiver.readLong());
        int rows = packetBufferReceiver.readInt();
        this.customMessages = new String[rows];
        this.rowHidden = new boolean[rows];
        this.filteredPlatforms = new LongAVLTreeSet();

        JCMPacketHandlerHelper.readArray(packetBufferReceiver, i -> this.customMessages[i] = packetBufferReceiver.readString());
        JCMPacketHandlerHelper.readArray(packetBufferReceiver, i -> this.rowHidden[i] = packetBufferReceiver.readBoolean());
        JCMPacketHandlerHelper.readArray(packetBufferReceiver, i -> filteredPlatforms.add(packetBufferReceiver.readLong()));

        this.hidePlatformNumber = packetBufferReceiver.readBoolean();
        this.presetId = packetBufferReceiver.readString();
    }

    public PIDSUpdatePacket(BlockPos blockPos, LongAVLTreeSet filteredPlatforms, String[] customMessages, boolean[] rowHidden, boolean hidePlatformNumber, String pidsPreset) {
        this.blockPos = blockPos;
        this.customMessages = customMessages;
        this.rowHidden = rowHidden;
        this.presetId = pidsPreset;
        this.hidePlatformNumber = hidePlatformNumber;
        this.filteredPlatforms = filteredPlatforms;
    }

    @Override
    public void write(PacketBufferSender packetBufferSender) {
        packetBufferSender.writeLong(blockPos.asLong());
        packetBufferSender.writeInt(customMessages.length);
        JCMPacketHandlerHelper.writeArray(packetBufferSender, customMessages, i -> packetBufferSender.writeString(customMessages[i]));
        JCMPacketHandlerHelper.writeArray(packetBufferSender, rowHidden, i -> packetBufferSender.writeBoolean(rowHidden[i]));
        JCMPacketHandlerHelper.writeArray(packetBufferSender, filteredPlatforms, packetBufferSender::writeLong);
        packetBufferSender.writeBoolean(hidePlatformNumber);
        packetBufferSender.writeString(presetId);
    }

    @Override
    public void runServer(MinecraftServer minecraftServer, ServerPlayerEntity serverPlayerEntity) {
        World world = serverPlayerEntity.getEntityWorld();
        BlockState state = BlockUtil.getBlockState(world, blockPos);

        if(state == null || !(state.getBlock().data instanceof JCMBlock)) return;

        ((JCMBlock)state.getBlock().data).loopStructure(state, world, blockPos, (bs, be) -> {
            if(be.data instanceof PIDSBlockEntity) {
                ((PIDSBlockEntity)be.data).setData(customMessages, filteredPlatforms, rowHidden, hidePlatformNumber, presetId);
            }
        });
    }
}