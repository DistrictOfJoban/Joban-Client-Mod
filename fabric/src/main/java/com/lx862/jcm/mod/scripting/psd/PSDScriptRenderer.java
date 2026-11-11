package com.lx862.jcm.mod.scripting.psd;

import com.lx862.jcm.mod.data.pids.PIDSManager;
import com.lx862.jcm.mod.data.pids.preset.PIDSPresetBase;
import com.lx862.jcm.mod.data.pids.preset.ScriptPIDSPreset;
import com.lx862.jcm.mod.block.APGGlassDRL;
import com.lx862.jcm.mod.block.APGGlassEndDRL;
import com.lx862.jcm.mod.render.RenderHelper;
import com.lx862.jcm.mod.scripting.JCMScripting;
import com.lx862.jcm.mod.util.ShaderDetector;
import com.lx862.jcm.mod.scripting.pids.PIDSWrapper;
import com.lx862.jcm.nativeapi.NativeContentDriver;
import com.lx862.jcm.nativeapi.NativeScriptManager;
import com.lx862.mtrscripting.core.primitive.ParsedScript;
import com.lx862.mtrscripting.core.primitive.ScriptInstance;
import com.lx862.mtrscripting.core.primitive.UniqueKey;
import com.lx862.mtrscripting.core.util.ScriptVector3f;
import com.lx862.mtrscripting.mod.resource.MtrScriptingResourceManager;
import com.lx862.mtrscripting.mod.resource.PSDResourceProvider;
import org.mtr.core.data.Platform;
import org.mtr.core.operation.ArrivalResponse;
import org.mtr.libraries.it.unimi.dsi.fastutil.longs.LongImmutableList;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.mtr.mapping.holder.Axis;
import org.mtr.mapping.holder.BlockPos;
import org.mtr.mapping.holder.BlockState;
import org.mtr.mapping.holder.ClientPlayerEntity;
import org.mtr.mapping.holder.Direction;
import org.mtr.mapping.holder.MinecraftClient;
import org.mtr.mapping.holder.World;
import org.mtr.mapping.mapper.GraphicsHolder;
import org.mtr.mapping.mapper.OptimizedRenderer;
import org.mtr.mod.InitClient;
import org.mtr.mod.block.BlockPSDAPGBase;
import org.mtr.mod.block.BlockPSDAPGGlassBase;
import org.mtr.mod.block.BlockPSDTop;
import org.mtr.mod.block.IBlock;
import org.mtr.mod.data.ArrivalsCacheClient;
import org.mtr.mod.render.MainRenderer;
import org.mtr.mod.render.QueuedRenderLayer;
import org.mtr.mod.render.StoredMatrixTransformations;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static com.lx862.jcm.mod.render.RenderHelper.MAX_RENDER_LIGHT;

/**
 * The per-frame PSD script render pass, driven from a HEAD injection into
 * {@code MainRenderer.render} (the same queued-render pipeline MTR's own PSD
 * renderers schedule into).
 *
 * <h2>Geometry</h2>
 * All faces live in the frame MTR's own PSD renderers use —
 * {@code translate(0.5+X, Y, 0.5+Z) → rotateY(-facing.asRotation())} — where
 * local +Z points toward the track and the vanilla pane glass occupies
 * z∈[4/16, 6/16]:
 *
 * <ul>
 *   <li><b>Glass</b> (bound per block, whole segment at once — the editor
 *       binds/unbinds every column, both halves, of the glass segment):
 *       the <b>glass segment</b> the bound blocks belong to (the contiguous
 *       glass/glass-end run between the doors — exactly the glass the brush
 *       merged, see {@link PSDRuns}) becomes ONE scripted panel — a canvas
 *       spanning the segment's full height (yanyang.6: the two block halves
 *       of a full-height door are ONE glass, so the canvas merges them
 *       vertically instead of drawing the script once per half) —
 *       <b>sandwiched INSIDE the pane</b>: an opaque covering
 *       0.45/16 behind the pane's platform-side face (drawn double-sided
 *       with a per-side tint — black toward the platform, white toward
 *       the track, yanyang.9 — spanning the pane's full geometric extent)
 *       and the script canvas 0.25/16 behind it — both at the run's
 *       DERIVED pane depth (see {@link PSDWindowBounds}; vanilla geometry
 *       → 4.45/16 and 4.25/16). Yanyang.7: both planes sit in the FRONT of
 *       the pane slab, ahead of the +0.5/16 plane where every canvas has
 *       provably rendered — packs place their own (white diffuser) panels
 *       behind that plane (the yanyang pack's sits between +0.5/16 and the
 *       old backing plane +1/16, which is why three rounds of "the glass
 *       never turns white" fixes never showed: the backing rendered fine,
 *       behind the pack's panel). The pane's own texture blends over the sandwich,
 *       so the script is visible exactly through the pane's <b>originally
 *       transparent window</b> while the opaque parts of the texture — the
 *       frame, divider bars, frosted bands, whatever the resource pack
 *       draws — stay fully visible and clip the canvas; nothing is painted
 *       over the frame. The covering hides the script from the track
 *       side (passengers inside a stopped train see plain white through the
 *       window, never mirrored content). Several bindings may point into
 *       the same segment; the FIRST one encountered renders it and the
 *       rest are skipped that pass — one canvas, one script run, one set
 *       of sounds per segment.</li>
 *   <li><b>Window clipping</b> (see {@link PSDWindowBounds}): the CANVAS is
 *       clipped to the pane's strict transparent window, derived at runtime
 *       from the block's actual baked model and texture alpha <b>by
 *       occlusion</b> (yanyang.5) — every pane-parallel quad's opaque rows
 *       count as frame, so packs whose pane texture is fully transparent but
 *       whose frame strips are separate model elements clip correctly too.
 *       Yanyang.10 completes the twin: the canvas is now clipped
 *       HORIZONTALLY too — the run's largest contiguous transparent span
 *       of columns, so the door frame's end stiles no longer half-eat the
 *       first/last glyph of every script row (the yanyang.9
 *       「脚本内容越界问题依然存在」 report). The COVERING is
 *       deliberately NOT alpha-clipped (yanyang.9): being a flat opaque
 *       quad BEHIND the pane it spans the pane's full geometric
 *       extent instead, so the pane's semi-transparent edge bands get solid
 *       colour behind them ("fully covered") while the opaque frame parts in
 *       front hide any excess. Blocks whose window cannot be derived
 *       fall back to the full face, matching the pre-clipping behaviour.
 *       DRL upper halves (invisible in vanilla) only render when their
 *       model actually shows something — a pack that gives them a visible
 *       frame/glass turns them into display surfaces.</li>
 *   <li><b>Draw path</b> (2026-11-10, round 11): the backing rides
 *       TWO channels, one per regime —
 *       channel N: <b>the NORMAL text layer, drawn as a plain quad</b>
 *       (see {@link #drawBackingText}) — {@code RenderLayer.getText(
 *       jsblock:white.png)}, the exact layer/program/texture-in-layer
 *       binding every visible glyph on these panes rides (script text
 *       and MTR's own PIDS text alike; bytecode-verified against the
 *       vanilla jar — NO text is drawn, the quad is hand-written with
 *       the glyph vertex sequence), through a private Tessellator
 *       immediate synchronously flushed inside the callback — MTR
 *       drawText's own discipline — gated to shader pipelines
 *       ({@link com.lx862.jcm.mod.util.ShaderDetector}; without shaders
 *       the depth-tested carrier B already carries the correct
 *       sandwich, and a no-depth rect would cover the script's
 *       depth-drawn images/shapes — MTR drains its queues by
 *       QueuedRenderLayer ordinal with TEXT LAST), window-clipped (the
 *       layer blends over the pane, flushed after it in Iris's
 *       GENERAL_TRANSPARENT bucket), one quad per view via the
 *       camera-side gate: BLACK toward the platform, WHITE toward the
 *       track (round 11 「轨道侧还有一层白色底色」); and
 *       carrier B: RenderRouteBase's exact route-sign pipeline (keyed
 *       EXTERIOR queue over the cached entity-cutout warm-up consumer)
 *       at the full pane extent with both tints, which carries the
 *       two-colour look whenever depth-tested quad layers render at
 *       all (every no-shader pipeline). Ten user-tested rounds proved
 *       every depth-tested quad carrier fails on the users' Iris 1.7.6
 *       + ComplementaryUnbound r5.5.1 stack while the NORMAL text
 *       layer renders on those panes every single frame — rounds 9/10
 *       drew channel T on the SEE_THROUGH variant (a different
 *       program) and stayed invisible; see {@link #drawBacking} and
 *       {@link #drawBackingText} for the full post-mortem.</li>
 *   <li><b>Vertical merge</b> (yanyang.6): when EVERY column of the segment
 *       has an upper half that is a display surface (a glass block whose
 *       model actually shows something — DRL uppers whose vanilla model is
 *       fully invisible never join), the canvas spans both rows as one
 *       frame: the window is the union of the lower-row windows
 *       ([0,1] of the frame) and the upper-row windows ([1,2]), so the
 *       script draws ONCE across the full-height glass instead of once per
 *       half. A single missing or invisible upper drops the frame back to
 *       one row — content must never float in open air above a column
 *       without visible geometry.</li>
 *   <li><b>Top</b> (bound to a psd_top block, whose own arrow/route content is
 *       cancelled group-wide by {@code RenderRouteBaseMixin}): the script's
 *       draw calls replay across the <b>psd_top group the bound block
 *       belongs to</b> — the psd_top stretch sitting on one wall section
 *       below (one brush-merged door run, or one glass segment; see
 *       {@link PSDRuns} for why the wall below defines the sections) — as one
 *       groupWidth×42 PIDS pixel canvas (side padding 0.125/16, top 7.5/16,
 *       bottom 1.5/16, straight from RenderPSDTop). Multiple bindings in
 *       the same group render the group once. The rest of the psd_top
 *       band keeps MTR's own arrows and route maps. No white base is forced;
 *       the script fully controls the content.</li>
 * </ul>
 *
 * <h2>Execution model</h2>
 * Scripts (JS and C++) are DRIVEN once per rendered section per frame (the
 * first binding encountered per glass segment / psd_top group); the
 * captured draw calls are REPLAYED onto the section's one merged canvas
 * (the full-height glass canvas / the merged top canvas). Sounds replay
 * exactly once per frame per section. Shader shadow passes replay the last
 * captured draw calls (silhouette correctness) without executing scripts,
 * uploading textures or driving the C++ side — mirroring the
 * eyecandy/PIDS shadow contract.
 */
public final class PSDScriptRenderer implements RenderHelper {

    private static final float SMALL_OFFSET = 0.0005F;

    /** psd_top display band paddings (16ths of the block), from RenderPSDTop
     *  (its constructor passes sidePadding = 0.125F, topPadding = 7.5F,
     *  bottomPadding = 1.5F). */
    private static final float TOP_SIDE_PADDING = 0.125F / 16;
    private static final float TOP_TOP_PADDING = 7.5F / 16;
    private static final float TOP_BOTTOM_PADDING = 1.5F / 16;
    private static final float TOP_DISPLAY_Z = 2F / 16 - 0.05F / 16;

    /** The scripted sandwich lives INSIDE the pane, in its FRONT: the
     *  black backing and the script canvas sit just behind the pane's
     *  platform-side face, both placed relative to the run's derived pane
     *  depth (see {@link PSDWindowBounds.Bounds#paneAlong}; for the
     *  vanilla 4/16 pane these are 4.45/16 and 4.25/16).
     *
     *  <p>Yanyang.7 repositioning: the yanyang.5/6 layout put the canvas
     *  at +0.5/16 and the backing at +1/16 — the canvas rendered (its
     *  text stayed visible through every round) but the backing never
     *  showed, because the user's pack carries its OWN white diffuser
     *  panel whose front face lies between those two planes (+0.5/16 to
     *  +1/16): the backing rendered correctly, merely behind the pack's
     *  panel. Both planes now sit between the pane face and the
     *  proven-visible +0.5/16 frontier — the backing at +0.45/16 is
     *  therefore guaranteed in front of any such panel, with the canvas
     *  0.2/16 ahead of it.</p>
     *
     *  <p>The planes stay ≥ 0.25/16 from the pane's platform face and
     *  0.2/16 from each other — clear of depth-precision z-fighting at
     *  any playable distance — and the pane's translucent faces still
     *  blend over both, so the pane texture itself decides where the
     *  sandwich shows: only through its transparent window pixels, never
     *  over the frame. */
    private static final float GLASS_BACKING_OFFSET = 0.45F / 16;
    private static final float GLASS_SCRIPT_OFFSET = 0.25F / 16;

    /** A window narrower than this (block fraction) is treated as no window. */
    private static final float MIN_WINDOW = 0.05F;

    /** Both channels' layer texture — this mod's own FULLY OPAQUE
     *  white (16×16, every pixel 255,255,255,255), tinted {@link
     *  #BACKING_COLOR} / {@link #TRACK_SIDE_COLOR} at draw time.
     *  Texture history: MTR's own {@code mtr:white} utility sprite is a
     *  TRANSPARENT-white in the users' YYMTR 4.0.5 build (the yanyang.5
     *  finding — an entity-cutout quad textured with it discards every
     *  fragment), so the mod ships and uses its own opaque white.
     *  Carrier B's KEYED scheduleRender makes the queue's warm-up
     *  pre-create the CACHED MoreRenderLayers layer for this very
     *  texture on the scheduled holder — the same stable-instance
     *  discipline every visible MTR/JCM draw uses (see drawBacking).
     *  Channel N binds the same texture through the vanilla text-layer
     *  factory ({@code RenderLayer.getText(jsblock:white.png)}, memoized
     *  per identifier by vanilla itself — see drawBackingText). */
    private static final org.mtr.mapping.holder.Identifier BACKING_TEXTURE = new org.mtr.mapping.holder.Identifier("jsblock", "textures/block/white.png");

    /** The covering tint toward the PLATFORM: fully opaque black (ARGB). */
    private static final int BACKING_COLOR = 0xFF000000;

    /** The track-facing white quad sits this much DEEPER (in the pane
     *  frame's local +Z, toward the track) than the black quad
     *  (yanyang.10 heritage; carrier B keeps the separation). */
    private static final float TRACK_BACK_Z = 0.0005F;

    /** Carrier B's whole pair sits this much deeper than the backing
     *  plane ({@link #GLASS_BACKING_OFFSET} + this) that channel N
     *  draws on — channel N's text layer ignores depth entirely, so
     *  the offset only matters where depth-tested quads render at all
     *  (no-shader pipelines): it keeps carrier B deterministic and
     *  clear of anything else at the same plane (2026-11-10; carrier A
     *  retired, its separation role inherited). */
    private static final float CARRIER_B_Z = 0.001F;

    /** The covering tint toward the TRACK — fully opaque white. Train
     *  passengers see a plain white panel, never mirrored content (the
     *  white quad's depth fails the canvas content's LEQUAL test on the
     *  track side, so script content never leaks through). */
    private static final int TRACK_SIDE_COLOR = 0xFFFFFFFF;

    /** Channel N2's carrier glyph (round 12): ONE character whose
     *  {@code TextRenderer.draw} call, with its background colour set to
     *  the same tint, produces a solid rectangle through the exact
     *  machine every VISIBLE script glyph rides — the glyph quad on the
     *  NORMAL text layer (rendertype_text) plus the Drawer's background
     *  rectangle (rendertype_text_background, the nametag program every
     *  shader pack supports). 'W' is the densest ASCII glyph with a
     *  measured advance; its pixels are tinted the same colour as the
     *  background, so the stretched result is an undifferentiated solid
     *  rectangle (no text is legible — the user's constraint is a visual
     *  one, and one single tinted glyph reads as a plain colour block). */
    private static final String BACKING_GLYPH_CHAR = "W";

    /** Channel N2's private draw buffer — a DEDICATED BufferBuilder, NOT
     *  the shared Tessellator: channel N2's platform half schedules on
     *  MTR's LIGHT queue, whose keyed warm-up starts a batch on the
     *  queue holder's own immediate over the Tessellator singleton — a
     *  private immediate over the same singleton would collide with that
     *  building batch on begin(). A dedicated buffer makes the synchronous
     *  flush self-contained (the frame's only allocation is the light
     *  Immediate wrapper, the same one every MTR drawText constructs). */
#if LOADER == "fabric"
    private static final net.minecraft.client.render.BufferBuilder N2_BUFFER = new net.minecraft.client.render.BufferBuilder(1024);
#else
    private static final com.mojang.blaze3d.vertex.BufferBuilder N2_BUFFER = new com.mojang.blaze3d.vertex.BufferBuilder(1024);
#endif

    /** Distance² gate — bindings beyond this are not driven. */
    private static final double MAX_DISTANCE_SQ = 192 * 192;

    /** One-shot breadcrumbs per channel (yanyang.12 heritage): the
     *  first EXCEPTION and the first successful execution are each
     *  logged once, so the next field log proves which channel ran.
     *  (Channel N is the round-11 text-layer quad carrier; carrier B
     *  the EXTERIOR route-sign pipeline; channel N2 the round-12
     *  glyph-pipeline carrier.) */
    private static volatile boolean BACKING_T_WARNED = false;
    private static volatile boolean BACKING_T_OK = false;
    private static volatile boolean BACKING_G2_WARNED = false;
    private static volatile boolean BACKING_G2_OK = false;
    private static volatile boolean BACKING_B_WARNED = false;
    private static volatile boolean BACKING_B_OK = false;

    private PSDScriptRenderer() {
    }

    /** Called from MainRendererPSDMixin at the head of MainRenderer.render.
     * Everything drawn here is SCHEDULED onto MTR's queued layers and
     * flushed within that same render call on MTR's own graphics holder;
     * camera anchoring comes from the queued callback's offset at flush
     * time. The backing quads ride TWO proven-visible carriers — the
     * keyed LIGHT_2 script-rect pipeline and the keyed EXTERIOR
     * route-sign pipeline (2.3.0-1.0.2; see drawBacking). */
    public static void renderAll() {
        final MinecraftClient minecraftClient = MinecraftClient.getInstance();
        /* MTR's mapping wrappers are unrelated types — ClientWorld reaches the
           World-typed helpers (native driver, InitClient) through World.cast. */
        final World world = World.cast(minecraftClient.getWorldMapped());
        final ClientPlayerEntity player = minecraftClient.getPlayerMapped();
        if (world == null || player == null) return;
        if (PSDClientBindings.isEmpty()) return;

        final boolean shadowPass = OptimizedRenderer.renderingShadows();
        /* Rounds 10/11: channel N (the no-depth-test text-layer quad
           backing) is gated to shader pipelines — the full story is on
           drawBackingText, the short version: without shaders the
           depth-tested carrier B already carries the correct sandwich
           (script images/shapes/text keep their proven layering over
           it), while a no-depth full-window rect on the TEXT queue
           would flush LAST (MTR drains the queues by
           QueuedRenderLayer ordinal, TEXT = 8 = last) and paint over
           everything the script drew on the depth-tested queues.
           Under a shader pack those queues' content behind the pane
           is discarded by the pipeline anyway (ten rounds proved
           it), so there is nothing visible left to cover — and the
           text still draws over the black (same queue, later
           submission). One reflective query per frame, no per-segment
           cost. */
        final boolean shaderPack = !shadowPass && ShaderDetector.isShaderPackInUse();
        final double playerX = player.getX();
        final double playerY = player.getY();
        final double playerZ = player.getZ();

        /* One canvas per wall section per pass (yanyang.6): the editor binds
           EVERY block of a glass segment / psd_top group, and legacy worlds
           may still hold per-column bindings — whichever binding of a
           section comes first renders it, the rest are skipped. */
        final Set<BlockPos> renderedSections = new HashSet<>();

        PSDClientBindings.forEach(binding -> {
            final BlockPos pos = binding.pos;
            final double dx = pos.getX() + 0.5 - playerX;
            final double dy = pos.getY() + 0.5 - playerY;
            final double dz = pos.getZ() + 0.5 - playerZ;
            if (dx * dx + dy * dy + dz * dz > MAX_DISTANCE_SQ) return;

            final BlockState state = world.getBlockState(pos);
            final Object block = state.getBlock().data;
            if (binding.type == PSDWrapper.PSDType.TOP) {
                if (block instanceof BlockPSDTop) {
                    renderTop(binding, pos, state, world, shadowPass, renderedSections);
                }
            } else {
                if (block instanceof BlockPSDAPGGlassBase) {
                    renderGlass(binding, pos, state, world, shadowPass, shaderPack, renderedSections);
                }
            }
        });
    }

    /* ==================================================================== */
    /* Glass                                                                 */
    /* ==================================================================== */

    private static void renderGlass(PSDBinding binding, BlockPos pos, BlockState state, World world, boolean shadowPass, boolean shaderPack, Set<BlockPos> renderedSections) {
        final Direction facing = IBlock.getStatePropertySafe(state, BlockPSDAPGBase.FACING);
        final boolean upper = IBlock.getStatePropertySafe(state, IBlock.HALF) == IBlock.DoubleBlockHalf.UPPER;

        /* Yanyang.6: the segment is identified by its LOWER row — a binding
           on either half of any column normalizes to the same anchor, and
           the whole segment renders exactly ONE canvas per pass.
           Previously every bound half drew its own row canvas, so a
           two-block glass displayed the script TWICE (once per half) —
           the two halves are ONE glass and now share one canvas. */
        BlockPos base = pos;
        if (upper && PSDRuns.isGlass(world.getBlockState(pos.down()))) {
            base = pos.down();
        }
        final PSDRuns.Run run = PSDRuns.glassRun(world, base, facing);
        if (!renderedSections.add(run.start)) {
            return; // another binding of this segment already rendered it
        }

        /* The pane's transparent window, per block, straight from the baked
           model + texture alpha (see PSDWindowBounds) — pack-dependent, so
           nothing is hardcoded. v2 (yanyang.5) derives the window from
           OCCLUSION — every pane-parallel quad's opaque rows count as
           frame, whether they belong to the pane's own texture or to
           separate frame/strip elements — and reports the pane's plane
           depth so the sandwich follows the pack's pane position instead
           of assuming the vanilla 4/16. */
        final int width = run.width;
        final float[] lowerTop = new float[width];
        final float[] lowerBottom = new float[width];
        final boolean[] lowerValid = new boolean[width];
        final float[] lowerExtentTop = new float[width];
        final float[] lowerExtentBottom = new float[width];
        /* Yanyang.10: the per-column HORIZONTAL window (block space,
           [left, right] ⊂ [0,1]) — the strict alpha scan's horizontal
           twin, so the canvas can dodge the door frame's vertical
           stiles at the run's ends exactly like it dodges the
           horizontal bands vertically. */
        final float[] lowerHLeft = new float[width];
        final float[] lowerHRight = new float[width];
        final float[] upperTop = new float[width];
        final float[] upperBottom = new float[width];
        final boolean[] upperValid = new boolean[width];
        final boolean[] upperIncludable = new boolean[width];
        final float[] upperExtentTop = new float[width];
        final float[] upperExtentBottom = new float[width];
        float paneAlong = PSDWindowBounds.DEFAULT_PANE_ALONG;
        boolean allUppers = true;
        for (int i = 0; i < width; i++) {
            final BlockPos lowerPos = run.start.offset(facing.rotateYClockwise(), i);
            final PSDWindowBounds.Bounds lowerBounds = PSDWindowBounds.window(world.getBlockState(lowerPos), facing);
            lowerTop[i] = Math.min(lowerBounds.top, 1);
            lowerBottom[i] = Math.max(lowerBounds.bottom, 0);
            lowerValid[i] = lowerTop[i] - lowerBottom[i] >= MIN_WINDOW;
            lowerExtentTop[i] = lowerBounds.extentTop;
            lowerExtentBottom[i] = lowerBounds.extentBottom;
            lowerHLeft[i] = lowerBounds.left;
            lowerHRight[i] = lowerBounds.right;
            /* The sandwich sits just behind the DEEPEST pane in the run so
               every block's pane stays in front of it. */
            paneAlong = Math.max(paneAlong, lowerBounds.paneAlong);

            /* The column's upper half joins the merged frame only when it is
               a display surface: a glass block whose model actually shows
               something. DRL uppers whose vanilla model is fully invisible
               never join (content would float in mid-air — the yanyang.2
               behaviour); a pack that gives them visible geometry (the
               yanyang.4 user pack's full-height screen doors) flips this
               gate and the full column becomes one display. */
            boolean includable = false;
            final BlockState upperState = world.getBlockState(lowerPos.up());
            final Object upperBlock = upperState.getBlock().data;
            if (upperBlock instanceof BlockPSDAPGGlassBase) {
                final PSDWindowBounds.Bounds upperBounds = PSDWindowBounds.window(upperState, facing);
                final boolean drlUpper = upperBlock instanceof APGGlassDRL || upperBlock instanceof APGGlassEndDRL;
                includable = !drlUpper || upperBounds.visibleGeometry;
                if (includable) {
                    upperTop[i] = Math.min(upperBounds.top, 1);
                    upperBottom[i] = Math.max(upperBounds.bottom, 0);
                    upperValid[i] = upperTop[i] - upperBottom[i] >= MIN_WINDOW;
                    upperExtentTop[i] = upperBounds.extentTop;
                    upperExtentBottom[i] = upperBounds.extentBottom;
                    /* The column's horizontal window is the INTERSECTION of
                       both rows' windows — a stile in either row clips the
                       column for the whole merged frame. */
                    lowerHLeft[i] = Math.max(lowerHLeft[i], upperBounds.left);
                    lowerHRight[i] = Math.min(lowerHRight[i], upperBounds.right);
                    paneAlong = Math.max(paneAlong, upperBounds.paneAlong);
                }
            }
            upperIncludable[i] = includable;
            if (!includable) {
                allUppers = false;
            }
        }
        /* The vertical frame spans both rows only when EVERY column's upper
           half is a display surface (walls are uniform) — a single
           missing/invisible upper would leave canvas floating in open air
           above that column. */
        final float frameHeight = allUppers ? 2 : 1;

        /* The shared window: the union of every column's windows — lower
           row at [0,1] of the merged frame, upper row at [1,2]. Canvas
           coordinates: y=0 at the frame top, y grows downward; the canvas
           is clipped to the window, so script content (which lays out
           against pids.height) never lands on the frame strips. */
        float windowTop = Float.NEGATIVE_INFINITY;
        float windowBottom = Float.POSITIVE_INFINITY;
        for (int i = 0; i < width; i++) {
            if (lowerValid[i]) {
                windowTop = Math.max(windowTop, lowerTop[i]);
                windowBottom = Math.min(windowBottom, lowerBottom[i]);
            }
            if (frameHeight > 1 && upperValid[i]) {
                windowTop = Math.max(windowTop, 1 + upperTop[i]);
                windowBottom = Math.min(windowBottom, 1 + upperBottom[i]);
            }
        }
        if (windowTop - windowBottom < MIN_WINDOW) {
            /* Nothing derivable anywhere — the full merged face, the
               pre-clipping fallback. */
            windowTop = frameHeight;
            windowBottom = 0;
        }

        /* The horizontal window (yanyang.10; edge semantics fixed
           yanyang.11): PSDWindowBounds reports each column's window as
           BLOCK-SPACE EDGES [left, right] along the pane's horizontal
           axis (block X when the wall faces Z, block Z when it faces X).
           This loop maps those edges into RUN coordinates: the canvas
           frame's +X runs along facing.rotateYClockwise(), so when that
           walk OPPOSES the block's horizontal axis (SOUTH / WEST walls)
           the block-space window maps as [i+(1-right), i+(1-left)],
           otherwise as [i+left, i+right]. The canvas then maps onto the
           LARGEST CONTIGUOUS such span across the run (adjacent columns'
           windows must actually touch — a mid-run vertical stile splits
           the run and the canvas takes the wider side, exactly like the
           vertical scan takes the largest unoccluded band). This is the
           fix for the yanyang.9 report 「脚本内容越界问题依然存在」:
           the canvas used to span every full column, so the script's own
           row layout (SIDE_PADDING of 3 px against pids.width) put the
           first/last glyph of every row right under the door frame's
           end stiles — half a "海" swallowed left, half a "钟" right.
           With the canvas mapped onto the transparent span, pids.width
           IS the visible width and every script's layout adapts by
           itself. (The yanyang.10 formula computed the right edge as
           `(i+1)-right`, treating the window's right EDGE as an
           occlusion WIDTH — every column with a real window collapsed
           to [i+left, i+left] or thinner, "fully occluded" split the
           span, and the canvas degenerated into a sliver at a wrong
           position: the 2026-11-08 「位置完全跑偏，比例完全错误」
           report.) Falls back to the full run (the pre-scan behaviour)
           when no span of MIN_WINDOW survives. */
        final Direction hWalk = facing.rotateYClockwise();
        final boolean hMirror = (facing.getAxis() == Axis.Z ? hWalk.getOffsetX() : hWalk.getOffsetZ()) < 0;
        float segLeft = 0;
        float segRight = width;
        {
            float bestL = 0, bestR = -1, curL = -1, curR = -1;
            for (int i = 0; i < width; i++) {
                final float a = hMirror ? i + (1 - lowerHRight[i]) : i + lowerHLeft[i];
                final float b = hMirror ? i + (1 - lowerHLeft[i]) : i + lowerHRight[i];
                if (b - a < MIN_WINDOW) {
                    /* Column fully occluded — splits the span. */
                    if (curL >= 0 && curR - curL > bestR - bestL) {
                        bestL = curL;
                        bestR = curR;
                    }
                    curL = -1;
                } else if (curL < 0) {
                    curL = a;
                    curR = b;
                } else if (a <= curR + 0.015625F) {
                    /* Touching/overlapping the current span (1/64 slack for
                       scan rounding) — extend it. */
                    curR = Math.max(curR, b);
                } else {
                    if (curR - curL > bestR - bestL) {
                        bestL = curL;
                        bestR = curR;
                    }
                    curL = a;
                    curR = b;
                }
            }
            if (curL >= 0 && curR - curL > bestR - bestL) {
                bestL = curL;
                bestR = curR;
            }
            if (bestR - bestL >= MIN_WINDOW) {
                segLeft = bestL;
                segRight = bestR;
            }
        }

        final int canvasWidth = Math.max(8, Math.round((segRight - segLeft) * 96));
        final float canvasLeft = segLeft;
        final int canvasHeight = Math.max(8, Math.round((windowTop - windowBottom) * 96));
        final float canvasTop = frameHeight - windowTop;

        /* The sandwich planes track the run's pane depth: the script
           canvas 0.25/16 behind the pane's platform-side face, the black
           backing 0.45/16 behind it (vanilla pane → 4.25/16 and 4.45/16;
           both in the pane slab's front, ahead of any pack-placed
           panel — see the constants' javadoc). */
        final float scriptZ = paneAlong + GLASS_SCRIPT_OFFSET;
        final float backingZ = paneAlong + GLASS_BACKING_OFFSET;

        /* The backing plane's transform, hoisted out of the shadow gate so
           channel N2's track half (scheduled after replayScript, below) can
           share it with everything drawn inside the gate. */
        final StoredMatrixTransformations backing = glassTransform(run.start, facing, backingZ, frameHeight);

        if (!shadowPass) {
            /* Opaque covering, one two-quad pair per block and row — but at
               the pane's FULL GEOMETRIC EXTENT (yanyang.9), NOT the strict
               alpha window: the covering is a flat quad BEHIND the pane, so
               every opaque part of the pane/frame in front hides any excess
               while the pane's semi-transparent edge bands (the rows the
               strict scan excludes as "frame") finally get solid colour
               behind them — the yanyang.8 leftover see-through strips at the
               window's top/bottom edges (user report 2026-11-08 「黑色覆盖
               不完全」). Each side carries its own tint on its own winding
               through carrier B (2026-11-10, see drawBacking):
               BLACK toward the platform, WHITE toward the track (train
               passengers see a plain white panel, never mirrored content
               — see TRACK_SIDE_COLOR). */
            /* Channel N (2026-11-10, round 11 — see drawBackingText),
               ONLY on shader pipelines: the NORMAL text layer — the
               one machine that provably draws behind the user's Iris
               + Complementary stack — carrying ONE window-clipped
               quad per view (BLACK toward the platform, WHITE toward
               the track) with NO text drawing at all. The window span
               is the same rect the script canvas maps onto: the layer
               blends over the pane (flushed after it), so anything
               wider would paint the pane frame. Without shaders the
               depth-tested carrier B already carries the correct
               sandwich, and channel N's no-depth rect would cover the
               script's images/shapes (they flush earlier — LIGHT
               ordinal 0 vs TEXT ordinal 8 — and cannot defend
               themselves against a layer that ignores depth). Skipped
               when no window could be derived (carrier B alone, the
               pre-N behaviour). */
            if (shaderPack && windowTop - windowBottom >= MIN_WINDOW) {
                drawBackingText(backing, segLeft, segRight, windowBottom, windowTop, frameHeight, run.start, facing);
                /* Channel N2 platform half (2026-11-11, round 12 — see
                   drawBackingGlyph2): the BLACK backing drawn by the glyph
                   pipeline itself on the NORMAL layer, scheduled on the
                   LIGHT queue BEFORE driveScript submits the script's
                   image/shape callbacks — the same queue, FIFO — so it
                   flushes first and every script element (images and
                   shapes on LIGHT after it, text on TEXT) paints OVER
                   it: the correct sandwich order, and unlike a TEXT-
                   queue quad it can never cover the script's images.
                   The layer tests no depth and writes none, but it
                   flushes BEFORE the translucent pane (entity phase,
                   ahead of endTranslucents) — the pane still blends
                   over it, the same "behind the glass" look carrier B
                   gives without shaders. */
                drawBackingGlyph2(backing, segLeft, segRight, windowBottom, windowTop, frameHeight, run.start, facing, false);
            }
            for (int i = 0; i < width; i++) {
                if (lowerExtentTop[i] > lowerExtentBottom[i]) {
                    drawBacking(backing, i, lowerExtentTop[i], lowerExtentBottom[i], frameHeight, facing);
                }
                if (frameHeight > 1 && upperIncludable[i] && upperExtentTop[i] > upperExtentBottom[i]) {
                    drawBacking(backing, i, 1 + upperExtentTop[i], 1 + upperExtentBottom[i], frameHeight, facing);
                }
            }
            driveScript(binding, pos, world, canvasWidth, canvasHeight);
        }

        /* The script canvas sits just behind the platform-side pane face:
           the translucent pane blends over it, so the content shows exactly
           through the pane's originally transparent window while every
           opaque pixel of the pane texture (frame, bars, frosted bands —
           whatever the resource pack draws) stays visible in front of it. */
        final StoredMatrixTransformations scriptSide = glassTransform(run.start, facing, scriptZ, frameHeight);
        scriptSide.add(graphicsHolder -> graphicsHolder.translate(canvasLeft, canvasTop, 0));
        replayScript(binding, pos, world, scriptSide, facing.getOpposite());
        if (!shadowPass) {
            /* Channel N2 track half (round 12): scheduled AFTER
               replayScript, so within the TEXT queue's shared "" list
               it sits behind every script text callback — it flushes
               LAST and paints the WHITE panel over everything the
               track-side view would otherwise leak (mirrored text,
               flipped images): the same "plain white panel" semantics
               carrier B's depth-tested white quad gives on no-shader
               pipelines, expressed on the layer that provably renders
               under the user's shader stack. The camera-side gate
               inside the callback keeps it inert on the platform
               view. */
            if (shaderPack && windowTop - windowBottom >= MIN_WINDOW) {
                drawBackingGlyph2(backing, segLeft, segRight, windowBottom, windowTop, frameHeight, run.start, facing, true);
            }
            replaySoundsOnce(binding, pos, world);
        }
    }

    /**
     * The canvas frame for the glass sandwich: the origin sits at the top-left
     * of the pane face as seen from the platform side, y runs down, and +Z
     * points toward the track (deeper into the pane). {@code frameHeight} is
     * the number of block rows the canvas spans (2 for a merged full-height
     * glass, 1 otherwise). All sandwich planes (script at 4.25/16, backing
     * at 4.45/16 for the vanilla pane) use the same frame — only the z
     * translation
     * differs.
     */
    private static StoredMatrixTransformations glassTransform(BlockPos pos, Direction facing, float z, float frameHeight) {
        final StoredMatrixTransformations transform = new StoredMatrixTransformations(0.5 + pos.getX(), pos.getY(), 0.5 + pos.getZ());
        transform.add(graphicsHolder -> graphicsHolder.rotateYDegrees(-facing.asRotation()));
        transform.add(graphicsHolder -> graphicsHolder.translate(0, frameHeight, 0));
        transform.add(graphicsHolder -> graphicsHolder.rotateZDegrees(180));
        transform.add(graphicsHolder -> graphicsHolder.translate(-0.5, 0, z));
        return transform;
    }

    /* ==================================================================== */
    /* Scheduled backing draw                                                */
    /* ==================================================================== */

    /**
     * The opaque pane covering — one block-wide quad (canvas x ∈ [x, x+1],
     * y ∈ [frameTop-top, frameTop-bottom] in the merged frame's canvas
     * coordinates), drawn as a two-winding pair (yanyang.8/9's proven
     * visual design: the platform-facing winding carries {@link
     * #BACKING_COLOR} (black), the track-facing winding {@link
     * #TRACK_SIDE_COLOR} (white) a hair deeper — each side sees exactly
     * its own quad, and the white quad's depth fails the canvas content's
     * LEQUAL test on the track side, so script content never leaks
     * through to the track).
     *
     * <p><b>Carrier B (2026-11-10) — the route-sign pipeline, kept for
     * every pipeline where depth-tested quads render at all</b> (that is
     * every no-shader setup, where this carrier has carried the correct
     * two-colour look since yanyang.9): the KEYED {@code
     * scheduleRender(BACKING_TEXTURE, false, EXTERIOR, …)}, so the
     * queue's warm-up pre-creates the cached {@code
     * getExterior(BACKING_TEXTURE)} entity-cutout consumer, and the
     * callback is transform → {@code drawTextureInWorld} → pop, nothing
     * else — RenderRouteBase's discipline verbatim.</p>
     *
     * <p><b>Why every depth-tested carrier failed under the user's
     * shaders — the ten-round post-mortem</b>: the user's stack (Iris
     * 1.7.6 + ComplementaryUnbound r5.5.1 + ImmediatelyFast, on YYMTR
     * 4.0.5) has now eaten, behind the pane, EVERY quad ever submitted
     * through a depth-tested layer — the MTR queue consumers in all
     * their forms (yanyang.8–10 fresh consumers, yanyang.13 beacon-beam,
     * 2.3.0-1.0.0/1.0.2 EXTERIOR keyed, 1.0.2 LIGHT_2 keyed) AND the
     * yanyang.11/12 private immediates, which wrote through {@code
     * drawTextureInWorld} — a writer whose OVERLAY element lands in the
     * text format's LIGHTMAP slot and whose trailing normal() overruns
     * the stride, corrupting the quad (the 2.3.0-1.0.1 explanation).
     * The ONE thing that has rendered on these very panes under these
     * very shaders, every version since yanyang.1, is the script text
     * itself — {@code TextWrapper} → MTR's TEXT queue → {@code
     * GraphicsHolder.drawText} → the real {@code TextRenderer.draw} on
     * the NORMAL text layer (bytecode-verified 2026-11-10: {@code
     * TextLayerType.field_33993} = NORMAL, both text layers carry
     * DepthTest "always" and a color-only write mask) through a private
     * Tessellator immediate flushed synchronously inside the callback.
     * Rounds 9/10 drew channel T on the SEE_THROUGH variant — a
     * different program pair — and stayed invisible; round 11's channel
     * N ({@link #drawBackingText}) rides the NORMAL layer itself, as a
     * plain quad; this carrier remains for every pipeline where
     * depth-tested quads render at all (no-shader setups).</p>
     *
     * <p>Per winding order the BLACK platform quad is submitted first,
     * the WHITE track quad second (yanyang.13's order); the canvas sits
     * 0.2/16 in front of the backing plane, so content always draws
     * over it.</p>
     */
    private static void drawBacking(StoredMatrixTransformations transform, float x, float top, float bottom, float frameTop, Direction facing) {
        final float y = frameTop - top;
        final float height = top - bottom;

        /* Carrier B — RenderRouteBase's exact recipe: the KEYED EXTERIOR
           queue pre-creates the cached entity-cutout consumer; the
           callback only transforms, draws and pops. */
        MainRenderer.scheduleRender(BACKING_TEXTURE, false, QueuedRenderLayer.EXTERIOR, (graphicsHolderNew, offset) -> {
            try {
                transform.transform(graphicsHolderNew, offset);
                /* Platform-facing BLACK. */
                graphicsHolderNew.drawTextureInWorld(x, y + height, CARRIER_B_Z, x + 1, y + height, CARRIER_B_Z, x + 1, y, CARRIER_B_Z, x, y, CARRIER_B_Z, 0, 0, 1, 1, facing.getOpposite(), BACKING_COLOR, MAX_RENDER_LIGHT);
                /* Track-facing WHITE, a hair deeper. */
                graphicsHolderNew.drawTextureInWorld(x, y, TRACK_BACK_Z + CARRIER_B_Z, x + 1, y, TRACK_BACK_Z + CARRIER_B_Z, x + 1, y + height, TRACK_BACK_Z + CARRIER_B_Z, x, y + height, TRACK_BACK_Z + CARRIER_B_Z, 0, 0, 1, 1, facing, TRACK_SIDE_COLOR, MAX_RENDER_LIGHT);
                graphicsHolderNew.pop();
                if (!BACKING_B_OK) {
                    BACKING_B_OK = true;
                    com.lx862.mtrscripting.mod.MTRScriptingModClient.LOGGER.info(
                            "[JCM] PSD backing carrier B active (route-sign pipeline)");
                }
            } catch (Throwable t) {
                if (!BACKING_B_WARNED) {
                    BACKING_B_WARNED = true;
                    com.lx862.mtrscripting.mod.MTRScriptingModClient.LOGGER.warn(
                            "[JCM] PSD backing carrier B failed once: " + t, t);
                }
            }
        });
    }

    /* ==================================================================== */
    /* Text-layer quad backing (channel N)                                  */
    /* ==================================================================== */

    /**
     * Channel N (2026-11-10, round 11) — the backing as ONE plain textured
     * quad on {@code RenderLayer.getText(jsblock:white.png)}, the NORMAL
     * text layer: the exact layer, program ({@code rendertype_text}),
     * texture-in-layer binding, translucent transparency, ALWAYS depth
     * test and color-only write mask that every VISIBLE glyph on these
     * panes rides (script text AND MTR's own PIDS text — bytecode-verified
     * against the vanilla 1.20.1 jar: both text layers share DepthTest
     * "always" 519 and WriteMask color=true/depth=false; the NORMAL and
     * SEE_THROUGH variants differ only in program and are picked by
     * {@code TextRenderer.draw} as {@code TextLayerType.field_33993} =
     * NORMAL — what MTR's {@code GraphicsHolder.drawText} passes).
     *
     * <p><b>No text is drawn at all</b> (the user's 2026-11-10 「请尽量避
     * 免使用文字填充黑色底色」): the quad is written by hand with the
     * exact glyph vertex sequence — vertex → color → texture → light →
     * next, NO overlay, NO normal — the discipline of 1.0.1's never-shipped
     * {@code glyphQuad} (compile-fixed in 7f07a43), NOT {@code
     * drawTextureInWorld} (whose OVERLAY write lands in this format's
     * LIGHTMAP slot and whose trailing normal() overruns the stride —
     * the yanyang.11/12 trap that masqueraded as "the layer is
     * invisible").</p>
     *
     * <p><b>Why channel T (rounds 9–10) was invisible</b>: it drew on the
     * SEE_THROUGH variant ({@code getTextSeeThrough}, program {@code
     * rendertype_translucent_text} + its background rect layer) — a
     * DIFFERENT program pair that never rendered on the user's Iris
     * 1.7.6 + ComplementaryUnbound r5.5.1 stack, while the NORMAL layer
     * renders every script glyph and every MTR PIDS line daily. The
     * immediate discipline (a private {@code VertexConsumerProvider.
     * immediate(Tessellator.getInstance().getBuffer())} flushed
     * synchronously inside the callback) is MTR {@code drawText}'s own,
     * disassembled and replicated byte for byte; it is kept verbatim.</p>
     *
     * <p><b>Sides</b> (「另外请注意轨道侧还有一层白色底色」): the callback
     * gates on the camera side of the pane — platform side draws the
     * BLACK platform winding, track side the WHITE track winding
     * (a hair deeper, {@link #TRACK_BACK_Z}). One quad per view; each
     * winding faces its own camera, so no cull-state or depth-order
     * assumption is needed (the layer tests no depth and writes none).
     * Under the user's shaders carrier B stays invisible, so this
     * channel now carries BOTH sides.</p>
     *
     * <p><b>Clip</b>: the quad spans the derived transparent window
     * ([left, right] × [bottom, top]) — the same rect the script canvas
     * maps onto. The layer flushes AFTER the translucent pane (Iris
     * GENERAL_TRANSPARENT bucket), so it blends over the pane's pixels;
     * anything wider than the window would paint the pane frame, and
     * the window is exactly where content may legally sit.</p>
     *
     * <p><b>Ordering vs content</b>: scheduled on MTR's TEXT queue with
     * the shared "" key, BEFORE {@code replayScript} submits the text
     * callbacks — same queue, same key list, FIFO — and flushed
     * synchronously inside this callback, so the black always lands
     * before the glyphs and the content draws over it. The script's
     * images/shapes ride the depth-tested LIGHT queue (ordinal 0,
     * flushed earlier) exactly as before — untouched by this channel,
     * which stays gated to shader pipelines (see the renderGlass call
     * site): without shaders carrier B already carries the correct
     * sandwich, and a no-depth rect here would flush LAST (TEXT ordinal
     * 8) and paint over those images/shapes.</p>
     *
     * <p><b>Cost</b> (「请确保该解决方案不会出现卡顿」): one quad (two
     * triangles) per segment per frame, on a layer vanilla memoizes per
     * texture identifier ({@code Util.memoize} inside {@code
     * RenderLayer.getText}) — no glyph rows, no per-frame string work,
     * and the only per-frame allocation is the same tiny Immediate
     * wrapper every MTR {@code drawText} call already constructs.</p>
     */
    private static void drawBackingText(StoredMatrixTransformations transform, float left, float right, float bottom, float top, float frameTop, BlockPos anchor, Direction facing) {
        final float y = frameTop - top;
        final float height = top - bottom;
        final float width = right - left;
        final double anchorX = anchor.getX() + 0.5;
        final double anchorY = anchor.getY() + 0.5;
        final double anchorZ = anchor.getZ() + 0.5;
        MainRenderer.scheduleRender(QueuedRenderLayer.TEXT, (graphicsHolderNew, offset) -> {
            try {
                /* Camera-side gate: facing points toward the TRACK, so a
                   camera with along = dot(camera - blockCenter, facing)
                   ≥ 0.25 is on (or past) the pane plane on the track
                   side — that view gets the WHITE quad; the platform
                   view (the normal one) the BLACK quad. Exactly one
                   quad is drawn per view. */
                final org.mtr.mapping.holder.Vector3i fv = facing.getVector();
                final double along = (offset.getXMapped() - anchorX) * fv.getX()
                        + (offset.getYMapped() - anchorY) * fv.getY()
                        + (offset.getZMapped() - anchorZ) * fv.getZ();
                final boolean trackSide = along >= 0.25;
#if LOADER == "fabric"
                /* The drawText carrier, step by step (disassembled from
                   MTR's GraphicsHolder.drawText): borrow the callback
                   holder's raw pose, build a private immediate over the
                   shared Tessellator — the exact source drawText
                   constructs every call — take the NORMAL text layer
                   bound to our own opaque white, write the quad with the
                   glyph vertex sequence, and flush synchronously, all
                   inside this callback. */
                final net.minecraft.client.util.math.MatrixStack matrices =
                        ((com.lx862.jcm.nativeapi.NativeMatrixApplier) (Object) graphicsHolderNew).jcm$getMatrixStack();
                final net.minecraft.client.render.VertexConsumerProvider.Immediate immediate =
                        net.minecraft.client.render.VertexConsumerProvider.immediate(net.minecraft.client.render.Tessellator.getInstance().getBuffer());
                GraphicsHolder.createInstanceSafe(matrices, immediate, priv -> {
                    transform.transform(priv, offset);
#if MC_VERSION >= "11903"
                    final org.joml.Matrix4f model = matrices.peek().getPositionMatrix();
#else
#if MC_VERSION <= "11701"
                    final net.minecraft.util.math.Matrix4f model = matrices.peek().getModel();
#else
                    final net.minecraft.util.math.Matrix4f model = matrices.peek().getPositionMatrix();
#endif
#endif
                    final net.minecraft.client.render.VertexConsumer vc = immediate.getBuffer(
                            net.minecraft.client.render.RenderLayer.getText(new net.minecraft.util.Identifier("jsblock", "textures/block/white.png")));
                    if (trackSide) {
                        /* WHITE toward the track — the user-requested
                           second layer, at the same hair-deeper plane
                           carrier B uses. */
                        glyphQuad(vc, model, left, y, height, width, TRACK_BACK_Z, TRACK_SIDE_COLOR, false);
                    } else {
                        /* BLACK toward the platform. */
                        glyphQuad(vc, model, left, y, height, width, 0, BACKING_COLOR, true);
                    }
                    priv.pop();
                });
                immediate.draw();
#else
                final com.mojang.blaze3d.vertex.PoseStack matrices =
                        ((com.lx862.jcm.nativeapi.NativeMatrixApplier) (Object) graphicsHolderNew).jcm$getMatrixStack();
                final net.minecraft.client.renderer.MultiBufferSource.BufferSource immediate =
                        net.minecraft.client.renderer.MultiBufferSource.immediate(com.mojang.blaze3d.vertex.Tesselator.getInstance().getBuilder());
                GraphicsHolder.createInstanceSafe(matrices, immediate, priv -> {
                    transform.transform(priv, offset);
#if MC_VERSION >= "11903"
                    final org.joml.Matrix4f model = matrices.last().pose();
#else
                    final com.mojang.math.Matrix4f model = matrices.last().pose();
#endif
                    final com.mojang.blaze3d.vertex.VertexConsumer vc = immediate.getBuffer(
                            net.minecraft.client.renderer.RenderType.text(new net.minecraft.resources.ResourceLocation("jsblock", "textures/block/white.png")));
                    if (trackSide) {
                        /* WHITE toward the track. */
                        glyphQuad(vc, model, left, y, height, width, TRACK_BACK_Z, TRACK_SIDE_COLOR, false);
                    } else {
                        /* BLACK toward the platform. */
                        glyphQuad(vc, model, left, y, height, width, 0, BACKING_COLOR, true);
                    }
                    priv.pop();
                });
                immediate.endBatch();
#endif
                if (!BACKING_T_OK) {
                    BACKING_T_OK = true;
                    com.lx862.mtrscripting.mod.MTRScriptingModClient.LOGGER.info(
                            "[JCM] PSD backing channel N active (text-layer quad, shader pack in use)");
                }
            } catch (Throwable t) {
                /* Never take the render thread down over a backing draw —
                   but the FIRST failure still leaves a breadcrumb
                   (yanyang.12 heritage). */
                if (!BACKING_T_WARNED) {
                    BACKING_T_WARNED = true;
                    com.lx862.mtrscripting.mod.MTRScriptingModClient.LOGGER.warn(
                            "[JCM] PSD backing channel N failed once: " + t, t);
                }
            }
        });
    }

    /* ==================================================================== */
    /* Glyph-pipeline backing (channel N2, round 12)                        */
    /* ==================================================================== */

    /**
     * Channel N2 (2026-11-11, round 12) — the backing drawn BY THE GLYPH
     * MACHINE ITSELF, on the layer type every VISIBLE script glyph rides:
     * {@code TextRenderer.draw(char, 0, 0, color, shadow=false, matrix,
     * immediate, NORMAL, backgroundColor=color, light)} — one single
     * {@link #BACKING_GLYPH_CHAR} whose pixels AND Drawer background are
     * tinted the same colour, non-uniformly scaled to exactly the
     * transparent window. Rounds 9–10 tried this machine on the
     * SEE_THROUGH layer (a different program pair that never rendered on
     * the user's stack); round 11 proved the NORMAL layer's OWN program
     * renders every script glyph daily — but its hand-written quad clone
     * (channel N) still came back invisible on EVERY shader pack (field
     * report on build.92, 2026-11-11: both breadcrumbs present, no
     * exceptions, still invisible), leaving exactly one untested
     * variable: the glyph machine's own draw path (ImmediatelyFast
     * intercepts TextRenderer draws into its font-atlas batches — the
     * hand-written BufferBuilder route may simply have no visible
     * precedent on this stack). Channel N2 removes that variable by
     * riding the machine verbatim: same TextRenderer, same font atlas
     * binding, same Drawer, same NORMAL layer choice MTR's
     * {@code GraphicsHolder.drawText} passes
     * ({@code TextLayerType.field_33993} = NORMAL — the round-11 bytecode
     * finding), producing BOTH a glyph quad (rendertype_text) and a
     * background rectangle (rendertype_text_background — the nametag
     * program) in one call: two independently-proven carriers of the
     * same solid colour.
     *
     * <p><b>No text is legible</b> (the user's 2026-11-10 constraint 「请
     * 尽量避免使用文字填充黑色底色」): ONE character, stretched ~100×
     * past legibility, with its glyph tinted the same colour as its
     * background — the result is an undifferentiated solid rectangle,
     * indistinguishable from a plain quad. This is NOT a grid of glyph
     * rows (the round-9 approach the constraint rejected); it is a
     * single tinted rectangle whose carrier happens to be the glyph
     * pipeline — chosen because that pipeline is the ONLY thing proven
     * to render behind the user's shaders, not for its typography.</p>
     *
     * <p><b>Sides and queues</b> (「另外请注意轨道侧还有一层白色底色」):
     * each half schedules on the queue whose flush order gives it the
     * correct sandwich position —</p>
     * <ul>
     * <li><b>Platform half (BLACK)</b> — scheduled on the LIGHT queue
     * (ordinal 0) BEFORE {@code driveScript} submits the script's
     * image/shape callbacks: it flushes FIRST, every script element
     * (images and shapes behind it in the same queue's FIFO list, text
     * on the later TEXT queue) paints over it, and the translucent pane
     * still blends over it (the entity phase runs ahead of
     * endTranslucents) — the exact sandwich order carrier B gives
     * without shaders, and unlike a TEXT-queue rect it can never cover
     * the script's images. The callback's camera-side gate renders it
     * only on the platform view.</li>
     * <li><b>Track half (WHITE)</b> — scheduled on the TEXT queue AFTER
     * {@code replayScript} has submitted the text callbacks: within the
     * shared "" list (FIFO) it flushes LAST, painting the plain white
     * panel over everything the track-side view would otherwise leak —
     * mirrored text, flipped images — the same semantics carrier B's
     * depth-tested white quad gives without shaders, expressed on the
     * layer that provably renders under the user's shaders.</li>
     * </ul>
     *
     * <p><b>Geometry</b>: the window rect [left, right] × [bottom, top]
     * in frame block units maps to the 96 px canvas (the script
     * canvas's own scale, {@code PIDSPresetBase.BASE_SCALE}); the glyph
     * draw sits at the window's top-left with a non-uniform scale
     * mapping the Drawer's background rectangle — vanilla hardcodes it
     * to (startX−1 … startX+advance) × (baseline−1 … baseline+9) text
     * pixels — exactly onto [0, wPx] × [0, hPx]: scale x by
     * wPx/(advance+1), y by hPx/10, then pre-translate by (1, 1) to
     * cancel the −1 margins. The advance is measured live
     * ({@code getWidth} — font-pack safe); the 10-px background height
     * and the 1-px margins are vanilla constants of the Drawer and do
     * not depend on the font.</p>
     *
     * <p><b>Buffer discipline</b>: a DEDICATED static BufferBuilder
     * ({@link #N2_BUFFER}) — NOT the Tessellator singleton — because the
     * LIGHT queue's keyed warm-up starts a batch on the queue holder's
     * own immediate over that singleton, and a private immediate
     * beginning a batch over the same building buffer would collide; the
     * dedicated buffer makes the synchronous flush self-contained. The
     * per-call Immediate wrapper is the same lightweight object every
     * MTR {@code drawText} constructs. Cost: ONE draw call (two quads —
     * glyph + background) per segment per frame per active side, zero
     * string work beyond the single-char measurement, zero allocation
     * beyond the Immediate wrapper.</p>
     */
    private static void drawBackingGlyph2(StoredMatrixTransformations transform, float left, float right, float bottom, float top, float frameTop, BlockPos anchor, Direction facing, boolean trackSide) {
        final float y = frameTop - top;
        final float height = top - bottom;
        final double anchorX = anchor.getX() + 0.5;
        final double anchorY = anchor.getY() + 0.5;
        final double anchorZ = anchor.getZ() + 0.5;
        MainRenderer.scheduleRender(trackSide ? QueuedRenderLayer.TEXT : QueuedRenderLayer.LIGHT, (graphicsHolderNew, offset) -> {
            try {
                /* Camera-side gate: facing points toward the TRACK, so a
                   camera with along = dot(camera - blockCenter, facing)
                   ≥ 0.25 is on (or past) the pane plane on the track
                   side — exactly one half renders per view. */
                final org.mtr.mapping.holder.Vector3i fv = facing.getVector();
                final double along = (offset.getXMapped() - anchorX) * fv.getX()
                        + (offset.getYMapped() - anchorY) * fv.getY()
                        + (offset.getZMapped() - anchorZ) * fv.getZ();
                if ((along >= 0.25) != trackSide) {
                    return; // the other half owns this view
                }
                final int color = trackSide ? TRACK_SIDE_COLOR : BACKING_COLOR;

                /* Window pixel geometry: 96 px per block, same as the
                   script canvas (PIDSPresetBase.BASE_SCALE = 1/96). */
                final float wPx = (right - left) * 96;
                final float hPx = height * 96;
#if LOADER == "fabric"
                final net.minecraft.client.util.math.MatrixStack matrices =
                        ((com.lx862.jcm.nativeapi.NativeMatrixApplier) (Object) graphicsHolderNew).jcm$getMatrixStack();
                final net.minecraft.client.render.VertexConsumerProvider.Immediate immediate =
                        net.minecraft.client.render.VertexConsumerProvider.immediate(N2_BUFFER);
                final net.minecraft.client.font.TextRenderer textRenderer =
                        net.minecraft.client.MinecraftClient.getInstance().textRenderer;
                final int advance = textRenderer.getWidth(BACKING_GLYPH_CHAR);
                if (advance > 0 && wPx > 0 && hPx > 0) {
                    final float sx = wPx / (advance + 1);
                    final float sy = hPx / 10;
                    GraphicsHolder.createInstanceSafe(matrices, immediate, priv -> {
                        transform.transform(priv, offset);
                        priv.scale(com.lx862.jcm.mod.data.pids.preset.PIDSPresetBase.BASE_SCALE, com.lx862.jcm.mod.data.pids.preset.PIDSPresetBase.BASE_SCALE, com.lx862.jcm.mod.data.pids.preset.PIDSPresetBase.BASE_SCALE);
                        priv.translate(left * 96, y * 96, 0);
                        priv.scale(sx, sy, 1);
                        priv.translate(1, 1, 0);
#if MC_VERSION >= "11903"
                        final org.joml.Matrix4f model = matrices.peek().getPositionMatrix();
                        textRenderer.draw(BACKING_GLYPH_CHAR, 0, 0, color, false, model, immediate, net.minecraft.client.font.TextRenderer.TextLayerType.NORMAL, color, MAX_RENDER_LIGHT);
#else
#if MC_VERSION <= "11701"
                        final net.minecraft.util.math.Matrix4f model = matrices.peek().getModel();
#else
                        final net.minecraft.util.math.Matrix4f model = matrices.peek().getPositionMatrix();
#endif
                        textRenderer.draw(BACKING_GLYPH_CHAR, 0, 0, color, false, model, immediate, false, color, MAX_RENDER_LIGHT);
#endif
                        priv.pop();
                    });
                    immediate.draw();
                }
#else
                final com.mojang.blaze3d.vertex.PoseStack matrices =
                        ((com.lx862.jcm.nativeapi.NativeMatrixApplier) (Object) graphicsHolderNew).jcm$getMatrixStack();
                final net.minecraft.client.renderer.MultiBufferSource.BufferSource immediate =
                        net.minecraft.client.renderer.MultiBufferSource.immediate(N2_BUFFER);
                final net.minecraft.client.gui.Font font =
                        net.minecraft.client.Minecraft.getInstance().font;
                final int advance = font.width(BACKING_GLYPH_CHAR);
                if (advance > 0 && wPx > 0 && hPx > 0) {
                    final float sx = wPx / (advance + 1);
                    final float sy = hPx / 10;
                    GraphicsHolder.createInstanceSafe(matrices, immediate, priv -> {
                        transform.transform(priv, offset);
                        priv.scale(com.lx862.jcm.mod.data.pids.preset.PIDSPresetBase.BASE_SCALE, com.lx862.jcm.mod.data.pids.preset.PIDSPresetBase.BASE_SCALE, com.lx862.jcm.mod.data.pids.preset.PIDSPresetBase.BASE_SCALE);
                        priv.translate(left * 96, y * 96, 0);
                        priv.scale(sx, sy, 1);
                        priv.translate(1, 1, 0);
#if MC_VERSION >= "11903"
                        final org.joml.Matrix4f model = matrices.last().pose();
                        font.drawInBatch(BACKING_GLYPH_CHAR, 0, 0, color, false, model, immediate, net.minecraft.client.gui.Font.DisplayMode.NORMAL, color, MAX_RENDER_LIGHT);
#else
                        final com.mojang.math.Matrix4f model = matrices.last().pose();
                        font.drawInBatch(BACKING_GLYPH_CHAR, 0, 0, color, false, model, immediate, false, color, MAX_RENDER_LIGHT);
#endif
                        priv.pop();
                    });
                    immediate.endBatch();
                }
#endif
                if (!BACKING_G2_OK) {
                    BACKING_G2_OK = true;
                    com.lx862.mtrscripting.mod.MTRScriptingModClient.LOGGER.info(
                            "[JCM] PSD backing channel N2 active (glyph pipeline, " + (trackSide ? "track" : "platform") + " half)");
                }
            } catch (Throwable t) {
                /* Never take the render thread down over a backing draw —
                   but the FIRST failure still leaves a breadcrumb
                   (yanyang.12 heritage). */
                if (!BACKING_G2_WARNED) {
                    BACKING_G2_WARNED = true;
                    com.lx862.mtrscripting.mod.MTRScriptingModClient.LOGGER.warn(
                            "[JCM] PSD backing channel N2 failed once: " + t, t);
                }
            }
        });
    }

    /**
     * One backing winding written with the EXACT glyph vertex sequence
     * (vertex → color → texture → light → next — the TextRenderer.Drawer
     * discipline, verified against the vanilla GlyphRenderer bytecode),
     * so the quad rides the text layer the same way every visible script
     * glyph does — the format is POSITION_COLOR_TEXTURE_LIGHT and this
     * sequence fills exactly its four elements.
     *
     * <p><b>Winding lineage</b>: the glyphs themselves are written
     * top-left → top-right → bottom-right → bottom-left (vanilla
     * GlyphRenderer.render, Rectangle minX/minY → maxX/minY → maxX/maxY
     * → minX/maxY) — THAT order is the one proven visible on this layer
     * under the user's shaders, so the PLATFORM-facing quad uses it
     * verbatim (the 1.0.1 draft instead copied carrier B's entity-cutout
     * order — the reverse winding, an unproven lineage on this layer
     * and potentially back-face-culled wherever the inherited cull
     * state is on). {@code flip} = false (track-facing) is the reverse
     * order — front-facing to the track camera by symmetry. {@code w}
     * is the quad's width (the window span, run units); the camera-side
     * gate in {@link #drawBackingText} draws exactly one of the two per
     * view, so no cull-state or depth-order assumption is needed.</p>
     */
#if LOADER == "fabric"
#if MC_VERSION >= "11903"
    private static void glyphQuad(net.minecraft.client.render.VertexConsumer vc, org.joml.Matrix4f model, float x, float y, float height, float w, float z, int argb, boolean flip) {
#else
    private static void glyphQuad(net.minecraft.client.render.VertexConsumer vc, net.minecraft.util.math.Matrix4f model, float x, float y, float height, float w, float z, int argb, boolean flip) {
#endif
#else
#if MC_VERSION >= "11903"
    private static void glyphQuad(com.mojang.blaze3d.vertex.VertexConsumer vc, org.joml.Matrix4f model, float x, float y, float height, float w, float z, int argb, boolean flip) {
#else
    private static void glyphQuad(com.mojang.blaze3d.vertex.VertexConsumer vc, com.mojang.math.Matrix4f model, float x, float y, float height, float w, float z, int argb, boolean flip) {
#endif
#endif
        final int a = (argb >>> 24) & 0xFF;
        final int r = (argb >>> 16) & 0xFF;
        final int g = (argb >>> 8) & 0xFF;
        final int b = argb & 0xFF;
        if (flip) {
            /* Platform-facing, the GLYPH winding verbatim: top-left →
               top-right → bottom-right → bottom-left (vanilla
               GlyphRenderer.render's own corner order — the winding
               every visible script glyph on these panes carries).
               Loader note: the fluent element setters carry different
               names per mapping — yarn texture/light/next vs mojmap
               uv/uv2/endVertex — while vertex(Matrix4f,...) and
               color(int...) are identical on both. */
#if LOADER == "fabric"
            vc.vertex(model, x, y, z).color(r, g, b, a).texture(0, 0).light(MAX_RENDER_LIGHT).next();
            vc.vertex(model, x + w, y, z).color(r, g, b, a).texture(1, 0).light(MAX_RENDER_LIGHT).next();
            vc.vertex(model, x + w, y + height, z).color(r, g, b, a).texture(1, 1).light(MAX_RENDER_LIGHT).next();
            vc.vertex(model, x, y + height, z).color(r, g, b, a).texture(0, 1).light(MAX_RENDER_LIGHT).next();
#else
            vc.vertex(model, x, y, z).color(r, g, b, a).uv(0, 0).uv2(MAX_RENDER_LIGHT).endVertex();
            vc.vertex(model, x + w, y, z).color(r, g, b, a).uv(1, 0).uv2(MAX_RENDER_LIGHT).endVertex();
            vc.vertex(model, x + w, y + height, z).color(r, g, b, a).uv(1, 1).uv2(MAX_RENDER_LIGHT).endVertex();
            vc.vertex(model, x, y + height, z).color(r, g, b, a).uv(0, 1).uv2(MAX_RENDER_LIGHT).endVertex();
#endif
        } else {
            /* Track-facing: the reverse order — bottom-left →
               bottom-right → top-right → top-left, front-facing to the
               track camera by symmetry. */
#if LOADER == "fabric"
            vc.vertex(model, x, y + height, z).color(r, g, b, a).texture(0, 1).light(MAX_RENDER_LIGHT).next();
            vc.vertex(model, x + w, y + height, z).color(r, g, b, a).texture(1, 1).light(MAX_RENDER_LIGHT).next();
            vc.vertex(model, x + w, y, z).color(r, g, b, a).texture(1, 0).light(MAX_RENDER_LIGHT).next();
            vc.vertex(model, x, y, z).color(r, g, b, a).texture(0, 0).light(MAX_RENDER_LIGHT).next();
#else
            vc.vertex(model, x, y + height, z).color(r, g, b, a).uv(0, 1).uv2(MAX_RENDER_LIGHT).endVertex();
            vc.vertex(model, x + w, y + height, z).color(r, g, b, a).uv(1, 1).uv2(MAX_RENDER_LIGHT).endVertex();
            vc.vertex(model, x + w, y, z).color(r, g, b, a).uv(1, 0).uv2(MAX_RENDER_LIGHT).endVertex();
            vc.vertex(model, x, y, z).color(r, g, b, a).uv(0, 0).uv2(MAX_RENDER_LIGHT).endVertex();
#endif
        }
    }

    /* ==================================================================== */
    /* Top panel                                                             */
    /* ==================================================================== */

    private static void renderTop(PSDBinding binding, BlockPos pos, BlockState state, World world, boolean shadowPass, Set<BlockPos> renderedSections) {
        final Direction facing = IBlock.getStatePropertySafe(state, BlockPSDTop.FACING);
        final boolean persistent = IBlock.getStatePropertySafe(state, BlockPSDTop.PERSISTENT) != BlockPSDTop.EnumPersistent.NONE;
        final float additionalOffset = persistent ? BlockPSDTop.PERSISTENT_OFFSET_SMALL : 0;

        /* One canvas across the psd_top GROUP the bound block belongs to —
           the psd_top stretch sitting on one wall section below (one door
           run / one glass segment; see PSDRuns for why the wall below
           defines the sections). Anchored at the group's left-most block,
           padded exactly like MTR's own band; the rest of the band keeps
           MTR's own arrows and route maps. Yanyang.6: the group renders
           ONCE however many of its blocks carry bindings. */
        final PSDRuns.Run run = PSDRuns.topRun(world, pos, facing);
        if (!renderedSections.add(run.start)) {
            return; // another binding of this group already rendered it
        }
        final StoredMatrixTransformations transform = new StoredMatrixTransformations(0.5 + run.start.getX(), pos.getY(), 0.5 + run.start.getZ());
        transform.add(graphicsHolder -> graphicsHolder.rotateYDegrees(-facing.asRotation()));
        transform.add(graphicsHolder -> graphicsHolder.translate(0, 1, 0));
        transform.add(graphicsHolder -> graphicsHolder.rotateZDegrees(180));
        transform.add(graphicsHolder -> graphicsHolder.translate(-0.5, -additionalOffset, TOP_DISPLAY_Z - SMALL_OFFSET));
        transform.add(graphicsHolder -> graphicsHolder.translate(TOP_SIDE_PADDING, TOP_TOP_PADDING, 0));

        final int canvasWidth = Math.round((run.width - TOP_SIDE_PADDING * 2) * 96);
        final int canvasHeight = Math.round((1 - TOP_TOP_PADDING - TOP_BOTTOM_PADDING) * 96);

        if (!shadowPass) {
            driveScript(binding, pos, world, canvasWidth, canvasHeight);
        }
        replayScript(binding, pos, world, transform, facing.getOpposite());
        if (!shadowPass) {
            replaySoundsOnce(binding, pos, world);
        }
    }

    /* ==================================================================== */
    /* Script driving (once per frame) and replay (once per face)            */
    /* ==================================================================== */

    private static void driveScript(PSDBinding binding, BlockPos pos, World world, int canvasWidth, int canvasHeight) {
        if (binding.pidsPreset) {
            final PIDSPresetBase preset = PIDSManager.getPreset(binding.scriptId);
            if (!(preset instanceof ScriptPIDSPreset)) return;
            final ScriptPIDSPreset scriptPreset = (ScriptPIDSPreset) preset;

            final PIDSWrapper wrapper = synthesizePidsWrapper(binding, pos, canvasWidth, canvasHeight);
            if (wrapper == null) return;

            if (NativeScriptManager.isLoaded(binding.scriptId)) {
                NativeContentDriver.pidsDrive(binding.scriptId, wrapper, world);
            }

            final ParsedScript parsedScript = scriptPreset.parsedScripts;
            if (parsedScript == null) return;
            final ScriptInstance<PIDSWrapper> instance = JCMScripting.getScriptManager().getInstanceManager().getInstance(
                    new UniqueKey("psd_pids", binding.scriptId, pos.getX(), pos.getY(), pos.getZ()),
                    () -> new PSDPidsScriptInstance(pos, binding.scriptId, parsedScript));
            if (!(instance instanceof PSDPidsScriptInstance)) return;
            final PSDPidsScriptInstance psdInstance = (PSDPidsScriptInstance) instance;

            instance.setWrapperObject(wrapper);
            instance.getScript().invokeRenderFunctions(instance, () -> {
                final PSDScriptContext context = (PSDScriptContext) psdInstance.getContextObject();
                psdInstance.saveRenderCalls(context.getRenderManager());
                psdInstance.saveSoundCalls(context.getSoundManager());
                context.resetForNextRun();
            });
        } else {
            final PSDResourceProvider.PSDScriptConfiguration config = MtrScriptingResourceManager.psd.getScript(binding.scriptId);
            if (config == null) return;

            final Platform platform = findPlatform(pos, binding.type);
            final PSDWallScanner.WallRun wallRun = PSDWallScanner.scan(world, pos);
            final PSDWrapper wrapper = new PSDWrapper(binding.type, pos, canvasWidth, canvasHeight, platform, wallRun, config.dataFetchMode());

            if (NativeScriptManager.isLoaded(binding.scriptId)) {
                NativeContentDriver.psdDrive(binding.scriptId, wrapper, world);
            }

            final ParsedScript parsedScript = config.parsedScript();
            if (parsedScript == null) return;
            final ScriptInstance<PSDWrapper> instance = JCMScripting.getScriptManager().getInstanceManager().getInstance(
                    new UniqueKey("psd", binding.scriptId, pos.getX(), pos.getY(), pos.getZ()),
                    () -> new PSDScriptInstance(pos, binding.scriptId, parsedScript));
            if (!(instance instanceof PSDScriptInstance)) return;
            final PSDScriptInstance psdInstance = (PSDScriptInstance) instance;

            instance.setWrapperObject(wrapper);
            instance.getScript().invokeRenderFunctions(instance, () -> {
                final PSDScriptContext context = (PSDScriptContext) psdInstance.getContextObject();
                psdInstance.saveRenderCalls(context.getRenderManager());
                psdInstance.saveSoundCalls(context.getSoundManager());
                context.resetForNextRun();
            });
        }
    }

    private static void replayScript(PSDBinding binding, BlockPos pos, World world, StoredMatrixTransformations transform, Direction drawDirection) {
        if (binding.pidsPreset) {
            final PIDSPresetBase preset = PIDSManager.getPreset(binding.scriptId);
            if (!(preset instanceof ScriptPIDSPreset)) return;
            final ScriptPIDSPreset scriptPreset = (ScriptPIDSPreset) preset;

            // Native replay (main pass only — the driver never ran on shadows).
            if (NativeScriptManager.isLoaded(binding.scriptId)) {
                NativeContentDriver.pidsReplay(binding.scriptId, pos, world, transform, drawDirection, MAX_RENDER_LIGHT);
            }

            final ParsedScript parsedScript = scriptPreset.parsedScripts;
            if (parsedScript == null) return;
            final ScriptInstance<?> instance = JCMScripting.getScriptManager().getInstanceManager().getInstance(
                    new UniqueKey("psd_pids", binding.scriptId, pos.getX(), pos.getY(), pos.getZ()));
            /* Yanyang fix: replay from the INSTANCE's render manager — the
               same contract as ScriptPIDSPreset.render. The old code invoked
               the CONTEXT's manager, which saveRenderCalls() had just emptied
               via the takeFrom() swap (the instance takes the captured calls,
               the context keeps the instance's spent list), so the script ran
               every frame yet nothing was ever drawn. */
            if (instance instanceof PSDPidsScriptInstance) {
                ((PSDPidsScriptInstance) instance).getRenderManager().invoke(world, transform, drawDirection, MAX_RENDER_LIGHT);
            }
        } else {
            if (NativeScriptManager.isLoaded(binding.scriptId)) {
                NativeContentDriver.psdReplay(binding.scriptId, pos, world, transform, drawDirection, MAX_RENDER_LIGHT);
            }
            final ScriptInstance<?> instance = JCMScripting.getScriptManager().getInstanceManager().getInstance(
                    new UniqueKey("psd", binding.scriptId, pos.getX(), pos.getY(), pos.getZ()));
            if (instance instanceof PSDScriptInstance) {
                ((PSDScriptInstance) instance).getRenderManager().invoke(world, transform, drawDirection, MAX_RENDER_LIGHT);
            }
        }
    }

    private static void replaySoundsOnce(PSDBinding binding, BlockPos pos, World world) {
        if (binding.pidsPreset) {
            if (NativeScriptManager.isLoaded(binding.scriptId)) {
                NativeContentDriver.pidsReplaySounds(binding.scriptId, pos, world);
            }
            final ScriptInstance<?> instance = JCMScripting.getScriptManager().getInstanceManager().getInstance(
                    new UniqueKey("psd_pids", binding.scriptId, pos.getX(), pos.getY(), pos.getZ()));
            /* Instance manager, not the (already-swapped-empty) context one —
               see replayScript. */
            if (instance instanceof PSDPidsScriptInstance) {
                ((PSDPidsScriptInstance) instance).getSoundManager().invoke(world, new ScriptVector3f(pos));
            }
        } else {
            if (NativeScriptManager.isLoaded(binding.scriptId)) {
                NativeContentDriver.psdReplaySounds(binding.scriptId, pos, world);
            }
            final ScriptInstance<?> instance = JCMScripting.getScriptManager().getInstanceManager().getInstance(
                    new UniqueKey("psd", binding.scriptId, pos.getX(), pos.getY(), pos.getZ()));
            if (instance instanceof PSDScriptInstance) {
                ((PSDScriptInstance) instance).getSoundManager().invoke(world, new ScriptVector3f(pos));
            }
        }
    }

    /** The PIDS-preset wrapper for a PSD binding — neutral identity, the PSD's
     *  own canvas and platform/arrival data. */
    private static PIDSWrapper synthesizePidsWrapper(PSDBinding binding, BlockPos pos, int canvasWidth, int canvasHeight) {
        final Platform platform = findPlatform(pos, binding.type);
        final LongImmutableList platformIds = platform == null ? new LongImmutableList(new long[0]) : new LongImmutableList(new long[]{platform.getId()});
        final ObjectArrayList<ArrivalResponse> arrivals = ArrivalsCacheClient.INSTANCE.requestArrivals(platformIds);
        /* Yanyang fix: custom messages must be EMPTY STRINGS, not nulls. A
           real PIDSBlockEntity presents "" after its NBT round-trip
           (CompoundTag.getString defaults to ""), and virtually every
           scripted preset branches on `getCustomMessage(i) != ""` — in JS,
           null != "" is TRUE, so a null row made the preset take the
           custom-text branch and draw cycleString(null) == "" on every row:
           the script ran every frame, captured its draw calls, replayed
           them — and the canvas stayed blank. This is exactly what
           pids_1a.js did on every bound PSD glass/top. */
        return new PIDSWrapper(
                "psd_" + binding.type.id, 3, pos, platformIds, arrivals,
                canvasWidth, canvasHeight,
                false, platform == null, false,
                new boolean[3], new String[]{"", "", ""}
        );
    }

    private static Platform findPlatform(BlockPos pos, PSDWrapper.PSDType type) {
        final AtomicReference<Platform> result = new AtomicReference<>();
        // psd_top blocks sit three above the platform; glass sits directly on it.
        final int primaryOffset = type == PSDWrapper.PSDType.TOP ? 3 : 1;
        InitClient.findClosePlatform(pos.down(primaryOffset), 5, result::set);
        if (result.get() == null && primaryOffset != 1) {
            InitClient.findClosePlatform(pos.down(1), 5, result::set);
        }
        return result.get();
    }
}