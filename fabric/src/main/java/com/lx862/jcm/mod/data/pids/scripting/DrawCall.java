package com.lx862.jcm.mod.data.pids.scripting;

import org.mtr.mapping.holder.Direction;
import org.mtr.mapping.holder.Vector3d;
import org.mtr.mapping.mapper.GraphicsHolder;
import org.mtr.mod.render.StoredMatrixTransformations;

public abstract class DrawCall {
    public double x;
    public double y;
    public double w;
    public double h;
    public StoredMatrixTransformations storedMatrixTransformations;

    public DrawCall(double defaultW, double defaultH) {
        this.w = defaultW;
        this.h = defaultH;
        this.storedMatrixTransformations = new StoredMatrixTransformations();
    }

    protected void setMatrix(StoredMatrixTransformations matrix) {
        this.storedMatrixTransformations = matrix;
    }

    public void draw(GraphicsHolder graphicsHolder, Direction facing) {
        graphicsHolder.push();
        storedMatrixTransformations.transform(graphicsHolder, Vector3d.getZeroMapped());
        graphicsHolder.translate(this.x, this.y, 0);
        drawTransformed(graphicsHolder, facing);
        graphicsHolder.pop();
    }

    protected abstract void drawTransformed(GraphicsHolder graphicsHolder, Direction facing);
}
