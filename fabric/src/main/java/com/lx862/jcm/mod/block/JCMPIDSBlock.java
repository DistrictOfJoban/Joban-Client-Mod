package com.lx862.jcm.mod.block;

import com.lx862.jcm.mod.block.base.Horizontal2MirroredBlock;
import com.lx862.jcm.mod.block.entity.PIDSBlockEntity;
import com.lx862.jcm.mod.network.gui.PIDSGUIPacket;
import com.lx862.jcm.mod.registry.Networking;
import org.mtr.mapping.holder.*;
import org.mtr.mapping.mapper.BlockWithEntity;
import org.mtr.mod.block.IBlock;

public abstract class JCMPIDSBlock extends Horizontal2MirroredBlock implements BlockWithEntity {
    public JCMPIDSBlock(BlockSettings settings) {
        super(settings);
    }

    @Override
    public ActionResult onUse2(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        return IBlock.checkHoldingBrush(world, player, () -> {
            PIDSBlockEntity be = (PIDSBlockEntity) world.getBlockEntity(pos).data;
            Networking.sendPacketToClient(player, new PIDSGUIPacket(pos, be.getCustomMessages(), be.getRowHidden(), be.platformNumberHidden(), be.getPresetId()));
        });
    }
}
