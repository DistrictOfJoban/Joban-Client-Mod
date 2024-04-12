package com.lx862.jcm.mod.data.pids.preset.components.base;

import com.lx862.jcm.mod.data.JCMClientStats;
import com.lx862.jcm.mod.data.JCMServerStats;
import com.lx862.jcm.mod.render.RenderHelper;
import com.lx862.jcm.mod.render.TextOverflowMode;
import com.lx862.jcm.mod.render.text.TextAlignment;
import com.lx862.jcm.mod.render.text.TextInfo;
import com.lx862.jcm.mod.render.text.TextRenderingManager;
import org.mtr.mapping.holder.Direction;
import org.mtr.mapping.mapper.GraphicsHolder;

public abstract class TextComponent extends DrawCall {
    public static final int SWITCH_LANG_DURATION = 60;
    protected final TextOverflowMode textOverflowMode;
    protected final String font;
    protected final int textColor;
    protected final double scale;

    public TextComponent(String font, TextOverflowMode textOverflowMode, int textColor, double x, double y, double width, double height, double scale) {
        super(x, y, width, height);
        this.font = font;
        this.textColor = textColor;
        this.scale = scale;
        this.textOverflowMode = textOverflowMode;
    }

    public TextComponent(String font, TextOverflowMode textOverflowMode, int textColor, double x, double y, double width, double height) {
        this(font, textOverflowMode, textColor, x, y, width, height, 1);
    }

    protected void drawText(GraphicsHolder graphicsHolder, TextAlignment textAlignment, Direction facing, String text) {
        drawText(graphicsHolder, textAlignment, facing, new TextInfo(text));
    }

    protected void drawText(GraphicsHolder graphicsHolder, TextAlignment textAlignment, Direction facing, TextInfo text) {
        TextInfo finalText = text.withColor(textColor).withFont(font).withTextAlignment(textAlignment);
        graphicsHolder.push();
        graphicsHolder.translate(x, y, 0);
        graphicsHolder.scale((float)scale, (float)scale, (float)scale);
        double textWidth = TextRenderingManager.getTextWidth(finalText);
        if(textOverflowMode == TextOverflowMode.MARQUEE && textWidth > width) {
            finalText = finalText.withScrollingText();
        } else {
            RenderHelper.scaleToFit(graphicsHolder, textWidth, width, textOverflowMode == TextOverflowMode.SCALE, 14);
        }

        TextRenderingManager.draw(graphicsHolder, finalText, facing, 0, 0);
        graphicsHolder.pop();
    }

    protected String cycleString(String mtrString) {
        String[] split = mtrString.split("\\|");
        if(split.length == 0) return "";
        return split[((int)JCMClientStats.getGameTick() / SWITCH_LANG_DURATION) % split.length];
    }

    protected String cycleString(String... string) {
        return string[((int)JCMClientStats.getGameTick() / SWITCH_LANG_DURATION) % string.length];
    }
}
