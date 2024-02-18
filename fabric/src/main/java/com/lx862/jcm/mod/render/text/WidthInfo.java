package com.lx862.jcm.mod.render.text;

public class WidthInfo {
    private float fixedWidth = -1;
    private float maxWidth = -1;
    public void setFixedWidth(float targetWidth) {
        this.fixedWidth = targetWidth;
    }

    public void setMaxWidth(float maxWidth) {
        this.maxWidth = maxWidth;
    }

    public float getTargetWidth() {
        return fixedWidth;
    }

    public float getMaxWidth() {
        return maxWidth;
    }

    public double clampWidth(double desiredWidth) {
        if(fixedWidth != -1) return fixedWidth;
        if(maxWidth == -1) return desiredWidth;
        return Math.min(maxWidth, desiredWidth);
    }
}
