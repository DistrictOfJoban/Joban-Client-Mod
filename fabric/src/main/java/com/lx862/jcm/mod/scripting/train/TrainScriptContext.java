package com.lx862.jcm.mod.scripting.train;

import com.lx862.mtrscripting.scripting.base.ScriptContext;
import com.lx862.mtrscripting.scripting.util.Matrices;
import com.lx862.mtrscripting.scripting.util.NewModel;

import java.util.ArrayList;
import java.util.List;

public class TrainScriptContext extends ScriptContext {
    private final List<TrainDrawCall> carModelDrawCalls = new ArrayList<>();
    private final List<TrainDrawCall> conectionModelDrawCalls = new ArrayList<>();

    public void drawCarModel(NewModel model, int carIndex, Matrices matrices) {
        this.carModelDrawCalls.add(new TrainDrawCall(model, carIndex, matrices.getStoredMatrixTransformations().copy()));
    }

    public void drawConnModel(NewModel model, int carIndex, Matrices matrices) {
        this.conectionModelDrawCalls.add(new TrainDrawCall(model, carIndex, matrices.getStoredMatrixTransformations().copy()));
    }

    public List<TrainDrawCall> getCarModelDrawCalls() {
        List<TrainDrawCall> drawCalls = new ArrayList<>(this.carModelDrawCalls);
        this.carModelDrawCalls.clear();
        return drawCalls;
    }

    public List<TrainDrawCall> getConnectionModelDrawCalls() {
        List<TrainDrawCall> drawCalls = new ArrayList<>(this.conectionModelDrawCalls);
        this.conectionModelDrawCalls.clear();
        return drawCalls;
    }
}
