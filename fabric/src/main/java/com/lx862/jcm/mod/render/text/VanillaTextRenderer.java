package com.lx862.jcm.mod.render.text;

import com.lx862.jcm.mod.render.RenderHelper;
import org.mtr.mapping.holder.MutableText;
import org.mtr.mapping.mapper.GraphicsHolder;

/**
 * Wrapper around vanilla text rendering
 */
@Deprecated
public class VanillaTextRenderer implements RenderHelper {
    public static void draw(GraphicsHolder graphicsHolder, TextInfo textInfo, double x, double y) {
        drawInternal(graphicsHolder, textInfo.toMutableText(), textInfo.getTextAlignment(), textInfo.getTextColor(), x, y);
    }

    private static void drawInternal(GraphicsHolder graphicsHolder, MutableText text, TextAlignment textAlignment, int textColor, double x, double y) {
        int textWidth = GraphicsHolder.getTextWidth(text);
        double finalX = textAlignment.getX(x, textWidth);

        graphicsHolder.drawText(text, (int)finalX, (int)y, textColor, false, MAX_RENDER_LIGHT);
    }
}
