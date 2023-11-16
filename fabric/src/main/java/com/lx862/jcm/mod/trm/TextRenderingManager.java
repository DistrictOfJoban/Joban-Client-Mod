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

    public static void drawCentered(GraphicsHolder graphicsHolder, String text, Direction facing, int x, int y, int color) {
        drawCentered(graphicsHolder, new TextInfo(text).withColor(color), facing, x, y);
    }

    public static void drawCentered(GraphicsHolder graphicsHolder, MutableText text, Direction facing, int x, int y) {
        drawCentered(graphicsHolder, new TextInfo(text), facing, x, y);
    }

    public static void drawCentered(GraphicsHolder graphicsHolder, TextInfo text, Direction facing, int x, int y) {
        draw(graphicsHolder, text, facing, x, y, true);
    }

    public static void draw(GraphicsHolder graphicsHolder, String text, Direction facing, int x, int y, int color) {
        draw(graphicsHolder, new TextInfo(text).withColor(color), facing, x, y, false);
    }

    public static void draw(GraphicsHolder graphicsHolder, MutableText text, Direction facing, int x, int y) {
        draw(graphicsHolder, new TextInfo(text), facing, x, y);
    }

    public static void draw(GraphicsHolder graphicsHolder, TextInfo text, Direction facing, int x, int y) {
        draw(graphicsHolder, text, facing, x, y, false);
    }


    public static void draw(GraphicsHolder graphicsHolder, TextInfo text, Direction facing, int x, int y, boolean centered) {
        if(FALLBACK) {
            int finalX = centered ? x - (GraphicsHolder.getTextWidth(text.getContent()) / 2) : x;
            graphicsHolder.drawText(text.getContent(), finalX, y, text.getTextColor(), false, MAX_RENDER_LIGHT);
        } else {
            TextureTextRenderer.draw(graphicsHolder, text, facing, x, y, centered);
        }
    }

    public static int getTextWidth(TextInfo text) {
        if(FALLBACK) {
            return GraphicsHolder.getTextWidth(text.getContent());
        } else {
            return TextureTextRenderer.getPhysicalWidth(text);
        }
    }
}
