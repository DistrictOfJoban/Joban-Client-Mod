package com.lx862.mtrscripting.util;

/* From https://github.com/zbx1425/mtr-nte/blob/master/common/src/main/java/cn/zbx1425/mtrsteamloco/render/scripting/util/MinecraftClientUtil.java */

import com.lx862.jcm.mapping.LoaderImpl;
import com.mojang.text2speech.Narrator;
import org.mtr.mapping.holder.*;
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

    public static boolean worldIsRainingAt(Vector3f pos) {
        return MinecraftClient.getInstance().getWorldMapped() != null
                && LoaderImpl.isRainingAt(World.cast(MinecraftClient.getInstance().getWorldMapped()), new BlockPos((int)pos.getX(), (int)pos.getY(), (int)pos.getZ()));
    }

    public static int worldDayTime() {
        return MinecraftClient.getInstance().getWorldMapped() != null
                ? (int) WorldHelper.getTimeOfDay(MinecraftClient.getInstance().getWorldMapped()) : 0;
    }

    public static void narrate(String message) {
        MinecraftClient.getInstance().execute(() -> {
            Narrator.getNarrator().say(message, true);
        });
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