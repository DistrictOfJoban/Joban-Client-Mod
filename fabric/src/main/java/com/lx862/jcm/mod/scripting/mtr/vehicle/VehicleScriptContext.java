package com.lx862.jcm.mod.scripting.mtr.vehicle;

import com.lx862.mtrscripting.core.ScriptContext;
import com.lx862.mtrscripting.util.Matrices;
import com.lx862.mtrscripting.util.ScriptedModel;

import java.util.ArrayList;
import java.util.List;

public class VehicleScriptContext extends ScriptContext {
    private final List<VehicleModelDrawCall> carModelDrawCalls = new ArrayList<>();
    private final List<VehicleModelDrawCall> conectionModelDrawCalls = new ArrayList<>();

    public void drawCarModel(ScriptedModel model, int carIndex, Matrices matrices) {
        this.carModelDrawCalls.add(new VehicleModelDrawCall(model, carIndex, matrices.getStoredMatrixTransformations().copy()));
    }

    public void drawConnModel(ScriptedModel model, int carIndex, Matrices matrices) {
        this.conectionModelDrawCalls.add(new VehicleModelDrawCall(model, carIndex, matrices.getStoredMatrixTransformations().copy()));
    }

    public List<VehicleModelDrawCall> getCarModelDrawCalls() {
        return new ArrayList<>(this.carModelDrawCalls);
    }

    public List<VehicleModelDrawCall> getConnectionModelDrawCalls() {
        return new ArrayList<>(this.conectionModelDrawCalls);
    }

    @Override
    public void reset() {
        this.carModelDrawCalls.clear();
        this.conectionModelDrawCalls.clear();
    }
}
