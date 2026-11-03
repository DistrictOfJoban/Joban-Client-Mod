package com.lx862.jcm.mod.scripting.psd;

import com.lx862.jcm.mod.data.pids.PIDSManager;
import com.lx862.jcm.mod.data.pids.preset.PIDSPresetBase;
import com.lx862.jcm.mod.data.pids.preset.ScriptPIDSPreset;
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
import org.mtr.core.data.Platform;
import org.mtr.core.operation.ArrivalResponse;
import org.mtr.libraries.it.unimi.dsi.fastutil.longs.LongImmutableList;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.mtr.mapping.holder.BlockPos;
import org.mtr.mapping.holder.BlockState;
import org.mtr.mapping.holder.ClientPlayerEntity;
import org.mtr.mapping.holder.ClientWorld;
import org.mtr.mapping.holder.Direction;
import org.mtr.mapping.holder.Identifier;
import org.mtr.mapping.holder.MinecraftClient;
import org.mtr.mapping.mapper.OptimizedRenderer;
import org.mtr.mod.InitClient;
import org.mtr.mod.block.BlockAPGGlass;
import org.mtr.mod.block.BlockAPGGlassEnd;
import org.mtr.mod.block.BlockPSDAPGBase;
import org.mtr.mod.block.BlockPSDAPGGlassBase;
import org.mtr.mod.block.BlockPSDTop;
import org.mtr.mod.block.IBlock;
import org.mtr.mod.data.ArrivalsCacheClient;
import org.mtr.mod.render.MainRenderer;
import org.mtr.mod.render.QueuedRenderLayer;
import org.mtr.mod.render.StoredMatrixTransformations;

import java.util.concurrent.atomic.AtomicReference;

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
 *       of a column at once): an opaque white quad pair is drawn just outside
 *       both pane faces (z = 4/16 - ε platform side, z = 6/16 + ε track side,
 *       x-mirrored so content reads correctly from behind), and the script's
 *       captured draw calls replay on top of the white. Each half's canvas is
 *       96×96 PIDS pixels (APG upper halves: 96×54, matching the 9-px gate
 *       glass).</li>
 *   <li><b>Top</b> (bound to a psd_top block, whose own arrow/route content
 *       is cancelled by {@code RenderRouteBaseMixin}): the script's draw
 *       calls replay on the psd_top display band — 78×42 PIDS pixels, the
 *       same band MTR's route map texture occupies. No white base is forced;
 *       the script fully controls the content.</li>
 * </ul>
 *
 * <h2>Execution model</h2>
 * Scripts (JS and C++) are DRIVEN once per bound block per frame; the
 * captured draw calls are REPLAYED onto each visible face (both glass faces /
 * the one top face). Sounds replay exactly once per frame. Shader shadow
 * passes replay the last captured draw calls (silhouette correctness)
 * without executing scripts, uploading textures or driving the C++ side —
 * mirroring the eyecandy/PIDS shadow contract.
 */
public final class PSDScriptRenderer implements RenderHelper {

    /** PIDS pixel scale — 96 px per block (PIDSPresetBase.BASE_SCALE). */
    private static final float BASE_SCALE = 1 / 96F;
    private static final float SMALL_OFFSET = 0.0005F;

    /** psd_top display band paddings (16ths of the block), from RenderPSDTop. */
    private static final float TOP_SIDE_PADDING = 1.5F / 16;
    private static final float TOP_TOP_PADDING = 7.5F / 16;
    private static final float TOP_BOTTOM_PADDING = 1.5F / 16;
    private static final float TOP_DISPLAY_Z = 2F / 16 - 0.05F / 16;

    /** Glass pane faces in the local frame (16ths). */
    private static final float GLASS_PLATFORM_Z = 4F / 16;
    private static final float GLASS_TRACK_Z = 6F / 16;

    /** APG (half-height gate) upper-half glass height. */
    private static final float APG_UPPER_HEIGHT = 9F / 16;

    private static final Identifier WHITE_TEXTURE = new Identifier("mtr", "textures/block/white.png");

    /** Distance² gate — bindings beyond this are not driven. */
    private static final double MAX_DISTANCE_SQ = 192 * 192;

    private PSDScriptRenderer() {
    }

    /** Called from MainRendererMixin at the head of MainRenderer.render. */
    public static void renderAll() {
        final MinecraftClient minecraftClient = MinecraftClient.getInstance();
        final ClientWorld world = minecraftClient.getWorldMapped();
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
                    renderGlass(binding, pos, state, world, shadowPass);
                }
            }
        });
    }

    /* ==================================================================== */
    /* Glass                                                                 */
    /* ==================================================================== */

    private static void renderGlass(PSDBinding binding, BlockPos pos, BlockState state, ClientWorld world, boolean shadowPass) {
        final Direction facing = IBlock.getStatePropertySafe(state, BlockPSDAPGBase.FACING);
        final boolean upper = IBlock.getStatePropertySafe(state, IBlock.HALF) == IBlock.DoubleBlockHalf.UPPER;
        final boolean apg = state.getBlock().data instanceof BlockAPGGlass || state.getBlock().data instanceof BlockAPGGlassEnd;

        // Half-height overlays for APG upper glass; full faces everywhere else.
        final float faceHeight = upper && apg ? APG_UPPER_HEIGHT : 1;
        final int canvasWidth = 96;
        final int canvasHeight = Math.round(faceHeight * 96);

        final StoredMatrixTransformations platformSide = glassTransform(pos, facing, GLASS_PLATFORM_Z - SMALL_OFFSET, false);
        final StoredMatrixTransformations trackSide = glassTransform(pos, facing, GLASS_TRACK_Z + SMALL_OFFSET, true);

        if (!shadowPass) {
            // Opaque white base on both faces — the glass "becomes" a white panel.
            scheduleWhite(platformSide, canvasWidth, canvasHeight, facing.getOpposite());
            scheduleWhite(trackSide, canvasWidth, canvasHeight, facing);
            driveScript(binding, pos, world, canvasWidth, canvasHeight);
        }

        // Replay the captured calls on both faces; sounds once.
        replayScript(binding, pos, world, platformSide, facing.getOpposite());
        replayScript(binding, pos, world, trackSide, facing);
        if (!shadowPass) {
            replaySoundsOnce(binding, pos, world);
        }
    }

    /**
     * The canvas frame for one glass face: the origin sits at the top-left of
     * the face (as seen from that side — the track side is x-mirrored so
     * content reads correctly from behind), y runs down, +Z faces the viewer.
     */
    private static StoredMatrixTransformations glassTransform(BlockPos pos, Direction facing, float z, boolean trackSide) {
        final StoredMatrixTransformations transform = new StoredMatrixTransformations(0.5 + pos.getX(), pos.getY(), 0.5 + pos.getZ());
        transform.add(graphicsHolder -> graphicsHolder.rotateYDegrees(-facing.asRotation()));
        transform.add(graphicsHolder -> graphicsHolder.translate(0, 1, 0));
        transform.add(graphicsHolder -> graphicsHolder.rotateZDegrees(180));
        transform.add(graphicsHolder -> graphicsHolder.translate(-0.5, 0, z));
        if (trackSide) {
            transform.add(graphicsHolder -> graphicsHolder.scale(-1, 1, 1));
        }
        return transform;
    }

    private static void scheduleWhite(StoredMatrixTransformations transform, int canvasWidth, int canvasHeight, Direction direction) {
        MainRenderer.scheduleRender(WHITE_TEXTURE, false, QueuedRenderLayer.EXTERIOR, (graphicsHolder, offset) -> {
            transform.transform(graphicsHolder, offset);
            RenderHelper.drawTexture(graphicsHolder, 0, 0, 0, canvasWidth * BASE_SCALE, canvasHeight * BASE_SCALE, direction, ARGB_WHITE, MAX_RENDER_LIGHT);
            graphicsHolder.pop();
        });
    }

    /* ==================================================================== */
    /* Top panel                                                             */
    /* ==================================================================== */

    private static void renderTop(PSDBinding binding, BlockPos pos, BlockState state, ClientWorld world, boolean shadowPass) {
        final Direction facing = IBlock.getStatePropertySafe(state, BlockPSDTop.FACING);
        final boolean persistent = IBlock.getStatePropertySafe(state, BlockPSDTop.PERSISTENT) != BlockPSDTop.EnumPersistent.NONE;
        final float additionalOffset = persistent ? BlockPSDTop.PERSISTENT_OFFSET_SMALL : 0;

        // Same band RenderRouteBase draws the arrow/route texture into.
        final StoredMatrixTransformations transform = new StoredMatrixTransformations(0.5 + pos.getX(), pos.getY(), 0.5 + pos.getZ());
        transform.add(graphicsHolder -> graphicsHolder.rotateYDegrees(-facing.asRotation()));
        transform.add(graphicsHolder -> graphicsHolder.translate(0, 1, 0));
        transform.add(graphicsHolder -> graphicsHolder.rotateZDegrees(180));
        transform.add(graphicsHolder -> graphicsHolder.translate(-0.5, -additionalOffset, TOP_DISPLAY_Z - SMALL_OFFSET));
        transform.add(graphicsHolder -> graphicsHolder.translate(TOP_SIDE_PADDING, TOP_TOP_PADDING, 0));

        final int canvasWidth = Math.round((1 - TOP_SIDE_PADDING * 2) * 96);
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

    private static void driveScript(PSDBinding binding, BlockPos pos, ClientWorld world, int canvasWidth, int canvasHeight) {
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

    private static void replayScript(PSDBinding binding, BlockPos pos, ClientWorld world, StoredMatrixTransformations transform, Direction drawDirection) {
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
            if (instance instanceof PSDPidsScriptInstance) {
                ((PSDScriptContext) instance.getContextObject()).getRenderManager().invoke(world, transform, drawDirection, MAX_RENDER_LIGHT);
            }
        } else {
            if (NativeScriptManager.isLoaded(binding.scriptId)) {
                NativeContentDriver.psdReplay(binding.scriptId, pos, world, transform, drawDirection, MAX_RENDER_LIGHT);
            }
            final ScriptInstance<?> instance = JCMScripting.getScriptManager().getInstanceManager().getInstance(
                    new UniqueKey("psd", binding.scriptId, pos.getX(), pos.getY(), pos.getZ()));
            if (instance instanceof PSDScriptInstance) {
                ((PSDScriptContext) instance.getContextObject()).getRenderManager().invoke(world, transform, drawDirection, MAX_RENDER_LIGHT);
            }
        }
    }

    private static void replaySoundsOnce(PSDBinding binding, BlockPos pos, ClientWorld world) {
        if (binding.pidsPreset) {
            if (NativeScriptManager.isLoaded(binding.scriptId)) {
                NativeContentDriver.pidsReplaySounds(binding.scriptId, pos, world);
            }
            final ScriptInstance<?> instance = JCMScripting.getScriptManager().getInstanceManager().getInstance(
                    new UniqueKey("psd_pids", binding.scriptId, pos.getX(), pos.getY(), pos.getZ()));
            if (instance instanceof PSDPidsScriptInstance) {
                ((PSDScriptContext) instance.getContextObject()).getSoundManager().invoke(world, new ScriptVector3f(pos));
            }
        } else {
            if (NativeScriptManager.isLoaded(binding.scriptId)) {
                NativeContentDriver.psdReplaySounds(binding.scriptId, pos, world);
            }
            final ScriptInstance<?> instance = JCMScripting.getScriptManager().getInstanceManager().getInstance(
                    new UniqueKey("psd", binding.scriptId, pos.getX(), pos.getY(), pos.getZ()));
            if (instance instanceof PSDScriptInstance) {
                ((PSDScriptContext) instance.getContextObject()).getSoundManager().invoke(world, new ScriptVector3f(pos));
            }
        }
    }

    /** The PIDS-preset wrapper for a PSD binding — neutral identity, the PSD's
     *  own canvas and platform/arrival data. */
    private static PIDSWrapper synthesizePidsWrapper(PSDBinding binding, BlockPos pos, int canvasWidth, int canvasHeight) {
        final Platform platform = findPlatform(pos, binding.type);
        final LongImmutableList platformIds = platform == null ? new LongImmutableList(new long[0]) : new LongImmutableList(new long[]{platform.getId()});
        final ObjectArrayList<ArrivalResponse> arrivals = ArrivalsCacheClient.INSTANCE.requestArrivals(platformIds);
        return new PIDSWrapper(
                "psd_" + binding.type.id, 3, pos, platformIds, arrivals,
                canvasWidth, canvasHeight,
                false, platform == null, false,
                new boolean[3], new String[3]
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