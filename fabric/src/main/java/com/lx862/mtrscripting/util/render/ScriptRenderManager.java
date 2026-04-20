package com.lx862.mtrscripting.util.render;

import com.lx862.mtrscripting.util.Matrices;
import com.lx862.mtrscripting.util.model.ModelJS;
import org.mtr.mapping.holder.Direction;
import org.mtr.mapping.holder.World;
import org.mtr.mod.render.StoredMatrixTransformations;

import java.util.ArrayList;
import java.util.List;

public class ScriptRenderManager {
    private final List<RenderDrawCall<?>> drawCalls;

    public ScriptRenderManager() {
        this.drawCalls = new ArrayList<>();
    }

    public ScriptRenderManager(List<RenderDrawCall<?>> drawCalls) {
        this.drawCalls = new ArrayList<>();
        this.drawCalls.addAll(drawCalls);
    }

    public void drawModel(ModelJS model, Matrices matrices) {
        draw(new ModelDrawCall().modelObject(model).matrices(matrices));
    }

    public void draw(RenderDrawCall<?> drawCall) {
        drawCall.validate();
        this.drawCalls.add(drawCall);
    }

    @Deprecated
    public void queue(RenderDrawCall<?> drawCall) {
        this.draw(drawCall);
    }

    public void invoke(World world, StoredMatrixTransformations storedMatrixTransformations, Direction facing, int light) {
        List<RenderDrawCall<?>> drawCalls = new ArrayList<>(this.drawCalls);

        for(RenderDrawCall<?> drawCall : drawCalls) {
            drawCall.run(world, storedMatrixTransformations.copy(), facing, light);
        }
    }

    public ScriptRenderManager copy() {
        return new ScriptRenderManager(this.drawCalls);
    }

    public void reset() {
        this.drawCalls.clear();
    }
}
