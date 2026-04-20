package com.lx862.mtrscripting.core.util.render;

import com.lx862.mtrscripting.core.util.model.ModelJS;
import org.mtr.mapping.holder.Direction;
import org.mtr.mapping.holder.World;
import org.mtr.mod.render.StoredMatrixTransformations;

public class ModelDrawCall extends RenderDrawCall<ModelDrawCall> {
    private ModelJS model;

    public static ModelDrawCall create() {
        return new ModelDrawCall();
    }

    public static ModelDrawCall create(String comment) {
        return create();
    }

    public ModelDrawCall modelObject(ModelJS model) {
        this.model = model;
        return this;
    }

    @Override
    public void run(World world, StoredMatrixTransformations storedMatrixTransformations, Direction facing, int light) {
        super.run(world, storedMatrixTransformations, facing, light);
        this.model.draw(storedMatrixTransformations, light);
    }

    @Override
    public void validate() {
        if(this.model == null) {
            throw new IllegalStateException("The model object is not specified!");
        }
    }
}
