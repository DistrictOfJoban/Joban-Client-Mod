package com.lx862.jcm.mod.render;

import org.mtr.mapping.holder.Identifier;
import org.mtr.mapping.mapper.GraphicsHolder;
import org.mtr.mapping.mapper.GuiDrawing;

public interface RenderHelper {
    int MAX_RENDER_LIGHT = 0xF000F0;

    default void drawTexture(GuiDrawing guiDrawing, Identifier identifier, double x, double y, double width, double height) {
        guiDrawing.beginDrawingTexture(identifier);
        guiDrawing.drawTexture(x, y, x + width, y + height, 0, 0, 1, 1);
        guiDrawing.finishDrawingTexture();
    }

    default void drawRectangle(GuiDrawing guiDrawing, double x, double y, double width, double height, int color) {
        guiDrawing.beginDrawingRectangle();
        guiDrawing.drawRectangle(x, y, x + width, y + height, color);
        guiDrawing.finishDrawingRectangle();
    }

    default boolean inRectangle(double targetX, double targetY, int rectX, int rectY, int rectW, int rectH) {
        return (targetX > rectX && targetX <= rectX + rectW) && (targetY > rectY && targetY <= rectY + rectH);
    }

    default void scaleToFitBoundary(GraphicsHolder graphicsHolder, int targetW, double maxW, boolean keepAspectRatio) {
        scaleToFitBoundary(graphicsHolder, targetW, maxW, keepAspectRatio, 0);
    }

    default void scaleToFitBoundary(GraphicsHolder graphicsHolder, int targetW, double maxW, boolean keepAspectRatio, int height) {
        double scaleX = Math.min(1, maxW / targetW);
        if(scaleX < 1) {
            if(keepAspectRatio) {
                if(height > 0) {
                    graphicsHolder.translate(0, height / 2.5, 0);
                }
                graphicsHolder.scale((float)scaleX, (float)scaleX, (float)scaleX);
                if(height > 0) {
                    graphicsHolder.translate(0, -height / 2.5, 0);
                }
            } else {
                graphicsHolder.scale((float)scaleX, 1, 1);
            }
        }
    }
}
