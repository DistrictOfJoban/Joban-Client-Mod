package com.lx862.jcm.mod.block;

import com.lx862.jcm.mod.block.base.JCMBlock;
import com.lx862.jcm.mod.block.entity.SoundLooperBlockEntity;
import com.lx862.jcm.mod.network.gui.SoundLooperGUIPacket;
import com.lx862.jcm.mod.registry.Networking;
import org.mtr.mapping.holder.*;
import org.mtr.mapping.mapper.BlockEntityExtension;
import org.mtr.mapping.mapper.BlockWithEntity;
import org.mtr.mod.block.IBlock;

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
        return IBlock.checkHoldingBrush(world, player, () -> {
            SoundLooperBlockEntity be = (SoundLooperBlockEntity) world.getBlockEntity(pos).data;
            Networking.sendPacketToClient(player, new SoundLooperGUIPacket(pos, be.getCorner1(), be.getCorner2(), be.getSoundId(), be.getSoundCategory(), be.getLoopInterval(), be.getSoundVolume(), be.needRedstone(), be.rangeLimited()));
        });
    }
}
