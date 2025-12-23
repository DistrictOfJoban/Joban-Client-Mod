package com.lx862.jcm.mod.scripting.jcm.pids;

import com.lx862.jcm.mod.scripting.mtr.render.ModelDrawCall;
import org.mtr.mapping.holder.Direction;
import org.mtr.mapping.holder.World;
import org.mtr.mapping.mapper.GraphicsHolder;
import org.mtr.mod.render.StoredMatrixTransformations;

/**
 * A ModelDrawCall that flips 180 degree around beforehand for block which is rotated the other way around
 */
public class PIDSModelDrawCall extends ModelDrawCall {

    public static PIDSModelDrawCall create() {
        return new PIDSModelDrawCall();
    }

    public static PIDSModelDrawCall create(String comment) {
        return create();
    }

    @Override
    public void run(World world, GraphicsHolder graphicsHolder, StoredMatrixTransformations storedMatrixTransformations, Direction facing, int light) {
        storedMatrixTransformations.add(graphicsHolderNew -> graphicsHolderNew.rotateXDegrees(-180));
        super.run(world, graphicsHolder, storedMatrixTransformations, facing, light);
    }
}
