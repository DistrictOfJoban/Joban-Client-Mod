package com.lx862.jcm.mod.scripting.eyecandy;

import com.lx862.mtrscripting.scripting.util.ScriptedModel;
import org.mtr.mod.render.StoredMatrixTransformations;

public class ModelDrawCall {
    private final ScriptedModel model;
    private final StoredMatrixTransformations storedMatrixTransformations;

    public ModelDrawCall(ScriptedModel model, StoredMatrixTransformations storedMatrixTransformations) {
        this.model = model;
        this.storedMatrixTransformations = storedMatrixTransformations;
    }

    public void draw(StoredMatrixTransformations storedMatrixTransformations, int light) {
        this.model.draw(storedMatrixTransformations, light);
    }

    public StoredMatrixTransformations getTransformation() {
        return this.storedMatrixTransformations;
    }
}
