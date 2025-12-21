package com.lx862.mtrscripting.wrapper;

/* From https://github.com/zbx1425/mtr-nte/blob/master/common/src/main/java/cn/zbx1425/mtrsteamloco/render/scripting/util/MinecraftClientUtil.java */

import com.lx862.jcm.mapping.LoaderImpl;
import com.lx862.mtrscripting.util.ScriptVector3f;
import com.mojang.text2speech.Narrator;
import org.mtr.mapping.holder.*;
import org.mtr.mapping.mapper.ScoreboardHelper;
import org.mtr.mapping.mapper.TextHelper;
import org.mtr.mapping.mapper.WorldHelper;

@SuppressWarnings("unused")
public class MinecraftClientWrapper {
    public static boolean worldIsRaining() {
        return MinecraftClient.getInstance().getWorldMapped() != null
                && MinecraftClient.getInstance().getWorldMapped().isRaining();
    }

    public static boolean worldIsThundering() {
        return MinecraftClient.getInstance().getWorldMapped() != null
                && MinecraftClient.getInstance().getWorldMapped().isThundering();
    }

    public static boolean worldIsRainingAt(ScriptVector3f pos) {
        return MinecraftClient.getInstance().getWorldMapped() != null
                && LoaderImpl.isRainingAt(World.cast(MinecraftClient.getInstance().getWorldMapped()), pos.rawBlockPos());
    }

    public static int worldDayTime() {
        return MinecraftClient.getInstance().getWorldMapped() != null
                ? (int) WorldHelper.getTimeOfDay(MinecraftClient.getInstance().getWorldMapped()) : 0;
    }

    public static boolean isEmittingRedstonePower(ScriptVector3f pos) {
        for(Direction direction : Direction.values()) {
            if(isEmittingRedstonePower(pos, direction)) return true;
        }
        return false;
    }

    public static boolean isEmittingRedstonePower(ScriptVector3f pos, Direction direction) {
        return MinecraftClient.getInstance().getWorldMapped() != null &&
                MinecraftClient.getInstance().getWorldMapped().isEmittingRedstonePower(pos.rawBlockPos(), direction);
    }

    public static void narrate(String message) {
        MinecraftClient.getInstance().execute(() -> {
            Narrator.getNarrator().say(message, true);
        });
    }

    public static int blockLightAt(ScriptVector3f pos) {
        return MinecraftClient.getInstance().getWorldMapped().getLightLevel(LightType.BLOCK, pos.rawBlockPos());
    }

    public static int skyLightAt(ScriptVector3f pos) {
        return MinecraftClient.getInstance().getWorldMapped().getLightLevel(LightType.SKY, pos.rawBlockPos());
    }

    public static int lightLevelAt(ScriptVector3f pos) {
        return Math.min(blockLightAt(pos), skyLightAt(pos));
    }

    public static PlayerEntityWrapper localPlayer() {
        ClientPlayerEntity clientPlayerEntity = MinecraftClient.getInstance().getPlayerMapped();
        if(clientPlayerEntity == null) return null;
        return new PlayerEntityWrapper(PlayerEntity.cast(clientPlayerEntity));
    }

    public static Integer getScoreboardScore(String objectiveName, String playerName) {
        ClientWorld world = MinecraftClient.getInstance().getWorldMapped();
        Scoreboard scoreboard = world.getScoreboard();
        ScoreboardObjective objective = ScoreboardHelper.getScoreboardObjective(scoreboard, objectiveName);
        if(objective == null) return null;
        return ScoreboardHelper.getPlayerScore(scoreboard, playerName, objective);
    }

    public static boolean gamePaused() {
        return MinecraftClient.getInstance().isPaused();
    }

    public static void displayMessage(String message, boolean actionBar) {
        displayMessage(Text.cast(TextHelper.literal(message)), actionBar);
    }

    public static void displayMessage(VanillaTextWrapper vanillaTextWrapper, boolean actionBar) {
        displayMessage(Text.cast(vanillaTextWrapper.impl()), actionBar);
    }

    private static void displayMessage(Text text, boolean actionBar) {
        final ClientPlayerEntity player = MinecraftClient.getInstance().getPlayerMapped();
        if (player != null) {
            MinecraftClient.getInstance().execute(() -> {
                player.sendMessage(text, actionBar);
            });
        }
    }
}