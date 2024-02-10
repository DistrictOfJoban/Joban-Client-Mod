package com.lx862.jcm.mod.network.block;

import com.lx862.jcm.mod.block.entity.ButterflyLightBlockEntity;
import com.lx862.jcm.mod.util.BlockUtil;
import org.mtr.mapping.holder.*;
import org.mtr.mapping.registry.PacketHandler;
import org.mtr.mapping.tool.PacketBufferReceiver;
import org.mtr.mapping.tool.PacketBufferSender;

public class ButterflyLightUpdatePacket extends PacketHandler {
    private final BlockPos blockPos;
    private final int secondsToBlink;

    public ButterflyLightUpdatePacket(PacketBufferReceiver packetBufferReceiver) {
        this.blockPos = BlockPos.fromLong(packetBufferReceiver.readLong());
        this.secondsToBlink = packetBufferReceiver.readInt();
    }

    public ButterflyLightUpdatePacket(BlockPos blockPos, int secondsToBlink) {
        this.blockPos = blockPos;
        this.secondsToBlink = secondsToBlink;
    }

    @Override
    public void runServer(MinecraftServer minecraftServer, ServerPlayerEntity serverPlayerEntity) {
        World world = serverPlayerEntity.getEntityWorld();
        BlockEntity be = BlockUtil.getBlockEntityOrNull(world, blockPos);

        if(be != null && be.data instanceof ButterflyLightBlockEntity) {
            ((ButterflyLightBlockEntity)be.data).setData(secondsToBlink);
        }
    }

    @Override
    public void write(PacketBufferSender packetBufferSender) {
        packetBufferSender.writeLong(blockPos.asLong());
        packetBufferSender.writeInt(secondsToBlink);
    }
}
