package com.lx862.jcm.mod.scripting.psd;

import com.lx862.jcm.mod.data.pids.PIDSManager;
import com.lx862.jcm.mod.data.pids.preset.PIDSPresetBase;
import com.lx862.jcm.mod.data.pids.preset.ScriptPIDSPreset;
import com.lx862.jcm.mod.block.APGGlassDRL;
import com.lx862.jcm.mod.block.APGGlassEndDRL;
import com.lx862.jcm.mod.render.RenderHelper;
import com.lx862.jcm.mod.scripting.JCMScripting;
import com.lx862.jcm.mod.scripting.pids.PIDSWrapper;
import com.lx862.jcm.nativeapi.NativeContentDriver;
import com.lx862.jcm.nativeapi.NativeScriptManager;
import com.lx862.mtrscripting.core.primitive.ParsedScript;
import com.lx862.mtrscripting.core.primitive.ScriptInstance;
import com.lx862.mtrscripting.core.primitive.UniqueKey;
import com.lx862.mtrscripting.core.util.ScriptVector3f;
import com.lx862.mtrscripting.mod.resource.MtrScriptingResourceManager;
import com.lx862.mtrscripting.mod.resource.PSDResourceProvider;
/* The synchronous backing draw needs a RAW immediate-style consumer
   (private buffer + synchronous flush, see drawSync). The raw type names
   are loader-specific — Yarn on fabric, Mojmap on forge — so this is the
   only loader branch in the file; everything else sticks to the
   org.mtr.mapping abstractions. */
#if LOADER == "fabric"
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
#else
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
#endif
import org.mtr.core.data.Platform;
import org.mtr.core.operation.ArrivalResponse;
import org.mtr.libraries.it.unimi.dsi.fastutil.longs.LongImmutableList;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.mtr.mapping.holder.BlockPos;
import org.mtr.mapping.holder.BlockState;
import org.mtr.mapping.holder.ClientPlayerEntity;
import org.mtr.mapping.holder.Direction;
import org.mtr.mapping.holder.MinecraftClient;
import org.mtr.mapping.holder.Vector3d;
import org.mtr.mapping.holder.World;
import org.mtr.mapping.mapper.GraphicsHolder;
import org.mtr.mapping.mapper.OptimizedRenderer;
import org.mtr.mod.InitClient;
import org.mtr.mod.block.BlockPSDAPGBase;
import org.mtr.mod.block.BlockPSDAPGGlassBase;
import org.mtr.mod.block.BlockPSDTop;
import org.mtr.mod.block.IBlock;
import org.mtr.mod.data.ArrivalsCacheClient;
import org.mtr.mod.render.StoredMatrixTransformations;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static com.lx862.jcm.mod.render.RenderHelper.ARGB_WHITE;
import static com.lx862.jcm.mod.render.RenderHelper.MAX_RENDER_LIGHT;

/**
 * The per-frame PSD script render pass, driven from a HEAD injection into
 * {@code MainRenderer.render} (the same queued-render pipeline MTR's own PSD
 * renderers schedule into).
 *
 * <h2>Geometry</h2>
 * All faces live in the frame MTR's own PSD renderers use —
 * {@code translate(0.5+X, Y, 0.5+Z) → rotateY(-facing.asRotation())} — where
 * local +Z points toward the track, the glass pane occupies z∈[4/16, 6/16]
 * and the psd_top display plane sits at z = 2/16 - ε:
 *
 * <ul>
 *   <li><b>Glass</b> (bound per block half; the editor item binds both halves
 *       of a column at once): the <b>glass segment</b> the bound column
 *       belongs to (the contiguous glass/glass-end run between the doors —
 *       exactly the glass the brush merged, see {@link PSDRuns}) becomes a
 *       scripted panel <b>sandwiched INSIDE the pane</b> (the pane glass is
 *       z∈[4/16, 6/16]): an opaque white backing at z = 5/16 (pane centre,
 *       drawn double-sided) and, just behind the platform-side pane face at
 *       z = 4.5/16, the script canvas. The pane's own texture blends over the
 *       sandwich, so the script is visible exactly through the pane's
 *       <b>originally transparent window</b> while the opaque parts of the
 *       texture — the frame, divider bars, frosted bands, whatever the
 *       resource pack draws — stay fully visible and clip the canvas; nothing
 *       is painted over the frame. The white backing hides the script from
 *       the track side (passengers inside a stopped train see plain white
 *       through the window, never mirrored content).</li>
 *   <li><b>Window clipping</b> (see {@link PSDWindowBounds}): the backing and
 *       the canvas are additionally clipped to the pane's transparent
 *       window, derived at runtime from the block's actual baked model and
 *       texture alpha — resource packs replace both, and a pack whose
 *       "window" is a tint and whose "frame" is only semi-transparent would
 *       otherwise let sandwiched content (and the white backing itself) show
 *       through the frame region. Blocks whose window cannot be derived fall
 *       back to the full face, matching the pre-clipping behaviour.</li>
 *   <li><b>Flush discipline</b>: the white backing is NOT drawn through the
 *       shared entity-phase vertex consumer. Deferred-batching pipelines
 *       (ImmediatelyFast + Iris/Sodium shaderpacks) may flush that shared
 *       consumer's buffers only AFTER the translucent terrain — the pane
 *       then already owns the depth buffer, and a depth-tested quad at the
 *       pane centre silently fails the depth test behind the pane's front
 *       face: the backing vanishes while text (which MTR's
 *       {@code GraphicsHolder.drawText} renders through its own private,
 *       synchronously flushed immediate) still shows. The backing therefore
 *       replicates drawText's discipline — a private
 *       {@code VertexConsumerProvider.Immediate} created and flushed inside
 *       the draw itself — so it always lands BEFORE the translucent pane,
 *       under every pipeline.</li>
 *   <li><b>Top</b> (bound to a psd_top block, whose own arrow/route content is
 *       cancelled group-wide by {@code RenderRouteBaseMixin}): the script's
 *       draw calls replay across the <b>psd_top group the bound block
 *       belongs to</b> — the psd_top stretch sitting on one wall section
 *       below (one brush-merged door run, or one glass segment; see
 *       {@link PSDRuns} for why the wall below defines the sections) — as one
 *       groupWidth×42 PIDS pixel canvas (side padding 0.125/16, top 7.5/16,
 *       bottom 1.5/16, straight from RenderPSDTop). The rest of the psd_top
 *       band keeps MTR's own arrows and route maps. No white base is forced;
 *       the script fully controls the content.</li>
 * </ul>
 *
 * <h2>Execution model</h2>
 * Scripts (JS and C++) are DRIVEN once per bound block per frame; the
 * captured draw calls are REPLAYED onto each visible face (the one merged
 * platform-side glass canvas / the one merged top canvas). Sounds replay
 * exactly once per frame. Shader shadow passes replay the last captured draw
 * calls (silhouette correctness) without executing scripts, uploading
 * textures or driving the C++ side — mirroring the eyecandy/PIDS shadow
 * contract.
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

    /** Glass pane faces in the local frame (16ths): the pane glass occupies
     *  z∈[4/16, 6/16] (platform-side face at 4/16, track-side face at 6/16 —
     *  the same geometry MTR's own APG glass models use, which
     *  RenderAPGGlass draws its station band against). */
    private static final float GLASS_PLATFORM_Z = 4F / 16;

    /** The scripted sandwich lives INSIDE the pane: the white backing at the
     *  pane centre and the script canvas just behind the platform-side pane
     *  face. Both are separated from every pane face by 1/32 of a block — far
     *  beyond any depth-precision z-fighting range — and because the pane's
     *  translucent faces blend over them afterwards, the pane texture itself
     *  decides where the sandwich shows: only through its transparent window
     *  pixels, never over the frame. */
    private static final float GLASS_BACKING_Z = 5F / 16;
    private static final float GLASS_SCRIPT_Z = GLASS_PLATFORM_Z + 0.5F / 16;

    /** A window narrower than this (block fraction) is treated as no window. */
    private static final float MIN_WINDOW = 0.05F;

    /** MTR ships a plain opaque white texture; the mapping-layer
        Identifier/RenderLayer pair is loader-neutral (same statics MTR's
        own MoreRenderLayers uses). */
    private static final org.mtr.mapping.holder.Identifier WHITE_TEXTURE = new org.mtr.mapping.holder.Identifier("mtr", "textures/block/white.png");

    /** Distance² gate — bindings beyond this are not driven. */
    private static final double MAX_DISTANCE_SQ = 192 * 192;

    private PSDScriptRenderer() {
    }

    /** Called from MainRendererMixin at the head of MainRenderer.render.
     * The camera {@code offset} feeds the synchronously-flushed backing
     * quads (drawn directly, ahead of the queued-layer flush). */
    public static void renderAll(Vector3d offset) {
        final MinecraftClient minecraftClient = MinecraftClient.getInstance();
        /* MTR's mapping wrappers are unrelated types — ClientWorld reaches the
           World-typed helpers (native driver, InitClient) through World.cast. */
        final World world = World.cast(minecraftClient.getWorldMapped());
        final ClientPlayerEntity player = minecraftClient.getPlayerMapped();
        if (world == null || player == null) return;
        if (PSDClientBindings.isEmpty()) return;

        final boolean shadowPass = OptimizedRenderer.renderingShadows();
        final double playerX = player.getX();
        final double playerY = player.getY();
        final double playerZ = player.getZ();

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
                    renderTop(binding, pos, state, world, shadowPass);
                }
            } else {
                if (block instanceof BlockPSDAPGGlassBase) {
                    renderGlass(binding, pos, state, world, shadowPass, offset);
                }
            }
        });
    }

    /* ==================================================================== */
    /* Glass                                                                 */
    /* ==================================================================== */

    private static void renderGlass(PSDBinding binding, BlockPos pos, BlockState state, World world, boolean shadowPass, Vector3d offset) {
        final Direction facing = IBlock.getStatePropertySafe(state, BlockPSDAPGBase.FACING);
        final boolean upper = IBlock.getStatePropertySafe(state, IBlock.HALF) == IBlock.DoubleBlockHalf.UPPER;
        final Object block = state.getBlock().data;

        /* Yanyang fix: JCM's own DRL APG glass (and its end cap) has NO pane
           in the upper half — the model is transparent above the ~2/16 light
           strip (the DRL gate is full glass in the LOWER half only), unlike
           MTR's own APG glass whose upper half carries a 9/16 pane. There
           is no meaningful surface there, so the upper half of a DRL glass
           column renders nothing at all — the lower half (the column's full
           display surface) covers the whole segment. */
        if (upper && (block instanceof APGGlassDRL || block instanceof APGGlassEndDRL)) {
            return;
        }

        /* The whole glass segment shares one canvas (see PSDRuns for the
           section walk and the anchor convention). */
        final PSDRuns.Run run = PSDRuns.glassRun(world, pos, facing);

        /* The pane's transparent window, per block, straight from the baked
           model + texture alpha (see PSDWindowBounds) — pack-dependent, so
           nothing is hardcoded. The window IS the pane face's actual
           transparent region (both its geometry extent and its texture
           alpha), so half-height panes anchor correctly on their own — the
           old code assumed every pane sits at the TOP of its block, which
           is wrong for MTR's APG gates (their upper-half pane sits at the
           BOTTOM of the upper block, world y 1.0–1.5625) and mislocated
           the canvas over thin air. The shared canvas is clipped to the
           UNION of the run's windows; the white backing uses each block's
           own window. Blocks whose window can't be derived fall back to
           the full face. */
        final float[] blockTop = new float[run.width];
        final float[] blockBottom = new float[run.width];
        float windowTop = 1;
        float windowBottom = 0;
        for (int i = 0; i < run.width; i++) {
            final float[] w = PSDWindowBounds.window(world.getBlockState(run.start.offset(facing.rotateYClockwise(), i)), facing);
            final float top = Math.min(w[0], 1);
            final float bottom = Math.max(w[1], 0);
            blockTop[i] = top;
            blockBottom[i] = bottom;
            if (top - bottom >= MIN_WINDOW) {
                windowTop = Math.min(windowTop, top);
                windowBottom = Math.max(windowBottom, bottom);
            }
        }

        /* Canvas coordinates: y=0 at the block top, y grows downward; the
           canvas is clipped to the window, so script content (which lays
           out against pids.height) never lands on the frame strips. */
        final int canvasWidth = run.width * 96;
        final int canvasHeight = Math.max(8, Math.round((windowTop - windowBottom) * 96));
        final float canvasTop = 1 - windowTop;

        if (!shadowPass) {
            /* Opaque white backing at the pane centre, drawn double-sided per
               block: it reads as "the glass turned white" through the pane's
               transparent window from BOTH sides, and it hides the script
               canvas from the track side (train passengers never see mirrored
               content). Drawn through a private, synchronously flushed
               immediate (see the class javadoc's flush-discipline note) —
               the shared entity-phase consumer may only be flushed after the
               translucent pane under deferred-batching pipelines (Immediately
               Fast + Iris), which used to make this quad fail the depth test
               behind the pane's front face and vanish. */
            final StoredMatrixTransformations backing = glassTransform(run.start, facing, GLASS_BACKING_Z);
            for (int i = 0; i < run.width; i++) {
                if (blockTop[i] - blockBottom[i] < MIN_WINDOW) continue;
                drawWhiteSync(backing, offset, i, blockTop[i], blockBottom[i], facing);
            }
            driveScript(binding, pos, world, canvasWidth, canvasHeight);
        }

        /* The script canvas sits just behind the platform-side pane face:
           the translucent pane blends over it, so the content shows exactly
           through the pane's originally transparent window while every
           opaque pixel of the pane texture (frame, bars, frosted bands —
           whatever the resource pack draws) stays visible in front of it. */
        final StoredMatrixTransformations scriptSide = glassTransform(run.start, facing, GLASS_SCRIPT_Z);
        scriptSide.add(graphicsHolder -> graphicsHolder.translate(0, canvasTop, 0));
        replayScript(binding, pos, world, scriptSide, facing.getOpposite());
        if (!shadowPass) {
            replaySoundsOnce(binding, pos, world);
        }
    }

    /**
     * The canvas frame for the glass sandwich: the origin sits at the top-left
     * of the pane face as seen from the platform side, y runs down, and +Z
     * points toward the track (deeper into the pane). All sandwich planes
     * (script at 4.5/16, backing at 5/16) use the same frame — only the z
     * translation differs.
     */
    private static StoredMatrixTransformations glassTransform(BlockPos pos, Direction facing, float z) {
        final StoredMatrixTransformations transform = new StoredMatrixTransformations(0.5 + pos.getX(), pos.getY(), 0.5 + pos.getZ());
        transform.add(graphicsHolder -> graphicsHolder.rotateYDegrees(-facing.asRotation()));
        transform.add(graphicsHolder -> graphicsHolder.translate(0, 1, 0));
        transform.add(graphicsHolder -> graphicsHolder.rotateZDegrees(180));
        transform.add(graphicsHolder -> graphicsHolder.translate(-0.5, 0, z));
        return transform;
    }

    /* ==================================================================== */
    /* Synchronous backing draw (private immediate, see class javadoc)      */
    /* ==================================================================== */

    /**
     * Draws through a PRIVATE {@code VertexConsumerProvider.Immediate} that
     * is flushed synchronously at the end of this call — the same flush
     * discipline MTR's own {@code GraphicsHolder.drawText} uses for every
     * scripted text. Deferred-batching pipelines (ImmediatelyFast with
     * Iris/Sodium shaderpacks) may hold the SHARED entity-phase consumer's
     * buffers until after the translucent terrain; anything drawn through
     * it from BEHIND a translucent face (the pane's platform-side face at
     * 4/16 is in front of the pane-centre backing at 5/16) then fails the
     * depth test and silently vanishes. A private immediate, drawn and
     * flushed right here, always lands before the translucent pane.
     */
    private static void drawSync(StoredMatrixTransformations transform, Vector3d offset, Consumer<GraphicsHolder> drawer) {
        try {
#if LOADER == "fabric"
            final MatrixStack matrices = new MatrixStack();
            final BufferBuilder buffer = new BufferBuilder(256);
            final VertexConsumerProvider.Immediate immediate = VertexConsumerProvider.immediate(buffer);
#else
            final PoseStack matrices = new PoseStack();
            final BufferBuilder buffer = new BufferBuilder(256);
            final MultiBufferSource.BufferSource immediate = MultiBufferSource.immediate(buffer);
#endif
            GraphicsHolder.createInstanceSafe(matrices, immediate, holder -> {
                transform.transform(holder, offset);
                drawer.accept(holder);
                holder.pop();
            });
#if LOADER == "fabric"
            immediate.draw();
#else
            immediate.endBatch();
#endif
        } catch (Throwable ignored) {
            /* Never take the render thread down over a backing quad. */
        }
    }

    /**
     * The opaque white pane backing — one block-wide quad (canvas x
     * ∈ [x, x+1], y ∈ [1-top, 1-bottom] in full-face canvas coordinates),
     * drawn double-sided via BOTH windings (the entity-cutout layer culls
     * back faces, so a second, reversed quad serves the track side).
     */
    private static void drawWhiteSync(StoredMatrixTransformations transform, Vector3d offset, float x, float top, float bottom, Direction facing) {
        final float y = 1 - top;
        final float height = top - bottom;
        drawSync(transform, offset, holder -> {
            holder.createVertexConsumer(org.mtr.mapping.holder.RenderLayer.getEntityCutout(WHITE_TEXTURE));
            holder.drawTextureInWorld(x, y + height, 0, x + 1, y + height, 0, x + 1, y, 0, x, y, 0, 0, 0, 1, 1, facing.getOpposite(), ARGB_WHITE, MAX_RENDER_LIGHT);
            holder.drawTextureInWorld(x, y, 0, x + 1, y, 0, x + 1, y + height, 0, x, y + height, 0, 0, 0, 1, 1, facing, ARGB_WHITE, MAX_RENDER_LIGHT);
        });
    }

    /* ==================================================================== */
    /* Top panel                                                             */
    /* ==================================================================== */

    private static void renderTop(PSDBinding binding, BlockPos pos, BlockState state, World world, boolean shadowPass) {
        final Direction facing = IBlock.getStatePropertySafe(state, BlockPSDTop.FACING);
        final boolean persistent = IBlock.getStatePropertySafe(state, BlockPSDTop.PERSISTENT) != BlockPSDTop.EnumPersistent.NONE;
        final float additionalOffset = persistent ? BlockPSDTop.PERSISTENT_OFFSET_SMALL : 0;

        /* One canvas across the psd_top GROUP the bound block belongs to —
           the psd_top stretch sitting on one wall section below (one door
           run / one glass segment; see PSDRuns for why the wall below
           defines the sections). Anchored at the group's left-most block,
           padded exactly like MTR's own band; the rest of the band keeps
           MTR's own arrows and route maps. */
        final PSDRuns.Run run = PSDRuns.topRun(world, pos, facing);
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
