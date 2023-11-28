package com.lx862.jcm.mod.render;

import com.lx862.jcm.mod.resources.mcmeta.McMetaManager;
import net.minecraft.util.Pair;
import org.mtr.mapping.holder.Direction;
import org.mtr.mapping.holder.Identifier;
import org.mtr.mapping.holder.MutableText;
import org.mtr.mapping.mapper.GraphicsHolder;

public interface RenderHelper {
    int MAX_RENDER_LIGHT = 0xF000F0;
    int ARGB_BLACK = 0xFF000000;
    int ARGB_WHITE = 0xFFFFFFFF;

    static void drawText(GraphicsHolder graphicsHolder, MutableText text, int x, int y, int textColor) {
        graphicsHolder.drawText(text, x, y, textColor, false, MAX_RENDER_LIGHT);
    }

    static void drawCenteredText(GraphicsHolder graphicsHolder, MutableText text, int x, int y, int textColor) {
        int w = GraphicsHolder.getTextWidth(text.asOrderedText());
        graphicsHolder.drawText(text, x - (w / 2), y, textColor, false, MAX_RENDER_LIGHT);
    }

    default boolean inRectangle(double targetX, double targetY, int rectX, int rectY, int rectW, int rectH) {
        return (targetX > rectX && targetX <= rectX + rectW) && (targetY > rectY && targetY <= rectY + rectH);
    }

    static void scaleToFit(GraphicsHolder graphicsHolder, int targetW, double maxW, boolean keepAspectRatio) {
        scaleToFit(graphicsHolder, targetW, maxW, keepAspectRatio, 0);
    }

    static void scaleToFit(GraphicsHolder graphicsHolder, int targetW, double maxW, boolean keepAspectRatio, int height) {
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

    static void drawTexture(GraphicsHolder graphicsHolder, float x, float y, float z, float width, float height, float u1, float v1, float u2, float v2, Direction facing, int color, int light) {
        drawTextureRaw(graphicsHolder, x, y, z, x + width, y + height, z, u1, v1, u2, v2, facing, color, light);
    }

    static void drawTexture(GraphicsHolder graphicsHolder, float x, float y, float z, float width, float height, Direction facing, int color, int light) {
        drawTextureRaw(graphicsHolder, x, y, z, x + width, y + height, z, 0, 0, 1, 1, facing, color, light);
    }

    static void drawTextureRaw(GraphicsHolder graphicsHolder, float x1, float y1, float z1, float x2, float y2, float z2, float u1, float v1, float u2, float v2, Direction facing, int color, int light) {
        graphicsHolder.drawTextureInWorld(x1, y2, z1, x2, y2, z2, x2, y1, z2, x1, y1, z1, u1, v1, u2, v2, facing, color, light);
    }
}