package com.lx862.mtrscripting.util;

import org.mtr.mod.render.StoredMatrixTransformations;

import java.util.Stack;
import java.util.stream.Collectors;

/** This is a wrapper for storedMatrixTransformations for scripting purposes */
@SuppressWarnings("unused")
public class Matrices {
    private final Stack<StoredMatrixTransformations> storedMatrixTransformations;

    public Matrices() {
        this.storedMatrixTransformations = new Stack<>();
        this.storedMatrixTransformations.add(new StoredMatrixTransformations());
    }

    private Matrices(Stack<StoredMatrixTransformations> storedMatrixTransformations) {
        this.storedMatrixTransformations = new Stack<>();
        this.storedMatrixTransformations.addAll(storedMatrixTransformations.stream().map(StoredMatrixTransformations::copy).collect(Collectors.toList()));
    }

    public void pushPose() {
        this.storedMatrixTransformations.push(new StoredMatrixTransformations());
    }

    public void translate(float x, float y, float z) {
        this.storedMatrixTransformations.peek().add(graphicsHolder -> graphicsHolder.translate(x, y, z));
    }

    public void rotateX(float xRad) {
        this.storedMatrixTransformations.peek().add(graphicsHolder -> graphicsHolder.rotateXRadians(xRad));
    }

    public void rotateY(float yRad) {
        this.storedMatrixTransformations.peek().add(graphicsHolder -> graphicsHolder.rotateYRadians(yRad));
    }

    public void rotateZ(float zRad) {
        this.storedMatrixTransformations.peek().add(graphicsHolder -> graphicsHolder.rotateZRadians(zRad));
    }

    public void rotateXDegrees(float xDeg) {
        this.storedMatrixTransformations.peek().add(graphicsHolder -> graphicsHolder.rotateXDegrees(xDeg));
    }

    public void rotateYDegrees(float yDeg) {
        this.storedMatrixTransformations.peek().add(graphicsHolder -> graphicsHolder.rotateYDegrees(yDeg));
    }

    public void rotateZDegrees(float zDeg) {
        this.storedMatrixTransformations.peek().add(graphicsHolder -> graphicsHolder.rotateZDegrees(zDeg));
    }

    public void rotate(float x, float y, float z, float radian) {
        float mX = x * radian;
        float mY = y * radian;
        float mZ = z * radian;
        rotateX(mX);
        rotateY(mY);
        rotateZ(mZ);
    }

    public void scale(float x, float y, float z) {
        this.storedMatrixTransformations.peek().add(graphicsHolder -> graphicsHolder.scale(x, y, z));
    }

    public void popPose() {
        this.storedMatrixTransformations.pop();
    }

    public void popPushPose() {
        popPose();
        pushPose();
    }

    public Matrices copy() {
        return new Matrices(this.storedMatrixTransformations);
    }

    public StoredMatrixTransformations compileStoredMatrixTransformations() {
        StoredMatrixTransformations compiled = new StoredMatrixTransformations();
        storedMatrixTransformations.forEach(compiled::add);
        return compiled;
    }
}