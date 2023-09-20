package com.lx862.jcm.blocks;

import com.lx862.jcm.blocks.base.WallAttachedBlock;
import com.lx862.jcm.blocks.entity.SubsidyMachineBlockEntity;
import com.lx862.jcm.data.JCMStats;
import com.lx862.jcm.util.*;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import net.minecraft.SharedConstants;
import org.mtr.mapping.holder.*;
import org.mtr.mapping.mapper.BlockEntityExtension;
import org.mtr.mapping.mapper.BlockWithEntity;

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
        super.onUse2(state, world, pos, player, hand, hit);
        return ActionResult.SUCCESS;
    }

    @Override
    public void onServerUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        SubsidyMachineBlockEntity thisEntity = (SubsidyMachineBlockEntity)world.getBlockEntity(pos).data;

        if (Utils.playerHoldingBrush(player)) {
            //TODO: Open GUI Screen
            return;
        }

        if(cooldownExpired(player, thisEntity.getCooldown())) {
            updateCooldown(player);
            int finalBalance = addMTRBalanceToPlayer(world, player, thisEntity.getSubsidyAmount());
            player.sendMessage(Text.cast(TextUtil.getTranslatable(TextUtil.CATEGORY.HUD, "subsidy_machine.success", thisEntity.getSubsidyAmount(), finalBalance)), true);
        } else {
            int remainingSec = (int)Math.round((thisEntity.getCooldown() - getCooldown(player)) / (double)SharedConstants.TICKS_PER_SECOND);
            player.sendMessage(Text.cast(TextUtil.getTranslatable(TextUtil.CATEGORY.HUD, "subsidy_machine.fail", remainingSec).formatted(TextFormatting.RED)), true);
        }
    }


    @Override
    public BlockEntityExtension createBlockEntity(BlockPos blockPos, BlockState blockState) {
        return new SubsidyMachineBlockEntity(blockPos, blockState);
    }

    private static int addMTRBalanceToPlayer(World world, PlayerEntity player, int amount) {
        ScoreboardPlayerScore mtrBalanceScore = ScoreboardUtil.getPlayerMTRBalanceScore(world, player);

        ScoreboardUtil.incrementNonOverflow(mtrBalanceScore, amount);
        return mtrBalanceScore.getScore();
    }

    private static boolean cooldownExpired(PlayerEntity player, int cooldownTick) {
        return getCooldown(player) > cooldownTick;
    }

    private static long getCooldown(PlayerEntity player) {
        return JCMStats.getGameTick() - cooldownMap.getOrDefault(player.getUuid(), 0);
    }

    private static void updateCooldown(PlayerEntity player) {
        cooldownMap.put(player.getUuid(), JCMStats.getGameTick());
    }
}