package com.lx862.jcm.mod.render.text;

import com.lx862.jcm.mod.data.JCMStats;
import com.lx862.jcm.mod.render.RenderHelper;
import com.lx862.jcm.mod.util.TextUtil;
import org.mtr.mapping.holder.MutableText;
import org.mtr.mapping.mapper.GraphicsHolder;

/**
 * Wrapper around vanilla text rendering
 */
public class VanillaTextRenderer implements RenderHelper {
    public static void draw(GraphicsHolder graphicsHolder, TextInfo text, int x, int y) {
        drawInternal(graphicsHolder, text, x, y);
    }

    private static void drawInternal(GraphicsHolder graphicsHolder, TextInfo text, int x, int y) {
        int textWidth = GraphicsHolder.getTextWidth(text.toMutableText());
        double finalX = text.getTextAlignment().getX(x, textWidth);
        MutableText finalText = text.toMutableText();
        graphicsHolder.drawText(finalText, (int)finalX, y, text.getTextColor(), false, MAX_RENDER_LIGHT);
    }

    public static void drawScrollingText(GraphicsHolder graphicsHolder, String text, int maxW, int x, int y, int textColor) {
        String str = text;
        int scrollSpeed = str.length() * 6;
        int fullTick = (JCMStats.getGameTick() % (int)(scrollSpeed * 1.5));
        int halfTick = scrollSpeed / 2;
        boolean opening = fullTick < halfTick;
        int tick = opening ? fullTick : fullTick - halfTick;

        double prg = (str.length() * (double)tick / scrollSpeed);
        int start = opening ? 0 : (int)prg;

        int widthSoFar = 0;
        int nextCharWidth = 0;
        int end = 0;
        for(int i = 0; i < str.length(); i++) {
            String s = String.valueOf(str.charAt(i));
            int width = GraphicsHolder.getTextWidth(s);
            if(i == 0 && !opening) {
                nextCharWidth = width;
            }

            if(widthSoFar + width < maxW) {
                widthSoFar += width;
                end++;
            } else {
                if(opening) nextCharWidth = width;
            }
        }

        end = opening ? (int)Math.max(0, prg - 2) : end - 1;

        MutableText newText = TextUtil.literal(str.substring(Math.max(0, start), Math.min(str.length(), start + end)));
        graphicsHolder.push();
        if(opening) {
            graphicsHolder.translate(maxW, 0, 0);
            graphicsHolder.translate(nextCharWidth * 2, 0, 0);
            graphicsHolder.translate(-(end * nextCharWidth), 0, 0);
            graphicsHolder.translate(-((prg - end) * nextCharWidth), 0, 0);
        } else {
            graphicsHolder.translate(nextCharWidth, 0, 0);
            graphicsHolder.translate(-((prg - start) * nextCharWidth), 0, 0);
        }

        graphicsHolder.drawText(newText, x, y, textColor, false, MAX_RENDER_LIGHT);
        graphicsHolder.pop();
    }

    public static int getTextWidth(TextInfo text) {
        return GraphicsHolder.getTextWidth(text.toMutableText());
    }
}
