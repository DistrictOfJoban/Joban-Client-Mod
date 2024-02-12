package com.lx862.jcm.mod.network.block;

import com.lx862.jcm.mod.block.base.JCMBlock;
import com.lx862.jcm.mod.block.entity.SubsidyMachineBlockEntity;
import com.lx862.jcm.mod.util.BlockUtil;
import org.mtr.mapping.holder.*;
import org.mtr.mapping.registry.PacketHandler;
import org.mtr.mapping.tool.PacketBufferReceiver;
import org.mtr.mapping.tool.PacketBufferSender;

public class SubsidyMachineUpdatePacket extends PacketHandler {
    private final BlockPos blockPos;
    private final int pricePerUse;
    private final int cooldown;

    public SubsidyMachineUpdatePacket(PacketBufferReceiver packetBufferReceiver) {
        this.blockPos = BlockPos.fromLong(packetBufferReceiver.readLong());
        this.pricePerUse = packetBufferReceiver.readInt();
        this.cooldown = packetBufferReceiver.readInt();
    }

    public SubsidyMachineUpdatePacket(BlockPos blockPos, int pricePerUse, int cooldown) {
        this.blockPos = blockPos;
        this.pricePerUse = pricePerUse;
        this.cooldown = cooldown;
    }

    @Override
    public void write(PacketBufferSender packetBufferSender) {
        packetBufferSender.writeLong(blockPos.asLong());
        packetBufferSender.writeInt(pricePerUse);
        packetBufferSender.writeInt(cooldown);
    }

    @Override
    public void runServer(MinecraftServer minecraftServer, ServerPlayerEntity serverPlayerEntity) {
        World world = serverPlayerEntity.getEntityWorld();
        BlockState state = BlockUtil.getBlockState(world, blockPos);
        if(state == null || !(state.getBlock().data instanceof JCMBlock)) return;

        ((JCMBlock)state.getBlock().data).forEachBlockEntity(state, world, blockPos, be -> {
            if(be.data instanceof SubsidyMachineBlockEntity) {
                ((SubsidyMachineBlockEntity)be.data).setData(pricePerUse, cooldown);
            }
        });
    }
}
