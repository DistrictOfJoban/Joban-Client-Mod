package com.lx862.jcm.nativeapi;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;

/** Copies packed native ARGB pixels without a texture-sized temporary array. */
public final class NativePixelBuffer {
    private NativePixelBuffer() {}

    /**
     * Source is a tightly packed rectangle at byteOffset, with sourceWidth
     * pixels per row. Source coordinates select a clipped part of that rect.
     * The copy finishes synchronously: no module-owned buffer is retained by
     * a render callback, since its lifetime ends at the next native render.
     */
    public static void copy(ByteBuffer source, int byteOffset, int sourceWidth,
                            int sourceX, int sourceY, int[] destination, int destinationStride,
                            int destinationX, int destinationY, int width, int height) {
        if (source == null || destination == null || byteOffset < 0 || sourceWidth <= 0
                || sourceX < 0 || sourceY < 0 || width <= 0 || height <= 0
                || (long) sourceX + width > sourceWidth || destinationStride <= 0
                || destinationX < 0 || destinationY < 0
                || (long) destinationX + width > destinationStride) {
            throw new IllegalArgumentException("Invalid native pixel rectangle");
        }
        final long firstPixel = (long) sourceY * sourceWidth + sourceX;
        final long lastPixel = ((long) sourceY + height - 1) * sourceWidth + sourceX + width;
        final long destinationEnd = ((long) destinationY + height - 1) * destinationStride + destinationX + width;
        if (byteOffset > source.limit() || lastPixel > (source.limit() - byteOffset) / 4
                || destinationEnd > destination.length) {
            throw new IllegalArgumentException("Native pixel rectangle exceeds its buffer");
        }
        final int first = (int) (byteOffset + firstPixel * 4);
        final int end = (int) (byteOffset + lastPixel * 4);
        final ByteBuffer view = source.duplicate().order(ByteOrder.LITTLE_ENDIAN);
        view.position(first);
        view.limit(end);
        final IntBuffer pixels = view.slice().order(ByteOrder.LITTLE_ENDIAN).asIntBuffer();
        int output = destinationY * destinationStride + destinationX;
        if (sourceWidth == width && destinationStride == width) {
            pixels.get(destination, output, width * height);
        } else {
            for (int row = 0; row < height; row++) {
                pixels.position(row * sourceWidth);
                pixels.get(destination, output, width);
                output += destinationStride;
            }
        }
    }
}
