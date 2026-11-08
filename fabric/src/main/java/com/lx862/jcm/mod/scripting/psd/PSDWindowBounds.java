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
 * thicknesses and window extents are pack-specific, and the yanyang.4
 * user pack proved the extreme: a pane texture that is <b>fully
 * transparent</b> (pure see-through glass) with the door's frame strips
 * as <b>separate model elements</b>. Deriving the window from the pane
 * quad's own texture alone then yields the full face and the canvas
 * bleeds over the strips.
 *
 * <h2>How (v2 — occlusion derived)</h2>
 * For the bound block's state this walks the BAKED model's quads (the
 * blockstate's rotation is already baked in) and keeps every
 * <b>pane-parallel</b> quad (geometric normal along the platform axis,
 * either direction) that is big enough to matter. Every sprite row of
 * such a quad whose sampled pixels are majority at-or-above the frame
 * threshold marks its block space y-range as <b>occluded frame</b> —
 * whether that row belongs to the pane's own texture (frosted bands,
 * printed frame) or to a separate frame/strip element anywhere in the
 * block. Yanyang.7: the scan runs TWICE — a strict pass (only
 * near-fully-transparent rows are window, so SEMI-TRANSPARENT frame
 * strips occlude like the yanyang pack's) and, when that finds no
 * window at all, the lenient legacy pass (majority-OPAQUE rows are
 * frame) so tinted-window packs keep their window. The window is
 * then the largest contiguous unoccluded span, intersected with the
 * <b>pane's own geometric extent</b> (so a half-height pane never
 * lets content float in the open air above it), and reported together
 * with the pane's plane position (the sandwich planes are placed just
 * behind the pane's platform-side face — pack panes do not necessarily
 * sit at the vanilla 4/16) and whether the block has any visible
 * pane-parallel geometry at all (the render gate for DRL upper halves,
 * whose vanilla models are invisible).
 *
 * <h2>Availability &amp; fallbacks</h2>
 * Requires the 1.20.x baked-sprite texture API (SpriteContents) AND the
 * fabric mappings — the scan walks raw baked-model quads and reads raw
 * atlas-sprite pixels, APIs the shared-source forge build (Mojmap names)
 * does not expose without a second per-loader implementation; on forge,
 * and on older MC versions, the scan is compiled out and every pane
 * renders the full face at the vanilla pane depth (the pre-scan
 * behaviour — DRL upper halves keep skipping on forge). Anything
 * unexpected (no model, no sprite, unreadable image, degenerate result)
 * also falls back. The scan is cached per block state; states are
 * interned by the game, so the cache is bounded by the number of
 * distinct glass variants in the world.
 */
final class PSDWindowBounds {

    /** Vanilla pane depth fallback: the pane's platform-side face sits
     *  4/16 toward the track from the block centre (both JCM's DRL APG
     *  and MTR's own APG glass models). */
    static final float DEFAULT_PANE_ALONG = 4F / 16;

#if MC_VERSION >= "12001"
#if LOADER == "fabric"
    /** Yanyang.7 strict-pass frame threshold: a sprite row whose
     *  majority of sampled pixels is at or above this alpha counts as
     *  OCCLUDED FRAME. The yanyang pack's frame strips are
     *  SEMI-TRANSPARENT (~50-60 % tint) — the old majority-opaque rule
     *  (see {@link #ALPHA_THRESHOLD}) let them through as "window", so
     *  the canvas and the white backing spanned them and the script
     *  content bled onto the strips. Only near-fully-transparent rows
     *  are window now (vanilla windows are 0). */
    private static final int FRAME_ALPHA_STRICT = 51;

    /** Lenient legacy threshold, used only when the strict pass finds no
     *  window at all: packs whose WINDOW itself is a ~50-60 % tint would
     *  have every row occluded under the strict rule, so this pass
     *  counts rows whose majority is at or above THIS alpha as frame
     *  instead — preserving the yanyang.5 behaviour for tinted-glass
     *  packs. */
    private static final int ALPHA_THRESHOLD = 170;

    /** A pane-parallel quad must span at least this much of the block
     *  face to be a frame/strip OCCLUDER — excludes micro slivers. */
    private static final float MIN_OCCLUDER_AREA = 0.005F;

    /** A quad must span at least this much of the block face to be the
     *  PANE candidate (the platform-facing quad with the largest area
     *  defines the pane plane and the window's geometric bound). */
    private static final float MIN_PANE_AREA = 0.05F;

    /** Window scan resolution (rows over the block height). */
    private static final int GRID = 128;

    /** Yanyang.9: the backing (not the canvas) spans the pane's geometric
     *  extent grown by this margin (block fraction) — enough to swallow the
     *  pane's own semi-transparent edge bands and hairline seams against
     *  adjacent elements, far too little to peek past opaque frame parts at
     *  any playable angle. */
    private static final float EXTENT_MARGIN = 1F / 64;

    private static final int CACHE_LIMIT = 512;
    private static final Map<BlockState, Bounds> CACHE = new ConcurrentHashMap<>();

    /** One pane-parallel quad of the baked model, parsed. */
    private static final class Quad {
        final float[] px = new float[4], py = new float[4], pz = new float[4];
        final float[] pu = new float[4], pv = new float[4];
        final float area;            // area in the plane ⊥ the platform axis
        final boolean facesPlatform; // normal points at the platform
        final float along;           // plane position along the platform axis
        final float yMin, yMax;      // block-space y extent
        /** Yanyang.10: whether the pane plane's horizontal axis runs along
         *  block X (true when the wall FACES the X axis — normal ±X, so the
         *  pane spreads in Z) or along block Z (facing Z, pane spreads in
         *  X). Mirrors the axis choice of the {@code area} computation. */
        final boolean hAlongX;
        /** Yanyang.10: block-space extent along the pane's HORIZONTAL axis
         *  (Z when facing X, X when facing Z) — the geometric bound of the
         *  horizontal window scan. */
        final float hMin, hMax;
        final Sprite sprite;
        final int frameWidth, frameHeight;

        Quad(BakedQuad baked, Direction platform, Direction facing) {
            final int[] data = baked.getVertexData();
            for (int i = 0; i < 4; i++) {
                px[i] = Float.intBitsToFloat(data[i * 8]);
                py[i] = Float.intBitsToFloat(data[i * 8 + 1]);
                pz[i] = Float.intBitsToFloat(data[i * 8 + 2]);
                pu[i] = Float.intBitsToFloat(data[i * 8 + 4]);
                pv[i] = Float.intBitsToFloat(data[i * 8 + 5]);
            }
            /* Baked quads may be centred on the block ([-0.5, 0.5]) instead
               of [0, 1] depending on the bake pipeline — a pane face always
               spans most of the block, so any negative coordinate betrays
               the centred convention. */
            if (min(px) < -0.01F || min(py) < -0.01F || min(pz) < -0.01F) {
                for (int i = 0; i < 4; i++) {
                    px[i] += 0.5F;
                    py[i] += 0.5F;
                    pz[i] += 0.5F;
                }
            }
            final float ax = px[1] - px[0], ay = py[1] - py[0], az = pz[1] - pz[0];
            final float bx = px[3] - px[0], by = py[3] - py[0], bz = pz[3] - pz[0];
            final float tx = ay * bz - az * by;
            final float ty = az * bx - ax * bz;
            final float tz = ax * by - ay * bx;
            final float len = (float) Math.sqrt(tx * tx + ty * ty + tz * tz);
            if (len < 1e-6F) {
                throw new IllegalArgumentException("degenerate");
            }
            final float dotPlatform = (tx * platform.getOffsetX() + ty * platform.getOffsetY() + tz * platform.getOffsetZ()) / len;
            if (Math.abs(dotPlatform) < 0.9F) {
                throw new IllegalArgumentException("not pane-parallel");
            }
            facesPlatform = dotPlatform > 0;
            float cx = 0, cy = 0, cz = 0;
            float yMin = py[0], yMax = py[0];
            for (int i = 0; i < 4; i++) {
                cx += px[i];
                cy += py[i];
                cz += pz[i];
                yMin = Math.min(yMin, py[i]);
                yMax = Math.max(yMax, py[i]);
            }
            cx = cx / 4 - 0.5F;
            cy = cy / 4 - 0.5F;
            cz = cz / 4 - 0.5F;
            along = cx * facing.getOffsetX() + cy * facing.getOffsetY() + cz * facing.getOffsetZ();
            if (facing.getAxis() == Direction.Axis.X) {
                area = (max(pz) - min(pz)) * (yMax - yMin);
            } else if (facing.getAxis() == Direction.Axis.Z) {
                area = (max(px) - min(px)) * (yMax - yMin);
            } else {
                throw new IllegalArgumentException("vertical facing");
            }
            hAlongX = facing.getAxis() == Direction.Axis.X;
            if (hAlongX) {
                hMin = min(pz);
                hMax = max(pz);
            } else {
                hMin = min(px);
                hMax = max(px);
            }
            this.yMin = yMin;
            this.yMax = yMax;
            sprite = baked.getSprite();
            SpriteContents contents = sprite == null ? null : sprite.getContents();
            NativeImage image = contents == null ? null : ((SpriteContentsAccessor) (Object) contents).jsblock$getImage();
            if (image == null || image.getWidth() <= 0 || image.getHeight() <= 0) {
                throw new IllegalArgumentException("no image");
            }
            /* Frame 0 of the original image: for static textures (every PSD
               glass texture in practice) the image IS the one frame, so its
               full height is the frame height. Animated textures degrade to
               reading the stacked image — no frame-count API is needed (the
               contents' own frame count is private in the 1.20.x mappings). */
            frameWidth = image.getWidth();
            frameHeight = image.getHeight();
        }

        /** Sprite row (frame-local) → atlas v coordinate. */
        float rowToV(int row) {
            final float local = (row + 0.5F) / frameHeight;
            return sprite.getMinV() + local * (sprite.getMaxV() - sprite.getMinV());
        }

        /** Sprite column (frame-local) → atlas u coordinate (yanyang.10,
         *  the horizontal twin of {@link #rowToV}). */
        float columnToU(int col) {
            final float local = (col + 0.5F) / frameWidth;
            return sprite.getMinU() + local * (sprite.getMaxU() - sprite.getMinU());
        }
    }

    /** The derived pane geometry of one block state. */
    static final class Bounds {
        /** Full-face sentinel: window = the whole block face at the vanilla
         *  pane depth, no visible pane-parallel geometry. */
        static final Bounds FULL_FACE = new Bounds(1, 0, DEFAULT_PANE_ALONG, false, 1, 0, 0, 1);

        /** Window top (block space, 1 = block top) — the STRICT alpha
         *  window, for the script canvas (content must stay clear of the
         *  semi-transparent bands and strips). */
        final float top;
        /** Window bottom (block space, 0 = block bottom) — strict, see
         *  {@link #top}. */
        final float bottom;
        /** The pane's platform-side face plane, measured toward the track
         *  from the block centre (vanilla: +4/16). The sandwich planes sit
         *  just behind it. */
        final float paneAlong;
        /** Whether ANY pane-parallel quad has any visible pixel — the
         *  render gate for DRL upper halves (their vanilla models are
         *  fully invisible; a pack that gives them a visible frame/glass
         *  turns them into real display surfaces). */
        final boolean visibleGeometry;
        /** Yanyang.9: the pane's own geometric y-extent (grown by
         *  {@link #EXTENT_MARGIN}, clamped to the block) — the COVERING
         *  extent. The backing is a flat opaque quad BEHIND the pane, so it
         *  does not need the alpha window at all: opaque frame parts in
         *  front hide any excess while the pane's semi-transparent edge
         *  bands (frosted strips, tint gradients — the rows the strict scan
         *  excludes) get solid colour behind them, reading as "fully
         *  covered". Previously the backing reused the strict window and
         *  those bands stayed see-through — the yanyang.8 "coverage
         *  incomplete" report. */
        final float extentTop;
        /** Yanyang.9: see {@link #extentTop}. */
        final float extentBottom;
        /** Yanyang.10 (semantics clarified yanyang.11): window LEFT edge,
         *  in BLOCK SPACE along the pane's horizontal axis — block X when
         *  the wall faces Z, block Z when it faces X; 0 = the block's
         *  LOW-coordinate edge along that axis (which is the platform
         *  viewer's left only for NORTH/EAST walls — the renderer applies
         *  the facing mirror when mapping into run coordinates, see
         *  PSDScriptRenderer's horizontal window). The STRICT horizontal
         *  alpha window, the exact horizontal twin of {@link #top}: sprite
         *  COLUMNS whose majority of sampled pixels is at-or-above the
         *  frame threshold count as occluded frame (door-frame stiles,
         *  printed edge bands, frosted side gradients), whether they
         *  belong to the pane's own texture or to separate vertical strip
         *  elements. The renderer maps the canvas onto the run's largest
         *  contiguous transparent horizontal span so script text never
         *  lands behind a stile — the yanyang.9 "script content still
         *  overflows" report, where the first/last glyph of every row was
         *  half-eaten by the door frame's vertical stiles while the canvas
         *  spanned the full run. */
        final float left;
        /** Yanyang.10: window RIGHT edge (block space, 1 = the block's
         *  HIGH-coordinate edge along the pane's horizontal axis) — an
         *  EDGE POSITION, not an occlusion width; strict, see
         *  {@link #left}. */
        final float right;

        Bounds(float top, float bottom, float paneAlong, boolean visibleGeometry, float extentTop, float extentBottom, float left, float right) {
            this.top = top;
            this.bottom = bottom;
            this.paneAlong = paneAlong;
            this.visibleGeometry = visibleGeometry;
            this.extentTop = extentTop;
            this.extentBottom = extentBottom;
            this.left = left;
            this.right = right;
        }
    }

    /** A window narrower than this (block fraction) is treated as no
     *  window at all — kept next to the scan for the renderer's MIN_WINDOW. */
    static final float MIN_WINDOW = 0.05F;

    /**
     * The block's derived pane geometry (see the class javadoc). Never
     * null; falls back to {@link Bounds#FULL_FACE}.
     */
    static Bounds window(org.mtr.mapping.holder.BlockState state, org.mtr.mapping.holder.Direction facing) {
        final BlockState nativeState = (BlockState) state.data;
        final Bounds cached = CACHE.get(nativeState);
        if (cached != null) {
            return cached;
        }
        final Bounds computed = compute(nativeState, (Direction) facing.data);
        if (CACHE.size() > CACHE_LIMIT) {
            CACHE.clear();
        }
        CACHE.put(nativeState, computed);
        return computed;
    }

    private static Bounds compute(BlockState state, Direction facing) {
        try {
            final MinecraftClient client = MinecraftClient.getInstance();
            if (client == null || client.getBlockRenderManager() == null) {
                return Bounds.FULL_FACE;
            }
            final BakedModel model = client.getBlockRenderManager().getModel(state);
            if (model == null) {
                return Bounds.FULL_FACE;
            }

            final Direction platform = facing.getOpposite();
            final Random random = Random.create();

            /* Collect pane-parallel quads. */
            final java.util.ArrayList<Quad> quads = new java.util.ArrayList<>();
            for (int d = 0; d <= 6; d++) {
                final Direction side = d < 6 ? Direction.values()[d] : null;
                final List<BakedQuad> list = model.getQuads(state, side, random);
                if (list == null || list.isEmpty()) {
                    continue;
                }
                for (final BakedQuad baked : list) {
                    try {
                        quads.add(new Quad(baked, platform, facing));
                    } catch (IllegalArgumentException ignored) {
                        /* Not pane-parallel / degenerate / no sprite. */
                    }
                }
            }

            /* The pane: the UNION of every platform-facing pane-scale
               quad (yanyang.12). Vanilla MTR's psd_glass splits the pane
               face into MULTIPLE sub-face quads — psd_glass_middle_base
               uses two 2px-thick elements (z 0–8 and 8–16), each
               contributing its own half-face platform-facing quad, and
               the yanyang.11-and-earlier "largest single quad" pick
               clipped the derived window to ONE sub-quad's extent —
               the canvas then collapsed into a half- or quarter-face
               pocket (the 2026-11-08 「左上角 1/4」 report). The union
               spans what the pane actually covers; the occlusion scan
               below already walks every quad's own UV mapping, so only
               the extent clip needed widening. */
            Quad paneAnchor = null;
            boolean anyVisible = false;
            float paneHMin = Float.MAX_VALUE, paneHMax = -Float.MAX_VALUE;
            float paneYMin = Float.MAX_VALUE, paneYMax = -Float.MAX_VALUE;
            float paneAlong = -Float.MAX_VALUE;
            for (final Quad quad : quads) {
                if (quad.area < MIN_OCCLUDER_AREA) {
                    continue;
                }
                try {
                    if (scanVisible(quad)) {
                        anyVisible = true;
                    }
                } catch (Throwable ignored) {
                    /* Unreadable sprite — skip the visibility probe. */
                }
                if (quad.facesPlatform && quad.area >= MIN_PANE_AREA) {
                    if (paneAnchor == null || quad.area > paneAnchor.area) {
                        paneAnchor = quad;
                    }
                    paneHMin = Math.min(paneHMin, quad.hMin);
                    paneHMax = Math.max(paneHMax, quad.hMax);
                    paneYMin = Math.min(paneYMin, quad.yMin);
                    paneYMax = Math.max(paneYMax, quad.yMax);
                    paneAlong = Math.max(paneAlong, quad.along);
                }
            }
            if (paneAnchor == null) {
                /* No pane-scale platform-facing face — full-face fallback
                   (with the visibility flag so DRL upper halves can still
                   be gated). */
                return new Bounds(1, 0, DEFAULT_PANE_ALONG, anyVisible, 1, 0, 0, 1);
            }

            /* Yanyang.9: the covering extent — the pane's own geometric
               y-range (now the UNION's, yanyang.12) grown by the margin,
               clamped to the block. Every return path below carries it. */
            final float extentTop = Math.min(1, paneYMax + EXTENT_MARGIN);
            final float extentBottom = Math.max(0, paneYMin - EXTENT_MARGIN);

            /* Window: the largest unoccluded span, clipped to the pane's
               own geometric extent. Two passes (yanyang.7): the STRICT
               pass first — semi-transparent frame strips must occlude —
               then the lenient legacy threshold when the strict pass
               finds no window (tinted-window packs keep their window). */
            final Bounds strict = scanWindow(quads, paneYMin, paneYMax, paneHMin, paneHMax, paneAlong, anyVisible, FRAME_ALPHA_STRICT, extentTop, extentBottom);
            if (strict != null) {
                return strict;
            }
            final Bounds lenient = scanWindow(quads, paneYMin, paneYMax, paneHMin, paneHMax, paneAlong, anyVisible, ALPHA_THRESHOLD, extentTop, extentBottom);
            if (lenient != null) {
                return lenient;
            }
            return new Bounds(1, 0, paneAlong, anyVisible, extentTop, extentBottom, 0, 1);
        } catch (Throwable t) {
            return Bounds.FULL_FACE;
        }
    }

    /** The largest unoccluded y-span across every occluder quad under the
     *  given frame threshold, clipped to the pane's own geometric extent;
     *  null when no span of {@link #MIN_WINDOW} survives. Yanyang.10: the
     *  same scan now also derives the largest unoccluded HORIZONTAL span
     *  (sprite columns instead of rows) — the window's left/right edges —
     *  falling back to the full block width when no span survives. */
    private static Bounds scanWindow(java.util.ArrayList<Quad> quads, float paneYMin, float paneYMax, float paneHMin, float paneHMax, float paneAlong, boolean anyVisible, int frameThreshold, float extentTop, float extentBottom) {
        final boolean[] occluded = new boolean[GRID];
        final boolean[] occludedH = new boolean[GRID];
        for (final Quad quad : quads) {
            if (quad.area < MIN_OCCLUDER_AREA) {
                continue;
            }
            try {
                markOcclusion(quad, occluded, frameThreshold);
                markOcclusionH(quad, occludedH, frameThreshold);
            } catch (Throwable ignored) {
                /* Unreadable sprite — contributes no occlusion. */
            }
        }
        final int yFrom = clampRow(paneYMin);
        final int yTo = clampRow(paneYMax);
        int bestStart = -1, bestLen = 0;
        int run = 0;
        for (int y = yFrom; y <= yTo; y++) {
            if (!occluded[y]) {
                run++;
                if (run > bestLen) {
                    bestLen = run;
                    bestStart = y - run + 1;
                }
            } else {
                run = 0;
            }
        }
        if (bestLen <= 0) {
            return null;
        }
        final float top = (float) (bestStart + bestLen) / GRID;
        final float bottom = (float) bestStart / GRID;
        if (top - bottom < MIN_WINDOW) {
            return null;
        }
        /* The horizontal twin: the largest unoccluded span of columns,
           clipped to the pane's own horizontal geometric extent. No span
           → the full block width (the pre-scan behaviour — the canvas
           spans the run's every column). */
        float hLeft = 0;
        float hRight = 1;
        final int hFrom = clampRow(paneHMin);
        final int hTo = clampRow(paneHMax);
        int hBestStart = -1, hBestLen = 0, hRun = 0;
        for (int c = hFrom; c <= hTo; c++) {
            if (!occludedH[c]) {
                hRun++;
                if (hRun > hBestLen) {
                    hBestLen = hRun;
                    hBestStart = c - hRun + 1;
                }
            } else {
                hRun = 0;
            }
        }
        if (hBestLen > 0) {
            final float hR = (float) (hBestStart + hBestLen) / GRID;
            final float hL = (float) hBestStart / GRID;
            if (hR - hL >= MIN_WINDOW) {
                hLeft = hL;
                hRight = hR;
            }
        }
        return new Bounds(top, bottom, paneAlong, anyVisible, extentTop, extentBottom, hLeft, hRight);
    }

    /** The horizontal twin of {@link #markOcclusion} (yanyang.10): marks the
     *  block-space horizontal ranges of the quad's sprite COLUMNS whose
     *  sampled pixels are majority at-or-above {@code frameThreshold} (and,
     *  when the quad's sampled region is majority-frame as a whole, its full
     *  horizontal extent — vertical stile strips) as occluded. */
    private static void markOcclusionH(Quad quad, boolean[] occluded, int frameThreshold) {
        final NativeImage image = ((SpriteContentsAccessor) (Object) quad.sprite.getContents()).jsblock$getImage();
        final int[][] rect = sampleRect(quad);
        final int xFrom = rect[0][0], xTo = rect[0][1], yFrom = rect[1][0], yTo = rect[1][1];
        final int rowStep = Math.max(1, (yTo - yFrom) / 16);

        int frameCols = 0;
        int totalCols = 0;
        for (int x = xFrom; x <= xTo; x++) {
            int frame = 0, total = 0;
            for (int y = yFrom; y <= yTo; y += rowStep) {
                total++;
                if ((image.getOpacity(x, y) & 0xFF) >= frameThreshold) {
                    frame++;
                }
            }
            totalCols++;
            if (total > 0 && frame * 2 >= total) {
                frameCols++;
                /* Map the column to block-space horizontal position through
                   the quad's u→h mapping, clamped to the quad's own extent. */
                final float hA = uToH(quad, quad.columnToU(x));
                final float hB = uToH(quad, quad.columnToU(Math.min(x + 1, quad.frameWidth - 1)));
                final float lo = Math.max(quad.hMin, Math.min(hA, hB));
                final float hi = Math.min(quad.hMax, Math.max(hA, hB));
                if (hi > lo) {
                    final int rFrom = clampRow(lo);
                    final int rTo = clampRow(hi);
                    for (int r = rFrom; r <= rTo; r++) {
                        occluded[r] = true;
                    }
                }
            }
        }
        /* Mostly-frame quad (a vertical stile / strip): occlude its whole
           horizontal extent — sub-pixel UV slivers would otherwise slip
           through the column mapping. */
        if (totalCols > 0 && frameCols * 2 >= totalCols) {
            final int rFrom = clampRow(quad.hMin);
            final int rTo = clampRow(quad.hMax);
            for (int r = rFrom; r <= rTo; r++) {
                occluded[r] = true;
            }
        }
    }

    /** Atlas u → block-space horizontal position through the quad's own
     *  (linear) u→h map (the twin of {@link #vToY}). */
    private static float uToH(Quad quad, float u) {
        int uMinIdx = 0, uMaxIdx = 0;
        for (int i = 1; i < 4; i++) {
            if (quad.pu[i] < quad.pu[uMinIdx]) {
                uMinIdx = i;
            }
            if (quad.pu[i] > quad.pu[uMaxIdx]) {
                uMaxIdx = i;
            }
        }
        final float hMin = quad.hAlongX ? quad.pz[uMinIdx] : quad.px[uMinIdx];
        final float hMax = quad.hAlongX ? quad.pz[uMaxIdx] : quad.px[uMaxIdx];
        final float slope = (hMax - hMin) / (quad.pu[uMaxIdx] - quad.pu[uMinIdx]);
        return hMin + (u - quad.pu[uMinIdx]) * slope;
    }

    /** Whether the quad's sampled region contains ANY visible pixel. */
    private static boolean scanVisible(Quad quad) {
        final NativeImage image = ((SpriteContentsAccessor) (Object) quad.sprite.getContents()).jsblock$getImage();
        final int[][] rect = sampleRect(quad);
        final int xFrom = rect[0][0], xTo = rect[0][1], yFrom = rect[1][0], yTo = rect[1][1];
        final int columnStep = Math.max(1, (xTo - xFrom) / 16);
        for (int y = yFrom; y <= yTo; y += Math.max(1, (yTo - yFrom) / 32)) {
            for (int x = xFrom; x <= xTo; x += columnStep) {
                if ((image.getOpacity(x, y) & 0xFF) > 0) {
                    return true;
                }
            }
        }
        return false;
    }

    /** Marks the block-space y-ranges of the quad's sprite rows whose
     *  sampled pixels are majority at-or-above {@code frameThreshold}
     *  (and, when the quad's sampled region is majority-frame as a whole,
     *  its full extent — small frame strips) as occluded. */
    private static void markOcclusion(Quad quad, boolean[] occluded, int frameThreshold) {
        final NativeImage image = ((SpriteContentsAccessor) (Object) quad.sprite.getContents()).jsblock$getImage();
        final int[][] rect = sampleRect(quad);
        final int xFrom = rect[0][0], xTo = rect[0][1], yFrom = rect[1][0], yTo = rect[1][1];
        final int columnStep = Math.max(1, (xTo - xFrom) / 16);

        int frameRows = 0;
        int totalRows = 0;
        for (int y = yFrom; y <= yTo; y++) {
            int frame = 0, total = 0;
            for (int x = xFrom; x <= xTo; x += columnStep) {
                total++;
                if ((image.getOpacity(x, y) & 0xFF) >= frameThreshold) {
                    frame++;
                }
            }
            totalRows++;
            if (total > 0 && frame * 2 >= total) {
                frameRows++;
                /* Map the row to block-space y through the quad's v→y
                   mapping, clamped to the quad's own y extent. */
                final float yA = vToY(quad, quad.rowToV(y));
                final float yB = vToY(quad, quad.rowToV(Math.min(y + 1, quad.frameHeight - 1)));
                final float lo = Math.max(quad.yMin, Math.min(yA, yB));
                final float hi = Math.min(quad.yMax, Math.max(yA, yB));
                if (hi > lo) {
                    final int rFrom = clampRow(lo);
                    final int rTo = clampRow(hi);
                    for (int r = rFrom; r <= rTo; r++) {
                        occluded[r] = true;
                    }
                }
            }
        }
        /* Mostly-frame quad (a frame strip / beam): occlude its whole
           y extent — sub-pixel UV slivers would otherwise slip through
           the row mapping. */
        if (totalRows > 0 && frameRows * 2 >= totalRows) {
            final int rFrom = clampRow(quad.yMin);
            final int rTo = clampRow(quad.yMax);
            for (int r = rFrom; r <= rTo; r++) {
                occluded[r] = true;
            }
        }
    }

    /** The quad's sampled pixel rectangle in the sprite's frame-0 image. */
    private static int[][] sampleRect(Quad quad) {
        final float uSpan = quad.sprite.getMaxU() - quad.sprite.getMinU();
        final float vSpan = quad.sprite.getMaxV() - quad.sprite.getMinV();
        if (uSpan <= 0 || vSpan <= 0) {
            throw new IllegalArgumentException("degenerate sprite");
        }
        final float suMin = clamp01((min(quad.pu) - quad.sprite.getMinU()) / uSpan);
        final float suMax = clamp01((max(quad.pu) - quad.sprite.getMinU()) / uSpan);
        final float svMin = clamp01((min(quad.pv) - quad.sprite.getMinV()) / vSpan);
        final float svMax = clamp01((max(quad.pv) - quad.sprite.getMinV()) / vSpan);
        return new int[][]{
                {(int) Math.floor(suMin * (quad.frameWidth - 1)), (int) Math.ceil(suMax * (quad.frameWidth - 1))},
                {(int) Math.floor(svMin * (quad.frameHeight - 1)), (int) Math.ceil(svMax * (quad.frameHeight - 1))}
        };
    }

    /** Atlas v → block-space y through the quad's own (linear) v→y map. */
    private static float vToY(Quad quad, float v) {
        int vMinIdx = 0, vMaxIdx = 0;
        for (int i = 1; i < 4; i++) {
            if (quad.pv[i] < quad.pv[vMinIdx]) {
                vMinIdx = i;
            }
            if (quad.pv[i] > quad.pv[vMaxIdx]) {
                vMaxIdx = i;
            }
        }
        final float slope = (quad.py[vMaxIdx] - quad.py[vMinIdx]) / (quad.pv[vMaxIdx] - quad.pv[vMinIdx]);
        return quad.py[vMinIdx] + (v - quad.pv[vMinIdx]) * slope;
    }

    private static int clampRow(float y) {
        return Math.max(0, Math.min(GRID - 1, (int) Math.floor(y * GRID)));
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
       face at the vanilla pane depth (the pre-scan behaviour of the
       glass sandwich, including the DRL upper-half skip). */
    static final class Bounds {
        static final Bounds FULL_FACE = new Bounds();
        final float top = 1;
        final float bottom = 0;
        final float paneAlong = DEFAULT_PANE_ALONG;
        final boolean visibleGeometry = false;
        final float extentTop = 1;
        final float extentBottom = 0;
        final float left = 0;
        final float right = 1;
        private Bounds() {
        }
    }

    static final float MIN_WINDOW = 0.05F;

    static Bounds window(org.mtr.mapping.holder.BlockState state, org.mtr.mapping.holder.Direction facing) {
        return Bounds.FULL_FACE;
    }
#endif
#else
    /* Pre-1.20.1: no baked-sprite texture API — window clipping is
       unavailable and every pane renders the full face at the vanilla
       pane depth (the pre-scan behaviour of the glass sandwich,
       including the DRL upper-half skip). */
    static final class Bounds {
        static final Bounds FULL_FACE = new Bounds();
        final float top = 1;
        final float bottom = 0;
        final float paneAlong = DEFAULT_PANE_ALONG;
        final boolean visibleGeometry = false;
        final float extentTop = 1;
        final float extentBottom = 0;
        final float left = 0;
        final float right = 1;
        private Bounds() {
        }
    }

    static final float MIN_WINDOW = 0.05F;

    static Bounds window(org.mtr.mapping.holder.BlockState state, org.mtr.mapping.holder.Direction facing) {
        return Bounds.FULL_FACE;
    }
#endif
}
