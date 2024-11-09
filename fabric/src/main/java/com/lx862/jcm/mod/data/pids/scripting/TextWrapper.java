package com.lx862.jcm.mod.data.pids.scripting;

import com.lx862.jcm.mod.util.TextUtil;
import org.mtr.mapping.holder.Direction;
import org.mtr.mapping.holder.Identifier;
import org.mtr.mapping.holder.MutableText;
import org.mtr.mapping.holder.Style;
import org.mtr.mapping.mapper.GraphicsHolder;
import org.mtr.mapping.mapper.TextHelper;
import org.mtr.mod.Init;
import org.mtr.mod.InitClient;

import java.util.ArrayList;
import java.util.List;

import static com.lx862.jcm.mod.render.RenderHelper.MAX_RENDER_LIGHT;

public class TextWrapper extends DrawCall {
    public String str;
    public Identifier fontId;
    public boolean shadow;
    public boolean styleItalic;
    public boolean styleBold;
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

    public TextWrapper wrapText() {
        this.overflowMode = 3;
        return this;
    }

    public TextWrapper marquee() {
        this.overflowMode = 4;
        return this;
    }

    public TextWrapper fontMC() {
        this.fontId = null;
        return this;
    }

    public TextWrapper font(String fontId) {
        return font(new Identifier(fontId));
    }

    public TextWrapper font(Identifier fontId) {
        this.fontId = fontId;
        return this;
    }

    public TextWrapper italic() {
        this.styleItalic = true;
        return this;
    }

    public TextWrapper bold() {
        this.styleBold = true;
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

        List<MutableText> texts = new ArrayList<>();
        final MutableText originalText = getFormattedText(str);

        int actualW = GraphicsHolder.getTextWidth(originalText);
        int actualH = 9;

        if(overflowMode == 1) { // Stretch XY
            if(actualW > w) {
                graphicsHolder.scale((float)(w / actualW), 1, 1);
            }
            if(actualH > h) {
                graphicsHolder.scale(1, (float)(h / actualH), 1);
            }
        } else if(overflowMode == 2) { // Scale XY
            double minScale = Math.min(actualW > w ? w / actualW : 1, actualH > h ? h / actualH : 1);
            graphicsHolder.translate(0, (h - (actualH * minScale)) / 2, 0); // Center it vertically
            graphicsHolder.scale((float)minScale, (float)minScale, 0);
        }

        if(overflowMode == 3) { // Wrap Text
            StringBuilder curLine = new StringBuilder();
            int wSoFar = 0;
            for(int i = 0; i < str.length(); i++) {
                char c = str.charAt(i);
                wSoFar += GraphicsHolder.getTextWidth(String.valueOf(c));
                if(wSoFar > w) {
                    texts.add(getFormattedText(curLine.toString()));
                    curLine = new StringBuilder(String.valueOf(c));
                    wSoFar = 0;
                } else {
                    curLine.append(c);
                }
            }
            if(!curLine.isEmpty()) {
                texts.add(getFormattedText(curLine.toString()));
            }
        } else {
            texts.add(getFormattedText(str));
        }

        if(overflowMode == 4 && actualW > w) { // Marquee
            drawMarqueeText(graphicsHolder, texts.get(0).getString(), color, shadow, MAX_RENDER_LIGHT);
        } else {
            int i = 0;
            for(MutableText text : texts) {
                drawText(graphicsHolder, text, i*9, color, shadow, MAX_RENDER_LIGHT);
                i++;
            }
        }
    }

    private void drawText(GraphicsHolder graphicsHolder, MutableText text, int y, int color, boolean shadow, int light) {
        int startX = 0;
        int totalW = GraphicsHolder.getTextWidth(text);
        if(alignment == 0) {
            startX -= totalW / 2;
        } else if(alignment == 1) {
            startX -= totalW;
        }
        graphicsHolder.drawText(text, startX, y, color, shadow, light);
    }

    private void drawMarqueeText(GraphicsHolder graphicsHolder, String str, int color, boolean shadow, int light) {
        final MutableText text = getFormattedText(str);
        int fullWidth = GraphicsHolder.getTextWidth(text);
        int cycleDuration = str.length() * 16;
        double marqueeProgress = ((InitClient.getGameTick() % cycleDuration) - (cycleDuration/2.0)) / (cycleDuration/2.0);

        double wSoFar = fullWidth * -marqueeProgress;
        for(int i = 0; i < str.length(); i++) {
            String st = String.valueOf(str.charAt(i));
            final MutableText tx = getFormattedText(st);

            if(wSoFar >= 0 && wSoFar <= w) {
                graphicsHolder.push();
                graphicsHolder.translate(wSoFar, 0, 0);
                graphicsHolder.drawText(tx, 0, 0, color, shadow, light);
                graphicsHolder.pop();
            }

            wSoFar += GraphicsHolder.getTextWidth(tx);
        }
    }

    private MutableText getFormattedText(String str) {
        final Style fontStyle;
        if(fontId != null) {
            fontStyle = TextUtil.withFontStyle(fontId).withBold(styleBold).withItalic(styleItalic);
        } else {
            fontStyle = Style.getEmptyMapped().withBold(styleBold).withItalic(styleItalic);
        }

        return TextHelper.setStyle(TextHelper.literal(str), fontStyle);
    }
}
