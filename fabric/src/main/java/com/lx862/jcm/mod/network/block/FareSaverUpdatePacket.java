package com.lx862.jcm.mod.network.block;

import com.lx862.jcm.mod.block.entity.FareSaverBlockEntity;
import com.lx862.jcm.mod.util.BlockUtil;
import org.mtr.mapping.holder.*;
import org.mtr.mapping.registry.PacketHandler;
import org.mtr.mapping.tool.PacketBufferReceiver;
import org.mtr.mapping.tool.PacketBufferSender;

public class FareSaverUpdatePacket extends PacketHandler {
    private final BlockPos blockPos;
    private final int discount;

    public FareSaverUpdatePacket(PacketBufferReceiver packetBufferReceiver) {
        this.blockPos = BlockPos.fromLong(packetBufferReceiver.readLong());
        this.discount = packetBufferReceiver.readInt();
    }

    public FareSaverUpdatePacket(BlockPos blockPos, int discount) {
        this.blockPos = blockPos;
        this.discount = discount;
    }

    @Override
    public void write(PacketBufferSender packetBufferSender) {
        packetBufferSender.writeLong(blockPos.asLong());
        packetBufferSender.writeInt(discount);
    }

    @Override
    public void runServer(MinecraftServer minecraftServer, ServerPlayerEntity serverPlayerEntity) {
        World world = serverPlayerEntity.getEntityWorld();
        BlockEntity be = BlockUtil.getBlockEntityOrNull(world, blockPos);

        if(be != null && be.data instanceof FareSaverBlockEntity) {
            ((FareSaverBlockEntity)be.data).setBlockData(discount);
        }
    }
}
