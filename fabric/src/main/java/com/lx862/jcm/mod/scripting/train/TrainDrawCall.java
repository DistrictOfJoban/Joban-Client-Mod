package com.lx862.jcm.mod.scripting.train;

import com.lx862.mtrscripting.scripting.util.NewModel;
import org.mtr.mod.render.StoredMatrixTransformations;

public class TrainDrawCall {
    private final NewModel model;
    private final StoredMatrixTransformations storedMatrixTransformations;
    private final int carIndex;

    public TrainDrawCall(NewModel model, int carIndex, StoredMatrixTransformations storedMatrixTransformations) {
        this.model = model;
        this.carIndex = carIndex;
        this.storedMatrixTransformations = storedMatrixTransformations;
    }

    public NewModel getModel() {
        return this.model;
    }

    public int getCarIndex() {
        return this.carIndex;
    }

    public StoredMatrixTransformations getTransformation() {
        return this.storedMatrixTransformations;
    }
}
