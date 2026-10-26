package com.lx862.jcm.nativeapi;

import com.lx862.mtrscripting.core.util.model.ModelJS;
import com.lx862.mtrscripting.core.util.render.RenderDrawCall;
import org.mtr.mapping.holder.Direction;
import org.mtr.mapping.holder.World;
import org.mtr.mapping.mapper.GraphicsHolder;
import org.mtr.mod.render.StoredMatrixTransformations;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayDeque;
import java.util.function.Consumer;

/**
 * NativeModelDrawCall — replays a {@code JCM_DRAW_MODEL} record produced by a
 * C++ script.
 *
 * <p>Behaviourally identical to {@link com.lx862.mtrscripting.core.util.render.ModelDrawCall}
 * (same {@link RenderDrawCall} matrix handling, same {@link ModelJS#draw}),
 * except the model is looked up from {@link NativeHost} by handle rather than
 * held as a field: the C++ side only knows opaque ints, and the concrete
 * {@code ModelJS} is replaced whenever a script instance rebuilds its quads
 * (car-count change).
 *
 * <h2>Zero per-frame garbage (yanyang)</h2>
 * The old {@code pose()} copied every matrix out of the frame arena into a
 * fresh {@code float[count][16]}, wrapped it in one fresh lambda per pose and
 * a fresh {@code StoredMatrixTransformations} — per model record, per car,
 * per frame (a 6-car train with 10 model records ≈ 460 KB/s of short-lived
 * arrays at 60 fps). Instances are now pooled: the matrices are copied into
 * this call's own flat store at <b>capture</b> time, and the persistent
 * per-slot pose consumers read from that store at replay time.
 *
 * <p><b>Why capture-time copy instead of reading the arena at replay
 * time:</b> the matrix arena is owned by the C++ module and only valid until
 * that module's next {@code mtrRender} call. The vehicle pipeline captures
 * ALL scripted vehicles first and replays their cars afterwards, and two
 * vehicles sharing one script module overwrite each other's arena in between
 * — a deferred arena read would alias the <i>last</i> vehicle's matrices into
 * every vehicle's replay. The instance-private store costs one 64-byte copy
 * per pose at capture time and cannot alias anything.
 */
public final class NativeModelDrawCall extends RenderDrawCall<NativeModelDrawCall> {

    /** JcmDrawModel pose matrices: 16 floats, 64 bytes, record-word offset. */
    private static final int POSE_BYTES = 64;
    private static final int POSE_FLOATS = 16;

    private static final ArrayDeque<NativeModelDrawCall> POOL = new ArrayDeque<>();
    private static final int POOL_CAP = 256;

    private int modelHandle;

    /**
     * Pose matrices flattened into this instance's own store (16 floats per
     * slot), copied out of the frame arena when {@link #pose} runs. Grows on
     * demand and is reused for the pooled instance's whole lifetime — a
     * steady frame copies matrices but allocates nothing.
     */
    private float[] poseMatrices = NO_MATRICES;
    private static final float[] NO_MATRICES = new float[0];

    /** Persistent pose consumers, one per matrix slot. */
    private NativePose[] poses = NO_POSES;
    private static final NativePose[] NO_POSES = new NativePose[0];

    /** One persistent transform holder per draw call; cleared and re-filled
     *  in place each frame (the jcm$clearTransformations accessor the
     *  StoredMatrixTransformations mixin adds to MTR's class). */
    private StoredMatrixTransformations pooledTransformations;

    private boolean inPool;

    /** Reads one 16-float matrix block from the owning call's store. */
    private static final class NativePose implements Consumer<GraphicsHolder> {
        private final NativeModelDrawCall owner;
        private final int slot;

        NativePose(NativeModelDrawCall owner, int slot) {
            this.owner = owner;
            this.slot = slot;
        }

        @Override
        public void accept(GraphicsHolder graphicsHolder) {
            ((NativeMatrixApplier) (Object) graphicsHolder)
                    .jcm$multiplyNativeMatrix(owner.poseMatrices, slot * POSE_FLOATS);
        }
    }

    private NativeModelDrawCall(int modelHandle) {
        this.modelHandle = modelHandle;
    }

    /** Only native drivers create these, and they never leak a reference out
     *  of one frame — pooling is unconditional. */
    public static NativeModelDrawCall create(int modelHandle) {
        final NativeModelDrawCall call = POOL.pollFirst();
        if (call != null) {
            call.inPool = false;
            call.modelHandle = modelHandle;
            return call;
        }
        return new NativeModelDrawCall(modelHandle);
    }

    @Override
    public void release() {
        if (this.inPool) return;
        this.inPool = true;
        this.storedMatrixTransformations = null;
        /* poseMatrices and the persistent consumers stay: they are private
           to this instance (no arena is referenced), and the next create()
           reuses them. */
        if (POOL.size() < POOL_CAP) POOL.offerFirst(this);
    }

    public NativeModelDrawCall pose(ByteBuffer arena, int offset, int count) {
        if (offset < 0 || count <= 0) {
            /* No matrices: make sure a pooled instance carries no stale
               entries (its persistent holder may survive from last frame). */
            this.storedMatrixTransformations = null;
            return this;
        }
        if (arena == null || ((long) offset + count) * POSE_BYTES > arena.limit()) {
            throw new IllegalArgumentException("Invalid native pose arena");
        }
        arena.order(ByteOrder.nativeOrder());
        /* Capture-time copy into the instance store (see class javadoc for
           why the arena itself must not be read at replay time). The region
           is one contiguous float stream: matrix i at byte (offset+i)*64. */
        final int floats = count * POSE_FLOATS;
        if (this.poseMatrices.length < floats) {
            this.poseMatrices = new float[Math.max(floats, this.poseMatrices.length * 2)];
        }
        final int base = offset * POSE_BYTES;
        for (int i = 0; i < floats; i++) {
            this.poseMatrices[i] = arena.getFloat(base + i * 4);
        }
        if (this.poses.length < count) {
            final NativePose[] grown = new NativePose[Math.max(count, this.poses.length * 2)];
            System.arraycopy(this.poses, 0, grown, 0, this.poses.length);
            for (int i = this.poses.length; i < grown.length; i++) grown[i] = new NativePose(this, i);
            this.poses = grown;
        }
        if (this.pooledTransformations == null) {
            this.pooledTransformations = new StoredMatrixTransformations();
        }
        ((NativeTransformScope) (Object) this.pooledTransformations).jcm$clearTransformations();
        for (int i = 0; i < count; i++) {
            this.pooledTransformations.add(this.poses[i]);
        }
        this.storedMatrixTransformations = this.pooledTransformations;
        return this;
    }

    @Override
    public void run(World world, StoredMatrixTransformations storedMatrixTransformations, Direction facing, int light) {
        super.run(world, storedMatrixTransformations, facing, light);
        final ModelJS model = NativeHost.get().model(modelHandle);
        if (model == null) return;   /* instance rebuilt between capture and replay */
        model.draw(storedMatrixTransformations, light);
    }

    @Override
    public void validate() {
        /* A missing model is a normal transient here (the host may have
           released it after capture); ModelDrawCall throws instead. */
    }
}
