package com.lx862.jcm.mod.render.text;

import com.lx862.jcm.mod.render.RenderHelper;
import org.mtr.mapping.holder.Direction;
import org.mtr.mapping.mapper.GraphicsHolder;
import org.mtr.mapping.mapper.GuiDrawing;

@Deprecated
public class TextRenderingManager implements RenderHelper {
    @Deprecated
    public static void draw(GraphicsHolder graphicsHolder, GuiDrawing guiDrawing, TextInfo text, double x, double y) {
        drawInternal(graphicsHolder, text, x, y);
    }

    public static void draw(GraphicsHolder graphicsHolder, TextInfo text, Direction facing, double x, double y) {
        drawInternal(graphicsHolder, text, x, y);
    }

    private static void drawInternal(GraphicsHolder graphicsHolder, TextInfo text, double x, double y) {
        if(text.getContent().isEmpty()) return;
        VanillaTextRenderer.draw(graphicsHolder, text, x, y);
    }
}
