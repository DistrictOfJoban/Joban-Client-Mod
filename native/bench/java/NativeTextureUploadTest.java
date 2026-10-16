package com.lx862.jcm.nativeapi;

import com.lx862.mtrscripting.core.util.GraphicsTexture;
import com.mojang.blaze3d.systems.RenderSystem;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL33;
import org.mtr.mapping.holder.MinecraftClient;
import java.awt.image.DataBufferInt;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.Random;

/** Production image copy and queued GL upload; source arenas are reused early. */
public final class NativeTextureUploadTest {
    private static void check(boolean value, String reason) { if (!value) throw new AssertionError(reason); }
    public static void main(String[] args) {
        check(GLFW.glfwInit(), "GLFW initialized");
        GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE);
        long window = GLFW.glfwCreateWindow(32, 32, "native texture regression", 0, 0);
        check(window != 0, "hidden GL context");
        try {
            GLFW.glfwMakeContextCurrent(window); GL.createCapabilities();
            System.out.println("GL renderer: " + GL33.glGetString(GL33.GL_RENDERER));
            GraphicsTexture texture = new GraphicsTexture(71, 37);
            int[] expected = new int[71 * 37];
            Random random = new Random(5917);
            for (int test = 0; test < 256; test++) {
                int sw=1+random.nextInt(71), sh=1+random.nextInt(37), offset=random.nextInt(11);
                int sx=random.nextInt(sw),sy=random.nextInt(sh),w=1+random.nextInt(sw-sx),h=1+random.nextInt(sh-sy);
                int dx=random.nextInt(72-w),dy=random.nextInt(38-h);
                byte[] bytes=new byte[offset+sw*sh*4];random.nextBytes(bytes);
                ByteBuffer arena=ByteBuffer.allocateDirect(bytes.length);arena.put(bytes).clear();
                for(int y=0;y<h;y++)for(int x=0;x<w;x++){
                    int at=offset+((sy+y)*sw+sx+x)*4;
                    expected[(dy+y)*71+dx+x]=bytes[at]&255|(bytes[at+1]&255)<<8|(bytes[at+2]&255)<<16|(bytes[at+3]&255)<<24;
                }
                texture.uploadRawARGB(arena,offset,sw,sx,sy,dx,dy,w,h);
                check(Arrays.equals(expected,((DataBufferInt)texture.bufferedImage.getRaster().getDataBuffer()).getData()),"owned ARGB data " + test);
                // JNI arena is module owned. Reuse it BEFORE the GL queue drains.
                for(int i=0;i<arena.capacity();i++)arena.put(i,(byte)0);
                RenderSystem.drain();
                var nativeTexture=MinecraftClient.getInstance().getTextureManager().get(texture.identifier);
                ByteBuffer gpu=ByteBuffer.allocateDirect(71*37*4).order(ByteOrder.LITTLE_ENDIAN);
                nativeTexture.bindTexture();
                GL33.glGetTexImage(GL33.GL_TEXTURE_2D,0,GL33.GL_RGBA,GL33.GL_UNSIGNED_BYTE,gpu);
                for(int n=0;n<expected.length;n++)check(gpu.getInt(n*4)==expected[n],"GPU pixel " +test+"/"+n);
                check(GL33.glGetTexParameteri(GL33.GL_TEXTURE_2D,GL33.GL_TEXTURE_SWIZZLE_R)==GL33.GL_BLUE,"existing BGR swizzle preserved");
                check(GL33.glGetError()==GL33.GL_NO_ERROR,"GL error");
            }
            texture.close();
            System.out.println("PASS: production GraphicsTexture, 256 partial uploads, exact CPU/GPU ARGB/alpha and deferred arena lifetime");
        } finally { GLFW.glfwDestroyWindow(window); GLFW.glfwTerminate(); }
    }
}
