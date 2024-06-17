package com.lx862.jcm.mod.block;

import com.lx862.jcm.mod.Constants;
import com.lx862.jcm.mod.block.base.WallAttachedBlock;
import com.lx862.jcm.mod.block.entity.SubsidyMachineBlockEntity;
import com.lx862.jcm.mod.data.JCMServerStats;
import com.lx862.jcm.mod.network.gui.SubsidyMachineGUIPacket;
import com.lx862.jcm.mod.registry.Networking;
import com.lx862.jcm.mod.util.*;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import org.mtr.mapping.holder.*;
import org.mtr.mapping.mapper.BlockEntityExtension;
import org.mtr.mapping.mapper.BlockWithEntity;
import org.mtr.mod.block.IBlock;
import org.mtr.mod.data.TicketSystem;

import java.util.UUID;

public class SubsidyMachineBlock extends WallAttachedBlock implements BlockWithEntity {
    private static final Object2LongOpenHashMap<UUID> cooldownMap = new Object2LongOpenHashMap<>();
    public SubsidyMachineBlock(BlockSettings settings) {
        super(settings);
    }

    @Override
    public VoxelShape getOutlineShape2(BlockState state, BlockView view, BlockPos pos, ShapeContext context) {
        return VoxelUtil.getDirectionalShape16(BlockUtil.getProperty(state, FACING), 4, 1.25, 0, 12, 14.75, 2);
    }

    @Override
    public ActionResult onUse2(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        BlockEntity be = world.getBlockEntity(pos);
        if(be == null) return ActionResult.FAIL;

        SubsidyMachineBlockEntity sbe = (SubsidyMachineBlockEntity)be.data;

        return IBlock.checkHoldingBrush(world, player, () -> {
            Networking.sendPacketToClient(player, new SubsidyMachineGUIPacket(pos, sbe.getSubsidyAmount(), sbe.getCooldown()));
        }, () -> {
            if(cooldownExpired(player, sbe.getCooldown())) {
                updateCooldown(player);
                int finalBalance = addMTRBalanceToPlayer(world, player, sbe.getSubsidyAmount());
                player.sendMessage(Text.cast(TextUtil.translatable(TextCategory.HUD, "subsidy_machine.success", sbe.getSubsidyAmount(), finalBalance)), true);
            } else {
                int remainingSec = Math.round(sbe.getCooldown() - getCooldown(player));
                player.sendMessage(Text.cast(TextUtil.translatable(TextCategory.HUD, "subsidy_machine.fail", remainingSec).formatted(TextFormatting.RED)), true);
            }
        });
    }

    @Override
    public BlockEntityExtension createBlockEntity(BlockPos blockPos, BlockState blockState) {
        return new SubsidyMachineBlockEntity(blockPos, blockState);
    }

    private static int addMTRBalanceToPlayer(World world, PlayerEntity player, int amount) {
        TicketSystem.addBalance(world, player, amount);
        return TicketSystem.getBalance(world, player);
    }

    private static boolean cooldownExpired(PlayerEntity player, int cooldown) {
        return getCooldown(player) >= cooldown;
    }

    private static long getCooldown(PlayerEntity player) {
        return (JCMServerStats.getGameTick() - cooldownMap.getOrDefault(player.getUuid(), 0)) / Constants.MC_TICK_PER_SECOND;
    }

    private static void updateCooldown(PlayerEntity player) {
        cooldownMap.put(player.getUuid(), JCMServerStats.getGameTick());
    }
}