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
}
