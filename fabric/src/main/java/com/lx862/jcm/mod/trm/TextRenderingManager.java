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

    public static void drawScrollingText(GraphicsHolder graphicsHolder, TextInfo text, Direction facing, int x, int y, float textWidth) {
        draw(graphicsHolder, text.withMaxWidth(textWidth).withScrollingText(), facing, x, y);
    }

    public static void drawCentered(GraphicsHolder graphicsHolder, TextInfo text, Direction facing, int x, int y) {
        drawInternal(graphicsHolder, text, facing, x, y, true);
    }

    public static void draw(GraphicsHolder graphicsHolder, MutableText text, Direction facing, int x, int y) {
        draw(graphicsHolder, new TextInfo(text), facing, x, y);
    }

    public static void draw(GraphicsHolder graphicsHolder, TextInfo text, Direction facing, int x, int y) {
        drawInternal(graphicsHolder, text, facing, x, y, false);
    }

    private static void drawInternal(GraphicsHolder graphicsHolder, TextInfo text, Direction facing, int x, int y, boolean centered) {
        if(FALLBACK) {
            if(text.isForScrollingText()) {
                VanillaTextRenderer.drawVanillaScrollingText(graphicsHolder, text.getContent(), (int)text.getWidthInfo().getMaxWidth(), x, y, text.getTextColor());
            } else {
                VanillaTextRenderer.draw(graphicsHolder, text, x, y, centered);
            }
        } else {
            TextureTextRenderer.draw(graphicsHolder, text, facing, x, y, centered);
        }
    }

    public static int getTextWidth(TextInfo text) {
        if(FALLBACK) {
            return VanillaTextRenderer.getTextWidth(text);
        } else {
            return TextureTextRenderer.getPhysicalWidth(text);
        }
    }
}
