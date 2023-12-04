package com.lx862.jcm.mod.network.block;

import com.lx862.jcm.mod.block.entity.ButterflyLightBlockEntity;
import com.lx862.jcm.mod.block.entity.FareSaverBlockEntity;
import com.lx862.jcm.mod.util.BlockUtil;
import org.mtr.mapping.holder.*;
import org.mtr.mapping.registry.PacketHandler;

public class ButterflyLightUpdatePacket extends PacketHandler {
    private final BlockPos blockPos;
    private final int secondsToBlink;

    public ButterflyLightUpdatePacket(PacketBuffer packetBuffer) {
        this.blockPos = packetBuffer.readBlockPos();
        this.secondsToBlink = packetBuffer.readInt();
    }

    public ButterflyLightUpdatePacket(BlockPos blockPos, int secondsToBlink) {
        this.blockPos = blockPos;
        this.secondsToBlink = secondsToBlink;
    }

    @Override
    public void write(PacketBuffer packetBuffer) {
        packetBuffer.writeBlockPos(blockPos);
        packetBuffer.writeVarInt(secondsToBlink);
    }

    @Override
    public void runServerQueued(MinecraftServer minecraftServer, ServerPlayerEntity serverPlayerEntity) {
        World world = serverPlayerEntity.getEntityWorld();
        BlockEntity be = BlockUtil.getBlockEntityOrNull(world, blockPos);

        if(be != null && be.data instanceof ButterflyLightBlockEntity) {
            ((ButterflyLightBlockEntity)be.data).setData(secondsToBlink);
        }
    }
}
