package com.lx862.jcm.mod.render;

import com.lx862.jcm.mod.JCMClient;
import com.lx862.jcm.mod.data.Pair;
import org.mtr.mapping.holder.Identifier;
import org.mtr.mapping.holder.MutableText;
import org.mtr.mapping.mapper.GraphicsHolder;
import org.mtr.mapping.mapper.GuiDrawing;

import static com.lx862.jcm.mod.render.RenderHelper.MAX_RENDER_LIGHT;

public interface GuiHelper {
    int MAX_CONTENT_WIDTH = 400;
    int BOTTOM_ROW_MARGIN = 6;
    int MAX_BUTTON_WIDTH = 375;
    int MAX_ALPHA = 0xFF000000;

    static void drawTexture(GuiDrawing guiDrawing, Identifier identifier, double x, double y, double width, double height) {
        Pair<Float, Float> uv = JCMClient.getMcMetaManager().getUV(identifier);
        drawTexture(guiDrawing, identifier, x, y, width, height, 0, uv.getLeft(), 1, uv.getRight());
    }

    static void drawTexture(GuiDrawing guiDrawing, Identifier identifier, double x, double y, double width, double height, float u1, float v1, float u2, float v2) {
        guiDrawing.beginDrawingTexture(identifier);
        guiDrawing.drawTexture(x, y, x + width, y + height, u1, v1, u2, v2);
        guiDrawing.finishDrawingTexture();
    }

    static void drawRectangle(GuiDrawing guiDrawing, double x, double y, double width, double height, int color) {
        guiDrawing.beginDrawingRectangle();
        guiDrawing.drawRectangle(x, y, x + width, y + height, color);
        guiDrawing.finishDrawingRectangle();
    }

    /**
     * Draw text that would shift back and fourth if there's not enough space to display
     * Similar to the scrollable text added in Minecraft 1.19.4
     * @param graphicsHolder Graphics holder
     * @param text The text to display
     * @param elapsed The time elapsed, this would dictate the scrolling animation speed
     * @param startX The start X where your text should be clipped. (Measure from the left edge of your window)
     * @param textX The text X that would be rendered
     * @param textY The text Y that would be rendered
     * @param maxW The maximum width allowed for your text
     * @param color Color of the text
     * @param shadow Whether text should be rendered with shadow
     */
    static void drawScrollableText(GraphicsHolder graphicsHolder, MutableText text, double elapsed, int startX, int textX, int textY, int maxW, int color, boolean shadow) {
        int textWidth = GraphicsHolder.getTextWidth(text);

        if(textWidth > maxW) {
            double slideProgress = ((Math.sin(elapsed / 4)) / 2) + 0.5;
            graphicsHolder.translate(-slideProgress * (textWidth - maxW), 0, 0);
            ClipStack.add(startX, 0, maxW, 500);
            graphicsHolder.drawText(text, textX, textY, color, shadow, MAX_RENDER_LIGHT);
            ClipStack.pop();
        } else {
            graphicsHolder.drawText(text, textX, textY, color, shadow, MAX_RENDER_LIGHT);
        }
    }

    /**
     * This clamps the inputted button width to {@link GuiHelper#MAX_BUTTON_WIDTH } to prevent button texture glitch on 1.16 if the button is too wide
     * @param width The desired button width
     * @return The clamped button width
     */
    static int getButtonWidth(double width) {
        return (int)Math.min(MAX_BUTTON_WIDTH, width);
    }
}
