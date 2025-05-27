package com.lx862.mtrscripting.util;

import org.mtr.mapping.mapper.GraphicsHolder;
import org.mtr.mod.render.StoredMatrixTransformations;

/** This is a wrapper for storedMatrixTransformations for scripting purposes */
@SuppressWarnings("unused")
public class Matrices {
    private final StoredMatrixTransformations storedMatrixTransformations;

    public Matrices() {
        this.storedMatrixTransformations = new StoredMatrixTransformations();
    }

    public void pushPose() {
        this.storedMatrixTransformations.add(GraphicsHolder::push);
    }

    public void translate(float x, float y, float z) {
        this.storedMatrixTransformations.add(graphicsHolder -> graphicsHolder.translate(x, y, z));
    }

    public void rotateX(float xRad) {
        this.storedMatrixTransformations.add(graphicsHolder -> graphicsHolder.rotateXRadians(xRad));
    }

    public void rotateY(float yRad) {
        this.storedMatrixTransformations.add(graphicsHolder -> graphicsHolder.rotateYRadians(yRad));
    }

    public void rotateZ(float zRad) {
        this.storedMatrixTransformations.add(graphicsHolder -> graphicsHolder.rotateZRadians(zRad));
    }

    public void rotateXDegrees(float xDeg) {
        this.storedMatrixTransformations.add(graphicsHolder -> graphicsHolder.rotateXDegrees(xDeg));
    }

    public void rotateYDegrees(float yDeg) {
        this.storedMatrixTransformations.add(graphicsHolder -> graphicsHolder.rotateYDegrees(yDeg));
    }

    public void rotateZDegrees(float zDeg) {
        this.storedMatrixTransformations.add(graphicsHolder -> graphicsHolder.rotateZDegrees(zDeg));
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