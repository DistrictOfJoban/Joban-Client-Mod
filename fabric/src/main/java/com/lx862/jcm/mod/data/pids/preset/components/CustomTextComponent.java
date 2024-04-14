package com.lx862.jcm.mod.data.pids.preset.components;

import com.lx862.jcm.mod.data.pids.preset.components.base.TextComponent;
import com.lx862.jcm.mod.render.TextOverflowMode;
import com.lx862.jcm.mod.render.text.TextAlignment;
import org.mtr.mapping.holder.Direction;
import org.mtr.mapping.holder.World;
import org.mtr.mapping.mapper.GraphicsHolder;
import org.mtr.mapping.mapper.GuiDrawing;

public class CustomTextComponent extends TextComponent {
    private final String text;
    public CustomTextComponent(String font, TextOverflowMode textOverflowMode, TextAlignment textAlignment, int textColor, String text, double x, double y, double width, double height, double scale) {
        super(font, textOverflowMode, textAlignment, textColor, x, y, width, height, scale);
        this.text = text;
    }

    @Override
    public void render(GraphicsHolder graphicsHolder, GuiDrawing guiDrawing, World world, Direction facing) {
        drawText(graphicsHolder, guiDrawing, facing, text);
    }
}
