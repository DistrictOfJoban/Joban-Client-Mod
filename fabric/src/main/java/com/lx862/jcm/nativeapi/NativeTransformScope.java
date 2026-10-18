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
 * <p>The mark/truncate pair below restores the same isolation with zero
 * allocation: remember the list length, let the draw call append what it
 * needs, cut back to the mark. Only usable on the render thread (the same
 * discipline the copy-based path already required).
 */
public interface NativeTransformScope {

    /** Current number of appended transformations. */
    int jcm$mark();

    /** Cut back to a previously taken {@link #jcm$mark()} (no-op when larger). */
    void jcm$truncate(int mark);

    /** Drop every appended transformation. */
    void jcm$clearTransformations();
}
