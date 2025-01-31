package com.lx862.mtrscripting.scripting.util;

/* From https://github.com/zbx1425/mtr-nte/blob/master/common/src/main/java/cn/zbx1425/mtrsteamloco/render/scripting/util/MinecraftClientUtil.java */

import com.mojang.text2speech.Narrator;
import org.apache.commons.lang3.NotImplementedException;
import org.mtr.mapping.holder.ClientPlayerEntity;
import org.mtr.mapping.holder.MinecraftClient;
import org.mtr.mapping.holder.Text;
import org.mtr.mapping.holder.Vector3f;
import org.mtr.mapping.mapper.TextHelper;
import org.mtr.mapping.mapper.WorldHelper;

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
        throw new NotImplementedException("Not implemented in Minecraft Mappings");
//        return MinecraftClient.getInstance().getWorldMapped() != null
//                && MinecraftClient.getInstance().getWorldMapped().isRainingAt(pos.toBlockPos());
    }

    public static int worldDayTime() {
        return MinecraftClient.getInstance().getWorldMapped() != null
                ? (int) WorldHelper.getTimeOfDay(MinecraftClient.getInstance().getWorldMapped()) : 0;
    }

    public static void narrate(String message) {
        Narrator.getNarrator().say(message, true);
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