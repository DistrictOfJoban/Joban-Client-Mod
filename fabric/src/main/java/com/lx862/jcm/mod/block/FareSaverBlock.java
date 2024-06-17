package com.lx862.jcm.mod.block;

import com.lx862.jcm.mod.block.base.Vertical3Block;
import com.lx862.jcm.mod.block.entity.FareSaverBlockEntity;
import com.lx862.jcm.mod.network.gui.FareSaverGUIPacket;
import com.lx862.jcm.mod.registry.Networking;
import com.lx862.jcm.mod.util.*;
import org.mtr.mapping.holder.*;
import org.mtr.mapping.mapper.BlockEntityExtension;
import org.mtr.mapping.mapper.BlockWithEntity;

import java.util.HashMap;
import java.util.UUID;

public class FareSaverBlock extends Vertical3Block implements BlockWithEntity {
    public static HashMap<UUID, Integer> discountList = new HashMap<>();
    public FareSaverBlock(BlockSettings settings) {
        super(settings);
    }

    @Override
    public VoxelShape getOutlineShape2(BlockState state, BlockView view, BlockPos pos, ShapeContext context) {
        return VoxelUtil.getDirectionalShape16(BlockUtil.getProperty(state, FACING), 3, 0, 6.5, 13, 16, 9.5);
    }

    @Override
    public ActionResult onUse2(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        if(world.isClient()) return ActionResult.SUCCESS;

        UUID playerUuid = player.getUuid();
        FareSaverBlockEntity be = (FareSaverBlockEntity)world.getBlockEntity(pos).data;
        int discount = be.getDiscount();

        if (JCMUtil.playerHoldingBrush(player)) {
            Networking.sendPacketToClient(player, new FareSaverGUIPacket(pos, be.getPrefix(), discount));
            return ActionResult.SUCCESS;
        }

        if(discountList.containsKey(playerUuid)) {
            player.sendMessage(Text.cast(TextUtil.translatable(TextCategory.HUD, "faresaver.fail", discountList.get(playerUuid))), true);
        } else {
            discountList.put(playerUuid, discount);

            if(discount > 0) {
                player.sendMessage(Text.cast(TextUtil.translatable(TextCategory.HUD, "faresaver.success", discount)), true);
            } else {
                player.sendMessage(Text.cast(TextUtil.translatable(TextCategory.HUD, "faresaver.success.sarcasm", discount)), true);
            }
        }

        return ActionResult.SUCCESS;
    }

    @Override
    public BlockEntityExtension createBlockEntity(BlockPos blockPos, BlockState blockState) {
        return new FareSaverBlockEntity(blockPos, blockState);
    }
}