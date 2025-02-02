package com.lx862.jcm.mod.scripting.train;

import com.lx862.jcm.mod.scripting.eyecandy.ModelDrawCall;
import com.lx862.mtrscripting.scripting.util.NewModel;
import org.mtr.mod.render.StoredMatrixTransformations;

public class TrainModelDrawCall extends ModelDrawCall {
    private final int carIndex;

    public TrainModelDrawCall(NewModel model, int carIndex, StoredMatrixTransformations storedMatrixTransformations) {
        super(model, storedMatrixTransformations);
        this.carIndex = carIndex;
    }

    public int getCarIndex() {
        return this.carIndex;
    }
}
