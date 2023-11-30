package com.lx862.jcm.mod.trm;

import com.lx862.jcm.mod.config.ConfigEntry;
import com.lx862.jcm.mod.render.RenderHelper;
import org.mtr.mapping.holder.Direction;
import org.mtr.mapping.holder.MutableText;
import org.mtr.mapping.mapper.GraphicsHolder;

/**
 * <h2>Text Rendering Manager</h2>
 * <p>The text rendering manager for JCM serves as a wrapper for {@link VanillaTextRenderer} and {@link TextureTextRenderer} depending on whether fallback rendering is used.</p>
 * <p>All text rendering in the 3D world should be performed in this class.</p>
 */
public class TextRenderingManager implements RenderHelper {
    /**
     * Initialize the texture atlas for {@link TextureTextRenderer}<br>
     * Nothing will be performed if fallback mode is enabled / using {@link VanillaTextRenderer}
     */
    public static void initialize() {
        if(ConfigEntry.NEW_TEXT_RENDERER.getBool()) {
            TextureTextRenderer.initialize();
        }
    }

    /**
     * This binds the texture in {@link TextureTextRenderer} for use when drawing the text, bind it before you draw texts.<br>
     * Nothing will be performed if fallback mode is enabled / using {@link VanillaTextRenderer}
     */
    public static void bind(GraphicsHolder graphicsHolder) {
        if(ConfigEntry.NEW_TEXT_RENDERER.getBool()) {
            TextureTextRenderer.bindTexture(graphicsHolder);
        }
    }

    public static void drawScrollingText(GraphicsHolder graphicsHolder, TextInfo text, Direction facing, int x, int y, float textWidth) {
        draw(graphicsHolder, text.withMaxWidth(textWidth).withScrollingText(), facing, x, y);
    }

    public static void drawCentered(GraphicsHolder graphicsHolder, TextInfo text, Direction facing, int x, int y) {
        drawInternal(graphicsHolder, text.withCentered(), facing, x, y);
    }

    public static void draw(GraphicsHolder graphicsHolder, MutableText text, Direction facing, int x, int y) {
        draw(graphicsHolder, new TextInfo(text), facing, x, y);
    }

    public static void draw(GraphicsHolder graphicsHolder, TextInfo text, Direction facing, int x, int y) {
        drawInternal(graphicsHolder, text, facing, x, y);
    }

    private static void drawInternal(GraphicsHolder graphicsHolder, TextInfo text, Direction facing, int x, int y) {
        if(!ConfigEntry.NEW_TEXT_RENDERER.getBool()) {
            if(text.isForScrollingText()) {
                VanillaTextRenderer.drawScrollingText(graphicsHolder, text.getContent(), (int)text.getWidthInfo().getMaxWidth(), x, y, text.getTextColor());
            } else {
                VanillaTextRenderer.draw(graphicsHolder, text, x, y);
            }
        } else {
            TextureTextRenderer.draw(graphicsHolder, text, facing, x, y);
        }
    }

    public static int getTextWidth(TextInfo text) {
        if(!ConfigEntry.NEW_TEXT_RENDERER.getBool()) {
            return VanillaTextRenderer.getTextWidth(text);
        } else {
            return TextureTextRenderer.getPhysicalWidth(text);
        }
    }
}
