package com.lx862.jcm.mod.scripting.mtr.render;

import com.lx862.jcm.mod.scripting.jcm.pids.PIDSDrawCall;
import com.lx862.jcm.mod.scripting.mtr.util.ScriptedModel;
import com.lx862.mtrscripting.api.ScriptResultCall;
import com.lx862.mtrscripting.util.Matrices;
import org.mtr.mapping.holder.Direction;
import org.mtr.mapping.holder.World;
import org.mtr.mapping.mapper.GraphicsHolder;
import org.mtr.mod.render.StoredMatrixTransformations;

import java.util.ArrayList;
import java.util.List;

public class ScriptRenderManager {
    public final List<ScriptResultCall> drawCalls;

    public ScriptRenderManager() {
        this.drawCalls = new ArrayList<>();
    }

    public void updateDrawCalls(ScriptRenderManager other) {
        drawCalls.clear();
        drawCalls.addAll(other.drawCalls);
    }

    public void drawModel(ScriptedModel model, Matrices matrices) {
        this.drawCalls.add(new ModelDrawCall(model, matrices == null ? null : matrices.getStoredMatrixTransformations().copy()));
    }

    public void drawPIDS(PIDSDrawCall drawCall) {
        this.drawCalls.add(drawCall);
    }

    public void invoke(World world, GraphicsHolder graphicsHolder, StoredMatrixTransformations storedMatrixTransformations, Direction facing, int light) {
        List<ScriptResultCall> drawCallsSaved = new ArrayList<>(drawCalls);
        List<ScriptResultCall> modelDrawCalls = drawCallsSaved.stream().filter(e -> !(e instanceof PIDSDrawCall)).toList();
        List<ScriptResultCall> pidsDrawCalls = drawCallsSaved.stream().filter(e -> e instanceof PIDSDrawCall).toList();

        for(ScriptResultCall drawCall : new ArrayList<>(modelDrawCalls)) {
            drawCall.run(world, graphicsHolder, storedMatrixTransformations, facing, light);
        }
        for(ScriptResultCall drawCall : new ArrayList<>(pidsDrawCalls)) {
            graphicsHolder.translate(0, 0, -0.02);
            graphicsHolder.push();
            drawCall.run(world, graphicsHolder, storedMatrixTransformations, facing, light);
            graphicsHolder.pop();
        }
    }

    public void reset() {
        this.drawCalls.clear();
    }
}
