package com.lx862.jcm.mod.trm;

import org.mtr.mapping.holder.MutableText;

public class TextInfo {
    private final String content;
    private final String font;
    private int textColor;
    private boolean forScrollingText = false;

    public TextInfo(String content) {
        this.content = content;
        this.textColor = 0;
        this.font = "Roboto";
    }

    public TextInfo(MutableText text) {
        this.content = text.getString();
        this.textColor = text.getStyle().getColor() == null ? 0 : text.getStyle().getColor().getRgb();
        this.font = "Roboto";/*text.getStyle().getFont().getNamespace() + ":" + text.getStyle().getFont().getPath()*/;
    }

    public String getContent() {
        return content;
    }

    public int getTextColor() {
        return textColor;
    }

    public String getFont() {
        return font;
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

    @Override
    public boolean equals(Object o) {
        if(!(o instanceof TextInfo)) return false;
        TextInfo other = ((TextInfo) o);
        boolean canReuseStretchedText = (!forScrollingText && other.forScrollingText) || (forScrollingText == other.forScrollingText);
        return other.content.equals(content) && other.textColor == textColor && canReuseStretchedText;
    }
}
