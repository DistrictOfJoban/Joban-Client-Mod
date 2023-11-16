package com.lx862.jcm.mod.trm;

import com.lx862.jcm.mod.render.RenderHelper;
import org.mtr.mapping.holder.Direction;
import org.mtr.mapping.holder.MutableText;
import org.mtr.mapping.holder.RenderLayer;
import org.mtr.mapping.mapper.GraphicsHolder;


public class TextRenderingManager implements RenderHelper {
    private static final boolean FALLBACK = false;
    public static void initialize() {
        if(!FALLBACK) {
            TextureTextRenderer.initialize();
        }
    }

    public static void bind(GraphicsHolder graphicsHolder) {
        if(!FALLBACK) {
            graphicsHolder.createVertexConsumer(RenderLayer.getBeaconBeam(TextureTextRenderer.textAtlas, true));
        }
    }


    public static void draw(GraphicsHolder graphicsHolder, MutableText text, Direction facing, int x, int y, int color) {
        draw(graphicsHolder, text, facing, x, y, color, false);
    }
    public static void draw(GraphicsHolder graphicsHolder, MutableText text, Direction facing, int x, int y, int color, boolean centered) {
        draw(graphicsHolder, text.getString(), facing, x, y, color);
    }

    public static void draw(GraphicsHolder graphicsHolder, String text, Direction facing, int x, int y, int color) {
        draw(graphicsHolder, text, facing, x, y, color, false);
    }

    public static void draw(GraphicsHolder graphicsHolder, String text, Direction facing, int x, int y, int color, boolean centered) {
        if(FALLBACK) {
            int finalX = centered ? x - (GraphicsHolder.getTextWidth(text) / 2) : x;
            graphicsHolder.drawText(text, finalX, y, color, false, MAX_RENDER_LIGHT);
        } else {
            TextureTextRenderer.draw(graphicsHolder, text, facing, x, y, color, centered);
        }
    }
}
