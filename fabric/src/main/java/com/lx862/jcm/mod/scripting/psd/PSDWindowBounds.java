package com.lx862.jcm.mod.scripting.psd;

#if MC_VERSION >= "12001"
#if LOADER == "fabric"
import com.lx862.jcm.mixin.mtrscripting.SpriteContentsAccessor;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.texture.SpriteContents;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
#endif
#endif

/**
 * Yanyang/PSD: the transparent <b>window</b> of a PSD/APG glass block,
 * derived from the block's <b>actual baked model and texture alpha</b> —
 * whatever the resource pack drew.
 *
 * <h2>Why</h2>
 * The scripted glass panel sandwiches its content INSIDE the pane (see
 * {@link PSDScriptRenderer}): the content shows through the pane's
 * originally-transparent window while the pane's opaque parts (frame,
 * bottom panel, light strip) stay in front and clip it. Resource packs
 * however replace both the glass models and the glass textures — frame
 * thicknesses and window extents are pack-specific, and tinted-glass
 * packs even make the "frame" semi-transparent (alpha in between), which
 * lets sandwiched content bleed through it. Clipping the script canvas
 * to hardcoded vanilla insets would be wrong for every pack but vanilla.
 *
 * <h2>How</h2>
 * For the bound block's state this walks the BAKED model's quads (the
 * blockstate's rotation is already baked in), picks the pane's
 * platform-facing face quads (geometric normal pointing at the platform,
 * plane inside the pane slab, face area large enough to exclude rim
 * strips), scans the quads' sprite pixels — through the baked atlas
 * sprite, so animations and atlas packing are handled — for rows whose
 * alpha is below {@link #ALPHA_THRESHOLD}, and maps the found transparent
 * v-range back to block-space Y through the quad's own UV→position
 * mapping. Result: the window's top/bottom in block space, correct for
 * vanilla AND for whatever the pack ships.
 *
 * <h2>Availability &amp; fallbacks</h2>
 * Requires the 1.20.x baked-sprite texture API (SpriteContents) AND the
 * fabric mappings — the scan walks raw baked-model quads and reads raw
 * atlas-sprite pixels, APIs the shared-source forge build (Mojmap names)
 * does not expose without a second per-loader implementation; on forge,
 * and on older MC versions, the scan is compiled out and every pane
 * renders the full face. Anything unexpected (no model, no sprite,
 * unreadable image, degenerate result) also falls back to the full
 * face — exactly the pre-clipping behaviour. The scan is cached per
 * block state; states are interned by the game, so the cache is bounded
 * by the number of distinct glass variants in the world.
 */
final class PSDWindowBounds {

    /** Full-face sentinel: top = 1, bottom = 0. */
    private static final float[] FULL_FACE = {1, 0};

#if MC_VERSION >= "12001"
#if LOADER == "fabric"
    /** An alpha below this counts as "window". Vanilla's window is 0 and
     *  its frame is 255; tinted-glass packs sit anywhere in between, so
     *  the threshold is set high enough to catch a ~60 % tint as a window
     *  while still excluding frame panels (typically ≥ ~70 % opaque). */
    private static final int ALPHA_THRESHOLD = 170;

    /** A pane face quad must span at least this much of the block face —
     *  excludes the light strips, rims and side trims around the pane. */
    private static final float MIN_PANE_AREA = 0.2F;

    /** The pane slab (local z, 16ths): the platform-side pane face sits
     *  around 4/16 toward the track from the block centre. */
    private static final float PANE_MIN_ALONG = 0.1F;
    private static final float PANE_MAX_ALONG = 0.4F;

    /** A window narrower than this (block fraction) is treated as no
     *  window at all. */
    private static final float MIN_WINDOW = 0.05F;

    private static final int CACHE_LIMIT = 512;
    private static final Map<BlockState, float[]> CACHE = new ConcurrentHashMap<>();

    /**
     * The block's transparent window in block-space Y (0 = block bottom,
     * 1 = block top): {@code [0] = window top, [1] = window bottom}. When
     * the window cannot be derived this returns the full face
     * ({@code {1, 0}}) — never null.
     */
    static float[] window(org.mtr.mapping.holder.BlockState state, org.mtr.mapping.holder.Direction facing) {
        final BlockState nativeState = (BlockState) state.data;
        final float[] cached = CACHE.get(nativeState);
        if (cached != null) {
            return cached;
        }
        final float[] computed = compute(nativeState, (Direction) facing.data);
        if (CACHE.size() > CACHE_LIMIT) {
            CACHE.clear();
        }
        CACHE.put(nativeState, computed);
        return computed;
    }

    private static float[] compute(BlockState state, Direction facing) {
        try {
            final MinecraftClient client = MinecraftClient.getInstance();
            if (client == null || client.getBlockRenderManager() == null) {
                return FULL_FACE;
            }
            final BakedModel model = client.getBlockRenderManager().getModel(state);
            if (model == null) {
                return FULL_FACE;
            }

            final Direction platform = facing.getOpposite();
            final Random random = Random.create();

            float top = 1;
            float bottom = 0;
            boolean any = false;

            for (int d = 0; d <= 6; d++) {
                final Direction side = d < 6 ? Direction.values()[d] : null;
                final List<BakedQuad> quads = model.getQuads(state, side, random);
                if (quads == null || quads.isEmpty()) {
                    continue;
                }
                for (final BakedQuad quad : quads) {
                    final float[] w = quadWindow(quad, platform, facing);
                    if (w == null) {
                        continue;
                    }
                    any = true;
                    top = Math.min(top, w[0]);
                    bottom = Math.max(bottom, w[1]);
                }
            }

            if (!any || top - bottom < MIN_WINDOW) {
                return FULL_FACE;
            }
            return new float[]{top, bottom};
        } catch (Throwable t) {
            return FULL_FACE;
        }
    }

    /**
     * The transparent window of ONE pane face quad, or null when the quad
     * is not a platform-facing pane face / has no transparent region.
     */
    private static float[] quadWindow(BakedQuad quad, Direction platform, Direction facing) {
        final int[] data = quad.getVertexData();
        if (data == null || data.length < 32) {
            return null;
        }

        final float[] px = new float[4], py = new float[4], pz = new float[4], pu = new float[4], pv = new float[4];
        for (int i = 0; i < 4; i++) {
            px[i] = Float.intBitsToFloat(data[i * 8]);
            py[i] = Float.intBitsToFloat(data[i * 8 + 1]);
            pz[i] = Float.intBitsToFloat(data[i * 8 + 2]);
            pu[i] = Float.intBitsToFloat(data[i * 8 + 4]);
            pv[i] = Float.intBitsToFloat(data[i * 8 + 5]);
        }

        /* Baked quads may be centred on the block ([-0.5, 0.5]) instead of
           [0, 1] depending on the bake pipeline — a pane face always spans
           most of the block, so any negative coordinate betrays the
           centred convention. */
        if (min(px) < -0.01F || min(py) < -0.01F || min(pz) < -0.01F) {
            for (int i = 0; i < 4; i++) {
                px[i] += 0.5F;
                py[i] += 0.5F;
                pz[i] += 0.5F;
            }
        }

        /* Geometric normal — must point AT the platform (the pane's
           platform-side face). */
        final float ax = px[1] - px[0], ay = py[1] - py[0], az = pz[1] - pz[0];
        final float bx = px[3] - px[0], by = py[3] - py[0], bz = pz[3] - pz[0];
        float nx = ay * bz - az * by;
        float ny = az * bx - ax * bz;
        float nz = ax * by - ay * bx;
        final float len = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
        if (len < 1e-6F) {
            return null;
        }
        nx /= len;
        ny /= len;
        nz /= len;
        final float towardsPlatform = nx * platform.getOffsetX() + ny * platform.getOffsetY() + nz * platform.getOffsetZ();
        if (towardsPlatform < 0.9F) {
            return null;
        }

        /* The quad must live in the pane slab: its centre, measured along
           the facing axis (toward the track) from the block centre. */
        float cx = 0, cy = 0, cz = 0;
        for (int i = 0; i < 4; i++) {
            cx += px[i];
            cy += py[i];
            cz += pz[i];
        }
        cx = cx / 4 - 0.5F;
        cy = cy / 4 - 0.5F;
        cz = cz / 4 - 0.5F;
        final float along = cx * facing.getOffsetX() + cy * facing.getOffsetY() + cz * facing.getOffsetZ();
        if (along < PANE_MIN_ALONG || along > PANE_MAX_ALONG) {
            return null;
        }

        /* Face area (in the plane perpendicular to the facing axis) —
           excludes the thin strips and trims around the pane. */
        final float area;
        if (facing.getAxis() == Direction.Axis.X) {
            area = (max(pz) - min(pz)) * (max(py) - min(py));
        } else if (facing.getAxis() == Direction.Axis.Z) {
            area = (max(px) - min(px)) * (max(py) - min(py));
        } else {
            return null;
        }
        if (area < MIN_PANE_AREA) {
            return null;
        }

        /* Scan the quad's sprite pixels (frame 0 of the original image,
           via the baked sprite so atlas packing is unwound). */
        final Sprite sprite = quad.getSprite();
        if (sprite == null) {
            return null;
        }
        final SpriteContents contents = sprite.getContents();
        if (contents == null) {
            return null;
        }
        final NativeImage image = ((SpriteContentsAccessor) (Object) contents).jsblock$getImage();
        if (image == null || image.getWidth() <= 0 || image.getHeight() <= 0) {
            return null;
        }
        /* Frame 0 of the original image: for static textures (every PSD
           glass texture in practice) the image IS the one frame, so its
           full height is the frame height. Animated textures degrade to
           reading the stacked image — no frame-count API is needed (the
           contents' own frame count is private in the 1.20.x mappings). */
        final int frameWidth = image.getWidth();
        final int frameHeight = image.getHeight();

        final float uSpan = sprite.getMaxU() - sprite.getMinU();
        final float vSpan = sprite.getMaxV() - sprite.getMinV();
        if (uSpan <= 0 || vSpan <= 0) {
            return null;
        }
        final float suMin = clamp01((min(pu) - sprite.getMinU()) / uSpan);
        final float suMax = clamp01((max(pu) - sprite.getMinU()) / uSpan);
        final float svMin = clamp01((min(pv) - sprite.getMinV()) / vSpan);
        final float svMax = clamp01((max(pv) - sprite.getMinV()) / vSpan);
        if (suMax - suMin < 1e-4F || svMax - svMin < 1e-4F) {
            return null;
        }

        final int xFrom = (int) Math.floor(suMin * (frameWidth - 1));
        final int xTo = (int) Math.ceil(suMax * (frameWidth - 1));
        final int yFrom = (int) Math.floor(svMin * (frameHeight - 1));
        final int yTo = (int) Math.ceil(svMax * (frameHeight - 1));
        final int columnStep = Math.max(1, (xTo - xFrom) / 16);

        int firstRow = -1;
        int lastRow = -1;
        for (int y = yFrom; y <= yTo; y++) {
            boolean clear = false;
            for (int x = xFrom; x <= xTo; x += columnStep) {
                if ((image.getOpacity(x, y) & 0xFF) < ALPHA_THRESHOLD) {
                    clear = true;
                    break;
                }
            }
            if (clear) {
                if (firstRow < 0) {
                    firstRow = y;
                }
                lastRow = y;
            }
        }
        if (firstRow < 0) {
            return null;
        }

        /* Map the transparent rows back to block-space Y through the
           quad's own v→y mapping (linear for axis-aligned pane faces). */
        int vMinIdx = 0, vMaxIdx = 0;
        for (int i = 1; i < 4; i++) {
            if (pv[i] < pv[vMinIdx]) {
                vMinIdx = i;
            }
            if (pv[i] > pv[vMaxIdx]) {
                vMaxIdx = i;
            }
        }
        if (pv[vMaxIdx] - pv[vMinIdx] < 1e-6F) {
            return null;
        }
        final float slope = (py[vMaxIdx] - py[vMinIdx]) / (pv[vMaxIdx] - pv[vMinIdx]);
        final float yAtFirst = py[vMinIdx] + (rowToV(firstRow, sprite, frameHeight) - pv[vMinIdx]) * slope;
        final float yAtLast = py[vMinIdx] + (rowToV(lastRow, sprite, frameHeight) - pv[vMinIdx]) * slope;
        return new float[]{Math.max(yAtFirst, yAtLast), Math.min(yAtFirst, yAtLast)};
    }

    /** Sprite row (frame-local) → atlas v coordinate. */
    private static float rowToV(int row, Sprite sprite, int frameHeight) {
        final float local = (row + 0.5F) / frameHeight;
        return sprite.getMinV() + local * (sprite.getMaxV() - sprite.getMinV());
    }

    private static float min(float[] arr) {
        float m = arr[0];
        for (int i = 1; i < arr.length; i++) {
            m = Math.min(m, arr[i]);
        }
        return m;
    }

    private static float max(float[] arr) {
        float m = arr[0];
        for (int i = 1; i < arr.length; i++) {
            m = Math.max(m, arr[i]);
        }
        return m;
    }

    private static float clamp01(float v) {
        return v < 0 ? 0 : Math.min(v, 1);
    }
#else
    /* Forge: the baked-sprite scan is fabric-only (see the class javadoc)
       — window clipping is unavailable and every pane renders the full
       face (the pre-clipping behaviour of the glass sandwich). */
    static float[] window(org.mtr.mapping.holder.BlockState state, org.mtr.mapping.holder.Direction facing) {
        return FULL_FACE;
    }
#endif
#else
    /* Pre-1.20.1: no baked-sprite texture API — window clipping is
       unavailable and every pane renders the full face (the pre-clipping
       behaviour of the glass sandwich). */
    static float[] window(org.mtr.mapping.holder.BlockState state, org.mtr.mapping.holder.Direction facing) {
        return FULL_FACE;
    }
#endif
}
