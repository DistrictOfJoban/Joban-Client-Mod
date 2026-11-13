package com.lx862.mtrscripting.core.util.render;

import com.lx862.mtrscripting.core.annotation.ApiInternal;
import com.lx862.mtrscripting.core.annotation.ValueNullable;
import com.lx862.mtrscripting.core.util.Matrices;
import com.lx862.mtrscripting.core.util.model.DynamicModelHolderJS;
import com.lx862.mtrscripting.core.util.model.ModelJS;
import org.mtr.mapping.holder.Direction;
import org.mtr.mapping.holder.World;
import org.mtr.mod.render.StoredMatrixTransformations;

import java.util.ArrayList;
import java.util.List;

public class ScriptRenderManager {
    private List<RenderDrawCall<?>> drawCalls;

    @ApiInternal
    public ScriptRenderManager() {
        this.drawCalls = new ArrayList<>();
    }

    @ApiInternal
    protected ScriptRenderManager(List<RenderDrawCall<?>> drawCalls) {
        this.drawCalls = new ArrayList<>();
        this.drawCalls.addAll(drawCalls);
    }

    public void drawModel(DynamicModelHolderJS modelHolder, @ValueNullable Matrices matrices) {
        draw(new DynamicModelDrawCall().modelHolder(modelHolder).matrices(matrices));
    }

    public void drawModel(ModelJS model, @ValueNullable Matrices matrices, int lightCoord) {
        draw(new ModelDrawCall().modelObject(model).matrices(matrices).light(lightCoord));
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

    /**
     * Replays the captured draw calls.
     *
     * <p>Every draw call replays against its own transform snapshot from
     * {@link TransformCapturePool}: {@code run()} may append consumers to
     * the holder it is given (PIDS {@code BASE_SCALE}+position, z-order
     * offsets, {@code .matrices()} merges), and the deferred render callback
     * it schedules reads that consumer list at <b>flush time</b> — long
     * after this method returns — so the additions must survive until then.
     * A shared holder with mark/truncate (the yanyang.4 rewrite) discarded
     * them the moment {@code run()} returned, which broke every scripted
     * PIDS text (see the pool's javadoc). The pool keeps the zero
     * steady-state allocation of that rewrite while restoring upstream
     * correctness; the shared base is only ever read, never mutated.
     *
     * <p>Draw calls queued DURING replay (re-entrant draw) are not executed
     * in this pass, matching the old snapshot-copy semantics exactly.
     */
    @ApiInternal
    public synchronized void invoke(World world, StoredMatrixTransformations storedMatrixTransformations, Direction facing, int light) {
        /* synchronized: the JS capture path (executor threads) swaps this
           manager's backing list via takeFrom() while the render thread
           replays it — without a common monitor the swap could land between
           the size read and a get(i), replay released/pooled draw calls, or
           IndexOutOfBoundsException straight into MTR's render. Capture and
           replay both hold this monitor only for microseconds. */
        final int count = this.drawCalls.size();
        if (count == 0) return;
        for (int i = 0; i < count; i++) {
            this.drawCalls.get(i).run(world, TransformCapturePool.acquire(storedMatrixTransformations), facing, light);
        }
    }

    @ApiInternal
    public ScriptRenderManager copy() {
        return new ScriptRenderManager(this.drawCalls);
    }

    /**
     * Takes the captured draw calls out of {@code other} without copying.
     *
     * <p>The JS capture path (VehicleScriptCallsHolder.Captured.capture) used
     * to {@code copy()} every per-car manager per frame — one new manager plus
     * a full list copy per car per script per frame, only for the copy to be
     * thrown away after the replay. This swaps the backing lists instead:
     * this manager keeps the captured calls, the committer keeps this
     * manager's (released and drained) list. Draw calls implementing
     * {@link RenderDrawCall#release()} are recycled into their pool here.
     *
     * <p>Historically documented "render-thread only" — but the JS capture
     * path runs it on the script executor threads; both sides now share
     * this manager's monitor (see invoke).
     */
    @ApiInternal
    public synchronized void takeFrom(ScriptRenderManager other) {
        final List<RenderDrawCall<?>> mine = this.drawCalls;
        for (int i = 0; i < mine.size(); i++) {
            mine.get(i).release();
        }
        mine.clear();
        this.drawCalls = other.drawCalls;
        other.drawCalls = mine;
    }

    @ApiInternal
    public synchronized void reset() {
        final List<RenderDrawCall<?>> calls = this.drawCalls;
        for (int i = 0; i < calls.size(); i++) {
            calls.get(i).release();
        }
        calls.clear();
    }
}
