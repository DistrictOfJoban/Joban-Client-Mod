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
            GL33.glGenerateMipmap(GL33.GL_TEXTURE_2D);
            GlStateManager._bindTexture(prevTextureBinding);

            // Register the texture to MC!
            MinecraftClient.getInstance().getTextureManager().registerTexture(identifier, new AbstractTexture(this.dynamicTexture.data));
        });
        bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        graphics = bufferedImage.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        graphics.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
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
        copyBuffer(this.bufferedImage, this.dynamicTexture, 0, 0, this.width, this.height, this.width, this.height);
        RenderSystem.recordRenderCall(dynamicTexture::upload);
    }

    public void upload(int x, int y, int width, int height) {
        upload(x, y, x, y, width, height);
    }

    public void upload(int dstOffsetX, int dstOffsetY, int srcOffsetX, int srcOffsetY, int uploadWidth, int uploadHeight) {
        upload(this.bufferedImage, dstOffsetX, dstOffsetY, srcOffsetX, srcOffsetY, uploadWidth, uploadHeight);
    }

    public void upload(BufferedImage sourceImage, int dstOffsetX, int dstOffsetY, int srcOffsetX, int srcOffsetY, int uploadWidth, int uploadHeight) {
        if(srcOffsetX + uploadWidth > this.width) {
            throw new IllegalArgumentException("offsetX + width should not be larger than the total image size! Have you subtracted width from offset?");
        }
        if(srcOffsetY + uploadHeight > this.height) {
            throw new IllegalArgumentException("offsetY + height should not be larger than the total image size! Have you subtracted height from offset?");
        }
        copyBuffer(sourceImage, this.dynamicTexture, dstOffsetX, dstOffsetY, uploadWidth, uploadHeight, this.width, this.height);
        RenderSystem.recordRenderCall(() -> {
            NativeImage nativeImage = dynamicTexture.getImage();
            if(nativeImage != null) {
                dynamicTexture.bindTexture();
                nativeImage.upload(0, dstOffsetX, dstOffsetY, srcOffsetX, srcOffsetY, uploadWidth, uploadHeight, false, false, false, false);
            }
        });
    }

    private static void copyBuffer(BufferedImage source, NativeImageBackedTexture destination, int x, int y, int width, int height, int imgWidth, int imgHeight) {
        int[] sourceData = ((DataBufferInt)source.getRaster().getDataBuffer()).getData();
        NativeImage destImg = destination.getImage();
        long destImgPointer = LoaderImplClient.getNativeImagePointer(destination.getImage());
        IntBuffer buffer = MemoryUtil.memByteBuffer(destImgPointer, imgWidth * imgHeight * 4).asIntBuffer();
        for(int i = y; i < y+height; i++) {
            int startSrc = (i * imgWidth) + x;
            buffer.position((i * imgWidth) + x);
            buffer.put(sourceData, startSrc, width);
        }
    }

    @Override
    public void close() {
        MinecraftClient.getInstance().execute(() -> {
            MinecraftClient.getInstance().getTextureManager().destroyTexture(identifier);
        });
        graphics.dispose();
    }
}
