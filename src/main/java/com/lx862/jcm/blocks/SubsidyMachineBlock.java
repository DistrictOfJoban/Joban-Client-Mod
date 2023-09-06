package com.lx862.jcm.blocks;

import com.lx862.jcm.blocks.base.WallAttachedBlock;
import com.lx862.jcm.blocks.blockentity.SubsidyMachineBlockEntity;
import com.lx862.jcm.util.*;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import org.mtr.mapping.holder.*;
import org.mtr.mapping.mapper.BlockEntityExtension;
import org.mtr.mapping.mapper.BlockWithEntity;
import org.mtr.mapping.mapper.TextHelper;

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
        if(world.isClient()) return ActionResult.SUCCESS;

        SubsidyMachineBlockEntity thisEntity = (SubsidyMachineBlockEntity)world.getBlockEntity(pos).data;

        if (Utils.playerHoldingBrush(player)) {
            //TODO: Open GUI Screen
        } else {
            if(cooldownExpired(player, thisEntity.getTimeout())) {
                int finalBalance = addMTRBalanceToPlayer(world, player, thisEntity.getSubsidyAmount());
                updateCooldown(player);
                player.sendMessage(Text.cast(TextUtil.getTranslatable(TextUtil.CATEGORY.HUD, "subsidy_machine.success", thisEntity.getSubsidyAmount(), finalBalance)), true);
            } else {
                player.sendMessage(Text.cast(TextUtil.getTranslatable(TextUtil.CATEGORY.HUD, "subsidy_machine.fail").formatted(TextFormatting.RED)), true);
            }
        }

        return ActionResult.SUCCESS;
    }

    @Override
    public BlockEntityExtension createBlockEntity(BlockPos blockPos, BlockState blockState) {
        return new SubsidyMachineBlockEntity(blockPos, blockState);
    }

    private int addMTRBalanceToPlayer(World world, PlayerEntity player, int amount) {
        String playerName = player.getGameProfile().getName();
        ScoreboardObjective mtrScoreboard = ScoreboardUtil.getOrCreateObjective(world, "mtr_balance", ScoreboardCriterion.getDummyMapped(), Text.cast(TextHelper.literal("Balance")), ScoreboardCriterionRenderType.INTEGER);
        ScoreboardPlayerScore mtrBalanceScore = world.getScoreboard().getPlayerScore(playerName, mtrScoreboard);

        ScoreboardUtil.incrementNonOverflow(mtrBalanceScore, amount);
        return mtrBalanceScore.getScore();
    }

    private boolean cooldownExpired(PlayerEntity player, int cooldownSeconds) {
        //TODO: Count tick instead of currentTimeMs
        return System.currentTimeMillis() - cooldownMap.getOrDefault(player.getUuid(), 0) > cooldownSeconds;
    }

    private void updateCooldown(PlayerEntity player) {
        cooldownMap.put(player.getUuid(), System.currentTimeMillis());
    }
}