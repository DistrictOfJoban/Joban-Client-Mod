package com.lx862.mtrscripting.core.util.render;

import com.lx862.jcm.nativeapi.NativeTransformScope;
import org.mtr.mod.render.StoredMatrixTransformations;

/**
 * Recycled per-draw-call transform snapshots for {@link ScriptRenderManager#invoke}.
 *
 * <h2>Why a snapshot at all</h2>
 * A draw call's {@code run()} appends the transforms it needs (the PIDS
 * {@code BASE_SCALE}+position consumer, z-order offsets, {@code .matrices()}
 * merges) to the {@link StoredMatrixTransformations} it was handed, then
 * schedules the actual drawing as a <b>deferred</b> callback via
 * {@code MainRenderer.scheduleRender}. That callback runs at queue-flush
 * time — at the end of {@code MainRenderer.render}, long after
 * {@code invoke()} has returned — and reads the transform list <b>at that
 * moment</b> ({@code StoredMatrixTransformations.transform} iterates the
 * consumer list live). So every draw call must leave behind a list that
 * still contains its own additions when the flush eventually reads it.
 *
 * <p>The upstream implementation gave every draw call a
 * {@code storedMatrixTransformations.copy()} — correct, but one new object
 * plus a full list copy per draw call per frame. The yanyang.4
 * zero-allocation rewrite replaced the copies with mark/truncate on one
 * shared instance, which discarded each call's additions the instant
 * {@code run()} returned: at flush time only the base transforms were left,
 * so every scripted PIDS text rendered at raw block scale (the
 * {@code jsblock:pids_1a} report: text ~96x oversized and anchored to the
 * block origin) and every {@code .matrices()} model lost its pose.
 *
 * <h2>This pool</h2>
 * {@link #acquire} hands out a recycled {@link StoredMatrixTransformations}
 * seeded with a copy of the base consumer list. The shared base is never
 * mutated, the snapshot survives until the flush, and steady-state
 * allocation is zero (handouts are recycled by
 * {@code com.lx862.jcm.mixin.mtrpatch.MainRendererMixin} at the tail of
 * {@code MainRenderer.render}).
 *
 * <h2>Recycling invariant</h2>
 * {@link #recycleAll} may only run after a queue flush has consumed every
 * pending scheduled callback: the mixin's TAIL injection on
 * {@code MainRenderer.render(GraphicsHolder, Vector3d)} fires after the
 * flush loops and is skipped on that method's early returns (no world /
 * disabled shadow pass), where callbacks stay queued for a later frame.
 * {@code invoke()} runs strictly in the capture phases (block entity /
 * vehicle / eyecandy rendering), never inside a flushing callback, so
 * nothing acquires between the flush loops and the TAIL hook. Under both
 * possible phase orders (block entities before or after entities) every
 * handed-out instance has been read — or permanently dropped by
 * {@code cancelRender} — by the time it is recycled.
 */
public final class TransformCapturePool {

    /** Free-list cap: a pathological frame cannot pin unbounded memory. */
    private static final int FREE_CAP = 1024;

    private static StoredMatrixTransformations[] free = new StoredMatrixTransformations[8];
    private static int freeCount;
    private static StoredMatrixTransformations[] inFlight = new StoredMatrixTransformations[8];
    private static int inFlightCount;

    /** Total instances created by the pool (test hook: proves steady-state
     *  zero growth once warmed). */
    private static int allocated;

    private TransformCapturePool() {
    }

    /** Number of {@link StoredMatrixTransformations} allocated by the pool
     *  so far — test hook, not for gameplay code. */
    public static synchronized int allocatedCount() {
        return allocated;
    }

    /**
     * Takes a transform holder from the pool, empties it and seeds it with
     * the base list's current consumers. The returned instance is captured
     * by the draw call's deferred render callback and must not be reused
     * until {@link #recycleAll()} runs after the frame's queue flush.
     */
    public static synchronized StoredMatrixTransformations acquire(StoredMatrixTransformations base) {
        final StoredMatrixTransformations smt;
        if (freeCount > 0) {
            smt = free[--freeCount];
        } else {
            smt = new StoredMatrixTransformations();
            allocated++;
        }
        ((NativeTransformScope) (Object) smt).jcm$clearTransformations();
        smt.add(base);
        if (inFlightCount == inFlight.length) {
            final StoredMatrixTransformations[] grown = new StoredMatrixTransformations[inFlightCount * 2];
            System.arraycopy(inFlight, 0, grown, 0, inFlightCount);
            inFlight = grown;
        }
        inFlight[inFlightCount++] = smt;
        return smt;
    }

    /**
     * Returns every handed-out instance to the free list and drops its
     * consumer references (they may capture draw-call wrappers that are
     * pooled or reused). Render-thread only, after a completed queue flush.
     */
    public static synchronized void recycleAll() {
        if (inFlightCount == 0) return;
        for (int i = 0; i < inFlightCount; i++) {
            final StoredMatrixTransformations smt = inFlight[i];
            ((NativeTransformScope) (Object) smt).jcm$clearTransformations();
            if (freeCount < FREE_CAP) {
                if (freeCount == free.length) {
                    final StoredMatrixTransformations[] grown = new StoredMatrixTransformations[Math.max(8, free.length * 2)];
                    System.arraycopy(free, 0, grown, 0, freeCount);
                    free = grown;
                }
                free[freeCount++] = smt;
            }
            /* Beyond the cap: simply drop the reference — GC reclaims it. */
        }
        inFlightCount = 0;
    }
}
