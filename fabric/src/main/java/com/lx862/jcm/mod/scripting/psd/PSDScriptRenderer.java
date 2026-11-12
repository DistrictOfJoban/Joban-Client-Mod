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
import com.lx862.jcm.mod.scripting.pids.TextWrapper;
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
 *   <li><b>Draw path</b> (2026-11-12, round 16): the backing rides
 *       TWO channels, one per regime —
 *       channel R: <b>text background rectangles</b> (see
 *       {@link #drawBackingTextRows}) — the backing is the script
 *       text's own BACKGROUND: fresh {@link TextWrapper} instances
 *       whose every line carries a nametag-style background rectangle
 *       drawn by the real TextRenderer's {@code backgroundColor}
 *       mechanism — a {@code BakedGlyph$Effect} quad over the font
 *       atlas's vanilla-synthesized {@code SpecialGlyphs.WHITE} region,
 *       on the very same RenderLayer/texture/program the visible
 *       glyphs ride — submitted through the wrapper's own
 *       {@code run()} → {@code drawTransformed()} →
 *       {@code scheduleRender(TEXT)} machinery from renderAll head
 *       (the same MainRenderer.render call and queue snapshot the
 *       script's replay lands in), near-black ({@link #Q_FILL_COLOR})
 *       toward the platform (plus the round-16 glyph probe row and
 *       diagnostic stripes), WHITE toward the track, gated to
 *       shader pipelines ({@link
 *       com.lx862.jcm.mod.util.ShaderDetector}; without shaders the
 *       depth-tested carrier B already carries the correct sandwich,
 *       and a no-depth fill would cover the script's depth-drawn
 *       images/shapes — MTR drains its queues by QueuedRenderLayer
 *       ordinal with TEXT LAST); and
 *       carrier B: RenderRouteBase's exact route-sign pipeline (keyed
 *       EXTERIOR queue over the cached entity-cutout warm-up consumer)
 *       at the full pane extent with both tints, which carries the
 *       two-colour look whenever depth-tested quad layers render at
 *       all (every no-shader pipeline). Fifteen user-tested rounds
 *       proved every OTHER carrier fails on the users' shader stack —
 *       every depth-tested quad form (buffered opaque entity segments
 *       die behind the translucent pane while vehicles prove they
 *       live in open space), the SEE_THROUGH program pair (rounds
 *       9/10), a hand-written NORMAL-layer quad through a private
 *       immediate (round 11), the real TextRenderer glyph machine
 *       through our own immediate on the LIGHT queue (round 12), the
 *       queue holder's own drawText with a hand-copied statement
 *       sequence (round 14), and — the decisive elimination — REAL
 *       TextWrapper rows of U+2588 FULL BLOCK through the script's
 *       very wrapper machinery (round 15: executed cleanly per the
 *       log, every colour incl. the proven orange, still invisible
 *       while the script's own CJK rows stayed visible — leaving the
 *       glyph's rasterization as the only variable, present with a
 *       sane advance yet drawing nothing: a font-pack blank glyph);
 *       see {@link #drawBacking} and {@link #drawBackingTextRows} for
 *       the full post-mortem.</li>
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
     *  font atlas through the vanilla text-layer factory; rounds 14-15
     *  need no texture of their own at all — the backing draws through
     *  the script text machinery itself, glyph rows on the font
     *  stack.) */
    private static final org.mtr.mapping.holder.Identifier BACKING_TEXTURE = new org.mtr.mapping.holder.Identifier("jsblock", "textures/block/white.png");

    /** The covering tint toward the PLATFORM: fully opaque black (ARGB). */
    private static final int BACKING_COLOR = 0xFF000000;

    /** The track-facing white quad sits this much DEEPER (in the pane
     *  frame's local +Z, toward the track) than the black quad
     *  (yanyang.10 heritage; carrier B keeps the separation). */
    private static final float TRACK_BACK_Z = 0.0005F;

    /** Carrier B's whole pair sits this much deeper than the backing
     *  plane ({@link #GLASS_BACKING_OFFSET} + this) that channel R's
     *  background-rectangle rows draw on — the text layer's depth
     *  handling is inherited state, so the offset only matters where
     *  depth-tested quads render at all (no-shader pipelines): it keeps
     *  carrier B deterministic and clear of anything else at the same
     *  plane (2026-11-10; carrier A retired, its separation role
     *  inherited). */
    private static final float CARRIER_B_Z = 0.001F;

    /** The covering tint toward the TRACK — fully opaque white. Train
     *  passengers see a plain white panel, never mirrored content (the
     *  white quad's depth fails the canvas content's LEQUAL test on the
     *  track side, so script content never leaks through). */
    private static final int TRACK_SIDE_COLOR = 0xFFFFFFFF;

    /** Channel R's fill tint toward the PLATFORM: NEAR black (10,10,10),
     *  not pure black. Round 15 heritage: 0x0A0A0A reads as plain black
     *  on any display while dodging any pure-black handling in the
     *  shader pack. Carried over unchanged — round 16 changes the
     *  CARRIER (text background rectangles), not the tint. */
    private static final int Q_FILL_COLOR = 0xFF0A0A0A;

    /** The text row pitch every TextWrapper row is laid out on (vanilla
     *  text px — TextWrapper.drawTransformed's own layout constant).
     *  The round-16 background rectangles are ~11px tall (the Drawer's
     *  y−1..y+10 constants), so the 9px pitch overlaps them by ~2px — a
     *  seamless solid field with no hairlines. */
    private static final float Q_TEXT_ROW_PX = 9;

    /** The glyph probe row's reserved height (probe row = ~16px at the
     *  script's own 1.725 scale + a 2px gap) before the first background
     *  rectangle stripe begins. */
    private static final float PROBE_ROW_PX = 18;

    /** Camera-side gate threshold, in blocks along the pane normal
     *  (facing points toward the track). Rounds 14-16 heritage. */
    private static final double Q_CAMERA_GATE = 0.25;

    /** Round-16 diagnostic stripe colour — the EXACT orange of the
     *  builtin PIDS 1A arrival text (pids_1a.js {@code color(0xFC9700)}),
     *  with an EXPLICIT opaque alpha this time (round 15 left it at
     *  0x00FC9700 and relied on TextRenderer's alpha==0 → opaque forcing,
     *  which vanilla does apply — but never rely on a quirk in a
     *  diagnostic). Painted on the platform half's FIRST background
     *  stripe AND on the glyph probe row, so ONE field test separates the
     *  remaining failure families:
     *  <ol>
     *    <li>everything invisible — even real-character glyphs die in our
     *        submission context → round 17 escalates to replay-injected
     *        rows (see PSD-SHADER-BACKING.md §7.0-16);</li>
     *    <li>probe glyphs visible but rectangles missing — the
     *        background-rect path itself dies on this stack → the doc's
     *        RenderDoc/other-pack diagnostics;</li>
     *    <li>everything visible — FIXED; stripes and probe get removed in
     *        the follow-up build.</li>
     *  </ol>
     *  Remove once the backing's shader visibility is settled. */
    private static final int DIAG_STRIPE_COLOR = 0xFFFC9700;

    /** Round-16 glyph probe row: real characters in the script's own
     *  visual language (CJK + latin + digits, the exact glyph families
     *  pids_1a.js renders visibly on these very panes), drawn as a PLAIN
     *  TextWrapper row — the round-15 channel-Q lineage verbatim, minus
     *  the one variable round 15 left: the glyph. If this row is visible
     *  while round 15's U+2588 rows were not, the font pack's full-block
     *  glyph is proven corrupt and the glyph-free background rectangles
     *  are the correct permanent carrier. Written as unicode escapes so
     *  the source stays encoding-proof through the API push and every
     *  loader's compiler. */
    private static final String R_PROBE_TEXT = "\u5E95\u8272R16\u6D4B\u8BD5";

    /** Distance² gate — bindings beyond this are not driven. */
    private static final double MAX_DISTANCE_SQ = 192 * 192;

    /** One-shot breadcrumbs per channel (yanyang.12 heritage): the
     *  first EXCEPTION and the first successful execution are each
     *  logged once, so the next field log proves which channel ran.
     *  (Channel R is the round-16 text-background-rectangle carrier;
     *  carrier B the EXTERIOR route-sign pipeline.) */
    private static volatile boolean BACKING_R_WARNED = false;
    private static volatile boolean BACKING_R_OK = false;
    private static volatile boolean BACKING_B_WARNED = false;
    private static volatile boolean BACKING_B_OK = false;

    /** Channel R fill metrics, measured once on first use (render
     *  thread): the advance of one fill character as the background
     *  rows will actually draw it. The rows use {@code fontMC()} —
     *  NO font style, the vanilla default font — exactly matching the
     *  String overload the background path feeds the real TextRenderer
     *  with, so measurement and drawing can never disagree. The fill
     *  character is a SPACE (its glyph rasterizes to nothing — only the
     *  advance matters, and the background rectangle is drawn per
     *  character position regardless of glyph pixels, nametag-behaviour
     *  proven); if a font pack somehow zeroes the space advance, the
     *  digit fallback keeps the rectangles sized. */
    private static int R_ADVANCE = Integer.MIN_VALUE;
    private static String R_FILL_CHAR = " ";
    private static int R_VANILLA_SPACE_ADVANCE;

    private PSDScriptRenderer() {
    }

    /** Called from MainRendererPSDMixin at the head of MainRenderer.render.
     * Everything drawn here is SCHEDULED onto MTR's queued layers and
     * flushed within that same render call on MTR's own graphics holder;
     * camera anchoring comes from the queued callback's offset at flush
     * time. The backing rides TWO channels, one per regime — channel R
     * (round 16: the script text's own background rectangles through the
     * wrapper machinery, shader pipelines only; see drawBackingTextRows)
     * and
     * carrier B (the keyed EXTERIOR route-sign pipeline, every
     * no-shader pipeline; see drawBacking). */
    public static void renderAll() {
        final MinecraftClient minecraftClient = MinecraftClient.getInstance();
        /* MTR's mapping wrappers are unrelated types — ClientWorld reaches the
           World-typed helpers (native driver, InitClient) through World.cast. */
        final World world = World.cast(minecraftClient.getWorldMapped());
        final ClientPlayerEntity player = minecraftClient.getPlayerMapped();
        if (world == null || player == null) return;
        if (PSDClientBindings.isEmpty()) return;

        final boolean shadowPass = OptimizedRenderer.renderingShadows();
        /* Rounds 10+: the no-depth-test text-pipeline backing
           (channel R since round 16; T/N/N2/P/Q before it) is
           gated to shader pipelines — the full story is on
           drawBackingTextRows, the short version: without shaders
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
                    renderGlass(binding, pos, state, world, shadowPass, shaderPack, renderedSections, playerX, playerY, playerZ);
                }
            }
        });
    }

    /* ==================================================================== */
    /* Glass                                                                 */
    /* ==================================================================== */

    private static void renderGlass(PSDBinding binding, BlockPos pos, BlockState state, World world, boolean shadowPass, boolean shaderPack, Set<BlockPos> renderedSections, double playerX, double playerY, double playerZ) {
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
           carrier B's per-column quads and channel R's wrapper rows can
           share the same plane. */
        final StoredMatrixTransformations backing = glassTransform(run.start, facing, backingZ, frameHeight);

        /* Channel R's plane (rounds 14-16): the same backing plane, but
           pre-translated to the transparent window's top-left corner in
           BLOCK units — each TextWrapper row then adds its own
           BASE_SCALE + position consumer on top (PIDSDrawCall.run), the
           exact transform chain every script draw call replays through.
           A SEPARATE instance: `backing` is shared with carrier B, whose
           drawTextureInWorld coordinates are absolute in the
           un-translated frame (and run() APPENDS consumers, so every
           row gets its own copy — see drawBackingTextRows). */
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
            /* Channel R (2026-11-12, round 16 — see
               drawBackingTextRows), ONLY on shader pipelines: the
               backing drawn as the script text's own BACKGROUND — real
               TextWrapper rows whose every line carries a nametag-style
               background rectangle, drawn by the real TextRenderer's
               backgroundColor mechanism over the font atlas's
               vanilla-synthesized white region on the glyphs' very
               RenderLayer — glyph-independent by construction (the
               round-16 answer to rounds 14-15's U+2588 rows executing
               cleanly yet rendering nothing on the user's font stack).
               Carrying BLACK toward the platform (plus the round-16
               glyph probe row and diagnostic stripes), WHITE toward the
               track. Scheduled from renderAll head (BEFORE
               driveScript/replayScript submit the script's callbacks —
               same queue, same "" key, FIFO) so the backing flushes
               first and every script element paints over it. Without
               shaders the depth-tested carrier B already carries the
               correct sandwich. Skipped when no window could be derived
               (carrier B alone). */
            if (shaderPack && windowTop - windowBottom >= MIN_WINDOW) {
                drawBackingTextRows(world, backingText, segLeft, segRight, windowBottom, windowTop, run.start, facing, false, playerX, playerY, playerZ);
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
            /* Channel R track half (round 16): scheduled AFTER
               replayScript, so within the TEXT queue's shared "" list
               it sits behind every script text callback — it flushes
               LAST and paints the WHITE rectangles over everything the
               track-side view would otherwise leak (mirrored text,
               flipped images): the same "plain white panel" semantics
               carrier B's depth-tested white quad gives on no-shader
               pipelines, expressed through the same glyph-free
               background-rectangle rows as the platform half. The
               player-side gate at schedule time keeps it inert on the
               platform view (see drawBackingTextRows for why the gate
               cannot wait for the callback). */
            if (shaderPack && windowTop - windowBottom >= MIN_WINDOW) {
                drawBackingTextRows(world, backingText, segLeft, segRight, windowBottom, windowTop, run.start, facing, true, playerX, playerY, playerZ);
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
     * (the holder's own drawText, hand-copied statements) and round
     * 15's channel Q (real TextWrapper rows — executed cleanly yet
     *  invisible on the user's font stack: the font pack's U+2588
     *  rasterization draws nothing) and round 16's channel R (the
     *  script text's own BACKGROUND rectangles — glyph-free by
     *  construction) carry the shader-pipeline regime; this carrier
     *  remains for every pipeline where depth-tested quads render at
     *  all (no-shader setups).</p>
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
    /* Text-background backing (channel R, round 16)                        */
    /* ==================================================================== */

    /**
     * Channel R (2026-11-12, round 16) — the backing is the script text's
     * own BACKGROUND: rows of real {@link TextWrapper}s whose every line
     * carries a nametag-style background rectangle, drawn by the real
     * TextRenderer's {@code backgroundColor} mechanism.
     *
     * <p><b>Why rounds 14-15 died and this should not</b>: build.96
     * (channel Q — real TextWrapper rows of U+2588 FULL BLOCK, same
     * queue/key/font/everything as the visible arrival text) executed
     * cleanly (breadcrumbs + metrics, no exceptions) yet stayed invisible
     * while the script's own CJK/latin rows stayed visible. With every
     * pipeline variable eliminated, the survivor is the content: the
     * user's font stack ("Source Han Sans Font Pack") supplies U+2588
     * with a sane advance (measured 9px) but — the only self-consistent
     * explanation — a glyph rasterization that draws nothing (a bitmap
     * row misalignment or an empty TTF outline; vanilla's MISSING glyph
     * would have rendered a visible tofu outline, and a present solid
     * block would have been visible, so the glyph itself must be
     * present-but-blank). Round 14 drew the same codepoint through the
     * holder's drawText and died identically — two independent carriers,
     * one common variable.</p>
     *
     * <p><b>The round-16 forensics that shaped this carrier</b> (vanilla
     * 1.20.1 client.jar bytecode, /home/z/psd16): the background
     * rectangle is drawn by {@code Font$StringRenderOutput.finish} as a
     * {@code BakedGlyph$Effect} quad over the font atlas's
     * vanilla-synthesized {@code SpecialGlyphs.WHITE} region (every pixel
     * 0xFFFFFFFF — no font pack participates in its rasterization),
     * through {@code whiteGlyph.renderType(NORMAL)} — the very same
     * RenderLayer/texture/program the visible glyphs ride. The
     * rectangle's size comes only from the string's ADVANCES (the
     * Drawer draws one continuous rect per line, spaces included — the
     * nametag behaviour). The fill is therefore COMPLETELY
     * glyph-independent: no character rasterization can corrupt it.
     * (Corrected forensics: the NORMAL text layer carries NO DepthTest
     * and NO WriteMask phase — it inherits the ambient state; the
     * round-11 doc note claiming DepthTest "always" had disassembled the
     * SEE_THROUGH lambdas. Vanilla visibility of text behind the pane
     * comes from draw ORDER — entity-phase text draws before translucent
     * terrain, the pane then blends over it — which holds for these rows
     * exactly as it holds for the visible arrival text.)</p>
     *
     * <p><b>Structure</b>: the rows keep channel Q's proven scheduling
     * skeleton verbatim — real TextWrappers through {@code run() →
     * drawTransformed → scheduleRender(TEXT)} from renderAll head, the
     * platform half BEFORE replayScript (FIFO ⇒ the script text paints
     * over the rectangles), the track half after. The only change is the
     * row content: {@code text(fillChars).fontMC().color(c).jcm$backgroundColor(c)}
     * — spaces (or a digit fallback if a pack zeroes the space advance)
     * sized to the window, each row's glyph tint set to the SAME colour
     * as its rectangle so any glyph pixels that do render blend
     * seamlessly into the fill. The background path itself lives in
     * {@link TextWrapper#jcm$drawTextWithBackground} — the real
     * TextRenderer invocation MTR's own drawText performs, with the
     * backgroundColor parameter finally non-zero.</p>
     *
     * <p><b>Diagnostics</b> (one field test, three outcomes — see
     * {@link #DIAG_STRIPE_COLOR}): a glyph PROBE row of real characters
     * (the script's own visual language, the proven orange) rides above
     * the rectangles, and the platform half's first background stripe is
     * orange and its last white. All visible ⇒ fixed (stripes/probe out
     * next build); probe visible but rectangles missing ⇒ the
     * background-rect path itself dies on this stack; everything
     * invisible ⇒ our submission context is the killer and round 17
     * escalates to replay-injected rows (see PSD-SHADER-BACKING.md
     * §7.0-16).</p>
     *
     * <p><b>Cost</b> (test build): one TextWrapper + one
     * {@code StoredMatrixTransformations.copy()} per row per segment per
     * frame (~17 rows for a 255px window) — the same order as the
     * script's own text rows, none of it on no-shader pipelines (the
     * channel stays {@link com.lx862.jcm.mod.util.ShaderDetector}-gated).
     * A shipping round can pool both if this works.</p>
     */
    private static void drawBackingTextRows(World world, StoredMatrixTransformations transform, float left, float right, float bottom, float top, BlockPos anchor, Direction facing, boolean trackSide, double playerX, double playerY, double playerZ) {
        /* Camera-side gate at SCHEDULE time (rounds 15-16 — the
           shadow-pass trap, see the class docs): facing points toward
           the TRACK, so a player with along = dot(player - blockCenter,
           facing) >= Q_CAMERA_GATE is on (or past) the pane plane on the
           track side — exactly one half is submitted per view. */
        final org.mtr.mapping.holder.Vector3i fv = facing.getVector();
        final double along = (playerX - anchor.getX() - 0.5) * fv.getX()
                + (playerY - anchor.getY() - 0.5) * fv.getY()
                + (playerZ - anchor.getZ() - 0.5) * fv.getZ();
        if ((along >= Q_CAMERA_GATE) != trackSide) {
            return; // the other half owns this view
        }

        /* Window pixel geometry: 96 px per block, the same canvas scale
           the script itself draws on. The fill advance is measured once
           with the EXACT font the rows will draw in — fontMC, the
           vanilla default font, matching the String overload the
           background path feeds the real TextRenderer with. */
        final float wPx = (right - left) * 96;
        final float hPx = (top - bottom) * 96;
        if (wPx <= 0 || hPx <= 0) {
            return;
        }
        if (R_ADVANCE == Integer.MIN_VALUE) {
            final int spaceAdvance = GraphicsHolder.getTextWidth(" ");
            R_VANILLA_SPACE_ADVANCE = spaceAdvance;
            if (spaceAdvance >= 1) {
                R_FILL_CHAR = " ";
                R_ADVANCE = spaceAdvance;
            } else {
                /* A font pack that zeroes the space advance would
                   collapse every rectangle — fall back to a digit, whose
                   advance every sane pack keeps (the script's own ETA
                   digits render visibly through it). */
                R_FILL_CHAR = "0";
                R_ADVANCE = Math.max(1, GraphicsHolder.getTextWidth("0"));
            }
        }
        final int advance = R_ADVANCE;
        if (advance <= 0) {
            if (!BACKING_R_WARNED) {
                BACKING_R_WARNED = true;
                com.lx862.mtrscripting.mod.MTRScriptingModClient.LOGGER.warn(
                        "[JCM] PSD backing channel R degenerate: fill advance " + advance + " — nothing drawn");
            }
            return;
        }

        /* +2 covers the Drawer's 1px rectangle margins on both sides;
           the wrapper's own stretchXY trims ≤1 character of overhang. */
        final int cols = Math.max(1, (int) Math.ceil((wPx + 2) / advance));
        final StringBuilder rowBuilder = new StringBuilder(cols);
        for (int i = 0; i < cols; i++) {
            rowBuilder.append(R_FILL_CHAR);
        }
        final String rowText = rowBuilder.toString();

        /* The glyph probe row (platform half only): real characters in
           the script's own visual language, the proven orange, the
           script's own 1.725 row scale — the round-15 channel-Q lineage
           verbatim minus the one variable round 15 left (the glyph). */
        if (!trackSide) {
            final TextWrapper probe = TextWrapper.create();
            probe.text(R_PROBE_TEXT).scale(1.725).size((wPx / 1.725) - 4, 9).stretchXY().fontMC().color(DIAG_STRIPE_COLOR).pos(2, 1);
            probe.run(world, transform.copy(), facing, MAX_RENDER_LIGHT);
        }

        /* The rectangle rows: the platform half starts below the probe
           row and paints its FIRST stripe orange and its LAST white;
           the track half is all white. */
        final float startY = trackSide ? 0 : PROBE_ROW_PX;
        int rows = 0;
        for (float y = startY; y < hPx; y += Q_TEXT_ROW_PX) {
            final float rowH = Math.min(Q_TEXT_ROW_PX, hPx - y);
            final boolean topStripe = !trackSide && y == startY;
            final boolean bottomStripe = !trackSide && y + Q_TEXT_ROW_PX >= hPx;
            final int color = trackSide ? TRACK_SIDE_COLOR
                    : topStripe ? DIAG_STRIPE_COLOR
                    : bottomStripe ? TRACK_SIDE_COLOR
                    : Q_FILL_COLOR;

            final TextWrapper wrapper = TextWrapper.create();
            /* fontMC — the vanilla default font — so the wrapper's
               measured width (getFormattedText → empty style) and the
               background path's String-overload advances can never
               disagree. The glyph tint EQUALS the rectangle colour: any
               glyph pixels that render blend seamlessly into the fill. */
            wrapper.text(rowText).pos(0, y).size(wPx, rowH).stretchXY().fontMC().color(color).jcm$backgroundColor(color);
            /* Each row gets its OWN transform copy: run() APPENDS the
               wrapper's position consumer (BASE_SCALE + translate) to
               the StoredMatrixTransformations it is given (see
               PIDSDrawCall.run), so a shared instance would accumulate
               every row's translation — the replay path hands each
               captured call its own snapshot for exactly this reason
               (TransformCapturePool). The wrapper stays referenced by
               the scheduled closure until the flush reads its fields. */
            wrapper.run(world, transform.copy(), facing, MAX_RENDER_LIGHT);
            rows++;
        }
        if (rows <= 0) {
            return; // window shorter than the probe row; probe only
        }

        if (!BACKING_R_OK) {
            BACKING_R_OK = true;
            com.lx862.mtrscripting.mod.MTRScriptingModClient.LOGGER.info(
                    "[JCM] PSD backing channel R active (text-background rows, " + (trackSide ? "track" : "platform") + " half)");
            if (!trackSide) {
                com.lx862.mtrscripting.mod.MTRScriptingModClient.LOGGER.info(
                        "[JCM] PSD backing channel R metrics: window " + Math.round(wPx) + "x" + Math.round(hPx)
                                + "px, fill '" + (R_FILL_CHAR.equals(" ") ? "space" : R_FILL_CHAR) + "' advance " + R_ADVANCE
                                + " (vanilla space " + R_VANILLA_SPACE_ADVANCE + "), rows " + rows + " x cols " + cols
                                + ", probe row orange glyphs + stripes orange(top)/white(bottom)");
            }
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
