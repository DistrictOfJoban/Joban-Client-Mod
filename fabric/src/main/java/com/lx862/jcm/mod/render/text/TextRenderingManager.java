package com.lx862.jcm.mod.render.text;

import com.lx862.jcm.mod.render.RenderHelper;
import org.mtr.mapping.holder.Direction;
import org.mtr.mapping.mapper.GraphicsHolder;
import org.mtr.mapping.mapper.GuiDrawing;

public class TextRenderingManager implements RenderHelper {
    public static void draw(GraphicsHolder graphicsHolder, GuiDrawing guiDrawing, TextInfo text, double x, double y) {
        drawInternal(graphicsHolder, guiDrawing, text, null, x, y);
    }

    public static void draw(GraphicsHolder graphicsHolder, TextInfo text, Direction facing, double x, double y) {
        drawInternal(graphicsHolder, null, text, facing, x, y);
    }

    private static void drawInternal(GraphicsHolder graphicsHolder, GuiDrawing guiDrawing, TextInfo text, Direction facing, double x, double y) {
        if(text.getContent().isEmpty()) return;
        VanillaTextRenderer.draw(graphicsHolder, text, x, y);
    }

    public static int getTextWidth(TextInfo text) {
        return VanillaTextRenderer.getTextWidth(text);
    }
}
