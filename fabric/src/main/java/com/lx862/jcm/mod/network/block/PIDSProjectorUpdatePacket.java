package com.lx862.jcm.mod.network.block;

import com.lx862.jcm.mod.block.base.JCMBlock;
import com.lx862.jcm.mod.block.entity.PIDSBlockEntity;
import com.lx862.jcm.mod.block.entity.PIDSProjectorBlockEntity;
import com.lx862.jcm.mod.network.JCMPacketHandlerHelper;
import com.lx862.jcm.mod.util.BlockUtil;
import org.mtr.libraries.it.unimi.dsi.fastutil.longs.LongAVLTreeSet;
import org.mtr.mapping.holder.*;
import org.mtr.mapping.registry.PacketHandler;
import org.mtr.mapping.tool.PacketBufferReceiver;
import org.mtr.mapping.tool.PacketBufferSender;

public class PIDSProjectorUpdatePacket extends PIDSUpdatePacket {
    private final float x;
    private final float y;
    private final float z;
    private final float scale;

    public PIDSProjectorUpdatePacket(PacketBufferReceiver packetBufferReceiver) {
        super(packetBufferReceiver);
        this.x = packetBufferReceiver.readFloat();
        this.y = packetBufferReceiver.readFloat();
        this.z = packetBufferReceiver.readFloat();
        this.scale = packetBufferReceiver.readFloat();
    }

    public PIDSProjectorUpdatePacket(BlockPos blockPos, LongAVLTreeSet filteredPlatforms, String[] customMessages, boolean[] rowHidden, boolean hidePlatformNumber, String pidsPreset, float x, float y, float z, float scale) {
        super(blockPos, filteredPlatforms, customMessages, rowHidden, hidePlatformNumber, pidsPreset);
        this.x = x;
        this.y = y;
        this.z = z;
        this.scale = scale;
    }

    @Override
    public void write(PacketBufferSender packetBufferSender) {
        super.write(packetBufferSender);
        packetBufferSender.writeFloat(x);
        packetBufferSender.writeFloat(y);
        packetBufferSender.writeFloat(z);
        packetBufferSender.writeFloat(scale);
    }

    @Override
    public void runServer(MinecraftServer minecraftServer, ServerPlayerEntity serverPlayerEntity) {
        super.runServer(minecraftServer, serverPlayerEntity);

        World world = serverPlayerEntity.getEntityWorld();
        BlockState state = BlockUtil.getBlockState(world, blockPos);

        ((JCMBlock)state.getBlock().data).loopStructure(state, world, blockPos, (bs, be) -> {
            if(be.data instanceof PIDSProjectorBlockEntity) {
                ((PIDSProjectorBlockEntity)be.data).setData(customMessages, filteredPlatforms, rowHidden, hidePlatformNumber, presetId, x, y, z, scale);
            }
        });
    }
}