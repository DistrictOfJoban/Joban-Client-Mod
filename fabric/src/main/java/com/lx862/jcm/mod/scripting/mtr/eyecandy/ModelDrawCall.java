package com.lx862.jcm.mod.scripting.mtr.eyecandy;

import com.lx862.mtrscripting.api.ScriptResultCall;
import com.lx862.mtrscripting.util.ScriptedModel;
import org.mtr.mapping.holder.Direction;
import org.mtr.mapping.holder.World;
import org.mtr.mapping.mapper.GraphicsHolder;
import org.mtr.mod.render.StoredMatrixTransformations;

public class ModelDrawCall extends ScriptResultCall {
    private final ScriptedModel model;
    private final StoredMatrixTransformations storedMatrixTransformations;

    public ModelDrawCall(ScriptedModel model, StoredMatrixTransformations storedMatrixTransformations) {
        this.model = model;
        this.storedMatrixTransformations = storedMatrixTransformations;
    }

    @Override
    public void run(World world, GraphicsHolder graphicsHolder, StoredMatrixTransformations storedMatrixTransformations, Direction facing, int light) {
        if(this.storedMatrixTransformations != null) {
            storedMatrixTransformations.add(this.storedMatrixTransformations);
        }

        this.model.draw(storedMatrixTransformations, light);
    }
}
