package com.lx862.jcm.mod.data.pids.preset.components;

import com.google.gson.JsonObject;
import com.lx862.jcm.mod.data.KVPair;
import com.lx862.jcm.mod.data.pids.preset.PIDSContext;
import com.lx862.jcm.mod.data.pids.preset.components.base.PIDSComponent;
import com.lx862.jcm.mod.data.pids.preset.components.base.TextComponent;
import org.mtr.mapping.holder.Direction;
import org.mtr.mapping.mapper.GraphicsHolder;
import org.mtr.mapping.mapper.GuiDrawing;

public class WeatherTextComponent extends TextComponent {
    private final String sunnyText;
    private final String rainyText;
    private final String thunderstormText;

    public WeatherTextComponent(double x, double y, double width, double height, KVPair additionalParam) {
        super(x, y, width, height, additionalParam);
        this.sunnyText = additionalParam.get("sunnyText", "Sunny");
        this.rainyText = additionalParam.get("rainyText", "Rainy");
        this.thunderstormText = additionalParam.get("thunderstormText", "Thunderstorm");
    }

    @Override
    public void render(GraphicsHolder graphicsHolder, GuiDrawing guiDrawing, Direction facing, PIDSContext context) {
        drawText(graphicsHolder, guiDrawing, facing, context.world.isRaining() ? rainyText : context.world.isThundering() ? thunderstormText : sunnyText);
    }

    public static PIDSComponent parseComponent(double x, double y, double width, double height, JsonObject jsonObject) {
        return new WeatherTextComponent(x, y, width, height, new KVPair(jsonObject));
    }
}
