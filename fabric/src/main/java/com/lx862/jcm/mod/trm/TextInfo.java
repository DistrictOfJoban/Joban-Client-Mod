package com.lx862.jcm.mod.trm;

import org.mtr.mapping.holder.Identifier;
import org.mtr.mapping.holder.MutableText;

import java.awt.*;

public class TextInfo {
    private final String content;
    private final WidthInfo widthInfo;
    private Identifier fontId;
    private int textColor;
    private boolean forScrollingText = false;
    private boolean centered = false;

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

    public boolean isCentered() {
        return centered;
    }

    public boolean isForScrollingText() {
        return this.forScrollingText;
    }

    public TextInfo withScrollingText() {
        this.forScrollingText = true;
        return this;
    }

    public TextInfo withColor(int color) {
        this.textColor = color;
        return this;
    }

    public TextInfo withCentered() {
        this.centered = true;
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

    /**
     * Get the vanilla font identifier
     */
    public Identifier getFontId() {
        return fontId;
    }

    public TextInfo withFont(String font) {
        this.fontId = new Identifier(font);
        return this;
    }

    /**
     * Get an awt Font, looked up from {@link FontManager}
     * @param fontSize The font size of the font
     * @return An awt Font with the corresponding font size
     */
    public Font getAwtFont(int fontSize) {
        Font preloadedFont = FontManager.getFont(fontId);
        if(preloadedFont != null) {
            return preloadedFont.deriveFont(Font.PLAIN, fontSize);
        } else {
            return new Font("Roboto", Font.PLAIN, fontSize);
        }
    }

    @Override
    public boolean equals(Object o) {
        if(!(o instanceof TextInfo)) return false;
        TextInfo other = ((TextInfo) o);
        return other.content.equals(content) && other.textColor == textColor && this.forScrollingText == other.forScrollingText && this.fontId.equals(other.fontId);
    }
}
