package com.lx862.jcm.mod.scripting.train;

import com.lx862.mtrscripting.core.ScriptContext;
import com.lx862.mtrscripting.util.Matrices;
import com.lx862.mtrscripting.util.ScriptedModel;

import java.util.ArrayList;
import java.util.List;

public class TrainScriptContext extends ScriptContext {
    private final List<TrainModelDrawCall> carModelDrawCalls = new ArrayList<>();
    private final List<TrainModelDrawCall> conectionModelDrawCalls = new ArrayList<>();

    public void drawCarModel(ScriptedModel model, int carIndex, Matrices matrices) {
        this.carModelDrawCalls.add(new TrainModelDrawCall(model, carIndex, matrices.getStoredMatrixTransformations().copy()));
    }

    public void drawConnModel(ScriptedModel model, int carIndex, Matrices matrices) {
        this.conectionModelDrawCalls.add(new TrainModelDrawCall(model, carIndex, matrices.getStoredMatrixTransformations().copy()));
    }

    public List<TrainModelDrawCall> getCarModelDrawCalls() {
        return new ArrayList<>(this.carModelDrawCalls);
    }

    public List<TrainModelDrawCall> getConnectionModelDrawCalls() {
        return new ArrayList<>(this.conectionModelDrawCalls);
    }

    @Override
    public void reset() {
        this.carModelDrawCalls.clear();
        this.conectionModelDrawCalls.clear();
    }
}
