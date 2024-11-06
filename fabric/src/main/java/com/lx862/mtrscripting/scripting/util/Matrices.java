package com.lx862.mtrscripting.scripting.util;

import org.mtr.mapping.mapper.GraphicsHolder;
import org.mtr.mod.render.StoredMatrixTransformations;

/* This is a wrapper for storedMatrixTransformations for scripting purposes */

public class Matrices {
    private final StoredMatrixTransformations storedMatrixTransformations;

    public Matrices() {
        this.storedMatrixTransformations = new StoredMatrixTransformations();
    }

    public void pushPose() {
        this.storedMatrixTransformations.add(GraphicsHolder::push);
    }

    public void translate(double x, double y, double z) {
        this.storedMatrixTransformations.add(graphicsHolder -> graphicsHolder.translate(x, y, z));
    }

    public void rotateX(float x) {
        this.storedMatrixTransformations.add(graphicsHolder -> graphicsHolder.rotateXDegrees(x));
    }

    public void rotateY(float y) {
        this.storedMatrixTransformations.add(graphicsHolder -> graphicsHolder.rotateYDegrees(y));
    }

    public void rotateZ(float z) {
        this.storedMatrixTransformations.add(graphicsHolder -> graphicsHolder.rotateZDegrees(z));
    }

    public void scale(float x, float y, float z) {
        this.storedMatrixTransformations.add(graphicsHolder -> graphicsHolder.scale(x, y, z));
    }

    public void popPose() {
        this.storedMatrixTransformations.add(GraphicsHolder::pop);
    }

    public void popPushPose() {
        this.storedMatrixTransformations.add(GraphicsHolder::pop);
        this.storedMatrixTransformations.add(GraphicsHolder::push);
    }

    public StoredMatrixTransformations getStoredMatrixTransformations() {
        return storedMatrixTransformations;
    }
}