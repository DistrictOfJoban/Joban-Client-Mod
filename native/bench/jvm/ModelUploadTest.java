package com.lx862.mtrscripting.core.util.model;

import com.lx862.jcm.mixin.mtrscripting.GlStateTrackerAccessor;
import com.lx862.jcm.mixin.mtrscripting.OptimizedModelAccessor;
import com.lx862.jcm.mixin.mtrscripting.OptimizedModelWrapperAccessor;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL33;
import org.mtr.mapping.holder.Identifier;
import org.mtr.mapping.render.object.VertexArray;
import org.mtr.mapping.render.tool.GlStateTracker;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;

/** Actual MTR buffer uploads and a framebuffer readback, with a hidden window. */
public final class ModelUploadTest {
    private static int checks;

    private static void check(boolean value, String reason) {
        if (!value) throw new AssertionError(reason);
        checks++;
    }

    private static RawModelJS quad() {
        final RawMeshBuilderJS b = new RawMeshBuilderJS(4, "interior", new Identifier("test:quad"));
        double[][] points = {{-.8,.6,0},{-.8,-.6,0},{.8,-.6,0},{.8,.6,0}};
        for (int i = 0; i < points.length; i++) {
            b.vertex(points[i][0], points[i][1], points[i][2]).normal(0, 1, 0)
                    .uv(i < 2 ? 0 : 1, i == 1 || i == 2 ? 1 : 0).endVertex();
        }
        check(b.getMesh().vertices.get(0).position.getY() == -.6f, "production builder flips the Y axis");
        check(b.getMesh().faces.size() == 2, "production builder triangulates the quad");
        return new RawModelJS(b.asRawModel());
    }

    private static List<VertexArray> parts(ModelJS model) {
        return ((OptimizedModelAccessor) ((OptimizedModelWrapperAccessor) model.getUnderlyingModel()).getOptimizedModel()).getUploadedParts();
    }

    private static int shader(int type, String source) {
        int id = GL33.glCreateShader(type);
        GL33.glShaderSource(id, source);
        GL33.glCompileShader(id);
        check(GL33.glGetShaderi(id, GL33.GL_COMPILE_STATUS) != 0, GL33.glGetShaderInfoLog(id));
        return id;
    }

    private static void restored(int vao, int vbo, int ebo, boolean protectedState) {
        check(GlStateTrackerAccessor.isStateProtected() == protectedState, "protection ownership survives upload");
        check(GL33.glGetInteger(GL33.GL_VERTEX_ARRAY_BINDING) == vao, "caller VAO restored");
        check(GL33.glGetInteger(GL33.GL_ARRAY_BUFFER_BINDING) == vbo, "caller VBO restored");
        check(GL33.glGetInteger(GL33.GL_ELEMENT_ARRAY_BUFFER_BINDING) == ebo, "caller EBO restored");
    }

    public static void main(String[] args) {
        check(GLFW.glfwInit(), "GLFW initialises");
        GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE);
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MAJOR, 3);
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MINOR, 3);
        GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_PROFILE, GLFW.GLFW_OPENGL_CORE_PROFILE);
        GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_FORWARD_COMPAT, GLFW.GLFW_TRUE);
        long window = GLFW.glfwCreateWindow(64, 64, "JCM model upload regression", 0, 0);
        check(window != 0, "hidden OpenGL context exists");
        try {
            GLFW.glfwMakeContextCurrent(window);
            GL.createCapabilities();
            System.out.println("GL renderer: " + GL33.glGetString(GL33.GL_RENDERER));
            System.out.println("MTR GPU classes: " + GlStateTracker.class.getProtectionDomain().getCodeSource().getLocation());
            int vao = GL33.glGenVertexArrays(), vbo = GL33.glGenBuffers(), ebo = GL33.glGenBuffers();
            GL33.glBindVertexArray(vao);
            GL33.glBindBuffer(GL33.GL_ARRAY_BUFFER, vbo);
            GL33.glBindBuffer(GL33.GL_ELEMENT_ARRAY_BUFFER, ebo);
            if (args.length > 0 && args[0].equals("old")) {
                try {
                    ModelJS.uploadRawModel(quad());
                    throw new AssertionError("old production source unexpectedly uploaded without protection");
                } catch (IllegalStateException e) {
                    check(e.getMessage().equals("GlStateTracker: Not protected"), "same exception as the game log");
                    System.out.println("OLD SOURCE REPRODUCED: " + e);
                }
                return;
            }
            ModelJS model = ModelJS.uploadRawModel(quad());
            restored(vao, vbo, ebo, false);
            check(parts(model).size() == 1 && parts(model).get(0).indexBuffer.getVertexCount() == 6,
                    "real MTR upload creates a six-index GPU quad");
            // The same function is also used inside reload/dynamic-upload scopes.
            GlStateTracker.capture();
            ModelJS nested = ModelJS.uploadRawModel(quad());
            check(GlStateTrackerAccessor.isStateProtected(), "nested upload retains outer protection");
            GlStateTracker.restore();
            restored(vao, vbo, ebo, false);
            try {
                ModelJS.uploadRawModel(new RawModelJS(null));
                throw new AssertionError("invalid model must fail");
            } catch (NullPointerException expected) {
                restored(vao, vbo, ebo, false);
            }
            GlStateTracker.capture();
            try {
                ModelJS.uploadRawModel(new RawModelJS(null));
                throw new AssertionError("nested invalid model must fail");
            } catch (NullPointerException expected) {
                check(GlStateTrackerAccessor.isStateProtected(), "failed nested upload retains outer protection");
            } finally {
                GlStateTracker.restore();
            }
            restored(vao, vbo, ebo, false);
            int vertex = shader(GL33.GL_VERTEX_SHADER, "#version 330 core\nlayout(location=0) in vec3 p; void main(){gl_Position=vec4(p,1);}");
            int fragment = shader(GL33.GL_FRAGMENT_SHADER, "#version 330 core\nout vec4 c; void main(){c=vec4(0,1,0,1);}");
            int program = GL33.glCreateProgram();
            GL33.glAttachShader(program, vertex);
            GL33.glAttachShader(program, fragment);
            GL33.glLinkProgram(program);
            check(GL33.glGetProgrami(program, GL33.GL_LINK_STATUS) != 0, GL33.glGetProgramInfoLog(program));
            GL33.glUseProgram(program);
            // Use an explicit framebuffer: an occluded Windows window's back
            // buffer may be discarded by the driver even with valid geometry.
            int colorTexture = GL33.glGenTextures(), framebuffer = GL33.glGenFramebuffers();
            GL33.glBindTexture(GL33.GL_TEXTURE_2D, colorTexture);
            GL33.glTexImage2D(GL33.GL_TEXTURE_2D, 0, GL33.GL_RGBA8, 64, 64, 0, GL33.GL_RGBA, GL33.GL_UNSIGNED_BYTE, (ByteBuffer) null);
            GL33.glBindFramebuffer(GL33.GL_FRAMEBUFFER, framebuffer);
            GL33.glFramebufferTexture2D(GL33.GL_FRAMEBUFFER, GL33.GL_COLOR_ATTACHMENT0, GL33.GL_TEXTURE_2D, colorTexture, 0);
            check(GL33.glCheckFramebufferStatus(GL33.GL_FRAMEBUFFER) == GL33.GL_FRAMEBUFFER_COMPLETE, "offscreen framebuffer complete");
            GL33.glViewport(0, 0, 64, 64);
            GL33.glDisable(GL33.GL_CULL_FACE);
            GL33.glDisable(GL33.GL_DEPTH_TEST);
            GL33.glClearColor(0, 0, 0, 1);
            GL33.glClear(GL33.GL_COLOR_BUFFER_BIT);
            GlStateTracker.capture();
            try {
                parts(model).get(0).bind();
                int loc = org.mtr.mapping.render.vertex.VertexAttributeType.POSITION.location;
                int uploadedVbo = GL33.glGetVertexAttribi(loc, GL33.GL_VERTEX_ATTRIB_ARRAY_BUFFER_BINDING);
                GL33.glBindBuffer(GL33.GL_ARRAY_BUFFER, uploadedVbo);
                ByteBuffer data = ByteBuffer.allocateDirect(GL33.glGetBufferParameteri(GL33.GL_ARRAY_BUFFER, GL33.GL_BUFFER_SIZE)).order(ByteOrder.nativeOrder());
                GL33.glGetBufferSubData(GL33.GL_ARRAY_BUFFER, 0, data);
                check(GL33.glGetVertexAttribi(loc, GL33.GL_VERTEX_ATTRIB_ARRAY_ENABLED) == 1
                        && GL33.glGetVertexAttribi(loc, GL33.GL_VERTEX_ATTRIB_ARRAY_DIVISOR) == 0, "GPU position attribute enabled per vertex");
                float[][] expectedPositions={{-.8f,-.6f,0},{-.8f,.6f,0},{.8f,.6f,0},{.8f,-.6f,0}};
                for(int k=0;k<4;k++) {
                    int p=k*parts(model).get(0).mapping.strideVertex;
                    check(data.getFloat(p) == expectedPositions[k][0] && data.getFloat(p+4) == expectedPositions[k][1]
                            && data.getFloat(p+8) == expectedPositions[k][2], "GPU retains quad corner " + k);
                }
                ByteBuffer indices = ByteBuffer.allocateDirect(24).order(ByteOrder.nativeOrder());
                GL33.glGetBufferSubData(GL33.GL_ELEMENT_ARRAY_BUFFER, 0, indices);
                int[] expectedIndices={0,1,2,0,2,3};
                for(int k=0;k<6;k++) check(indices.getInt(k*4) == expectedIndices[k], "GPU triangle index " + k);
                parts(model).get(0).draw();
            } finally {
                GlStateTracker.restore();
            }
            ByteBuffer pixel = ByteBuffer.allocateDirect(4);
            GL33.glReadPixels(32, 32, 1, 1, GL33.GL_RGBA, GL33.GL_UNSIGNED_BYTE, pixel);
            check((pixel.get(1) & 255) == 255 && pixel.get(0) == 0 && pixel.get(2) == 0,
                    "uploaded production geometry draws green into the framebuffer; pixel="
                    + (pixel.get(0) & 255) + "," + (pixel.get(1) & 255) + "," + (pixel.get(2) & 255)
                    + "," + (pixel.get(3) & 255) + "; GL error=" + GL33.glGetError());
            model.close();
            nested.close();
            GL33.glDeleteProgram(program);
            GL33.glDeleteShader(vertex);
            GL33.glDeleteShader(fragment);
            GL33.glDeleteBuffers(vbo);
            GL33.glDeleteBuffers(ebo);
            GL33.glDeleteVertexArrays(vao);
            GL33.glBindFramebuffer(GL33.GL_FRAMEBUFFER, 0);
            GL33.glDeleteFramebuffers(framebuffer);
            GL33.glDeleteTextures(colorTexture);
            check(GL33.glGetError() == GL33.GL_NO_ERROR, "no GL errors during upload, draw and restore");
            System.out.println("REAL MTR MODEL UPLOAD OK (" + checks + " checks)");
        } finally {
            GLFW.glfwDestroyWindow(window);
            GLFW.glfwTerminate();
        }
    }
}
