package com.lx862.jcm.mod.scripting.train;

import com.lx862.mtrscripting.scripting.base.ScriptContext;
import com.lx862.mtrscripting.scripting.util.Matrices;
import com.lx862.mtrscripting.scripting.util.NewModel;

import java.util.ArrayList;
import java.util.List;

public class TrainScriptContext extends ScriptContext {
    private final List<TrainModelDrawCall> carModelDrawCalls = new ArrayList<>();
    private final List<TrainModelDrawCall> conectionModelDrawCalls = new ArrayList<>();

    public void drawCarModel(NewModel model, int carIndex, Matrices matrices) {
        this.carModelDrawCalls.add(new TrainModelDrawCall(model, carIndex, matrices.getStoredMatrixTransformations().copy()));
    }

    public void drawConnModel(NewModel model, int carIndex, Matrices matrices) {
        this.conectionModelDrawCalls.add(new TrainModelDrawCall(model, carIndex, matrices.getStoredMatrixTransformations().copy()));
    }

    public List<TrainModelDrawCall> getCarModelDrawCalls() {
        List<TrainModelDrawCall> drawCalls = new ArrayList<>(this.carModelDrawCalls);
        this.carModelDrawCalls.clear();
        return drawCalls;
    }

    public List<TrainModelDrawCall> getConnectionModelDrawCalls() {
        List<TrainModelDrawCall> drawCalls = new ArrayList<>(this.conectionModelDrawCalls);
        this.conectionModelDrawCalls.clear();
        return drawCalls;
    }
}
