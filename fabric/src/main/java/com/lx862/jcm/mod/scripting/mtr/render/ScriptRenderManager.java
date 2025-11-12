package com.lx862.jcm.mod.scripting.mtr.render;

import com.lx862.jcm.mod.scripting.jcm.pids.PIDSDrawCall;
import com.lx862.jcm.mod.scripting.jcm.pids.PIDSScriptContext;
import com.lx862.mtrscripting.api.ScriptResultCall;
import com.lx862.mtrscripting.util.Vector3dWrapper;
import org.mtr.mapping.holder.Direction;
import org.mtr.mapping.holder.World;
import org.mtr.mapping.mapper.GraphicsHolder;
import org.mtr.mod.render.StoredMatrixTransformations;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ScriptRenderManager {
    public final List<ScriptResultCall> drawCalls;

    public ScriptRenderManager() {
        this.drawCalls = new ArrayList<>();
    }

    public ScriptRenderManager(List<ScriptResultCall> drawCalls) {
        this.drawCalls = new ArrayList<>();
        this.drawCalls.addAll(drawCalls);
    }

    /**
     * Internally invoked by {@link PIDSScriptContext#draw(Object)}
     * Although user may also decide not to use method chaining and manually invoke this with their PIDS draw call instead.
     * */
    public void queue(ScriptResultCall call) {
        this.drawCalls.add(call);
    }

    public void invoke(World world, Vector3dWrapper basePos, GraphicsHolder graphicsHolder, StoredMatrixTransformations storedMatrixTransformations, Direction facing, int light) {
        List<ScriptResultCall> drawCallsSaved = new ArrayList<>(drawCalls);
        List<ScriptResultCall> modelDrawCalls = drawCallsSaved.stream().filter(e -> !(e instanceof PIDSDrawCall)).collect(Collectors.toList());
        List<ScriptResultCall> pidsDrawCalls = drawCallsSaved.stream().filter(e -> e instanceof PIDSDrawCall).collect(Collectors.toList());

        for(ScriptResultCall drawCall : new ArrayList<>(modelDrawCalls)) {
            drawCall.run(world, basePos, graphicsHolder, storedMatrixTransformations, facing, light);
        }
        for(ScriptResultCall drawCall : new ArrayList<>(pidsDrawCalls)) {
            graphicsHolder.translate(0, 0, -0.02);
            graphicsHolder.push();
            drawCall.run(world, basePos, graphicsHolder, storedMatrixTransformations, facing, light);
            graphicsHolder.pop();
        }
    }

    public ScriptRenderManager copy() {
        return new ScriptRenderManager(this.drawCalls);
    }

    public void reset() {
        this.drawCalls.clear();
    }
}
