package com.lx862.jcm.mod.block;

import com.lx862.jcm.mod.block.base.JCMBlock;
import com.lx862.jcm.mod.block.entity.SoundLooperBlockEntity;
import com.lx862.jcm.mod.network.gui.SoundLooperGUIPacket;
import com.lx862.jcm.mod.registry.Networking;
import com.lx862.jcm.mod.util.BlockUtil;
import com.lx862.jcm.mod.util.JCMUtil;
import org.mtr.mapping.holder.*;
import org.mtr.mapping.mapper.BlockEntityExtension;
import org.mtr.mapping.mapper.BlockWithEntity;

public class SoundLooperBlock extends JCMBlock implements BlockWithEntity {
    public SoundLooperBlock(BlockSettings settings) {
        super(settings);
    }

    @Override
    public BlockEntityExtension createBlockEntity(BlockPos blockPos, BlockState blockState) {
        return new SoundLooperBlockEntity(blockPos, blockState);
    }

    @Override
    public ActionResult onUse2(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        super.onUse2(state, world, pos, player, hand, hit);
        return getBrushActionResult(player);
    }

    @Override
    public void onServerUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        BlockEntity be = BlockUtil.getBlockEntityOrNull(world, pos);
        if(be == null) return;
        SoundLooperBlockEntity sbe = (SoundLooperBlockEntity) be.data;

        if (JCMUtil.playerHoldingBrush(player)) {
            Networking.sendPacketToClient(player, new SoundLooperGUIPacket(pos, sbe.getCorner1(), sbe.getCorner2(), sbe.getSoundId(), sbe.getSoundCategory(), sbe.getLoopInterval(), sbe.getSoundVolume(), sbe.needRedstone(), sbe.rangeLimited()));
        }
    }
}
