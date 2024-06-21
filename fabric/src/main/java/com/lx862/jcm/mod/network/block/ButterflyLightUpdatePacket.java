package com.lx862.jcm.mod.network.block;

import com.lx862.jcm.mod.block.base.JCMBlock;
import com.lx862.jcm.mod.block.entity.ButterflyLightBlockEntity;
import com.lx862.jcm.mod.util.BlockUtil;
import org.mtr.mapping.holder.*;
import org.mtr.mapping.registry.PacketHandler;
import org.mtr.mapping.tool.PacketBufferReceiver;
import org.mtr.mapping.tool.PacketBufferSender;

public class ButterflyLightUpdatePacket extends PacketHandler {
    private final BlockPos blockPos;
    private final int startBlinkingSeconds;

    public ButterflyLightUpdatePacket(PacketBufferReceiver packetBufferReceiver) {
        this.blockPos = BlockPos.fromLong(packetBufferReceiver.readLong());
        this.startBlinkingSeconds = packetBufferReceiver.readInt();
    }

    public ButterflyLightUpdatePacket(BlockPos blockPos, int startBlinkingSeconds) {
        this.blockPos = blockPos;
        this.startBlinkingSeconds = startBlinkingSeconds;
    }

    @Override
    public void runServer(MinecraftServer minecraftServer, ServerPlayerEntity serverPlayerEntity) {
        World world = serverPlayerEntity.getEntityWorld();
        BlockState state = BlockUtil.getBlockState(world, blockPos);
        if(state == null || !(state.getBlock().data instanceof JCMBlock)) return;

        ((JCMBlock)state.getBlock().data).loopStructure(state, world, blockPos, (bs, be) -> {
            if(be.data instanceof ButterflyLightBlockEntity) {
                ((ButterflyLightBlockEntity)be.data).setData(startBlinkingSeconds);
            }
        });
    }

    @Override
    public void write(PacketBufferSender packetBufferSender) {
        packetBufferSender.writeLong(blockPos.asLong());
        packetBufferSender.writeInt(startBlinkingSeconds);
    }
}
