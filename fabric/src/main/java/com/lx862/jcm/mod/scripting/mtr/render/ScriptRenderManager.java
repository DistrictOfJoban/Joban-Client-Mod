package com.lx862.jcm.mod.scripting.mtr.render;

import com.lx862.jcm.mod.scripting.jcm.pids.PIDSDrawCall;
import com.lx862.jcm.mod.scripting.jcm.pids.PIDSScriptContext;
import com.lx862.mtrscripting.api.ScriptResultCall;
import com.lx862.mtrscripting.util.ScriptVector3f;
import org.mtr.mapping.holder.Direction;
import org.mtr.mapping.holder.World;
import org.mtr.mapping.mapper.GraphicsHolder;
import org.mtr.mod.render.StoredMatrixTransformations;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ScriptRenderManager {
    private final List<RenderDrawCall<?>> drawCalls;

    public ScriptRenderManager() {
        this.drawCalls = new ArrayList<>();
    }

    public ScriptRenderManager(List<RenderDrawCall<?>> drawCalls) {
        this.drawCalls = new ArrayList<>();
        this.drawCalls.addAll(drawCalls);
    }

    /**
     * Internally invoked by {@link PIDSScriptContext#draw(Object)}
     * Although user may also decide not to use method chaining and manually invoke this with their PIDS draw call instead.
     * */
    public void queue(RenderDrawCall<?> call) {
        call.validate();
        this.drawCalls.add(call);
    }

    public void invoke(World world, StoredMatrixTransformations storedMatrixTransformations, Direction facing, int light) {
        List<RenderDrawCall<?>> drawCalls = new ArrayList<>(this.drawCalls);
        List<RenderDrawCall<?>> normalDrawCalls = drawCalls.stream().filter(e -> !(e instanceof PIDSDrawCall)).collect(Collectors.toList());
        List<RenderDrawCall<?>> pidsDrawCalls = drawCalls.stream().filter(e -> e instanceof PIDSDrawCall).collect(Collectors.toList());

        for(RenderDrawCall<?> drawCall : new ArrayList<>(normalDrawCalls)) {
            drawCall.run(world, storedMatrixTransformations.copy(), facing, light);
        }

        StoredMatrixTransformations pidsStoredMatrixTransformation = storedMatrixTransformations.copy();
        for(RenderDrawCall<?> drawCall : new ArrayList<>(pidsDrawCalls)) {
            pidsStoredMatrixTransformation.add(graphicsHolderNew -> graphicsHolderNew.translate(0, 0, -0.0002)); // Prevent z-fighting
            drawCall.run(world, pidsStoredMatrixTransformation.copy(), facing, light);
        }
    }

    public ScriptRenderManager copy() {
        return new ScriptRenderManager(this.drawCalls);
    }

    public void reset() {
        this.drawCalls.clear();
    }
}
