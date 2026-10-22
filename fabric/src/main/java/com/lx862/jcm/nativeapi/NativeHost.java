package com.lx862.jcm.nativeapi;

import com.lx862.jcm.mod.util.JCMLogger;
import com.lx862.mtrscripting.core.util.GraphicsTexture;
import com.lx862.mtrscripting.core.util.ScriptResourceUtil;
import com.lx862.mtrscripting.core.util.model.ModelJS;
import com.lx862.mtrscripting.core.util.model.ModelManagerJS;
import com.lx862.mtrscripting.core.util.model.RawMeshBuilderJS;
import com.lx862.mtrscripting.core.util.model.RawModelJS;
import org.mtr.mapping.holder.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * NativeHost — the JVM-side owner of every resource a C++ script asks for.
 *
 * <p>The bridge ({@code native/jni/jni_bridge.cpp}) never touches OpenGL or MTR
 * classes. While a module runs {@code mtrCreate} it only RECORDS the textures
 * and quads the script described, then calls {@link #createResources} once per
 * script instance on the render thread. This class turns that batch into real
 * objects — {@link GraphicsTexture} for pixels, {@link ModelJS} for geometry —
 * and returns the handle arrays the frame records must reference.
 *
 * <h2>Handle = SLOT, not id</h2>
 * The bridge rewrites every handle in a frame record through the array returned
 * by {@link #createResources}: <em>slot</em> i becomes {@code textureHandles[i]}
 * and quad slot q becomes {@code modelHandles[q]}. That is why the two lookup
 * methods here are plain array indexing rather than hash lookups on the hot
 * path, and why a stale instance releases by SLOT too.
 *
 * <p>Both entry points are reached through the bridge's invocation planes
 * (the Panama FFM upcall vtable, or the poller plane's SVC adapters —
 * both delegate to the SAME static adapters in PanamaFfmUpcalls):
 * <pre>
 *   int[] createResources(int[] widths, int[] heights,
 *                         float[] quadVertices, float[] quadUv,
 *                         int[] quadTextureSlot, int[] quadStages)
 *   void  uploadPixels(int textureHandle, int x, int y, int w, int h, byte[] rgba)
 * </pre>
 */
public final class NativeHost {

    /** Render stages, matching the JS slot "interior"/"exterior" wording. */
    public static final int STAGE_EXTERIOR = 0;
    public static final int STAGE_INTERIOR = 1;

    private static final NativeHost INSTANCE = new NativeHost();

    /** One script instance's resources, addressed by slot. */
    private static final class Instance {
        GraphicsTexture[] textures = new GraphicsTexture[0];
        ModelJS[] models = new ModelJS[0];
        /** Global handle per quad slot, so close() can unpublish them. */
        int[] modelHandles = new int[0];

        void close() {
            for (ModelJS model : models) {
                if (model == null) continue;
                try {
                    model.close();
                } catch (Throwable ignored) {
                }
            }
            for (int handle : modelHandles) {
                if (handle > 0) INSTANCE.modelsByHandle.remove(handle);
            }
            for (GraphicsTexture tex : textures) {
                if (tex == null) continue;
                try {
                    tex.close();
                } catch (Throwable ignored) {
                }
            }
            models = new ModelJS[0];
            textures = new GraphicsTexture[0];
            modelHandles = new int[0];
        }
    }

    /** instanceKey -> the resources that instance created. */
    private final java.util.Map<String, Instance> instances = new java.util.concurrent.ConcurrentHashMap<>();

    /**
     * The instance currently being created. {@code createResources} is called
     * from {@code mtrRender} while {@code mtrCreate} runs for exactly one
     * instance, and the bridge serialises render calls per module, so a single
     * pending slot is enough — the driver tells us which key to file it under.
     */
    private String pendingInstanceKey;

    /** Instance whose frame is currently being applied (see {@link #setActiveInstance}). */
    private volatile String activeInstanceKey;

    /**
     * Global quad-handle -> model, so the draw replay is an O(1) map get.
     *
     * <p>Handles are per-instance SLOTS (several vehicles each have a model at
     * slot 0), so this maps the GLOBAL handle returned by
     * {@link #createResources} instead. The per-instance arrays stay as the
     * ownership record that decides what {@link #releaseInstance} frees.
     * Without this, replaying N vehicles x M quads per frame walked every
     * instance's model array for every single draw call — O(V^2) per frame.
     */
    private final java.util.Map<Integer, ModelJS> modelsByHandle =
            new java.util.concurrent.ConcurrentHashMap<>();

    /** Monotonic source of global handles. */
    private final java.util.concurrent.atomic.AtomicInteger nextHandle =
            new java.util.concurrent.atomic.AtomicInteger(1);
    private java.awt.Font nativeFont;
    private final java.util.Map<String, java.awt.image.BufferedImage> glyphMasks = new java.util.HashMap<>();
    private final NativeFontRenderer fontRenderer = new NativeFontRenderer();

    /** ABI 7 font callbacks. The invocation planes pass resource IDs and
     *     text as UTF-8 raw addresses. */
    public double[] measureFontText(byte[] font, byte[] text, double size, double sx, double sy) {
        return fontRenderer.measure(font, text, size, sx, sy);
    }

    public int[] drawFontText(byte[] font, byte[] text, double size, double x, double baseline,
                              double sx, double sy, double compression, int color,
                              int clipX0, int clipY0, int clipX1, int clipY1,
                              java.nio.ByteBuffer pixels, int width, int height) {
        return fontRenderer.draw(font, text, size, x, baseline, sx, sy, compression, color,
                clipX0, clipY0, clipX1, clipY1, pixels, width, height);
    }

    /** JNI font callback. Pixels are packed ARGB ints, stored as BGRA bytes. */
    public int rasterizeText(byte[] utf8, int x, int y, int maxWidth,
                             int red, int green, int blue, java.nio.ByteBuffer pixels,
                             int width, int height) {
        if (utf8 == null || pixels == null || maxWidth <= 1) return -1;
        final String text = new String(utf8, java.nio.charset.StandardCharsets.UTF_8);
        if (nativeFont == null) nativeFont = ScriptResourceUtil.getSystemFont("Noto Sans");
        if (nativeFont == null || nativeFont.canDisplayUpTo(text) >= 0) return -1;
        final int size = maxWidth - 1;
        final String key = size + "/" + text;
        java.awt.image.BufferedImage mask = glyphMasks.get(key);
        if (mask == null) {
            mask = new java.awt.image.BufferedImage(size, size, java.awt.image.BufferedImage.TYPE_INT_ARGB);
            final java.awt.Graphics2D graphics = mask.createGraphics();
            graphics.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
            final java.awt.Shape outline = nativeFont.deriveFont((float) size)
                    .createGlyphVector(graphics.getFontRenderContext(), text).getOutline();
            final java.awt.geom.Rectangle2D bounds = outline.getBounds2D();
            if (bounds.getWidth() > 0 && bounds.getHeight() > 0) {
                final double scale = Math.min((size - 2.0) / bounds.getWidth(), (size - 2.0) / bounds.getHeight());
                graphics.translate(1, 1);
                graphics.scale(scale, scale);
                graphics.translate(-bounds.getX(), -bounds.getY());
                graphics.setColor(java.awt.Color.WHITE);
                graphics.fill(outline);
            }
            graphics.dispose();
            if (glyphMasks.size() >= 4096) glyphMasks.clear();
            glyphMasks.put(key, mask);
        }
        pixels.order(java.nio.ByteOrder.nativeOrder());
        /* Direct int[] view of the mask raster: getRGB(gx,gy) per pixel went
           through the color-model slow path (allocating a per-call int[1] and
           a bounds check) for every pixel of every glyph blit, every frame.
           TYPE_INT_ARGB rasters are backed by a DataBufferInt, so the array
           read is exact. */
        final int[] maskPixels = ((java.awt.image.DataBufferInt) mask.getRaster().getDataBuffer()).getData();
        for (int gy = 0; gy < size; gy++) {
            if (y + gy < 0 || y + gy >= height) continue;
            for (int gx = 0; gx < size; gx++) {
                if (x + gx < 0 || x + gx >= width) continue;
                final int alpha = maskPixels[gy * size + gx] >>> 24;
                if (alpha == 0) continue;
                final int offset = ((y + gy) * width + x + gx) * 4;
                final int dst = pixels.getInt(offset);
                final int remaining = (dst >>> 24) * (255 - alpha) / 255;
                final int outAlpha = alpha + remaining;
                final int r = (red * alpha + ((dst >>> 16) & 255) * remaining) / outAlpha;
                final int g = (green * alpha + ((dst >>> 8) & 255) * remaining) / outAlpha;
                final int b = (blue * alpha + (dst & 255) * remaining) / outAlpha;
                pixels.putInt(offset, (outAlpha << 24) | (r << 16) | (g << 8) | b);
            }
        }
        return size;
    }

    private NativeHost() {
    }

    public static NativeHost get() {
        return INSTANCE;
    }

    /** Driver hook: which instance the next {@link #createResources} belongs to. */
    public void setPendingInstance(String instanceKey) {
        this.pendingInstanceKey = instanceKey;
    }

    /**
     * Driver hook: which instance the frame being applied belongs to.
     * Handle uploads only carry a per-instance SLOT, so the driver names the
     * instance before translating a frame.
     */
    public void setActiveInstance(String instanceKey) {
        this.activeInstanceKey = instanceKey;
    }

    /* ------------------------------------------------------------------ */
    /* Bridge entry point 1: build everything one instance asked for       */
    /* ------------------------------------------------------------------ */

    /**
     * @param widths          texture widths  (one per create_texture call)
     * @param heights         texture heights (one per create_texture call)
     * @param quadVertices    12 floats per quad (4 vertices, model space)
     * @param quadUv          8 floats per quad (u0,v0 .. u3,v3)
     * @param quadTextureSlot texture slot each quad samples
     * @param quadStages      render stage per quad (STAGE_*)
     * @return {@code [texture handles..., model handles...]}; a model handle of
     *         -1 means "do not draw this quad".
     */
    public int[] createResources(int[] widths, int[] heights,
                                 float[] quadVertices, float[] quadUv,
                                 int[] quadTextureSlot, int[] quadStages) {
        final int texCount = widths == null ? 0 : widths.length;
        final int quadCount = quadTextureSlot == null ? 0 : quadTextureSlot.length;
        final int[] out = new int[texCount + quadCount];
        java.util.Arrays.fill(out, -1);
        final String key = pendingInstanceKey;
        final Instance previous = key == null ? null : instances.get(key);

        final Instance instance = new Instance();
        instance.textures = new GraphicsTexture[texCount];
        instance.models = new ModelJS[quadCount];
        instance.modelHandles = new int[quadCount];

        /* 1) textures */
        for (int i = 0; i < texCount; i++) {
            final int w = widths[i];
            final int h = heights[i];
            if (w <= 0 || h <= 0) continue; // released native slot
            try {
                if (previous != null && i < previous.textures.length
                        && previous.textures[i] != null
                        && previous.textures[i].bufferedImage.getWidth() == w
                        && previous.textures[i].bufferedImage.getHeight() == h) {
                    // Slots are append-only in the bridge. Preserve live pixels
                    // when a different texture or quad is created/released.
                    instance.textures[i] = previous.textures[i];
                    previous.textures[i] = null;
                } else {
                    instance.textures[i] = new GraphicsTexture(w, h);
                }
            } catch (Throwable t) {
                JCMLogger.error("NativeHost: failed to create a {}x{} GraphicsTexture: {}", w, h, t.toString());
            }
            if (instance.textures[i] != null) out[i] = i;
        }

        /* 2) quad models; several quads may share one texture */
        for (int q = 0; q < quadCount; q++) {
            out[texCount + q] = -1;
            final int slot = quadTextureSlot[q];
            if (slot < 0 || slot >= texCount) continue;
            final GraphicsTexture tex = instance.textures[slot];
            if (tex == null) continue;
            try {
                if (previous != null && q < previous.models.length
                        && previous.models[q] != null) {
                    instance.models[q] = previous.models[q];
                    instance.modelHandles[q] = previous.modelHandles[q];
                    out[texCount + q] = previous.modelHandles[q];
                    previous.models[q] = null;
                    previous.modelHandles[q] = 0;
                    continue;
                }
                final ModelJS model = buildQuad(quadVertices, q, quadUv, q,
                        quadStages == null || q >= quadStages.length ? STAGE_INTERIOR : quadStages[q],
                        tex.identifier);
                if (model != null) {
                    instance.models[q] = model;
                    /* publish under a GLOBAL handle: the bridge rewrites every
                       model record through this value, so the replay can be a
                       plain map get even though the script only knows slots */
                    final int handle = nextHandle.getAndIncrement();
                    modelsByHandle.put(handle, model);
                    instance.modelHandles[q] = handle;
                    out[texCount + q] = handle;
                }
            } catch (Throwable t) {
                JCMLogger.error("NativeHost: failed to build a quad model: {}", t.toString());
            }
        }

        /* File it under the instance the driver announced, and make sure a
           stale instance from an earlier build (car count changed, script
           reloaded) is torn down instead of leaking its textures. */
        if (key != null) {
            instances.put(key, instance);
            if (previous != null) previous.close();
        }
        final long readyTextures = java.util.Arrays.stream(instance.textures).filter(java.util.Objects::nonNull).count();
        final long readyModels = java.util.Arrays.stream(instance.models).filter(java.util.Objects::nonNull).count();
        final long requestedTextures = widths == null ? 0 : java.util.Arrays.stream(widths).filter(w -> w > 0).count();
        final long requestedModels = quadTextureSlot == null ? 0 : java.util.Arrays.stream(quadTextureSlot).filter(slot -> slot >= 0).count();
        JCMLogger.info("NativeHost: resources for {}: textures={}/{}, models={}/{}",
                key, readyTextures, requestedTextures, readyModels, requestedModels);
        return out;
    }

    /**
     * Port of JCM's own DisplayHelper mesh construction: a 4-vertex quad in the
     * SAME axis convention {@link RawMeshBuilderJS#vertex} uses ({@code x, -y,
     * -z}), one shared material, and the {@link GraphicsTexture}'s dynamic
     * identifier as the texture — so live pixel updates show up without any
     * model re-upload.
     */
    private ModelJS buildQuad(float[] verts, int quadIndex, float[] uv, int uvIndex,
                              int stage, Identifier texture) {
        if (verts == null || verts.length < (quadIndex + 1) * 12) return null;
        if (uv == null || uv.length < (uvIndex + 1) * 8) return null;

        final String renderType = stage == STAGE_EXTERIOR ? "exterior" : "interior";
        final RawMeshBuilderJS builder = new RawMeshBuilderJS(4, renderType, texture);
        final int vi = quadIndex * 12;
        final int ui = uvIndex * 8;
        for (int v = 0; v < 4; v++) {
            builder.vertex(verts[vi + v * 3], verts[vi + v * 3 + 1], verts[vi + v * 3 + 2]);
            /* Display panels are thin two-sided quads; a fixed normal keeps the
               shader happy and matches what the JS DisplayHelper produced. */
            builder.normal(0, 1, 0);
            builder.uv(uv[ui + v * 2], uv[ui + v * 2 + 1]);
            builder.endVertex();
        }
        final RawModelJS raw = builder.asRawModel();
        final ModelJS model = ModelManagerJS.upload(raw);
        if (model == null) return null;
        /* The JS DisplayHelper never applies a matrix either: the slot quad is
           already authored in car space, so drawCarModel(..., null) is right. */
        return model;
    }

    /* ------------------------------------------------------------------ */
    /* Bridge entry point 2: blit one dirty rect of BGRA8 pixels            */
    /* ------------------------------------------------------------------ */

    /**
     * The C++ side hands raw row-major BGRA8 — its little-endian ARGB
     * {@code uint32} storage, byte for byte. {@link GraphicsTexture} backs
     * itself with a {@code TYPE_INT_ARGB} BufferedImage whose DataBufferInt is
     * the very memory {@code upload()} reads, so writing straight into that
     * int[] is both correct and far cheaper than per-pixel calls.
     */
    public void uploadPixels(int textureHandle, int x, int y, int width, int height, byte[] rgba) {
        if (rgba == null || width <= 0 || height <= 0
                || (long) width * height * 4 > rgba.length) return;
        final GraphicsTexture tex = texture(textureHandle, activeInstanceKey);
        if (tex == null) return;
        final int imgW = tex.bufferedImage.getWidth();
        final int imgH = tex.bufferedImage.getHeight();
        final int sx = Math.max(0, x), sy = Math.max(0, y);
        final int w = (int) Math.min(imgW, (long) x + width) - sx;
        final int h = (int) Math.min(imgH, (long) y + height) - sy;
        if (w <= 0 || h <= 0) return;
        byte[] pixels = rgba;
        if (sx != x || sy != y || w != width || h != height) {
            pixels = new byte[w * h * 4];
            for (int row = 0; row < h; row++) {
                final int src = (int) (((long) sy - y + row) * width + (long) sx - x) * 4;
                System.arraycopy(rgba, src, pixels, row * w * 4, w * 4);
            }
        }

        /* Upload the changed rectangle without copying the entire image. */
        tex.uploadRawABGR(pixels, sx, sy, w, h, PIXEL_PPM);
    }

    /** Zero staging-array path for both vehicle and block-content frames.
     * The legacy byte[] overload is kept for JNI/API compatibility. */
    public void uploadPixels(int textureHandle, int x, int y, int width, int height,
                             java.nio.ByteBuffer arena, long offset, long length) {
        if (arena == null || offset < 0 || length < 0 || offset > arena.limit()
                || length > arena.limit() - offset || width <= 0 || height <= 0
                || (long) width * height > length / 4) {
            throw new IllegalArgumentException("Invalid native pixel arena");
        }
        final GraphicsTexture tex = texture(textureHandle, activeInstanceKey);
        if (tex == null) return;
        final int sx = Math.max(0, x), sy = Math.max(0, y);
        final int w = (int) Math.min(tex.width, (long) x + width) - sx;
        final int h = (int) Math.min(tex.height, (long) y + height) - sy;
        if (w <= 0 || h <= 0) return;
        tex.uploadRawARGB(arena, (int) offset, width, sx - x, sy - y, sx, sy, w, h);
    }

    /**
     * Lookup table backing {@link #uploadPixels}: maps one byte value to its
     * position inside the packed ARGB int, so the per-pixel work is four
     * table reads and three ORs instead of shifts and masks.
     * Layout: [0..255] blue, [256..511] green, [512..767] red, [768..1023] alpha.
     */
    private static final int[] PIXEL_PPM = new int[1024];

    static {
        for (int v = 0; v < 256; v++) {
            PIXEL_PPM[v] = v;             /* B -> bits 0..7   */
            PIXEL_PPM[256 + v] = v << 8;  /* G -> bits 8..15  */
            PIXEL_PPM[512 + v] = v << 16; /* R -> bits 16..23 */
            PIXEL_PPM[768 + v] = v << 24; /* A -> bits 24..31 */
        }
    }

    /* ------------------------------------------------------------------ */
    /* Lookups used by the draw-call replay                                */
    /* ------------------------------------------------------------------ */

    /**
     * The model behind a GLOBAL handle (see {@link #modelsByHandle}).
     *
     * <p>Called once per model draw record, i.e. up to a few dozen times per
     * frame per vehicle — hence the map rather than a scan over instances
     * (which would make replay O(vehicles x quads) per frame).
     */
    private final Map<String, java.util.Set<Integer>> externalModels = new java.util.HashMap<>();
    public int acquireModel(String path) {
        com.lx862.mtrscripting.core.util.model.RawModelJS raw = ModelManagerJS.loadModel(new Identifier(path));
        if(raw==null) return -1;
        ModelJS model=ModelJS.uploadRawModel(raw);
        int handle=nextHandle.getAndIncrement(); modelsByHandle.put(handle,model);
        externalModels.computeIfAbsent(pendingInstanceKey,k->new java.util.HashSet<>()).add(handle);
        return handle;
    }
    public void releaseModel(int handle) {
        ModelJS model=modelsByHandle.remove(handle); if(model!=null) model.close();
        externalModels.values().forEach(set->set.remove(handle));
    }
    public ModelJS model(int handle) {
        if (handle < 0) return null;
        return modelsByHandle.get(handle);
    }

    /** Texture behind a per-instance SLOT within one instance ({@code null} key = scan). */
    public GraphicsTexture texture(int handle, String instanceKey) {
        if (handle < 0) return null;
        if (instanceKey != null) {
            final Instance instance = instances.get(instanceKey);
            if (instance == null || handle >= instance.textures.length) return null;
            return instance.textures[handle];
        }
        for (Instance instance : instances.values()) {
            if (handle < instance.textures.length && instance.textures[handle] != null) {
                return instance.textures[handle];
            }
        }
        return null;
    }

    /* ------------------------------------------------------------------ */
    /* Lifetime                                                            */
    /* ------------------------------------------------------------------ */

    /** Drop one instance's resources (script instance dropped / state rebuilt). */
    public void releaseInstance(String instanceKey) {
        java.util.Set<Integer> extra=externalModels.remove(instanceKey);
        if(extra!=null) for(int handle:extra) releaseModel(handle);
        final Instance instance = instances.remove(instanceKey);
        if (instance == null) return;
        for (int handle : instance.modelHandles) modelsByHandle.remove(handle);
        instance.close();
    }

    /** Drop every resource (script reload / world unload). */
    public void reset() {
        final List<Instance> all = new ArrayList<>(instances.values());
        instances.clear();
        new java.util.ArrayList<>(externalModels.keySet()).forEach(this::releaseInstance);
        externalModels.clear();
        modelsByHandle.clear();
        pendingInstanceKey = null;
        activeInstanceKey = null;
        glyphMasks.clear();
        nativeFont = null;
        fontRenderer.reset();
        for (Instance instance : all) instance.close();
    }
}
