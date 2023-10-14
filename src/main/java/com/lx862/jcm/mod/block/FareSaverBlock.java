package com.lx862.jcm.mod.block;

import com.lx862.jcm.mod.block.base.Vertical3Block;
import com.lx862.jcm.mod.block.entity.FareSaverBlockEntity;
import com.lx862.jcm.mod.network.gui.FareSaverScreenPacket;
import com.lx862.jcm.mod.util.BlockUtil;
import com.lx862.jcm.mod.util.TextUtil;
import com.lx862.jcm.mod.util.Utils;
import com.lx862.jcm.mod.util.VoxelUtil;
import org.mtr.mapping.holder.*;
import org.mtr.mapping.mapper.BlockEntityExtension;
import org.mtr.mapping.mapper.BlockWithEntity;
import org.mtr.mapping.registry.Registry;

import java.util.HashMap;
import java.util.UUID;

public class FareSaverBlock extends Vertical3Block implements BlockWithEntity {
    public static HashMap<UUID, Integer> discountList = new HashMap<>();
    public FareSaverBlock(BlockSettings settings) {
        super(settings);
    }

    @Override
    public VoxelShape getOutlineShape2(BlockState state, BlockView view, BlockPos pos, ShapeContext context) {
        return VoxelUtil.getDirectionalShape16(BlockUtil.getProperty(state, FACING), 3, 0, 6, 13, 16, 9);
    }

    @Override
    public ActionResult onUse2(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        super.onUse2(state, world, pos, player, hand, hit);
        return ActionResult.SUCCESS;
    }

    @Override
    public void onServerUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        UUID playerUuid = player.getUuid();
        FareSaverBlockEntity thisEntity = (FareSaverBlockEntity)world.getBlockEntity(pos).data;
        int discount = thisEntity.getDiscount();

        if (Utils.playerHoldingBrush(player)) {
            Registry.sendPacketToClient(ServerPlayerEntity.cast(player), new FareSaverScreenPacket(pos, discount));
            return;
        }

        if(discountList.containsKey(playerUuid)) {
            player.sendMessage(Text.cast(TextUtil.translatable(TextUtil.TextCategory.HUD, "faresaver.fail", discountList.get(playerUuid))), true);
            return;
        }

        discountList.put(playerUuid, discount);

        if(discount > 0) {
            player.sendMessage(Text.cast(TextUtil.translatable(TextUtil.TextCategory.HUD, "faresaver.success", discount)), true);
        } else {
            player.sendMessage(Text.cast(TextUtil.translatable(TextUtil.TextCategory.HUD, "faresaver.success.sarcasm", discount)), true);
        }
    }

    @Override
    public BlockEntityExtension createBlockEntity(BlockPos blockPos, BlockState blockState) {
        return new FareSaverBlockEntity(blockPos, blockState);
    }
}