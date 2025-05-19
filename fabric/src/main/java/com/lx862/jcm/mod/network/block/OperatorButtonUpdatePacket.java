package com.lx862.jcm.mod.network.block;

import com.lx862.jcm.mod.block.OperatorButtonBlock;
import com.lx862.jcm.mod.block.base.JCMBlock;
import com.lx862.jcm.mod.block.entity.OperatorButtonBlockEntity;
import com.lx862.jcm.mod.util.BlockUtil;
import org.mtr.mapping.holder.*;
import org.mtr.mapping.registry.PacketHandler;
import org.mtr.mapping.tool.PacketBufferReceiver;
import org.mtr.mapping.tool.PacketBufferSender;

public class OperatorButtonUpdatePacket extends PacketHandler {
    private final BlockPos blockPos;
    private final boolean[] keyRequirements;

    public OperatorButtonUpdatePacket(PacketBufferReceiver packetBufferReceiver) {
        this.blockPos = BlockPos.fromLong(packetBufferReceiver.readLong());
        this.keyRequirements = new boolean[OperatorButtonBlock.ACCEPTED_KEYS.length];
        for (int i = 0; i < OperatorButtonBlock.ACCEPTED_KEYS.length; i++) {
            keyRequirements[i] = packetBufferReceiver.readBoolean();
        }
    }

    public OperatorButtonUpdatePacket(BlockPos blockPos, boolean[] keyRequirements) {
        this.blockPos = blockPos;
        this.keyRequirements = keyRequirements;
    }

    @Override
    public void write(PacketBufferSender packetBufferSender) {
        packetBufferSender.writeLong(blockPos.asLong());
        for (boolean keyAccepted : keyRequirements) {
            packetBufferSender.writeBoolean(keyAccepted);
        }
    }

    @Override
    public void runServer(MinecraftServer minecraftServer, ServerPlayerEntity serverPlayerEntity) {
        World world = serverPlayerEntity.getEntityWorld();
        BlockState state = BlockUtil.getBlockState(world, blockPos);
        if(state == null || !(state.getBlock().data instanceof JCMBlock)) return;

        ((JCMBlock)state.getBlock().data).loopStructure(state, world, blockPos, (bs, be) -> {
            if(be.data instanceof OperatorButtonBlockEntity) {
                ((OperatorButtonBlockEntity)be.data).setData(keyRequirements);
            }
        });
    }
}
