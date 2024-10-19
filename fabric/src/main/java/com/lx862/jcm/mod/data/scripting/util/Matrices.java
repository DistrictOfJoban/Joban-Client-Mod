package com.lx862.jcm.mod.data.scripting.util;

import org.mtr.mapping.mapper.GraphicsHolder;

public class Matrices {
    private final GraphicsHolder graphicsHolder;

    public Matrices(GraphicsHolder graphicsHolder) {
        this.graphicsHolder = graphicsHolder;
    }

    public void push() {
        this.graphicsHolder.push();
    }

    public void translate(double x, double y, double z) {
        this.graphicsHolder.translate(x, y, z);
    }

    public void rotateX(float x) {
        this.graphicsHolder.rotateXDegrees(x);
    }

    public void rotateY(float y) {
        this.graphicsHolder.rotateYDegrees(y);
    }

    public void rotateZ(float z) {
        this.graphicsHolder.rotateZDegrees(z);
    }

    public void pop() {
        this.graphicsHolder.pop();
    }
}