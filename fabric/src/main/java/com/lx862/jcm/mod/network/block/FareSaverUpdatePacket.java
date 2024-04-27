package com.lx862.jcm.mod.network.block;

import com.lx862.jcm.mod.block.base.JCMBlock;
import com.lx862.jcm.mod.block.entity.FareSaverBlockEntity;
import com.lx862.jcm.mod.util.BlockUtil;
import org.mtr.mapping.holder.*;
import org.mtr.mapping.registry.PacketHandler;
import org.mtr.mapping.tool.PacketBufferReceiver;
import org.mtr.mapping.tool.PacketBufferSender;

public class FareSaverUpdatePacket extends PacketHandler {
    private final BlockPos blockPos;
    private final String currency;
    private final int discount;

    public FareSaverUpdatePacket(PacketBufferReceiver packetBufferReceiver) {
        this.blockPos = BlockPos.fromLong(packetBufferReceiver.readLong());
        this.currency = packetBufferReceiver.readString();
        this.discount = packetBufferReceiver.readInt();
    }

    public FareSaverUpdatePacket(BlockPos blockPos, String currency, int discount) {
        this.blockPos = blockPos;
        this.currency = currency;
        this.discount = discount;
    }

    @Override
    public void write(PacketBufferSender packetBufferSender) {
        packetBufferSender.writeLong(blockPos.asLong());
        packetBufferSender.writeString(currency);
        packetBufferSender.writeInt(discount);
    }

    @Override
    public void runServer(MinecraftServer minecraftServer, ServerPlayerEntity serverPlayerEntity) {
        World world = serverPlayerEntity.getEntityWorld();
        BlockState state = BlockUtil.getBlockState(world, blockPos);
        if(state == null || !(state.getBlock().data instanceof JCMBlock)) return;

        ((JCMBlock)state.getBlock().data).forEachBlockEntity(state, world, blockPos, be -> {
            if(be.data instanceof FareSaverBlockEntity) {
                ((FareSaverBlockEntity)be.data).setData(currency, discount);
            }
        });
    }
}
