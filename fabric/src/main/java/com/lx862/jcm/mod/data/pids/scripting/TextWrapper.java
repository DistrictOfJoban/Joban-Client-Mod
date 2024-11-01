package com.lx862.jcm.mod.data.pids.scripting;

import com.lx862.jcm.mod.util.TextUtil;
import org.mtr.mapping.holder.Direction;
import org.mtr.mapping.holder.Identifier;
import org.mtr.mapping.holder.MutableText;
import org.mtr.mapping.mapper.GraphicsHolder;
import org.mtr.mapping.mapper.TextHelper;
import org.mtr.mod.Init;

import static com.lx862.jcm.mod.render.RenderHelper.MAX_RENDER_LIGHT;

public class TextWrapper extends DrawCall {
    public String str;
    public Identifier fontId;
    public boolean shadow;
    public double scale;
    public int overflowMode;
    public int alignment;
    public int color;

    protected TextWrapper() {
        super(100, 25);
        this.alignment = -1;
        this.scale = 1;
        this.fontId = new Identifier(Init.MOD_ID, "mtr");
    }

    public static TextWrapper create() {
        return new TextWrapper();
    }

    public static TextWrapper create(String comment) {
        return create();
    }

    public TextWrapper text(String str) {
        this.str = str;
        return this;
    }

    public TextWrapper scale(double i) {
        this.scale = i;
        return this;
    }

    public TextWrapper leftAlign() {
        this.alignment = -1;
        return this;
    }

    public TextWrapper centerAlign() {
        this.alignment = 0;
        return this;
    }

    public TextWrapper rightAlign() {
        this.alignment = 1;
        return this;
    }

    public TextWrapper shadowed() {
        this.shadow = true;
        return this;
    }

    public TextWrapper stretchXY() {
        this.overflowMode = 1;
        return this;
    }

    public TextWrapper scaleXY() {
        this.overflowMode = 2;
        return this;
    }

    public TextWrapper font(String fontId) {
        return font(new Identifier(fontId));
    }

    public TextWrapper font(Identifier fontId) {
        this.fontId = fontId;
        return this;
    }

    public TextWrapper color(int color) {
        this.color = color;
        return this;
    }

    @Override
    protected void validate() {
        if(str == null) throw new IllegalArgumentException("Text must be filled");
    }

    @Override
    protected void drawTransformed(GraphicsHolder graphicsHolder, Direction facing) {
        graphicsHolder.scale((float)scale, (float)scale, (float)scale);
        MutableText finalText = TextHelper.literal(str);
        if(fontId != null) {
            finalText = TextUtil.withFont(finalText, fontId);
        }

        int totalW = GraphicsHolder.getTextWidth(finalText);
        int totalH = 9;

        if(overflowMode == 1) {
            if(totalW > w) {
                graphicsHolder.scale((float)(w / totalW), 1, 1);
            }
            if(h > 9) {
                graphicsHolder.scale(1, (float)(totalH / h), 1);
            }
        } else if(overflowMode == 2) {
            double minScale = Math.min(totalW > w ? w / totalW : 1, h > totalH ? totalH / h : 1);
            graphicsHolder.translate(0, (h - (totalH * minScale)) / 2, 0); // Center it vertically
            graphicsHolder.scale((float)minScale, (float)minScale, 0);
        }

        int startX = 0;
        if(alignment == 0) {
            startX -= totalW / 2;
        } else if(alignment == 1) {
            startX -= totalW;
        }

        graphicsHolder.drawText(finalText, startX, 0, color, shadow, MAX_RENDER_LIGHT);
    }
}
