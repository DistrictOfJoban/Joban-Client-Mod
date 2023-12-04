package com.lx862.jcm.mod.trm;

import org.jetbrains.annotations.NotNull;

public class TextSlot implements Comparable<TextSlot> {
    private static final long EXPIRE_TIME = 500;
    private final int startX;
    private TextInfo text = null;
    private int startY;
    private long lastAccessTime = System.currentTimeMillis();
    private int width;
    private int height = 64;

    public TextSlot(int startX, int startY) {
        this.startX = startX;
        this.startY = startY;
    }

    /**
     * @return Whether this TextSlot is empty and does not have any text associated with it
     */
    public boolean unused() {
        return text == null;
    }

    /**
     * @return Whether this TextSlot has expired and can be reassigned with another text (EXPIRE_TIME).
     */
    public boolean reusable() {
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

    public float getMaxWidth() {
        return text.getWidthInfo().getMaxWidth();
    }

    public int getHeight() {
        return height;
    }

    public double getActualPhysicalWidth() {
        return ((double)width / TextureTextRenderer.FONT_RESOLUTION) * TextureTextRenderer.RENDERED_TEXT_SIZE;
    }

    public double getPhysicalWidth() {
        if(text.getWidthInfo().getTargetWidth() != -1) {
            return text.getWidthInfo().getTargetWidth();
        }

        boolean needBoundText = text != null && text.isForScrollingText();
        return needBoundText ? Math.min(text.getWidthInfo().getMaxWidth(), getActualPhysicalWidth()) : getActualPhysicalWidth();
    }

    public void setContent(TextInfo text, int textWidth, int textHeight) {
        this.text = text;
        this.lastAccessTime = System.currentTimeMillis();
        this.width = textWidth;
        this.height = textHeight;

        startY = startY - TextureTextRenderer.FONT_RESOLUTION + height;
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
