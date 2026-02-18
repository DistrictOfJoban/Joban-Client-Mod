package com.lx862.jcm.mod.scripting.mtr.render;

import com.lx862.mtrscripting.api.ScriptResultCall;
import com.lx862.mtrscripting.util.Matrices;
import org.mtr.mapping.holder.Direction;
import org.mtr.mapping.holder.World;
import org.mtr.mod.render.StoredMatrixTransformations;

public abstract class RenderDrawCall<T extends RenderDrawCall<?>> implements ScriptResultCall {
    protected StoredMatrixTransformations storedMatrixTransformations;

    public T matrices(Matrices matrices) {
        if(matrices != null) {
            this.storedMatrixTransformations = matrices.getStoredMatrixTransformations().copy();
        } else {
            this.storedMatrixTransformations = null;
        }
        return (T)this;
    }

    protected T setMatrices(StoredMatrixTransformations m) {
        this.storedMatrixTransformations = m;
        return (T)this;
    }

    public void run(World world, StoredMatrixTransformations storedMatrixTransformations, Direction facing, int light) {
        if (this.storedMatrixTransformations != null) {
            storedMatrixTransformations.add(this.storedMatrixTransformations);
        }
    }
}
