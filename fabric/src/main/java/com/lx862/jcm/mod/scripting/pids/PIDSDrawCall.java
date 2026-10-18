package com.lx862.jcm.mod.scripting.pids;

import com.lx862.jcm.mod.data.pids.preset.PIDSPresetBase;
import com.lx862.mtrscripting.core.annotation.ApiInternal;
import com.lx862.mtrscripting.core.util.render.RenderDrawCall;
import org.mtr.mapping.holder.Direction;
import org.mtr.mapping.holder.World;
import org.mtr.mapping.mapper.GraphicsHolder;
import org.mtr.mod.render.StoredMatrixTransformations;

import java.util.function.Consumer;

public abstract class PIDSDrawCall<T extends PIDSDrawCall<?>> extends RenderDrawCall<T> {
    public double x;
    public double y;
    public double w;
    public double h;
    public int zOrderOverride = -1;

    /**
     * Positioning consumer, allocated once per draw-call instance instead of
     * once per run: it captures {@code this} and reads the mutable x/y fields
     * when the queued transform is applied, so a pooled instance re-renders
     * correctly after its fields are overwritten for a new frame. The old
     * per-run lambda was one of the steady per-frame allocations behind the
     * periodic GC stutter.
     */
    private final Consumer<GraphicsHolder> jcm$position = graphicsHolder -> {
        graphicsHolder.scale(PIDSPresetBase.BASE_SCALE, PIDSPresetBase.BASE_SCALE, PIDSPresetBase.BASE_SCALE);
        graphicsHolder.translate(PIDSDrawCall.this.x, PIDSDrawCall.this.y, 0);
    };

    /** Z-offset consumer for auto z-ordering; fields set by
     *  {@link #incrementZOrder(int, double)} each frame, allocated once. */
    private final Consumer<GraphicsHolder> jcm$zOffset = graphicsHolder ->
            graphicsHolder.translate(0, 0, PIDSDrawCall.this.jcm$zValue * PIDSDrawCall.this.jcm$zStep);
    private double jcm$zValue;
    private double jcm$zStep;

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

    public T zOrder(int order) {
        if(order < 0) throw new IllegalArgumentException("Z-Order must be 0 or above.");
        this.zOrderOverride = order;
        return (T)this;
    }

    public void incrementZOrder(int contextZIndex, double stepValue) {
        int zToUse = this.zOrderOverride == -1 ? contextZIndex : this.zOrderOverride;
        this.jcm$zValue = zToUse;
        this.jcm$zStep = stepValue;
        if(storedMatrixTransformations == null) {
            storedMatrixTransformations = new StoredMatrixTransformations();
        }
        /* One persistent consumer re-added per frame (the previous entry is
           dropped when the wrapper's transform list is cleared for reuse);
           the old code allocated a fresh SMT plus a capturing lambda per
           draw call per frame. */
        storedMatrixTransformations.add(this.jcm$zOffset);
    }

    public void draw(PIDSScriptContext ctx) {
        ctx.draw(this);
    }

    @Override
    public void run(World world, StoredMatrixTransformations storedMatrixTransformations, Direction facing, int light) {
        super.run(world, storedMatrixTransformations, facing, light);
        storedMatrixTransformations.add(this.jcm$position);
        drawTransformed(storedMatrixTransformations, facing, light);
    }

    /**
     * Returns the wrapper to a pristine state for pooling (native replay):
     * geometry, z-order and any queued transform entries. Persistent
     * consumers stay — they read the mutable fields.
     */
    @ApiInternal
    public void resetDefaults(double defaultW, double defaultH) {
        this.x = 0;
        this.y = 0;
        this.w = defaultW;
        this.h = defaultH;
        this.zOrderOverride = -1;
        if (this.storedMatrixTransformations != null) {
            /* Clear rather than null: the SMT instance is reused, its entries
               are re-added per frame. Compiled JS matrices set their own SMT
               instance via matrices(); those are simply cleared too. */
            ((com.lx862.jcm.nativeapi.NativeTransformScope) (Object) this.storedMatrixTransformations)
                    .jcm$clearTransformations();
        }
    }

    @ApiInternal
    public abstract void validate();

    @ApiInternal
    protected abstract void drawTransformed(StoredMatrixTransformations storedMatrixTransformations, Direction facing, int light);
}
