package com.lx862.jcm.nativeapi;

import org.mtr.mod.render.StoredMatrixTransformations;

/**
 * Duck-typed surface added to {@link StoredMatrixTransformations} by
 * {@code StoredMatrixTransformationsMixin}.
 *
 * <p>MTR's transform holder is a flat list of {@code Consumer<GraphicsHolder>}
 * entries. The scripting capture/replay pipeline used to give every draw
 * call its own {@code copy()} of the whole list so the per-call additions
 * (rotate sandwich in {@code RenderDrawCall.run}, position translate in
 * {@code PIDSDrawCall.run}) could not leak into the next call — one
 * StoredMatrixTransformations plus one ObjectArrayList plus a full element
 * copy per draw call per car per frame, which is exactly the steady young-gen
 * churn that surfaces as periodic GC stutter.
 *
 * <p>The replay path now hands each draw call a recycled snapshot from
 * {@code TransformCapturePool} instead ({@code jcm$clearTransformations} +
 * re-seed), which keeps zero steady-state allocation <b>and</b> keeps each
 * call's additions alive until the deferred render callback reads them at
 * flush time — a shared list cut back with mark/truncate lost them the
 * instant {@code run()} returned (the {@code jsblock:pids_1a} oversized-text
 * regression). {@code jcm$mark}/{@code jcm$truncate} remain available for
 * addons and the regression test.
 */
public interface NativeTransformScope {

    /** Current number of appended transformations. */
    int jcm$mark();

    /** Cut back to a previously taken {@link #jcm$mark()} (no-op when larger). */
    void jcm$truncate(int mark);

    /** Drop every appended transformation. */
    void jcm$clearTransformations();
}
