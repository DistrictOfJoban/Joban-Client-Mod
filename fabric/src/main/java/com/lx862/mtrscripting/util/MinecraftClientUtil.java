package com.lx862.mtrscripting.util;

/* From https://github.com/zbx1425/mtr-nte/blob/master/common/src/main/java/cn/zbx1425/mtrsteamloco/render/scripting/util/MinecraftClientUtil.java */

import com.lx862.jcm.mapping.LoaderImpl;
import com.mojang.text2speech.Narrator;
import org.mtr.mapping.holder.*;
import org.mtr.mapping.mapper.ScoreboardHelper;
import org.mtr.mapping.mapper.TextHelper;
import org.mtr.mapping.mapper.WorldHelper;

@SuppressWarnings("unused")
public class MinecraftClientUtil {
    public static boolean worldIsRaining() {
        return MinecraftClient.getInstance().getWorldMapped() != null
                && MinecraftClient.getInstance().getWorldMapped().isRaining();
    }

    public static boolean worldIsThundering() {
        return MinecraftClient.getInstance().getWorldMapped() != null
                && MinecraftClient.getInstance().getWorldMapped().isThundering();
    }

    public static boolean worldIsRainingAt(Vector3dWrapper pos) {
        return MinecraftClient.getInstance().getWorldMapped() != null
                && LoaderImpl.isRainingAt(World.cast(MinecraftClient.getInstance().getWorldMapped()), pos.rawBlockPos());
    }

    public static int worldDayTime() {
        return MinecraftClient.getInstance().getWorldMapped() != null
                ? (int) WorldHelper.getTimeOfDay(MinecraftClient.getInstance().getWorldMapped()) : 0;
    }

    public static boolean isEmittingRedstonePower(Vector3dWrapper pos) {
        for(Direction direction : Direction.values()) {
            if(isEmittingRedstonePower(pos, direction)) return true;
        }
        return false;
    }

    public static boolean isEmittingRedstonePower(Vector3dWrapper pos, Direction direction) {
        return MinecraftClient.getInstance().getWorldMapped() != null &&
                MinecraftClient.getInstance().getWorldMapped().isEmittingRedstonePower(pos.rawBlockPos(), direction);
    }

    public static void narrate(String message) {
        MinecraftClient.getInstance().execute(() -> {
            Narrator.getNarrator().say(message, true);
        });
    }

    public static int blockLightAt(Vector3dWrapper pos) {
        return MinecraftClient.getInstance().getWorldMapped().getLightLevel(LightType.BLOCK, pos.rawBlockPos());
    }

    public static int skyLightAt(Vector3dWrapper pos) {
        return MinecraftClient.getInstance().getWorldMapped().getLightLevel(LightType.SKY, pos.rawBlockPos());
    }

    public static int lightLevelAt(Vector3dWrapper pos) {
        return Math.min(blockLightAt(pos), skyLightAt(pos));
    }

    public static Vector3dWrapper playerPos() {
        Vector3d pos = MinecraftClient.getInstance().getPlayerMapped().getPos();
        return new Vector3dWrapper((float)pos.getXMapped(), (float)pos.getYMapped(), (float)pos.getZMapped());
    }

    public static Vector3dWrapper playerBlockPos() {
        return new Vector3dWrapper(MinecraftClient.getInstance().getPlayerMapped().getBlockPos());
    }

    public static boolean isHoldingItem(String id) {
        Item itm = LoaderImpl.getItemFromId(new Identifier(id));
        if(itm == null) return false;
        return MinecraftClient.getInstance().getPlayerMapped().isHolding(itm);
    }

    public static Integer getScoreboardScore(String objectiveName) {
        ClientWorld world = MinecraftClient.getInstance().getWorldMapped();
        Scoreboard scoreboard = world.getScoreboard();
        ScoreboardObjective objective = ScoreboardHelper.getScoreboardObjective(scoreboard, objectiveName);
        if(objective == null) return null;
        return ScoreboardHelper.getPlayerScore(scoreboard, MinecraftClient.getInstance().getPlayerMapped().getGameProfile().getName(), objective);
    }

    public static void displayMessage(String message, boolean actionBar) {
        final ClientPlayerEntity player = MinecraftClient.getInstance().getPlayerMapped();
        if (player != null) {
            MinecraftClient.getInstance().execute(() -> {
                player.sendMessage(Text.cast(TextHelper.literal(message)), actionBar);
            });
        }
    }
}