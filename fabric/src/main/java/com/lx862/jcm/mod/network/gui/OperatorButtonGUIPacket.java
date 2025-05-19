package com.lx862.jcm.mod.network.gui;

import com.lx862.jcm.mod.block.OperatorButtonBlock;
import org.mtr.mapping.holder.BlockPos;
import org.mtr.mapping.registry.PacketHandler;
import org.mtr.mapping.tool.PacketBufferReceiver;
import org.mtr.mapping.tool.PacketBufferSender;

public class OperatorButtonGUIPacket extends PacketHandler {
    private final BlockPos blockPos;
    private final boolean[] keyRequirements;

    public OperatorButtonGUIPacket(PacketBufferReceiver packetBufferReceiver) {
        this.blockPos = BlockPos.fromLong(packetBufferReceiver.readLong());
        this.keyRequirements = new boolean[OperatorButtonBlock.ACCEPTED_KEYS.length];
        for(int i = 0; i < OperatorButtonBlock.ACCEPTED_KEYS.length; i++) {
            keyRequirements[i] = packetBufferReceiver.readBoolean();
        }
    }

    public OperatorButtonGUIPacket(BlockPos blockPos, boolean[] keyRequirements) {
        this.blockPos = blockPos;
        this.keyRequirements = keyRequirements;
    }

    @Override
    public void write(PacketBufferSender packetBufferSender) {
        packetBufferSender.writeLong(blockPos.asLong());
        for(int i = 0; i < OperatorButtonBlock.ACCEPTED_KEYS.length; i++) {
            packetBufferSender.writeBoolean(keyRequirements[i]);
        }
    }

    @Override
    public void runClient() {
        ClientHelper.openOperatorButtonGUIScreen(blockPos, keyRequirements);
    }
}
