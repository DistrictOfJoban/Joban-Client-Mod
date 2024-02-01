package com.lx862.jcm.mod.render.text;

public class WidthInfo {
    private float targetWidth = -1;
    private float maxWidth = -1;
    public void setTargetWidth(float targetWidth) {
        this.targetWidth = targetWidth;
    }

    public void setMaxWidth(float maxWidth) {
        this.maxWidth = maxWidth;
    }

    public float getTargetWidth() {
        return targetWidth;
    }

    public float getMaxWidth() {
        return maxWidth;
    }
}
