package com.lx862.jcm.mod.scripting.jcm.pids;

import com.lx862.jcm.mod.data.pids.preset.PIDSPresetBase;
import com.lx862.jcm.mod.scripting.mtr.render.RenderDrawCall;
import org.mtr.mapping.holder.Direction;
import org.mtr.mapping.holder.World;
import org.mtr.mapping.mapper.GraphicsHolder;
import org.mtr.mod.render.StoredMatrixTransformations;

public abstract class PIDSDrawCall<T extends PIDSDrawCall<?>> extends RenderDrawCall<T> {
    public double x;
    public double y;
    public double w;
    public double h;

    public PIDSDrawCall(double defaultW, double defaultH) {
        this.w = defaultW;
        this.h = defaultH;
    }

    public T pos(double x, double y) {
        this.x = x;
        this.y = y;
        return (T)this;
    }

    public T size(double w, double h) {
        this.w = w;
        this.h = h;
        return (T)this;
    }

    public void draw(PIDSScriptContext ctx) {
        ctx.draw(this);
    }

    @Override
    public void run(World world, StoredMatrixTransformations storedMatrixTransformations, Direction facing, int light) {
        super.run(world, storedMatrixTransformations, facing, light);

        storedMatrixTransformations.add(graphicsHolder1 -> {
            graphicsHolder1.scale(PIDSPresetBase.BASE_SCALE, PIDSPresetBase.BASE_SCALE, PIDSPresetBase.BASE_SCALE);
            graphicsHolder1.translate(this.x, this.y, 0);
        });
        drawTransformed(storedMatrixTransformations, facing);
    }

    public abstract void validate();

    protected abstract void drawTransformed(StoredMatrixTransformations storedMatrixTransformations, Direction facing);
}
