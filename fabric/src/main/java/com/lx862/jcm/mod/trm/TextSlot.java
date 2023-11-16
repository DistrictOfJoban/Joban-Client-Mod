package com.lx862.jcm.mod.trm;

import org.jetbrains.annotations.NotNull;

public class TextSlot implements Comparable<TextSlot> {
    private static final long EXPIRE_TIME = 500;
    private final int startX;
    private final int startY;
    private long lastAccessTime = System.currentTimeMillis();
    private int width;
    private TextInfo text = null;

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

    public TextInfo getText() {
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

    public int getPixelWidth() {
        return width;
    }

    public double getPhysicalWidth() {
        return ((double)width / TextureTextRenderer.FONT_RESOLUTION) * TextureTextRenderer.RENDERED_TEXT_SIZE;
    }

    public void setContent(TextInfo text, int textWidth) {
        this.text = text;
        this.lastAccessTime = System.currentTimeMillis();
        this.width = textWidth;
    }

    public boolean isHoldingText(TextInfo text) {
        if(this.text == null || text == null) return false;
        return this.text.equals(text);
    }

    @Override
    public int compareTo(@NotNull TextSlot o) {
        return (int)(this.lastAccessTime - o.lastAccessTime);
    }
}
