package com.lx862.mtrscripting.util;

import com.lx862.jcm.mapping.LoaderImplClient;
import com.mojang.blaze3d.systems.RenderSystem;
import org.lwjgl.system.MemoryUtil;
import org.mtr.mapping.holder.*;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferInt;
import java.io.Closeable;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.UUID;

import static com.lx862.jcm.mod.render.text.TextureTextRenderer.toAbgr;

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
        dynamicTexture = new NativeImageBackedTexture(new NativeImage(width, height, false));
        identifier = new Identifier("mtrscripting", String.format("dynamic/graphics/%s", UUID.randomUUID()));
        MinecraftClient.getInstance().execute(() -> {
            MinecraftClient.getInstance().getTextureManager().registerTexture(identifier, new AbstractTexture(dynamicTexture.data));
        });
        bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        graphics = bufferedImage.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        graphics.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
    }

    public static BufferedImage createArgbBufferedImage(BufferedImage src) {
        BufferedImage newImage = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = newImage.createGraphics();
        graphics.drawImage(src, 0, 0, null);
        graphics.dispose();
        return newImage;
    }

    public void upload() {
        int[] imgData = ((DataBufferInt)bufferedImage.getData().getDataBuffer()).getData();
        IntBuffer imgBuffer = IntBuffer.wrap(imgData);
        long nativeImagePointer = LoaderImplClient.getNativeImagePointer(dynamicTexture.getImage());
        ByteBuffer target = MemoryUtil.memByteBuffer(nativeImagePointer, width * height * 4);
        for (int i = 0; i < width * height; i++) {
            // ARGB to RGBA
            int pixel = imgBuffer.get();
            target.put((byte)((pixel >> 16) & 0xFF));
            target.put((byte)((pixel >> 8) & 0xFF));
            target.put((byte)(pixel & 0xFF));
            target.put((byte)((pixel >> 24) & 0xFF));
        }

        RenderSystem.recordRenderCall(dynamicTexture::upload);
    }

    @Override
    public void close() {
        MinecraftClient.getInstance().execute(() -> {
            MinecraftClient.getInstance().getTextureManager().destroyTexture(identifier);
        });
        graphics.dispose();
    }
}
