package com.lx862.jcm.mixin.mtrscripting;

import com.lx862.jcm.nativeapi.NativeMatrixApplier;
import org.mtr.mapping.mapper.GraphicsHolder;
import org.spongepowered.asm.mixin.*;
import java.nio.FloatBuffer;

#if LOADER == "fabric"
import net.minecraft.client.util.math.MatrixStack;
#else
import com.mojang.blaze3d.vertex.PoseStack;
#endif

@Mixin(value=GraphicsHolder.class,remap=false)
public abstract class GraphicsHolderMatrixMixin implements NativeMatrixApplier {
    @Shadow @Final
#if LOADER == "fabric"
    private MatrixStack matrixStack;
#else
    private PoseStack matrixStack;
#endif

#if MC_VERSION >= "11903"
    /* Pooled JOML scratch (render thread): the old path allocated a fresh
       Matrix4f + Matrix3f per native pose application — per pose per car
       per frame. Both objects are fully overwritten by set() before every
       use and only read within this call, so reuse is safe. */
    private static final ThreadLocal<org.joml.Matrix4f> jcm$ScratchMatrix4f =
            ThreadLocal.withInitial(org.joml.Matrix4f::new);
    private static final ThreadLocal<org.joml.Matrix3f> jcm$ScratchMatrix3f =
            ThreadLocal.withInitial(org.joml.Matrix3f::new);
#endif

    /* Scratch for the offset pose variant: one small array per render
       thread, reused every call. */
    private static final ThreadLocal<float[]> jcm$NativePoseScratch =
            ThreadLocal.withInitial(() -> new float[16]);

    @Override public void jcm$multiplyNativeMatrix(float[] values, int floatOffset) {
        final float[] scratch = jcm$NativePoseScratch.get();
        System.arraycopy(values, floatOffset, scratch, 0, 16);
        jcm$multiplyNativeMatrix(scratch);
    }
    @Override public void jcm$multiplyNativeMatrix(float[] values) {
#if MC_VERSION >= "11903"
        org.joml.Matrix4f matrix=jcm$ScratchMatrix4f.get();
        matrix.set(values);
        org.joml.Matrix3f normal=jcm$ScratchMatrix3f.get();
        normal.set(matrix.m00(),matrix.m01(),matrix.m02(),
                   matrix.m10(),matrix.m11(),matrix.m12(),
                   matrix.m20(),matrix.m21(),matrix.m22());
        normal.invert().transpose();
#if LOADER == "fabric"
        matrixStack.peek().getPositionMatrix().mul(matrix);
        matrixStack.peek().getNormalMatrix().mul(normal);
#else
        matrixStack.last().pose().mul(matrix);
        matrixStack.last().normal().mul(normal);
#endif
#else
#if LOADER == "fabric"
        net.minecraft.util.math.Matrix4f matrix=new net.minecraft.util.math.Matrix4f();
        matrix.readColumnMajor(FloatBuffer.wrap(values));
#if MC_VERSION <= "11701"
        matrixStack.peek().getModel().multiply(matrix);
#else
        matrixStack.peek().getPositionMatrix().multiply(matrix);
#endif
        net.minecraft.util.math.Matrix3f normal = new net.minecraft.util.math.Matrix3f(matrix);
        normal.invert(); normal.transpose();
#if MC_VERSION <= "11701"
        matrixStack.peek().getNormal().multiply(normal);
#else
        matrixStack.peek().getNormalMatrix().multiply(normal);
#endif
#else
        com.mojang.math.Matrix4f matrix=new com.mojang.math.Matrix4f();
        matrix.load(FloatBuffer.wrap(values));
        matrixStack.last().pose().multiply(matrix);
        com.mojang.math.Matrix3f normal = new com.mojang.math.Matrix3f(matrix);
        normal.invert(); normal.transpose();
        matrixStack.last().normal().mul(normal);
#endif
#endif
    }
}
