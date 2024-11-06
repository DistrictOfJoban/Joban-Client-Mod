package com.lx862.jcm.mod.data.pids.scripting;

import com.lx862.mtrscripting.scripting.util.Matrices;
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

    public DrawCall pos(double x, double y) {
        this.x = x;
        this.y = y;
        return this;
    }

    public DrawCall size(double w, double h) {
        this.w = w;
        this.h = h;
        return this;
    }

    public DrawCall matrices(Matrices matrices) {
        this.storedMatrixTransformations = matrices.getStoredMatrixTransformations().copy();
        return this;
    }

    public void draw(PIDSScriptContext ctx) {
        ctx.draw(this);
    }

    public void draw(GraphicsHolder graphicsHolder, Direction facing) {
        graphicsHolder.push();
        storedMatrixTransformations.transform(graphicsHolder, Vector3d.getZeroMapped());
        graphicsHolder.translate(this.x, this.y, 0);
        drawTransformed(graphicsHolder, facing);
        graphicsHolder.pop();
    }

    protected abstract void validate();

    protected abstract void drawTransformed(GraphicsHolder graphicsHolder, Direction facing);
}
