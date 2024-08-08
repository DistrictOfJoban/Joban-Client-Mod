package com.lx862.jcm.mod.block;

import com.lx862.jcm.mod.block.base.DirectionalBlock;
import com.lx862.jcm.mod.block.entity.PIDSProjectorBlockEntity;
import com.lx862.jcm.mod.network.gui.PIDSProjectorGUIPacket;
import com.lx862.jcm.mod.registry.Networking;
import org.mtr.mapping.holder.*;
import org.mtr.mapping.mapper.BlockWithEntity;
import org.mtr.mod.block.IBlock;

public abstract class JCMProjectorBlock extends DirectionalBlock implements BlockWithEntity {
    public JCMProjectorBlock(BlockSettings settings) {
        super(settings);
    }

    @Override
    public ActionResult onUse2(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        return IBlock.checkHoldingBrush(world, player, () -> {
            PIDSProjectorBlockEntity be = (PIDSProjectorBlockEntity) world.getBlockEntity(pos).data;
            Networking.sendPacketToClient(player, new PIDSProjectorGUIPacket(pos, be.getCustomMessages(), be.getRowHidden(), be.platformNumberHidden(), be.getPresetId(), be.getX(), be.getY(), be.getZ(), be.getRotateX(), be.getRotateY(), be.getRotateZ(), be.getScale()));
        });
    }
}