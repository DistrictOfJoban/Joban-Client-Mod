package com.lx862.mtrscripting.core.util.render;

import com.lx862.mtrscripting.core.util.model.DynamicModelHolderJS;
import org.mtr.mapping.holder.Direction;
import org.mtr.mapping.holder.World;
import org.mtr.mod.render.StoredMatrixTransformations;

public class DynamicModelDrawCall extends RenderDrawCall<DynamicModelDrawCall> {
    private DynamicModelHolderJS dynamicModelHolder;

    public static DynamicModelDrawCall create() {
        return new DynamicModelDrawCall();
    }

    public static DynamicModelDrawCall create(String comment) {
        return create();
    }

    public DynamicModelDrawCall modelHolder(DynamicModelHolderJS model) {
        this.dynamicModelHolder = model;
        return this;
    }

    @Override
    public void run(World world, StoredMatrixTransformations storedMatrixTransformations, Direction facing, int light) {
        super.run(world, storedMatrixTransformations, facing, light);
        if(this.dynamicModelHolder.getUploadedModel() != null) {
            this.dynamicModelHolder.getUploadedModel().draw(storedMatrixTransformations, light);
        }
    }

    @Override
    public void validate() {
        if(this.dynamicModelHolder == null) {
            throw new IllegalStateException("The model holder is not specified!");
        }
    }
}
