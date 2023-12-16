package com.lx862.jcm.mod.network.block;

import com.lx862.jcm.mod.block.entity.RVPIDSBlockEntity;
import com.lx862.jcm.mod.util.BlockUtil;
import org.mtr.mapping.holder.*;

public class RVPIDSUpdatePacket extends PIDSUpdatePacket {
    private final boolean hidePlatformNumber;

    public RVPIDSUpdatePacket(PacketBuffer packetBuffer) {
        super(packetBuffer);
        this.hidePlatformNumber = packetBuffer.readBoolean();
    }

    public RVPIDSUpdatePacket(BlockPos blockPos, String[] customMessages, boolean[] rowHidden, boolean hidePlatformNumber, String presetId) {
        super(blockPos, customMessages, rowHidden, presetId);
        this.hidePlatformNumber = hidePlatformNumber;
    }

    @Override
    public void write(PacketBuffer packetBuffer) {
        super.write(packetBuffer);
        packetBuffer.writeBoolean(hidePlatformNumber);
    }

    @Override
    public void runServerQueued(MinecraftServer minecraftServer, ServerPlayerEntity serverPlayerEntity) {
        World world = serverPlayerEntity.getEntityWorld();
        BlockEntity be = BlockUtil.getBlockEntityOrNull(world, blockPos);

        if(be != null && be.data instanceof RVPIDSBlockEntity) {
            ((RVPIDSBlockEntity)be.data).setData(customMessages, rowHidden, hidePlatformNumber, presetId);
        }
    }
}
