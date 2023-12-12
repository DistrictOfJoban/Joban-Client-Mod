package com.lx862.jcm.mod.util;

import org.mtr.mapping.holder.*;
import org.mtr.mapping.mapper.ScoreboardHelper;
import org.mtr.mapping.mapper.TextHelper;

/**
 * Scoreboard Utilities
 * @deprecated To be removed as this is solely used for adding MTR's ticket balance (We should call MTR's method instead once we are able to depend on it)
 * Also 1.20.4.
 */
public class ScoreboardUtil {
    public static ScoreboardObjective getOrCreateObjective(World world, String name, ScoreboardCriterion criterion, Text displayName, ScoreboardCriterionRenderType renderType) {
//        if(!world.getScoreboard().getObjectiveNames().contains(name)) {
//            ScoreboardHelper.addObjective(name, criterion, displayName, renderType)
//        }
        return ScoreboardHelper.getScoreboardObjective(world.getScoreboard(), name);
    }
//
//    public static boolean incrementNonOverflow(Scoreboard scoreboard, int originalAmount, int amount) {
//        if((long)score.getScore() + amount > Integer.MAX_VALUE) {
//            return false;
//        } else {
//            ScoreboardHelper.incrementPlayerScore();
//            score.incrementScore(amount);
//            return true;
//        }
//    }
//
    public static int getPlayerMTRBalanceScore(World world, PlayerEntity player) {
        String playerName = player.getGameProfile().getName();
        ScoreboardObjective mtrScoreboard = getOrCreateObjective(world, "mtr_balance", ScoreboardCriterion.getDummyMapped(), Text.cast(TextHelper.literal("Balance")), ScoreboardCriterionRenderType.INTEGER);
        return ScoreboardHelper.getPlayerScore(world.getScoreboard(), playerName, mtrScoreboard);
    }
}
