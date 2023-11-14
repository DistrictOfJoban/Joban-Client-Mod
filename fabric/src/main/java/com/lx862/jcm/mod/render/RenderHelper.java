package com.lx862.jcm.mod.render;

import com.lx862.jcm.mod.data.JCMStats;
import com.lx862.jcm.mod.resources.mcmeta.McMetaManager;
import com.lx862.jcm.mod.util.TextUtil;
import net.minecraft.util.Pair;
import org.mtr.mapping.holder.Direction;
import org.mtr.mapping.holder.Identifier;
import org.mtr.mapping.holder.MutableText;
import org.mtr.mapping.mapper.GraphicsHolder;
import org.mtr.mapping.mapper.GuiDrawing;

public interface RenderHelper {
    int MAX_RENDER_LIGHT = 0xF000F0;
    int ARGB_BLACK = 0xFF000000;
    int ARGB_WHITE = 0xFFFFFFFF;

    default void drawTexture(GuiDrawing guiDrawing, Identifier identifier, double x, double y, double width, double height) {
        drawTexture(guiDrawing, identifier, x, y, width, height, 0, 0, 1, 1);
    }

    default void drawTexture(GuiDrawing guiDrawing, Identifier identifier, double x, double y, double width, double height, float u1, float v1, float u2, float v2) {
        guiDrawing.beginDrawingTexture(identifier);
        guiDrawing.drawTexture(x, y, x + width, y + height, u1, v1, u2, v2);
        guiDrawing.finishDrawingTexture();
    }

    default void drawRectangle(GuiDrawing guiDrawing, double x, double y, double width, double height, int color) {
        guiDrawing.beginDrawingRectangle();
        guiDrawing.drawRectangle(x, y, x + width, y + height, color);
        guiDrawing.finishDrawingRectangle();
    }

    default void drawText(GraphicsHolder graphicsHolder, MutableText text, int x, int y, int textColor) {
        graphicsHolder.drawText(text, x, y, textColor, false, MAX_RENDER_LIGHT);
    }

    default void drawMarqueeText(GraphicsHolder graphicsHolder, MutableText text, int maxW, int x, int y, int textColor) {
        String str = text.getString();
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
        newText.setStyle(text.getStyle());
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

        drawText(graphicsHolder, newText, x, y, textColor);
        graphicsHolder.pop();
    }

    default void drawCenteredText(GraphicsHolder graphicsHolder, MutableText text, int x, int y, int textColor) {
        int w = GraphicsHolder.getTextWidth(text.asOrderedText());
        graphicsHolder.drawText(text, x - (w / 2), y, textColor, false, MAX_RENDER_LIGHT);
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

    /**
     * Draw a texture to the world, it will automatically look up for mcmeta data and set the appropriate UV.<br>
     * Use this method if you need support for drawing animated McMeta textures
     */
    static void drawTexture(GraphicsHolder graphicsHolder, Identifier textureId, float x, float y, float z, float width, float height, Direction facing, int color, int light) {
        Pair<Float, Float> uv = McMetaManager.getUV(textureId);
        drawTextureRaw(graphicsHolder, x, y, z, x + width, y + height, z, 0, uv.getLeft(), 1, uv.getRight(), facing, color, light);
    }

    static void drawTexture(GraphicsHolder graphicsHolder, float x, float y, float z, float width, float height, int u1, int v1, int u2, int v2, Direction facing, int color, int light) {
        drawTextureRaw(graphicsHolder, x, y, z, x + width, y + height, z, u1, v1, u2, v2, facing, color, light);
    }

    static void drawTexture(GraphicsHolder graphicsHolder, float x, float y, float z, float width, float height, Direction facing, int color, int light) {
        drawTextureRaw(graphicsHolder, x, y, z, x + width, y + height, z, 0, 0, 1, 1, facing, color, light);
    }

    static void drawTextureRaw(GraphicsHolder graphicsHolder, float x1, float y1, float z1, float x2, float y2, float z2, float u1, float v1, float u2, float v2, Direction facing, int color, int light) {
        graphicsHolder.drawTextureInWorld(x1, y2, z1, x2, y2, z2, x2, y1, z2, x1, y1, z1, u1, v1, u2, v2, facing, color, light);
    }
}