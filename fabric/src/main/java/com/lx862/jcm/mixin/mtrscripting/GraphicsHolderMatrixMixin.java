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
    @Override public void jcm$multiplyNativeMatrix(float[] values) {
#if MC_VERSION >= "11903"
        org.joml.Matrix4f matrix=new org.joml.Matrix4f().set(values);
#if LOADER == "fabric"
        matrixStack.peek().getPositionMatrix().mul(matrix);
        matrixStack.peek().getNormalMatrix().mul(new org.joml.Matrix3f(matrix).invert().transpose());
#else
        matrixStack.last().pose().mul(matrix);
        matrixStack.last().normal().mul(new org.joml.Matrix3f(matrix).invert().transpose());
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
