package com.lx862.jcm.mod.trm;

import org.jetbrains.annotations.NotNull;

import java.awt.*;

public class TextSlot implements Comparable<TextSlot> {
    private static final long EXPIRE_TIME = 1000;
    private final int startX;
    private final int startY;
    private long lastAccessTime = System.currentTimeMillis();
    private int width;
    private int color;
    private String text = null;

    public TextSlot(int startX, int startY) {
        this.startX = startX;
        this.startY = startY;
    }
    public boolean unused() {
        return text == null;
    }

    public boolean canReuse() {
        return System.currentTimeMillis() - lastAccessTime > EXPIRE_TIME;
    }

    public String getText() {
        return text;
    }

    public void updateLastAccessTime() {
        this.lastAccessTime = System.currentTimeMillis();
    }

    public int getStartX() {
        return startX;
    }

    public int getStartY() {
        return startY;
    }

    public int getWidth() {
        return width;
    }

    public int getColor() {
        return color;
    }

    public double getScaledWidth() {
        return (double)width / TextureTextRenderer.FONT_RESOLUTION;
    }

    public void setText(String text, Graphics graphics) {
        this.text = text;
        this.lastAccessTime = System.currentTimeMillis();
        this.width = graphics.getFontMetrics().stringWidth(text);
    }

    public void setColor(int color) {
        this.color = color;
    }

    @Override
    public boolean equals(Object other) {
        if(!(other instanceof TextSlot)) return false;
        TextSlot textSlot = (TextSlot) other;
        return textSlot.getText().equals(this.getText()) &&
                textSlot.getColor() == this.getColor();
    }

    public boolean equals(String text, int color) {
        if(getText() == null || text == null) return false;
        return this.getText().equals(text) && this.color == color;
    }

    @Override
    public int compareTo(@NotNull TextSlot o) {
        return (int)(this.lastAccessTime - o.lastAccessTime);
    }
}
