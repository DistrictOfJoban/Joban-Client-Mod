package com.lx862.jcm.mod.data.pids.preset.components;

import com.google.gson.JsonObject;
import com.lx862.jcm.mod.data.KVPair;
import com.lx862.jcm.mod.data.pids.preset.PIDSContext;
import com.lx862.jcm.mod.data.pids.preset.components.base.PIDSComponent;
import com.lx862.jcm.mod.data.pids.preset.components.base.TextComponent;
import org.mtr.mapping.holder.Direction;
import org.mtr.mapping.mapper.GraphicsHolder;
import org.mtr.mapping.mapper.GuiDrawing;
import org.mtr.mapping.mapper.WorldHelper;

public class CustomTextComponent extends TextComponent {
    private final String text;
    public CustomTextComponent(double x, double y, double width, double height, KVPair addtionalParam) {
        super(x, y, width, height, addtionalParam);
        this.text = addtionalParam.get("text", "");
    }

    @Override
    public void render(GraphicsHolder graphicsHolder, GuiDrawing guiDrawing, Direction facing, PIDSContext context) {
        drawText(graphicsHolder, guiDrawing, facing, parsePIDSVariable(text, context));
    }

    @Override
    public boolean canRender(PIDSContext context) {
        return !text.isEmpty();
    }

    public static PIDSComponent parseComponent(double x, double y, double width, double height, JsonObject jsonObject) {
        return new CustomTextComponent(x, y, width, height, new KVPair(jsonObject));
    }

    private static String parsePIDSVariable(String str, PIDSContext context) {
        long time = WorldHelper.getTimeOfDay(context.world) + 6000;
        long hours = time / 1000;
        long minutes = Math.round((time - (hours * 1000)) / 16.8);
        String timeString = String.format("%02d:%02d", hours % 24, minutes % 60);
        String weatherString = context.world.isRaining() ? "Raining" : context.world.isThundering() ? "Thundering" : "Sunny";
        String weatherChinString = context.world.isRaining() ? "下雨" : context.world.isThundering() ? "雷暴" : "晴天";
        int worldDay = (int) (WorldHelper.getTimeOfDay(context.world) / 24000L);
        String timeGreetings;

        if (time >= 6000 & time <= 12000) {
            timeGreetings = "Morning";
        } else if (time >= 12000 & time <= 18000) {
            timeGreetings = "Afternoon";
        } else {
            timeGreetings = "Night";
        }

        return str.replace("{time}", timeString)
                .replace("{day}", String.valueOf(worldDay))
                .replace("{weather}", weatherString)
                .replace("{time_period}", timeGreetings)
                .replace("{weatherChin}", weatherChinString)
                .replace("{worldPlayer}", "?");
    }
}
