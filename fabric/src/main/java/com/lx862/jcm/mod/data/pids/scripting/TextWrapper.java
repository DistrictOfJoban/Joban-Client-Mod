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
    public int alignment;
    public int color;

    public TextWrapper(String str) {
        super(100, 25);
        this.str = str;
        this.alignment = -1;
        this.scale = 1;
        this.fontId = new Identifier(Init.MOD_ID, "mtr");
    }

    public TextWrapper pos(double x, double y) {
        this.x = x;
        this.y = y;
        return this;
    }

    public TextWrapper size(double w, double h) {
        this.w = w;
        this.h = h;
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
    protected void drawTransformed(GraphicsHolder graphicsHolder, Direction facing) {
        graphicsHolder.scale((float)scale, (float)scale, (float)scale);
        MutableText finalText = TextHelper.literal(str);
        if(fontId != null) {
            finalText = TextUtil.withFont(finalText, fontId);
        }

        int totalW = GraphicsHolder.getTextWidth(finalText);
        int startX = 0;
        if(alignment == 0) {
            startX -= totalW / 2;
        } else if(alignment == 1) {
            startX -= totalW;
        }

        graphicsHolder.drawText(finalText, startX, 0, color, shadow, MAX_RENDER_LIGHT);
    }
}
