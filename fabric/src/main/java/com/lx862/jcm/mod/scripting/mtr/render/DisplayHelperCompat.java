package com.lx862.jcm.mod.scripting.mtr.render;

import com.lx862.mtrscripting.util.ScriptVector3f;
import org.mtr.mapping.holder.Direction;
import org.mtr.mapping.holder.Identifier;
import org.mtr.mapping.holder.World;
import org.mtr.mapping.mapper.GraphicsHolder;
import org.mtr.mod.render.StoredMatrixTransformations;

import java.util.ArrayList;
import java.util.List;

public class DisplayHelperCompat extends RenderDrawCall<DisplayHelperCompat> {
    private List<QuadDrawCall> quadDrawCalls;

    private DisplayHelperCompat() {
        this.quadDrawCalls = new ArrayList<>();
    }

    private DisplayHelperCompat(List<QuadDrawCall> calls) {
        this.quadDrawCalls = calls;
    }

    public static DisplayHelperCompat create() {
        return new DisplayHelperCompat();
    }

    public DisplayHelperCompat addQuad(QuadDrawCall model) {
        this.quadDrawCalls.add(model);
        return this;
    }

    public DisplayHelperCompat copy(Identifier textureId) {
        List<QuadDrawCall> newDrawCalls = new ArrayList<>();
        for(QuadDrawCall quadDrawCall : quadDrawCalls) {
            QuadDrawCall copyCall = quadDrawCall.copy();
            copyCall.texture(textureId);
            newDrawCalls.add(copyCall);
        }
        return new DisplayHelperCompat(newDrawCalls);
    }

    @Override
    public void run(World world, ScriptVector3f basePos, GraphicsHolder graphicsHolder, StoredMatrixTransformations storedMatrixTransformations, Direction facing, int light) {
        super.run(world, basePos, graphicsHolder, storedMatrixTransformations, facing, light);
        for(QuadDrawCall drawCall : quadDrawCalls) {
            drawCall.run(world, basePos, graphicsHolder, storedMatrixTransformations, facing, light);
        }
    }

    @Override
    public void validate() {
        for(QuadDrawCall drawCall : quadDrawCalls) {
            drawCall.validate();
        }
    }
}
