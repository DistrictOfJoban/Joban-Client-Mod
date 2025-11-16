package com.lx862.jcm.mod.scripting.mtr.render;

import com.lx862.jcm.mod.scripting.mtr.util.ScriptedModel;
import com.lx862.mtrscripting.util.ScriptVector3f;
import org.mtr.mapping.holder.Direction;
import org.mtr.mapping.holder.World;
import org.mtr.mapping.mapper.GraphicsHolder;
import org.mtr.mod.render.StoredMatrixTransformations;

public class ModelDrawCall extends RenderDrawCall<ModelDrawCall> {
    private ScriptedModel model;

    public static ModelDrawCall create() {
        return new ModelDrawCall();
    }

    public static ModelDrawCall create(String comment) {
        return create();
    }

    public ModelDrawCall modelObject(ScriptedModel model) {
        this.model = model;
        return this;
    }

    @Override
    public void run(World world, ScriptVector3f basePos, GraphicsHolder graphicsHolder, StoredMatrixTransformations storedMatrixTransformations, Direction facing, int light) {
        super.run(world, basePos, graphicsHolder, storedMatrixTransformations, facing, light);
        this.model.draw(storedMatrixTransformations, light);
    }
}
