package com.lx862.jcm.mod.network.block;

import com.lx862.jcm.mod.block.entity.FareSaverBlockEntity;
import com.lx862.jcm.mod.util.BlockUtil;
import org.mtr.mapping.holder.*;
import org.mtr.mapping.registry.PacketHandler;

public class FareSaverUpdatePacket extends PacketHandler {
    private final BlockPos blockPos;
    private final int discount;

    public FareSaverUpdatePacket(PacketBuffer packetBuffer) {
        this.blockPos = packetBuffer.readBlockPos();
        this.discount = packetBuffer.readInt();
    }

    public FareSaverUpdatePacket(BlockPos blockPos, int discount) {
        this.blockPos = blockPos;
        this.discount = discount;
    }

    @Override
    public void write(PacketBuffer packetBuffer) {
        packetBuffer.writeBlockPos(blockPos);
        packetBuffer.writeInt(discount);
    }

    @Override
    public void runServerQueued(MinecraftServer minecraftServer, ServerPlayerEntity serverPlayerEntity) {
        World world = serverPlayerEntity.getEntityWorld();
        BlockEntity be = BlockUtil.getBlockEntityOrNull(world, blockPos);

        if(be != null && be.data instanceof FareSaverBlockEntity) {
            ((FareSaverBlockEntity)be.data).setBlockData(discount);
        }
    }
}
