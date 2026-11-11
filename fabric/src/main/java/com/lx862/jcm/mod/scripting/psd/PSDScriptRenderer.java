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
 *   <li><b>Draw path</b> (2026-11-11, round 14): the backing rides
 *       TWO channels, one per regime —
 *       channel P: <b>the queue holder's own drawText</b> (see
 *       {@link #drawBackingHolderText}) — the exact statement lineage
 *       of {@code TextWrapper.drawTransformed}, the one draw path
 *       thirteen rounds of field testing ever proved visible behind
 *       the user's Iris 1.7.6 + ComplementaryUnbound r5.5.1 stack
 *       (every script glyph on these panes rides it since yanyang.1):
 *       a TEXT-queue callback that transforms the holder, scales into
 *       canvas px, stretches and calls {@code holder.drawText} — ONE
 *       stretched U+2588 FULL BLOCK per view, BLACK toward the
 *       platform (plus round-14 diagnostic stripes), WHITE toward the
 *       track, gated to shader pipelines ({@link
 *       com.lx862.jcm.mod.util.ShaderDetector}; without shaders the
 *       depth-tested carrier B already carries the correct sandwich,
 *       and a no-depth fill would cover the script's depth-drawn
 *       images/shapes — MTR drains its queues by QueuedRenderLayer
 *       ordinal with TEXT LAST); and
 *       carrier B: RenderRouteBase's exact route-sign pipeline (keyed
 *       EXTERIOR queue over the cached entity-cutout warm-up consumer)
 *       at the full pane extent with both tints, which carries the
 *       two-colour look whenever depth-tested quad layers render at
 *       all (every no-shader pipeline). Thirteen user-tested rounds
 *       proved every OTHER carrier fails on the users' shader stack —
 *       every depth-tested quad form (buffered opaque entity segments
 *       die behind the translucent pane while vehicles prove they
 *       live in open space), the SEE_THROUGH program pair (rounds
 *       9/10), a hand-written NORMAL-layer quad through a private
 *       immediate (round 11), even the real TextRenderer glyph machine
 *       through our own immediate on the LIGHT queue (round 12,
 *       build.94 — ran clean, still invisible). Only script-submitted
 *       holder drawText has ever shown through the pane; channel P is
 *       that path, verbatim; see {@link #drawBacking} and {@link
 *       #drawBackingHolderText} for the full post-mortem.</li>
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

    /** Carrier B's layer texture — this mod's own FULLY OPAQUE
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
     *  (Rounds 11-12's channels N/N2 also bound this texture / the
     *  font atlas through the vanilla text-layer factory; round 14's
     *  channel P needs no texture of its own at all — it draws a
     *  glyph through the holder's drawText, exactly like the script
     *  text.) */
    private static final org.mtr.mapping.holder.Identifier BACKING_TEXTURE = new org.mtr.mapping.holder.Identifier("jsblock", "textures/block/white.png");

    /** The covering tint toward the PLATFORM: fully opaque black (ARGB). */
    private static final int BACKING_COLOR = 0xFF000000;

    /** The track-facing white quad sits this much DEEPER (in the pane
     *  frame's local +Z, toward the track) than the black quad
     *  (yanyang.10 heritage; carrier B keeps the separation). */
    private static final float TRACK_BACK_Z = 0.0005F;

    /** Carrier B's whole pair sits this much deeper than the backing
     *  plane ({@link #GLASS_BACKING_OFFSET} + this) that channel P
     *  draws on — channel P's text layer ignores depth entirely, so
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

    /** Channel P's carrier glyph (round 14): U+2588 FULL BLOCK — the
     *  one character whose glyph IS a solid filled rectangle in every
     *  complete font (default MC font and CJK font packs alike), so ONE
     *  instance, non-uniformly stretched to the transparent window,
     *  reads as a plain solid colour block — no text pattern is
     *  legible (the user's 2026-11-10 「请尽量避免使用文字填充黑色底色」
     *  is a VISUAL constraint; a single stretched full-block satisfies
     *  it while riding the one draw path ever proven visible behind
     *  the user's shaders). Written as a unicode escape so the source
     *  stays encoding-proof through the API push and every loader's
     *  compiler. */
    private static final String BACKING_GLYPH_CHAR = "\u2588";

    /** Diagnostic stripe height in canvas px (round 14). See
     *  {@link #DIAG_STRIPE_COLOR}. */
    private static final float STRIPE_PX = 6;

    /** Round-14 diagnostic stripe colour — the EXACT orange of the
     *  builtin PIDS 1A arrival text (pids_1a.js {@code color(0xFC9700)}),
     *  i.e. the one colour PROVEN visible on these very panes under the
     *  user's shader stack. The platform half additionally paints two
     *  6px stripes (orange along the window's top edge, white along its
     *  bottom edge) through the very same holder-drawText statements as
     *  the black fill, so ONE field test cleanly separates the three
     *  failure families:
     *  <ol>
     *    <li>all three invisible — the draw path itself is still not
     *        the visible text's path (schedule/context theory);</li>
     *    <li>stripes visible, black missing — the path works and the
     *        pure-black tint is what the pipeline discards (colour
     *        theory — next round chases the shader's black handling);</li>
     *    <li>everything visible — the backing is FIXED and the stripes
     *        get removed in the follow-up build.</li>
     *  </ol>
     *  Remove once the backing's shader visibility is settled. */
    private static final int DIAG_STRIPE_COLOR = 0xFC9700;

    /** Distance² gate — bindings beyond this are not driven. */
    private static final double MAX_DISTANCE_SQ = 192 * 192;

    /** One-shot breadcrumbs per channel (yanyang.12 heritage): the
     *  first EXCEPTION and the first successful execution are each
     *  logged once, so the next field log proves which channel ran.
     *  (Channel P is the round-14 holder-drawText carrier; carrier B
     *  the EXTERIOR route-sign pipeline.) */
    private static volatile boolean BACKING_P_WARNED = false;
    private static volatile boolean BACKING_P_OK = false;
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
        /* Rounds 10+: the no-depth-test holder-drawText backing
           (channel P since round 14; channels T/N/N2 before it) is
           gated to shader pipelines — the full story is on
           drawBackingHolderText, the short version: without shaders
           the depth-tested carrier B already carries the correct
           sandwich (script images/shapes/text keep their proven
           layering over it), while a no-depth full-window fill on the
           TEXT queue would flush LAST (MTR drains the queues by
           QueuedRenderLayer ordinal, TEXT = 8 = last) and paint over
           everything the script drew on the depth-tested queues.
           Under a shader pack those queues' content behind the pane
           is discarded by the pipeline anyway (thirteen rounds
           proved it), so there is nothing visible left to cover —
           and the text still draws over the black (same queue, later
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
           carrier B's per-column quads and channel P's halves can share
           the same plane. */
        final StoredMatrixTransformations backing = glassTransform(run.start, facing, backingZ, frameHeight);

        /* Channel P's plane (round 14): the same backing plane, but
           pre-translated to the transparent window's top-left corner in
           BLOCK units — the callback then only scales into canvas px
           and stretches, the exact statement order of
           TextWrapper.drawTransformed. A SEPARATE instance: `backing`
           is shared with carrier B, whose drawTextureInWorld coordinates
           are absolute in the un-translated frame. */
        final StoredMatrixTransformations backingText = glassTransform(run.start, facing, backingZ, frameHeight);
        backingText.add(graphicsHolder -> graphicsHolder.translate(canvasLeft, canvasTop, 0));

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
            /* Channel P (2026-11-11, round 14 — see drawBackingHolderText),
               ONLY on shader pipelines: the backing drawn by the queue
               HOLDER'S OWN drawText — the one draw path thirteen rounds
               of field testing ever proved visible behind the user's
               Iris + ComplementaryUnbound stack (every script glyph on
               these panes rides it since yanyang.1) — carrying ONE
               stretched U+2588 FULL BLOCK per view: BLACK toward the
               platform (plus the round-14 diagnostic stripes), WHITE
               toward the track. Scheduled on the TEXT queue BEFORE
               driveScript/replayScript submit the script's callbacks —
               same queue, same "" key, FIFO — so the backing flushes
               first and every script element paints over it. Without
               shaders the depth-tested carrier B already carries the
               correct sandwich. Skipped when no window could be
               derived (carrier B alone). */
            if (shaderPack && windowTop - windowBottom >= MIN_WINDOW) {
                drawBackingHolderText(backingText, segLeft, segRight, windowBottom, windowTop, run.start, facing, false);
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
            /* Channel P track half (round 14): scheduled AFTER
               replayScript, so within the TEXT queue's shared "" list
               it sits behind every script text callback — it flushes
               LAST and paints the WHITE panel over everything the
               track-side view would otherwise leak (mirrored text,
               flipped images): the same "plain white panel" semantics
               carrier B's depth-tested white quad gives on no-shader
               pipelines, expressed through the holder's own drawText —
               the path that provably renders under the user's shader
               stack. The camera-side gate inside the callback keeps it
               inert on the platform view. */
            if (shaderPack && windowTop - windowBottom >= MIN_WINDOW) {
                drawBackingHolderText(backingText, segLeft, segRight, windowBottom, windowTop, run.start, facing, true);
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
     * N (hand quad) and round 12's channel N2 (the real glyph machine
     * through a private immediate) rode the NORMAL layer and STILL
     * stayed invisible (build.94, 2026-11-11); round 14's channel P
     * ({@link #drawBackingHolderText}) finally submits through the
     * holder's own drawText — the script text's exact lineage; this
     * carrier remains for every pipeline where depth-tested quads
     * render at all (no-shader setups).</p>
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
    /* Holder-drawText backing (channel P, round 14)                        */
    /* ==================================================================== */

    /**
     * Channel P (2026-11-11, round 14) — the backing drawn by THE QUEUE
     * HOLDER'S OWN {@code drawText}, inside a TEXT-queue callback built
     * with the exact statement order of {@code TextWrapper.drawTransformed}
     * — the one draw path thirteen rounds of field testing ever proved
     * visible behind the user's Iris 1.7.6 + ComplementaryUnbound r5.5.1
     * + ImmediatelyFast 1.5.5 + Sodium 0.5 stack (every script glyph on
     * these panes has ridden it since yanyang.1: {@code
     * MainRenderer.scheduleRender(TEXT, cb)} → cb does {@code
     * storedMatrixTransformations.transform(holder, offset)} → {@code
     * holder.scale(...)} → {@code holder.drawText(...)} → {@code
     * holder.pop()}).
     *
     * <p><b>Why every previous carrier failed while this lineage lives —
     * the thirteen-round post-mortem</b> (full history in
     * PSD-SHADER-BACKING.md): the user's script ({@code pids_1a.js}) is
     * TEXT-ONLY, so the only content ever SEEN through the pane under
     * shaders is {@code drawText} output submitted by a script draw call.
     * Every backing carrier so far differed from that lineage in at
     * least one link — the depth-tested MTR queue consumers (carrier B,
     * yanyang.8-10/13, 2.3.0-1.0.x: buffered opaque entity segments,
     * which vehicles prove render in open space but which die behind a
     * translucent pane on this stack), the SEE_THROUGH program pair
     * (channel T, rounds 9-10), a hand-written quad on the NORMAL text
     * layer through a private immediate + borrowed matrices (channel N,
     * round 11), even the real {@code TextRenderer.draw} glyph machine
     * through our OWN private immediate on the LIGHT queue (channel N2,
     * round 12 — build.94 ran it clean, still invisible). Channel P
     * removes every remaining difference: no private immediate, no
     * borrowed MatrixStack, no raw Minecraft class at all — the backing
     * is a string drawn by the very same {@code GraphicsHolder.drawText}
     * overload on the very same holder the script's text uses, scaled
     * with the very same statement sequence. If THIS is invisible while
     * the orange arrival text floats over the scene, the only variables
     * left are the tint and the glyph — which is exactly what the
     * diagnostic stripes isolate.</p>
     *
     * <p><b>Geometry</b>: ONE {@link #BACKING_GLYPH_CHAR} (U+2588 FULL
     * BLOCK — a solid filled rectangle in every complete font) drawn at
     * the window's top-left and non-uniformly stretched (advance and the
     * vanilla 9px text row height measured live) to cover the derived
     * transparent window exactly — the same stretch discipline the
     * visible arrival text's {@code stretchXY()} applies. Visually the
     * result is a plain solid colour block; no text pattern is legible
     * (the user's 2026-11-10 constraint is a visual one).</p>
     *
     * <p><b>Sides and order</b>: the platform half (BLACK fill + the
     * round-14 diagnostic stripes) is scheduled on the TEXT queue BEFORE
     * {@code driveScript}/{@code replayScript} submit the script's
     * callbacks — same queue, same {@code ""} key, FIFO — so it flushes
     * first and every script element paints over it; the track half
     * (WHITE fill, no stripes) is scheduled AFTER {@code replayScript}
     * so it flushes LAST and covers the mirrored content train
     * passengers would otherwise see through the window. Each half's
     * callback carries the camera-side gate ({@code along >= 0.25} = the
     * track side): exactly one half draws per view.</p>
     *
     * <p><b>Diagnostic stripes</b> (platform half only, round 14 — see
     * {@link #DIAG_STRIPE_COLOR}): an orange 6px stripe along the
     * window's top edge (the EXACT orange of the visible arrival text)
     * and a white 6px stripe along its bottom edge, drawn through the
     * very same holder-drawText statements as the black fill. One field
     * test then cleanly separates the failure families: all three
     * invisible = the path is still not the text's path; stripes
     * visible but black missing = the tint is what dies; everything
     * visible = fixed (stripes removed in the follow-up build).</p>
     *
     * <p><b>Cost</b>: one to three drawText calls per segment per frame
     * per active side — the same order as a single arrival row of the
     * script's own text, on a layer vanilla memoizes per texture
     * identifier. Zero allocation beyond the tiny Immediate wrapper each
     * MTR drawText already constructs.</p>
     */
    private static void drawBackingHolderText(StoredMatrixTransformations transform, float left, float right, float bottom, float top, BlockPos anchor, Direction facing, boolean trackSide) {
        final float height = top - bottom;
        final double anchorX = anchor.getX() + 0.5;
        final double anchorY = anchor.getY() + 0.5;
        final double anchorZ = anchor.getZ() + 0.5;
        MainRenderer.scheduleRender(QueuedRenderLayer.TEXT, (graphicsHolderNew, offset) -> {
            try {
                /* Camera-side gate: facing points toward the TRACK, so a
                   camera with along = dot(camera - blockCenter, facing)
                   >= 0.25 is on (or past) the pane plane on the track
                   side — exactly one half renders per view. */
                final org.mtr.mapping.holder.Vector3i fv = facing.getVector();
                final double along = (offset.getXMapped() - anchorX) * fv.getX()
                        + (offset.getYMapped() - anchorY) * fv.getY()
                        + (offset.getZMapped() - anchorZ) * fv.getZ();
                if ((along >= 0.25) != trackSide) {
                    return; // the other half owns this view
                }
                final int color = trackSide ? TRACK_SIDE_COLOR : BACKING_COLOR;

                /* Window pixel geometry: 96 px per block, the same canvas
                   scale the script itself draws on. The advance is
                   measured live (font-pack safe); 9 is the vanilla text
                   row height drawText lays glyphs out on. */
                final float wPx = (right - left) * 96;
                final float hPx = height * 96;
                final int advance = GraphicsHolder.getTextWidth(BACKING_GLYPH_CHAR);
                if (advance > 0 && wPx > 0 && hPx > 0) {
                    final float sx = wPx / advance;
                    final float sy = hPx / 9;

                    /* TextWrapper.drawTransformed, statement for statement:
                       transform the holder (pushes), scale into canvas
                       pixels, stretch to the window, drawText ON THE
                       HOLDER — MTR's method, its own private immediate,
                       its own matrix stack — then pop. The depth counter
                       unwinds OUR pushes even if a draw throws mid-chain
                       (createInstanceSafe's discipline, inlined — a
                       leaked pose on the shared queue holder would
                       corrupt every later draw of the frame). */
                    int depth = 0;
                    try {
                        transform.transform(graphicsHolderNew, offset);
                        depth = 1;
                        graphicsHolderNew.scale(PIDSPresetBase.BASE_SCALE, PIDSPresetBase.BASE_SCALE, PIDSPresetBase.BASE_SCALE);
                        graphicsHolderNew.push();
                        depth = 2;
                        graphicsHolderNew.scale(sx, sy, 1);
                        graphicsHolderNew.drawText(BACKING_GLYPH_CHAR, 0, 0, color, false, MAX_RENDER_LIGHT);
                        graphicsHolderNew.pop();
                        depth = 1;

                        if (!trackSide) {
                            /* Round-14 diagnostic stripes (see
                               DIAG_STRIPE_COLOR): the same glyph, the same
                               statements, three colours total. Orange top,
                               white bottom, 6px each. */
                            graphicsHolderNew.push();
                            depth = 2;
                            graphicsHolderNew.scale(sx, STRIPE_PX / 9F, 1);
                            graphicsHolderNew.drawText(BACKING_GLYPH_CHAR, 0, 0, DIAG_STRIPE_COLOR, false, MAX_RENDER_LIGHT);
                            graphicsHolderNew.pop();
                            depth = 1;
                            graphicsHolderNew.push();
                            depth = 2;
                            graphicsHolderNew.translate(0, hPx - STRIPE_PX, 0);
                            graphicsHolderNew.scale(sx, STRIPE_PX / 9F, 1);
                            graphicsHolderNew.drawText(BACKING_GLYPH_CHAR, 0, 0, TRACK_SIDE_COLOR, false, MAX_RENDER_LIGHT);
                            graphicsHolderNew.pop();
                            depth = 1;
                        }
                        graphicsHolderNew.pop(); // the transform's push
                        depth = 0;
                    } finally {
                        while (depth > 0) {
                            graphicsHolderNew.pop();
                            depth--;
                        }
                    }

                    if (!BACKING_P_OK) {
                        BACKING_P_OK = true;
                        com.lx862.mtrscripting.mod.MTRScriptingModClient.LOGGER.info(
                                "[JCM] PSD backing channel P active (holder drawText, " + (trackSide ? "track" : "platform") + " half)");
                        if (!trackSide) {
                            com.lx862.mtrscripting.mod.MTRScriptingModClient.LOGGER.info(
                                    "[JCM] PSD backing channel P metrics: window " + Math.round(wPx) + "x" + Math.round(hPx)
                                            + "px, U+2588 advance " + advance + "; diagnostic stripes orange(top)/white(bottom) " + STRIPE_PX + "px");
                        }
                    }
                } else if (!BACKING_P_WARNED) {
                    /* Degenerate geometry or a font without the full-block
                       glyph: better to SAY so than to log "active" over a
                       no-draw (the round-12 breadcrumb trap). */
                    BACKING_P_WARNED = true;
                    com.lx862.mtrscripting.mod.MTRScriptingModClient.LOGGER.warn(
                            "[JCM] PSD backing channel P degenerate: window " + Math.round(wPx) + "x" + Math.round(hPx)
                                    + "px, U+2588 advance " + advance + " — nothing drawn");
                }
            } catch (Throwable t) {
                /* Never take the render thread down over a backing draw —
                   but the FIRST failure still leaves a breadcrumb
                   (yanyang.12 heritage). */
                if (!BACKING_P_WARNED) {
                    BACKING_P_WARNED = true;
                    com.lx862.mtrscripting.mod.MTRScriptingModClient.LOGGER.warn(
                            "[JCM] PSD backing channel P failed once: " + t, t);
                }
            }
        });
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
