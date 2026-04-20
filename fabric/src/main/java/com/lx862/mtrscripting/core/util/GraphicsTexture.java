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
        dynamicTexture = new NativeImageBackedTexture(new NativeImage(width, height, false));

        MinecraftClient.getInstance().execute(() -> {
            int prevTextureBinding = GL33.glGetInteger(GL33.GL_TEXTURE_BINDING_2D);
            dynamicTexture.bindTexture();
            GL33.glTexParameteriv(GL33.GL_TEXTURE_2D, GL33.GL_TEXTURE_SWIZZLE_RGBA,
                    new int[] { GL33.GL_BLUE, GL33.GL_GREEN, GL33.GL_RED, GL33.GL_ALPHA });
            GlStateManager._bindTexture(prevTextureBinding);
        });
        identifier = MTRScriptingMod.id(String.format("dynamic/graphics/%s", UUID.randomUUID()));
        MinecraftClient.getInstance().execute(() -> {
            MinecraftClient.getInstance().getTextureManager().registerTexture(identifier, new AbstractTexture(dynamicTexture.data));
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

    public void upload() {
        int[] imgData = ((DataBufferInt)bufferedImage.getRaster().getDataBuffer()).getData();
        long nativeImagePointer = LoaderImplClient.getNativeImagePointer(dynamicTexture.getImage());
        IntBuffer target = MemoryUtil.memByteBuffer(nativeImagePointer, width * height * 4).asIntBuffer();
        target.put(imgData);

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
