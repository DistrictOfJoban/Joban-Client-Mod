package com.lx862.jcm.mod.trm;

import org.mtr.mapping.holder.MutableText;

public class TextInfo {
    private final String content;
    private final String font;
    private final WidthInfo widthInfo;
    private int textColor;
    private boolean forScrollingText = false;

    public TextInfo(String content) {
        this.content = content;
        this.textColor = 0;
        this.font = "Roboto";
        this.widthInfo = new WidthInfo();
    }

    public TextInfo(MutableText text) {
        this.content = text.getString();
        this.textColor = text.getStyle().getColor() == null ? 0 : text.getStyle().getColor().getRgb();
        this.font = "Roboto";/*text.getStyle().getFont().getNamespace() + ":" + text.getStyle().getFont().getPath()*/;
        this.widthInfo = new WidthInfo();
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

    public WidthInfo getWidthInfo() {
        return widthInfo;
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

    public TextInfo withTargetWidth(int targetWidth) {
        this.widthInfo.setTargetWidth(targetWidth);
        return this;
    }

    public TextInfo withMaxWidth(float maxWidth) {
        this.widthInfo.setMaxWidth(maxWidth);
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if(!(o instanceof TextInfo)) return false;
        TextInfo other = ((TextInfo) o);
        return other.content.equals(content) && other.textColor == textColor && this.forScrollingText == other.forScrollingText;
    }
}
