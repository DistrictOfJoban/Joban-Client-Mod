package com.lx862.jcm.mod.render.text;

public enum TextAlignment {
    LEFT((x, width) -> x),
    CENTER((x, width) -> x - (width / 2)),
    RIGHT((x, width) -> x - width);

    private final CalculateTextCallback callback;

    TextAlignment(CalculateTextCallback callback) {
        this.callback = callback;
    }

    public double getX(double x, double width) {
        return callback.getTextX(x, width);
    }

    public interface CalculateTextCallback {
        double getTextX(double x, double width);
    }
}