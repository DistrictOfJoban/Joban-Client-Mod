package com.lx862.jcm.mod.render;

import com.mojang.blaze3d.systems.RenderSystem;
import org.mtr.mapping.holder.Identifier;
import org.mtr.mapping.holder.MinecraftClient;
import org.mtr.mapping.holder.MutableText;
import org.mtr.mapping.mapper.GraphicsHolder;
import org.mtr.mapping.mapper.GuiDrawing;

public interface Renderable {
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
        double scaleX = Math.min(1, maxW / targetW);
        if(scaleX < 1) {
            if(keepAspectRatio) {
                graphicsHolder.scale((float)scaleX, (float)scaleX, (float)scaleX);
            } else {
                graphicsHolder.scale((float)scaleX, 1, 1);
            }
        }
    }

    default void drawScrollableText(GraphicsHolder graphicsHolder, MutableText text, double elapsed, int x, int y, int maxW, int fullHeight, int color, boolean shadow) {
        int textWidth = GraphicsHolder.getTextWidth(text);
        double d = MinecraftClient.getInstance().getWindow().getScaleFactor();
        int i = MinecraftClient.getInstance().getWindow().getFramebufferHeight();

        if(textWidth > maxW) {
            double slideProgress = -((Math.sin(elapsed) / 2) + 0.5);
            graphicsHolder.translate(slideProgress * (textWidth - maxW), 0, 0);
            RenderSystem.setShaderColor(1F, 0.5F, 0.5F, 0.5F);
            // TODO: Scissor not working :(
            RenderSystem.enableScissor((int) (x * d), 0, (int) (maxW * d), i);
            graphicsHolder.drawText(text, x, y, color, shadow, MAX_RENDER_LIGHT);
            RenderSystem.disableScissor();
        } else {
            graphicsHolder.drawText(text, x, y, color, shadow, MAX_RENDER_LIGHT);
        }
    }
}
