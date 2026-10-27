package com.lx862.mtrscripting.core.util;

import com.lx862.jcm.mapping.LoaderImplClient;
import com.lx862.mtrscripting.core.annotation.ApiInternal;
import com.lx862.mtrscripting.mod.MTRScriptingMod;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import org.lwjgl.opengl.GL33;
import org.lwjgl.system.MemoryUtil;
import org.mtr.mapping.holder.*;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.Closeable;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.UUID;

@SuppressWarnings("unused")
public class GraphicsTexture implements Closeable {
    private final NativeImageBackedTexture dynamicTexture;
    public final Identifier identifier;

    public final BufferedImage bufferedImage;
    public final Graphics2D graphics;

    public final int width, height;

    public GraphicsTexture(int width, int height) {
        this.width = width;
        this.height = height;
        this.dynamicTexture = new NativeImageBackedTexture(new NativeImage(width, height, false));
        this.identifier = MTRScriptingMod.id(String.format("dynamic/graphics/%s", UUID.randomUUID()));

        MinecraftClient.getInstance().execute(() -> {
            // Use GL Swizzle to remap color. MC's NativeImage is BGRA
            int prevTextureBinding = GL33.glGetInteger(GL33.GL_TEXTURE_BINDING_2D);
            this.dynamicTexture.bindTexture();
            GL33.glTexParameteriv(GL33.GL_TEXTURE_2D, GL33.GL_TEXTURE_SWIZZLE_RGBA,
                    new int[] { GL33.GL_BLUE, GL33.GL_GREEN, GL33.GL_RED, GL33.GL_ALPHA });
            GlStateManager._bindTexture(prevTextureBinding);

            // Register the texture to MC!
            MinecraftClient.getInstance().getTextureManager().registerTexture(identifier, new AbstractTexture(this.dynamicTexture.data));
        });
        bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        graphics = bufferedImage.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        graphics.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
    }

    public GraphicsTexture fullAA() {
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        return this;
    }

    public GraphicsTexture noAA() {
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
        return this;
    }

    @ApiInternal
    public static BufferedImage createArgbBufferedImage(BufferedImage src) {
        BufferedImage newImage = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = newImage.createGraphics();
        graphics.drawImage(src, 0, 0, null);
        graphics.dispose();
        return newImage;
    }

    /**
     * Upload the full image
     */
    public void upload() {
        copyBuffer(this.bufferedImage, this.dynamicTexture, 0, 0, 0, 0, this.width, this.height, this.width, this.height, this.width, this.height);
        RenderSystem.recordRenderCall(dynamicTexture::upload);
    }

    public void upload(int x, int y, int width, int height) {
        upload(x, y, x, y, width, height);
    }

    public void upload(int dstOffsetX, int dstOffsetY, int srcOffsetX, int srcOffsetY, int uploadWidth, int uploadHeight) {
        upload(this.bufferedImage, dstOffsetX, dstOffsetY, srcOffsetX, srcOffsetY, uploadWidth, uploadHeight);
    }

    public void upload(BufferedImage sourceImage, int dstOffsetX, int dstOffsetY, int srcOffsetX, int srcOffsetY, int uploadWidth, int uploadHeight) {
        int srcImgWidth = sourceImage.getWidth();
        int srcImgHeight = sourceImage.getHeight();
        if(dstOffsetX + uploadWidth > this.width) {
            throw new IllegalArgumentException("Destination offsetX + width should not be larger than the destination image size! Have you subtracted width from offset?");
        }
        if(dstOffsetY + uploadHeight > this.height) {
            throw new IllegalArgumentException("Destination offsetY + height should not be larger than the destination image size! Have you subtracted height from offset?");
        }
        if (srcOffsetX + uploadWidth > srcImgWidth) {
            throw new IllegalArgumentException("Source offsetX + width should not be larger than the source image size!");
        }
        if (srcOffsetY + uploadHeight > srcImgHeight) {
            throw new IllegalArgumentException("Source offsetY + height should not be larger than the source image size!");
        }
        copyBuffer(sourceImage, this.dynamicTexture, dstOffsetX, dstOffsetY, srcOffsetX, srcOffsetY, uploadWidth, uploadHeight, this.width, this.height, srcImgWidth, srcImgHeight);
        RenderSystem.recordRenderCall(() -> {
            NativeImage nativeImage = dynamicTexture.getImage();
            if(nativeImage != null) {
                dynamicTexture.bindTexture();
                nativeImage.upload(0, dstOffsetX, dstOffsetY, dstOffsetX, dstOffsetY, uploadWidth, uploadHeight, false, false, false, false);
            }
        });
    }

    private static void copyBuffer(
            BufferedImage source,
            NativeImageBackedTexture destination,
            int dstX, int dstY,
            int srcX, int srcY,
            int width, int height,
            int dstImgWidth, int dstImgHeight,
            int srcImgWidth, int srcImgHeight
    ) {
        int[] sourceData = ((DataBufferInt)source.getRaster().getDataBuffer()).getData();
        NativeImage destImg = destination.getImage();
        long destImgPointer = LoaderImplClient.getNativeImagePointer(destination.getImage());
        IntBuffer buffer = MemoryUtil.memByteBuffer(destImgPointer, dstImgWidth * dstImgHeight * 4).asIntBuffer();
        int srcPixelStart = srcY * srcImgWidth + srcX;
        int dstPixelStart = dstY * dstImgWidth + dstX;
        for(int i = 0; i < height; i++) {
            buffer.position(dstPixelStart);
            buffer.put(sourceData, srcPixelStart, width);
            srcPixelStart += srcImgWidth;  // To next line
            dstPixelStart += dstImgWidth;  // To next line
        }
    }

    /**
     * Upload a rectangle from raw <b>BGRA bytes</b> (little-endian ARGB —
     * exactly how a native script stores its {@code uint32} pixels) into
     * {@link #bufferedImage} at (dstOffsetX, dstOffsetY), then push that
     * rectangle to the GL texture.
     *
     * <p>Why this exists: {@link #upload()} copies and uploads the WHOLE
     * image. A native script usually repaints only a small band (a blinking
     * progress segment, one changed glyph), so a vehicle LCD with 12 textures
     * would move ~150 MB per repaint frame instead of a few hundred KB.
     *
     * <p>The conversion is done row-at-a-time with {@link System#arraycopy}
     * plus a 4-byte lookup table rather than per pixel: a 4 Mpx rect costs a
     * few ms instead of tens of ms.
     *
     * @param source   {@code width*height*4} bytes, row-major, BGRA order
     * @param ppm      lookup table of size {@code 4 * (maxByteValue + 1)}
     */
    public void uploadRawABGR(byte[] source, int dstOffsetX, int dstOffsetY,
                              int width, int height, int[] ppm) {
        if (source == null || width <= 0 || height <= 0) return;
        if (dstOffsetX < 0 || dstOffsetY < 0
                || dstOffsetX + width > this.width
                || dstOffsetY + height > this.height) {
            throw new IllegalArgumentException("uploadRawABGR rect is outside the texture");
        }
        if (source.length < width * height * 4) {
            throw new IllegalArgumentException("uploadRawABGR source is too short");
        }
        if (!(bufferedImage.getRaster().getDataBuffer() instanceof DataBufferInt)) return;

        final int[] dst =
                ((DataBufferInt) bufferedImage.getRaster().getDataBuffer()).getData();
        final int stride = this.width;
        final int lutB = 0, lutG = 256, lutR = 512, lutA = 768;

        int src = 0;
        for (int row = 0; row < height; row++) {
            final int base = (dstOffsetY + row) * stride + dstOffsetX;
            for (int col = 0; col < width; col++) {
                dst[base + col] = ppm[lutA + (source[src + 3] & 0xFF)]
                                | ppm[lutR + (source[src + 2] & 0xFF)]
                                | ppm[lutG + (source[src + 1] & 0xFF)]
                                | ppm[lutB + (source[src + 0] & 0xFF)];
                src += 4;
            }
        }

        copyBuffer(bufferedImage, this.dynamicTexture,
                dstOffsetX, dstOffsetY, dstOffsetX, dstOffsetY, width, height,
                this.width, this.height, this.width, this.height);
        RenderSystem.recordRenderCall(() -> {
            NativeImage nativeImage = this.dynamicTexture.getImage();
            if (nativeImage != null) {
                this.dynamicTexture.bindTexture();
                nativeImage.upload(0, dstOffsetX, dstOffsetY, dstOffsetX, dstOffsetY,
                        width, height, false, false, false, false);
            }
        });
    }

    /** Internal native-frame path. Consume the arena before the next bridge call;
     * only our owned image memory is used by the queued GPU upload. */
    @ApiInternal
    public void uploadRawARGB(ByteBuffer source, int byteOffset, int sourceStride,
                              int sourceX, int sourceY, int dstX, int dstY, int w, int h) {
        if (dstY < 0 || h <= 0 || (long) dstY + h > height) {
            throw new IllegalArgumentException("Native upload is outside the texture");
        }
        final int[] pixels = ((DataBufferInt) bufferedImage.getRaster().getDataBuffer()).getData();
        com.lx862.jcm.nativeapi.NativePixelBuffer.copy(source, byteOffset, sourceStride,
                sourceX, sourceY, pixels, width, dstX, dstY, w, h);
        upload(dstX, dstY, w, h);
    }

    @Override
    public void close() {
        MinecraftClient.getInstance().execute(() -> {
            MinecraftClient.getInstance().getTextureManager().destroyTexture(identifier);
        });
        graphics.dispose();
    }
}
