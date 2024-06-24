package com.lx862.jcm.mod.data.pids.preset.components;

import com.google.gson.JsonObject;
import com.lx862.jcm.mod.data.KVPair;
import com.lx862.jcm.mod.data.pids.preset.PIDSContext;
import com.lx862.jcm.mod.data.pids.preset.components.base.PIDSComponent;
import com.lx862.jcm.mod.data.pids.preset.components.base.TextComponent;
import org.mtr.mapping.holder.Direction;
import org.mtr.mapping.mapper.GraphicsHolder;
import org.mtr.mapping.mapper.GuiDrawing;

public class CustomTextComponent extends TextComponent {
    private final String text;
    public CustomTextComponent(double x, double y, double width, double height, KVPair addtionalParam) {
        super(x, y, width, height, addtionalParam);
        this.text = addtionalParam.get("text", "");
    }

    @Override
    public void render(GraphicsHolder graphicsHolder, GuiDrawing guiDrawing, Direction facing, PIDSContext context) {
        drawText(graphicsHolder, guiDrawing, facing, text);
    }

    @Override
    public boolean canRender(PIDSContext context) {
        return !text.isEmpty();
    }

    public static PIDSComponent parseComponent(double x, double y, double width, double height, JsonObject jsonObject) {
        return new CustomTextComponent(x, y, width, height, new KVPair(jsonObject));
    }
}
