package com.lx862.jcm.mod.data.pids.preset.components;

import com.google.gson.JsonObject;
import com.lx862.jcm.mod.data.KVPair;
import com.lx862.jcm.mod.data.pids.preset.PIDSContext;
import com.lx862.jcm.mod.data.pids.preset.components.base.PIDSComponent;
import com.lx862.jcm.mod.data.pids.preset.components.base.TextComponent;
import com.lx862.jcm.mod.render.TextOverflowMode;
import com.lx862.jcm.mod.render.text.TextAlignment;
import org.mtr.mapping.holder.Direction;
import org.mtr.mapping.mapper.GraphicsHolder;
import org.mtr.mapping.mapper.GuiDrawing;

public class CustomTextComponent extends TextComponent {
    private final String text;
    public CustomTextComponent(double x, double y, double width, double height, String font, TextAlignment textAlignment, TextOverflowMode textOverflowMode, int textColor, double scale, KVPair param) {
        super(x, y, width, height, font, textAlignment, textOverflowMode, textColor, scale);
        this.text = param.get("text", "");
    }

    @Override
    public void render(GraphicsHolder graphicsHolder, GuiDrawing guiDrawing, Direction facing, PIDSContext context) {
        drawText(graphicsHolder, guiDrawing, facing, text);
    }

    public static PIDSComponent parseComponent(double x, double y, double width, double height, JsonObject jsonObject) {
        TextAlignment textAlignment = TextAlignment.valueOf(jsonObject.get("textAlignment").getAsString());
        TextOverflowMode textOverflowMode = TextOverflowMode.valueOf(jsonObject.get("textOverflowMode").getAsString());
        String font = jsonObject.get("font").getAsString();
        int textColor = jsonObject.get("color").getAsInt();
        double scale = jsonObject.get("scale").getAsDouble();
        return new CustomTextComponent(x, y, width, height, font, textAlignment, textOverflowMode, textColor, scale, new KVPair(jsonObject));
    }
}
