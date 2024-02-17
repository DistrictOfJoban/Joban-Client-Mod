package com.lx862.jcm.mod.render.text;

import com.lx862.jcm.mod.render.text.font.FontManager;
import com.lx862.jcm.mod.render.text.font.FontSet;
import com.lx862.jcm.mod.util.TextUtil;
import org.mtr.mapping.holder.Identifier;
import org.mtr.mapping.holder.MutableText;
import org.mtr.mapping.holder.TextFormatting;

import java.awt.*;

public class TextInfo {
    private final String content;
    private final WidthInfo widthInfo;
    private Identifier fontId;
    private TextAlignment textAlignment = TextAlignment.LEFT;
    private int textColor;
    private boolean forScrollingText = false;

    public TextInfo(String content) {
        this.content = content;
        this.widthInfo = new WidthInfo();
    }

    public TextInfo(MutableText text) {
        this.content = text.getString();
        this.widthInfo = new WidthInfo();
    }

    public String getContent() {
        return content;
    }

    public int getTextColor() {
        return textColor;
    }

    public WidthInfo getWidthInfo() {
        return widthInfo;
    }

    public TextAlignment getTextAlignment() {
        return textAlignment;
    }

    public boolean isForScrollingText() {
        return this.forScrollingText;
    }

    public TextInfo withScrollingText() {
        this.forScrollingText = true;
        return this;
    }

    public TextInfo withColor(TextFormatting formatting) {
        if(formatting.getColorValue() != null) {
            this.textColor = formatting.getColorValue();
        }
        return this;
    }

    public TextInfo withColor(int color) {
        this.textColor = color;
        return this;
    }

    public TextInfo withTextAlignment(TextAlignment textAlignment) {
        this.textAlignment = textAlignment;
        return this;
    }

    public TextInfo withTargetWidth(int targetWidth) {
        this.widthInfo.setTargetWidth(targetWidth);
        return this;
    }

    public TextInfo withMaxWidth(float maxWidth) {
        this.widthInfo.setMaxWidth(maxWidth);
        return this;
    }

    public TextInfo withFont(String font) {
        this.fontId = new Identifier(font);
        return this;
    }

    /**
     * Get a Font Set, looked up from {@link FontManager}
     */
    public FontSet getFontSet() {
        FontSet preloadedFont = FontManager.getFontSet(fontId);
        if(preloadedFont != null) {
            return preloadedFont;
        } else {
            return new FontSet(Font.getFont(Font.SANS_SERIF));
        }
    }

    public MutableText toMutableText() {
        MutableText text = TextUtil.literal(content);
        if(fontId != null) {
            text = TextUtil.withFont(text, fontId);
        }
        return text;
    }

    @Override
    public boolean equals(Object o) {
        if(!(o instanceof TextInfo)) return false;
        TextInfo other = ((TextInfo) o);
        return other.content.equals(content) && other.textColor == textColor && this.forScrollingText == other.forScrollingText && this.fontId.equals(other.fontId);
    }
}
