package com.lx862.jcm.mod.block;

import com.lx862.jcm.mod.block.base.Horizontal2MirroredBlock;
import com.lx862.jcm.mod.block.entity.PIDSBlockEntity;
import com.lx862.jcm.mod.network.gui.PIDSGUIPacket;
import com.lx862.jcm.mod.registry.Networking;
import com.lx862.jcm.mod.util.JCMUtil;
import org.mtr.mapping.holder.*;
import org.mtr.mapping.mapper.BlockWithEntity;

public abstract class JCMPIDSBlock extends Horizontal2MirroredBlock implements BlockWithEntity {
    public JCMPIDSBlock(BlockSettings settings) {
        super(settings);
    }

    @Override
    public ActionResult onUse2(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        super.onUse2(state, world, pos, player, hand, hit);
        return getBrushActionResult(player);
    }

    @Override
    public void onServerUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        PIDSBlockEntity thisEntity = (PIDSBlockEntity) world.getBlockEntity(pos).data;

        if (JCMUtil.playerHoldingBrush(player)) {
            Networking.sendPacketToClient(player, new PIDSGUIPacket(pos, thisEntity.getCustomMessages(), thisEntity.getRowHidden(), thisEntity.platformNumberHidden(), thisEntity.getPresetId()));
        }
    }
}
