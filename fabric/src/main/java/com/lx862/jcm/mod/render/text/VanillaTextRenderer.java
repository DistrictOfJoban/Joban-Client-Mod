package com.lx862.jcm.mod.render.text;

import com.lx862.jcm.mod.data.JCMServerStats;
import com.lx862.jcm.mod.render.RenderHelper;
import org.mtr.mapping.holder.MutableText;
import org.mtr.mapping.mapper.GraphicsHolder;

/**
 * Wrapper around vanilla text rendering
 */
public class VanillaTextRenderer implements RenderHelper {
    public static void draw(GraphicsHolder graphicsHolder, TextInfo text, double x, double y) {
        drawInternal(graphicsHolder, text, x, y);
    }

    private static void drawInternal(GraphicsHolder graphicsHolder, TextInfo text, double x, double y) {
        int textWidth = GraphicsHolder.getTextWidth(text.toMutableText());
        double finalX = text.getTextAlignment().getX(x, textWidth);

        if(text.isForScrollingText()) {
            drawScrollingText(graphicsHolder, text, textWidth, finalX, y);
        } else {
            MutableText finalText = text.toMutableText();
            graphicsHolder.drawText(finalText, (int)finalX, (int)y, text.getTextColor(), false, MAX_RENDER_LIGHT);
        }
    }

    // HEAVY WIP
    public static void drawScrollingText(GraphicsHolder graphicsHolder, TextInfo text, int textWidth, double x, double y) {
        int maxWidth = (int)text.getWidthInfo().getMaxWidth();
        int textColor = text.getTextColor();
        String str = text.getContent();

        int totalScrollDuration = str.length() * 6;
        int tickNow = (JCMServerStats.getGameTick() % totalScrollDuration);
        double prg = (double)tickNow / totalScrollDuration; //
//        double prg = (str.length() * (double)fullTick / scrollSpeed);

        int shiftX = 0;
        graphicsHolder.push();
        graphicsHolder.translate(-prg * textWidth, 0, 0);
        for(int i = 0; i < str.length(); i++) {
            String s = String.valueOf(str.charAt(i));
            MutableText mt = text.copy(s).toMutableText();
            int finalCharX = (int)(x + shiftX);

            if(shiftX - (-prg * textWidth) >= 0) {
                graphicsHolder.drawText(mt, finalCharX, (int)y, textColor, false, MAX_RENDER_LIGHT);
            }

            shiftX += GraphicsHolder.getTextWidth(mt);
        }
        graphicsHolder.pop();
    }

    public static int getTextWidth(TextInfo text) {
        return GraphicsHolder.getTextWidth(text.toMutableText());
    }
}
