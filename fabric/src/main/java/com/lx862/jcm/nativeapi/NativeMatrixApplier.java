package com.lx862.jcm.nativeapi;

public interface NativeMatrixApplier {
    void jcm$multiplyNativeMatrix(float[] matrix);

    /**
     * Offset variant used by the pooled native model draw calls: the 16
     * floats live in the draw call's own flat matrix store at
     * {@code floatOffset..floatOffset+16} (copied out of the frame arena at
     * capture time — the arena itself is module-owned and only valid until
     * the next native render call). The implementation reads them through a
     * render-thread scratch, then applies like
     * {@link #jcm$multiplyNativeMatrix(float[])}.
     */
    void jcm$multiplyNativeMatrix(float[] matrix, int floatOffset);

    /**
     * Yanyang.11: exposes the holder's wrapped raw pose so a queued render
     * callback can rebuild a {@code GraphicsHolder} around a PRIVATE
     * {@code VertexConsumerProvider} (the drawText carrier — see
     * {@code PSDScriptRenderer#drawBacking}) without losing the flush-time
     * camera matrices. Implemented by the {@code GraphicsHolderMatrixMixin}.
     */
#if LOADER == "fabric"
    net.minecraft.client.util.math.MatrixStack jcm$getMatrixStack();
#else
    com.mojang.blaze3d.vertex.PoseStack jcm$getMatrixStack();
#endif
}
