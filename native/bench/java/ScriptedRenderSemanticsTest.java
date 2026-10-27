package com.lx862.jcm.test;

import com.lx862.jcm.nativeapi.NativeTransformScope;
import com.lx862.mtrscripting.core.annotation.ApiInternal;
import com.lx862.mtrscripting.core.util.render.RenderDrawCall;
import com.lx862.mtrscripting.core.util.render.ScriptRenderManager;
import com.lx862.mtrscripting.core.util.render.TransformCapturePool;
import org.mtr.mapping.holder.Direction;
import org.mtr.mapping.holder.Vector3d;
import org.mtr.mapping.holder.World;
import org.mtr.mapping.mapper.GraphicsHolder;
import org.mtr.mod.render.StoredMatrixTransformations;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Flush-time semantics regression for the scripted render replay path.
 *
 * <p>A draw call's {@code run()} appends transforms (the PIDS
 * {@code BASE_SCALE}+position consumer, the {@code .matrices()} rotate
 * sandwich) to the holder it is given and schedules the actual drawing as a
 * DEFERRED callback ({@code MainRenderer.scheduleRender}). The callback
 * reads the holder's consumer list at flush time — after
 * {@code ScriptRenderManager.invoke} has already returned. This test drives
 * exactly that lifecycle with the production {@link ScriptRenderManager},
 * the production {@link TransformCapturePool} and a {@link StoredMatrixTransformations}
 * stub whose semantics were copied from the decompiled MTR 4.0.5 jar
 * (transform() pushes and does not pop; consumers are iterated live).
 *
 * <p>It also proves its own discriminating power: the yanyang.4..yanyang.8
 * mark/truncate replay (embedded below as {@link #legacyReplay}) must
 * demonstrably LOSE the per-call transforms — that loss is what rendered
 * every scripted PIDS text (e.g. {@code jsblock:pids_1a}) ~96x oversized
 * and anchored to the block origin. S6/S7 guard the other half of
 * upstream's {@code copy()} semantics — the world anchor: a pooled
 * snapshot that drops it renders at the rendering entity instead of its
 * world position, which is the floating-text regression (scripted PIDS
 * text following the camera as the player moved).
 */
public final class ScriptedRenderSemanticsTest {

    /** PIDSPresetBase.BASE_SCALE — the consumer the regression dropped. */
    private static final float BASE_SCALE = 1 / 96F;
    /** Script scale from the builtin pids_1a.js (Text.create(...).scale(1.725)). */
    private static final float SCRIPT_SCALE = 1.725F;

    /** Simulates MainRenderer's queued render callbacks. */
    private static final List<BiConsumer<GraphicsHolder, Vector3d>> SCHEDULED = new ArrayList<>();

    private static final Vector3d OFFSET = new Vector3d(12, 3, -7);
    private static final World WORLD = new World();

    /** Draw call that reproduces PIDSDrawCall.run + TextWrapper.drawTransformed:
     *  a persistent position consumer (BASE_SCALE + pos) appended to the
     *  holder, then a deferred callback that transforms against that same
     *  holder, applies the script scale and balances the push. */
    private static final class FakeTextCall extends RenderDrawCall<FakeTextCall> {
        double px, py;
        final Consumer<GraphicsHolder> position = gh -> {
            gh.scale(BASE_SCALE, BASE_SCALE, BASE_SCALE);
            gh.translate(px, py, 0);
        };

        FakeTextCall(double px, double py) {
            this.px = px;
            this.py = py;
        }

        @Override
        public void run(World world, StoredMatrixTransformations storedMatrixTransformations, Direction facing, int light) {
            super.run(world, storedMatrixTransformations, facing, light);
            storedMatrixTransformations.add(this.position);
            scheduleRender((gh, offset) -> {
                storedMatrixTransformations.transform(gh, offset);
                gh.scale(SCRIPT_SCALE, SCRIPT_SCALE, SCRIPT_SCALE);
                gh.pop();
            });
        }

        @ApiInternal
        @Override
        public void validate() {
        }
    }

    /** Draw call that only relies on the {@code .matrices()} rotate sandwich
     *  merged by RenderDrawCall.run (the ModelDrawCall pattern). */
    private static final class FakeModelCall extends RenderDrawCall<FakeModelCall> {
        FakeModelCall() {
            matrices(new com.lx862.mtrscripting.core.util.Matrices());
        }

        @Override
        public void run(World world, StoredMatrixTransformations storedMatrixTransformations, Direction facing, int light) {
            super.run(world, storedMatrixTransformations, facing, light);
            scheduleRender((gh, offset) -> {
                storedMatrixTransformations.transform(gh, offset);
                gh.pop();
            });
        }

        @ApiInternal
        @Override
        public void validate() {
        }
    }

    /** A draw call that queues another call while being replayed. */
    private static final class ReentrantCall extends RenderDrawCall<ReentrantCall> {
        final ScriptRenderManager manager;
        boolean chained;

        ReentrantCall(ScriptRenderManager manager) {
            this.manager = manager;
        }

        @Override
        public void run(World world, StoredMatrixTransformations storedMatrixTransformations, Direction facing, int light) {
            super.run(world, storedMatrixTransformations, facing, light);
            scheduleRender((gh, offset) -> {
                storedMatrixTransformations.transform(gh, offset);
                gh.pop();
            });
            if (!chained) {
                chained = true;
                manager.draw(new FakeTextCall(99, 99));
            }
        }

        @ApiInternal
        @Override
        public void validate() {
        }
    }

    private static void scheduleRender(BiConsumer<GraphicsHolder, Vector3d> callback) {
        SCHEDULED.add(callback);
    }

    /** The yanyang.4..yanyang.8 invoke body, verbatim semantics: one shared
     *  holder, mark before the loop, truncate after every run. */
    private static void legacyReplay(List<? extends RenderDrawCall<?>> calls, StoredMatrixTransformations base) {
        final NativeTransformScope scope = (NativeTransformScope) (Object) base;
        final int mark = scope.jcm$mark();
        try {
            for (RenderDrawCall<?> call : calls) {
                call.run(WORLD, base, Direction.NORTH, 0xF000F0);
                scope.jcm$truncate(mark);
            }
        } finally {
            scope.jcm$truncate(mark);
        }
    }

    /** Runs every scheduled callback against a fresh recording holder, then
     *  performs the tail-of-render recycle (the mtrpatch.MainRendererMixin
     *  TAIL hook). Returns the per-callback op lists, in scheduling order. */
    private static List<List<GraphicsHolder.Op>> flush() {
        final List<List<GraphicsHolder.Op>> frames = new ArrayList<>();
        for (BiConsumer<GraphicsHolder, Vector3d> callback : SCHEDULED) {
            final GraphicsHolder holder = new GraphicsHolder();
            callback.accept(holder, OFFSET);
            frames.add(holder.ops);
        }
        SCHEDULED.clear();
        TransformCapturePool.recycleAll();
        return frames;
    }

    private static StoredMatrixTransformations pidsBase() {
        // PIDSRenderer + PIDS1ARenderer base: rotateY, rotateZ, translate.
        final StoredMatrixTransformations base = new StoredMatrixTransformations();
        base.add(gh -> gh.rotateYDegrees(90));
        base.add(gh -> gh.rotateZDegrees(180));
        base.add(gh -> gh.translate(-0.47, -0.155, -0.130));
        return base;
    }

    /** The real PIDSRenderer base form: a 4-arg world anchor (block center)
     *  plus local rotation consumers. */
    private static StoredMatrixTransformations anchoredPidsBase(double bx, double by, double bz) {
        final StoredMatrixTransformations base = new StoredMatrixTransformations(bx, by, bz);
        base.add(gh -> gh.rotateYDegrees(90));
        base.add(gh -> gh.rotateZDegrees(180));
        base.add(gh -> gh.translate(-0.47, -0.155, -0.130));
        return base;
    }

    private static boolean has(List<GraphicsHolder.Op> ops, String type, double x, double y, double z) {
        for (GraphicsHolder.Op op : ops) {
            if (op.type.equals(type)
                    && Math.abs(op.x - x) < 1e-6 && Math.abs(op.y - y) < 1e-6 && Math.abs(op.z - z) < 1e-6) {
                return true;
            }
        }
        return false;
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    public static void main(String[] args) {
        /* ---------- S1: the jsblock:pids_1a regression ---------- */
        {
            final StoredMatrixTransformations base = pidsBase();
            final ScriptRenderManager manager = new ScriptRenderManager();
            manager.draw(new FakeTextCall(3, 10));
            manager.draw(new FakeTextCall(7, 20));
            manager.invoke(WORLD, base, Direction.NORTH, 0xF000F0);

            check(base.size() == 3, "shared base must not be mutated by invoke (eyecandy transform-cache safety), size=" + base.size());
            check(SCHEDULED.size() == 2, "two callbacks scheduled, got " + SCHEDULED.size());

            final List<List<GraphicsHolder.Op>> frames = flush();
            check(frames.size() == 2, "two flushed callbacks");

            final List<GraphicsHolder.Op> first = frames.get(0);
            check(has(first, "push", 0, 0, 0), "callback 1: transform must push");
            check(has(first, "rotateY", 90, 0, 0), "callback 1: base rotateY applied");
            check(has(first, "rotateZ", 180, 0, 0), "callback 1: base rotateZ applied");
            check(has(first, "translate", -0.47, -0.155, -0.130), "callback 1: base translate applied");
            check(has(first, "scale", BASE_SCALE, BASE_SCALE, BASE_SCALE),
                    "callback 1: BASE_SCALE consumer must survive until flush (the pids_1a regression)");
            check(has(first, "translate", 3, 10, 0), "callback 1: own position applied");
            check(has(first, "scale", SCRIPT_SCALE, SCRIPT_SCALE, SCRIPT_SCALE), "callback 1: script scale applied");
            check(has(first, "pop", 0, 0, 0), "callback 1: push/pop balanced");
            check(first.get(0).type.equals("push") && first.get(first.size() - 1).type.equals("pop"),
                    "callback 1: push first, pop last");

            final List<GraphicsHolder.Op> second = frames.get(1);
            check(has(second, "scale", BASE_SCALE, BASE_SCALE, BASE_SCALE), "callback 2: BASE_SCALE applied");
            check(has(second, "translate", 7, 20, 0), "callback 2: own position applied");
            check(!has(second, "translate", 3, 10, 0), "callback 2: must NOT inherit callback 1's position (isolation)");
        }

        /* ---------- S2: .matrices() merge survives until flush ---------- */
        {
            final ScriptRenderManager manager = new ScriptRenderManager();
            manager.draw(new FakeModelCall());
            manager.invoke(WORLD, pidsBase(), Direction.NORTH, 0xF000F0);
            final List<List<GraphicsHolder.Op>> frames = flush();
            check(frames.size() == 1, "model callback scheduled");
            final List<GraphicsHolder.Op> ops = frames.get(0);
            check(has(ops, "rotateX", 180, 0, 0), "matrices: rotateX(180) sandwich half applied");
            check(has(ops, "translate", 0.5, 0.5, 0.5), "matrices: merged consumer applied at flush");
            check(has(ops, "rotateX", -180, 0, 0), "matrices: rotateX(-180) sandwich half applied");
        }

        /* ---------- S3: pool reaches steady state (zero allocation) ---------- */
        {
            final StoredMatrixTransformations base = pidsBase();
            /* Frame 0 warms the pool up to this scenario's high-water mark
             * (3 concurrent handouts); every later frame must reuse exactly
             * those instances — no further allocation. */
            int steady = -1;
            for (int frame = 0; frame < 5; frame++) {
                final ScriptRenderManager manager = new ScriptRenderManager();
                manager.draw(new FakeTextCall(1, 2));
                manager.draw(new FakeTextCall(3, 4));
                manager.draw(new FakeModelCall());
                manager.invoke(WORLD, base, Direction.NORTH, 0xF000F0);
                final List<List<GraphicsHolder.Op>> frames = flush();
                check(frames.size() == 3, "steady frame schedules 3 callbacks");
                check(has(frames.get(0), "translate", 1, 2, 0), "steady frame " + frame + ": first position applied");
                check(has(frames.get(2), "translate", 0.5, 0.5, 0.5), "steady frame " + frame + ": matrices merge applied");
                if (frame == 0) {
                    steady = TransformCapturePool.allocatedCount();
                    check(steady > 0, "pool allocated during warm-up");
                } else {
                    check(TransformCapturePool.allocatedCount() == steady,
                            "pool must not grow in steady state (frame " + frame + "): " + steady + " -> "
                                    + TransformCapturePool.allocatedCount());
                }
            }
        }

        /* ---------- S4: re-entrant draws are not executed this pass ---------- */
        {
            final ScriptRenderManager manager = new ScriptRenderManager();
            manager.draw(new ReentrantCall(manager));
            manager.invoke(WORLD, pidsBase(), Direction.NORTH, 0xF000F0);
            check(SCHEDULED.size() == 1, "re-entrant draw must not execute in this pass, scheduled=" + SCHEDULED.size());
            flush();
        }

        /* ---------- S5: the legacy mark/truncate replay must FAIL these
             assertions — the test proves its own discriminating power.
             (Exactly the yanyang.4..yanyang.8 invoke semantics.) ---------- */
        {
            final StoredMatrixTransformations base = pidsBase();
            final List<RenderDrawCall<?>> calls = new ArrayList<>();
            calls.add(new FakeTextCall(3, 10));
            calls.add(new FakeTextCall(7, 20));
            legacyReplay(calls, base);

            check(base.size() == 3, "legacy: base truncated back to mark");
            final List<List<GraphicsHolder.Op>> frames = flush();
            check(frames.size() == 2, "legacy: two callbacks scheduled");

            final boolean legacyHasPosition = has(frames.get(0), "scale", BASE_SCALE, BASE_SCALE, BASE_SCALE)
                    || has(frames.get(0), "translate", 3, 10, 0);
            check(!legacyHasPosition,
                    "DISCRIMINATOR INVALID: the embedded legacy (mark/truncate) replay unexpectedly preserved the "
                            + "per-call transforms. If this fires, this test no longer reproduces the yanyang.4 "
                            + "regression it was written to guard — investigate before shipping.");
            check(!has(frames.get(1), "translate", 7, 20, 0),
                    "legacy: second callback position must also be lost (the reported pids_1a symptom)");
        }

        /* ---------- S6: the world anchor must survive into the pooled
             snapshot — the floating-text regression. PIDSRenderer builds its
             base with the 4-arg constructor (block center); at flush time
             transform() turns the anchor into
             translate(initialTranslate - offset). A pooled snapshot seeded
             without it (the yanyang.8 pool) never applies that translate and
             the geometry renders at the rendering entity / follows the
             camera — the reported jsblock:pids_1a bug. ---------- */
        {
            final double bx = 100.5, by = 64.5, bz = -200.5;
            final StoredMatrixTransformations base = anchoredPidsBase(bx, by, bz);
            final ScriptRenderManager manager = new ScriptRenderManager();
            manager.draw(new FakeTextCall(3, 10));
            manager.invoke(WORLD, base, Direction.NORTH, 0xF000F0);

            check(base.size() == 3, "S6: anchored base must not be mutated");
            final List<List<GraphicsHolder.Op>> frames = flush();
            check(frames.size() == 1, "S6: one callback scheduled");
            final List<GraphicsHolder.Op> ops = frames.get(0);
            check(ops.size() > 0 && ops.get(0).type.equals("push"), "S6: transform pushes first");
            /* The anchor translate must be the FIRST op after the push,
               exactly where MTR's transform() emits it. */
            check(ops.size() > 1 && ops.get(1).type.equals("translate")
                            && Math.abs(ops.get(1).x - (bx - OFFSET.getXMapped())) < 1e-6
                            && Math.abs(ops.get(1).y - (by - OFFSET.getYMapped())) < 1e-6
                            && Math.abs(ops.get(1).z - (bz - OFFSET.getZMapped())) < 1e-6,
                    "S6: world anchor must apply translate(initialTranslate - offset) at flush "
                            + "(the floating-text regression)");
            /* And the per-call transforms must still be there too. */
            check(has(ops, "scale", BASE_SCALE, BASE_SCALE, BASE_SCALE), "S6: BASE_SCALE still applied");
            check(has(ops, "translate", 3, 10, 0), "S6: own position still applied");

            /* Reference check against upstream's copy() semantics: the
               pooled snapshot must transform identically to a fresh
               base.copy() carrying the same call additions (plus the
               callback's script-scale tail, which both sides share). */
            final StoredMatrixTransformations upstreamReference = base.copy();
            upstreamReference.add(gh -> { gh.scale(BASE_SCALE, BASE_SCALE, BASE_SCALE); gh.translate(3, 10, 0); });
            final GraphicsHolder referenceHolder = new GraphicsHolder();
            upstreamReference.transform(referenceHolder, OFFSET);
            referenceHolder.ops.add(new GraphicsHolder.Op("scale", SCRIPT_SCALE, SCRIPT_SCALE, SCRIPT_SCALE));
            referenceHolder.ops.add(new GraphicsHolder.Op("pop", 0, 0, 0));
            // Re-run the same capture/flush for the comparison ops.
            final ScriptRenderManager manager2 = new ScriptRenderManager();
            manager2.draw(new FakeTextCall(3, 10));
            manager2.invoke(WORLD, base, Direction.NORTH, 0xF000F0);
            final List<List<GraphicsHolder.Op>> frames2 = flush();
            check(frames2.size() == 1, "S6: reference re-run scheduled");
            check(frames2.get(0).size() == referenceHolder.ops.size(),
                    "S6: pooled snapshot op count matches upstream copy() semantics");
            boolean opsEqual = frames2.get(0).size() == referenceHolder.ops.size();
            for (int i = 0; opsEqual && i < referenceHolder.ops.size(); i++) {
                final GraphicsHolder.Op a = frames2.get(0).get(i);
                final GraphicsHolder.Op b = referenceHolder.ops.get(i);
                opsEqual = a.type.equals(b.type) && Math.abs(a.x - b.x) < 1e-6
                        && Math.abs(a.y - b.y) < 1e-6 && Math.abs(a.z - b.z) < 1e-6;
            }
            check(opsEqual, "S6: pooled snapshot must transform identically to upstream's copy()");
        }

        /* ---------- S7: recycled instances re-anchor to each frame's base.
             Frame 1 anchors at one block, frame 2 at another — the pooled
             handout of frame 2 must carry frame 2's anchor, not the stale
             one from frame 1 (guards the re-seed inside acquire()). ---------- */
        {
            final StoredMatrixTransformations baseA = anchoredPidsBase(1.5, 2.5, 3.5);
            final ScriptRenderManager managerA = new ScriptRenderManager();
            managerA.draw(new FakeTextCall(0, 0));
            managerA.invoke(WORLD, baseA, Direction.NORTH, 0xF000F0);
            final List<List<GraphicsHolder.Op>> framesA = flush();
            check(framesA.size() == 1, "S7: frame A scheduled");
            check(framesA.get(0).size() > 1 && Math.abs(framesA.get(0).get(1).x - (1.5 - OFFSET.getXMapped())) < 1e-6,
                    "S7: frame A anchored at its own block");

            final double bx2 = 900.5, by2 = 30.5, bz2 = 777.5;
            final StoredMatrixTransformations baseB = anchoredPidsBase(bx2, by2, bz2);
            final ScriptRenderManager managerB = new ScriptRenderManager();
            managerB.draw(new FakeTextCall(0, 0));
            managerB.invoke(WORLD, baseB, Direction.NORTH, 0xF000F0);
            final List<List<GraphicsHolder.Op>> framesB = flush();
            check(framesB.size() == 1, "S7: frame B scheduled");
            final List<GraphicsHolder.Op> opsB = framesB.get(0);
            check(opsB.size() > 1 && opsB.get(1).type.equals("translate")
                            && Math.abs(opsB.get(1).x - (bx2 - OFFSET.getXMapped())) < 1e-6
                            && Math.abs(opsB.get(1).y - (by2 - OFFSET.getYMapped())) < 1e-6
                            && Math.abs(opsB.get(1).z - (bz2 - OFFSET.getZMapped())) < 1e-6,
                    "S7: recycled handout must re-anchor to frame B's base (stale anchors are the "
                            + "floating-text regression in recycling form)");
        }

        System.out.println("ScriptedRenderSemanticsTest: all assertions passed (fixed replay correct, legacy replay demonstrably broken, world anchor preserved)");
    }

    private ScriptedRenderSemanticsTest() {
    }
}
