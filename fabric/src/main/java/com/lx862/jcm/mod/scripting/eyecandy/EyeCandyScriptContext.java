package com.lx862.jcm.mod.scripting.eyecandy;

import com.lx862.mtrscripting.scripting.base.ScriptContext;
import com.lx862.mtrscripting.scripting.util.Matrices;
import com.lx862.mtrscripting.scripting.util.NewModel;

import java.util.ArrayList;
import java.util.List;

public class EyeCandyScriptContext extends ScriptContext {
    private final List<ModelDrawCall> drawCalls = new ArrayList<>();

    public void drawModel(NewModel model, Matrices matrices) {
        this.drawCalls.add(new ModelDrawCall(model, matrices == null ? null : matrices.getStoredMatrixTransformations().copy()));
    }

    public List<ModelDrawCall> getDrawCalls() {
        List<ModelDrawCall> drawCalls = new ArrayList<>(this.drawCalls);
        this.drawCalls.clear();
        return drawCalls;
    }
}
