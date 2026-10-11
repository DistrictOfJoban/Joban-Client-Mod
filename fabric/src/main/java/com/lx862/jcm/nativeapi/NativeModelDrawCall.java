package com.lx862.jcm.nativeapi;

import com.lx862.mtrscripting.core.util.model.ModelJS;
import com.lx862.mtrscripting.core.util.render.RenderDrawCall;
import org.mtr.mapping.holder.Direction;
import org.mtr.mapping.holder.World;
import org.mtr.mod.render.StoredMatrixTransformations;

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
 * <p>MTR replays the queued {@code StoredMatrixTransformations} on the render
 * thread later in the frame, so this object must not capture frame-scoped
 * state.
 */
public final class NativeModelDrawCall extends RenderDrawCall<NativeModelDrawCall> {

    private final int modelHandle;

    public NativeModelDrawCall(int modelHandle) {
        this.modelHandle = modelHandle;
    }

    public static NativeModelDrawCall create(int modelHandle) {
        return new NativeModelDrawCall(modelHandle);
    }

    public NativeModelDrawCall pose(java.nio.ByteBuffer arena,int offset,int count) {
        if(offset<0||count<=0) return this;
        if(arena==null||((long)offset+count)*64>arena.limit()) throw new IllegalArgumentException("Invalid native pose arena");
        final float[][] poses=new float[count][16];
        arena=arena.duplicate().order(java.nio.ByteOrder.nativeOrder());
        for(int i=0;i<count;i++) for(int j=0;j<16;j++) poses[i][j]=arena.getFloat((offset+i)*64+j*4);
        storedMatrixTransformations=new StoredMatrixTransformations();
        for(float[] pose:poses) storedMatrixTransformations.add(gh->((NativeMatrixApplier)(Object)gh).jcm$multiplyNativeMatrix(pose));
        return this;
    }

    @Override
    public void run(World world, StoredMatrixTransformations storedMatrixTransformations,
                    Direction facing, int light) {
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
