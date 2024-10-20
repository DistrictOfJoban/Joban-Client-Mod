package com.lx862.jcm.mod.data.pids.scripting;

import org.mtr.mapping.holder.Vector3d;
import org.mtr.mapping.mapper.GraphicsHolder;
import org.mtr.mod.render.StoredMatrixTransformations;

public abstract class DrawCall {
    public final StoredMatrixTransformations storedMatrixTransformations;

    public DrawCall(StoredMatrixTransformations storedMatrixTransformations) {
        this.storedMatrixTransformations = storedMatrixTransformations;
    }

    public void draw(GraphicsHolder graphicsHolder) {
        graphicsHolder.push();
        storedMatrixTransformations.transform(graphicsHolder, Vector3d.getZeroMapped());
        drawTransformed(graphicsHolder);
        graphicsHolder.pop();
    }

    protected abstract void drawTransformed(GraphicsHolder graphicsHolder);
}
