package com.lx862.mtrscripting.core.util.render;

import com.lx862.mtrscripting.core.annotation.ApiInternal;
import com.lx862.mtrscripting.core.annotation.ValueNullable;
import com.lx862.mtrscripting.core.util.Matrices;
import com.lx862.mtrscripting.core.util.model.ModelJS;
import org.mtr.mapping.holder.Direction;
import org.mtr.mapping.holder.World;
import org.mtr.mod.render.StoredMatrixTransformations;

import java.util.ArrayList;
import java.util.List;

public class ScriptRenderManager {
    private final List<RenderDrawCall<?>> drawCalls;

    @ApiInternal
    public ScriptRenderManager() {
        this.drawCalls = new ArrayList<>();
    }

    @ApiInternal
    protected ScriptRenderManager(List<RenderDrawCall<?>> drawCalls) {
        this.drawCalls = new ArrayList<>();
        this.drawCalls.addAll(drawCalls);
    }

    public void drawModel(ModelJS model, @ValueNullable Matrices matrices) {
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

    @ApiInternal
    public void invoke(World world, StoredMatrixTransformations storedMatrixTransformations, Direction facing, int light) {
        List<RenderDrawCall<?>> drawCalls = new ArrayList<>(this.drawCalls);

        for(RenderDrawCall<?> drawCall : drawCalls) {
            drawCall.run(world, storedMatrixTransformations.copy(), facing, light);
        }
    }

    @ApiInternal
    public ScriptRenderManager copy() {
        return new ScriptRenderManager(this.drawCalls);
    }

    @ApiInternal
    public void reset() {
        this.drawCalls.clear();
    }
}
