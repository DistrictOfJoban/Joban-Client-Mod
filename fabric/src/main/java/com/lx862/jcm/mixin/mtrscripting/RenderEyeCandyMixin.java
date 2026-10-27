package com.lx862.jcm.mixin.mtrscripting;

import com.lx862.mtrscripting.core.util.ScriptVector3f;
import com.lx862.mtrscripting.mod.resource.EyecandyResourceProvider;
import com.lx862.mtrscripting.mod.resource.MtrScriptingResourceManager;
import com.lx862.mtrscripting.mod.impl.mtr.MTRContentScripting;
import com.lx862.mtrscripting.mod.impl.mtr.eyecandy.EyeCandyScriptContext;
import com.lx862.mtrscripting.mod.impl.mtr.eyecandy.EyeCandyScriptInstance;
import com.lx862.mtrscripting.mod.impl.mtr.eyecandy.EyecandyBlockEntityWrapper;
import com.lx862.mtrscripting.core.primitive.ParsedScript;
import com.lx862.mtrscripting.core.primitive.UniqueKey;
import com.lx862.mtrscripting.core.primitive.ScriptInstance;
import org.mtr.mapping.holder.BlockPos;
import org.mtr.mapping.holder.Direction;
import org.mtr.mapping.holder.World;
import org.mtr.mapping.mapper.GraphicsHolder;
import org.mtr.mapping.mapper.OptimizedRenderer;
import org.mtr.mod.block.BlockEyeCandy;
import org.mtr.mod.block.IBlock;
import org.mtr.mod.render.RenderEyeCandy;
import org.mtr.mod.render.StoredMatrixTransformations;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;
import java.util.WeakHashMap;

@Mixin(value = RenderEyeCandy.class, remap = false)
public class RenderEyeCandyMixin {
    /**
     * Per-block-entity transform cache: the block's pos/translate/rotate
     * values change only when its NBT changes, but the old path rebuilt a
     * fresh base {@code StoredMatrixTransformations} (position entry plus a
     * rotation lambda) per eyecandy per frame — and the JS path built a
     * base+copy pair on top. Entries die with the block entity (weak keys);
     * field comparison catches NBT edits exactly.
     *
     * <p><b>Capture discipline:</b> the cached consumer must capture
     * PRIMITIVES only. The map is a {@link WeakHashMap} keyed by the block
     * entity — a lambda capturing {@code be} would be stored in the value
     * and strongly reference the key, making the entry permanently
     * reachable (the classic WeakHashMap self-reference leak).
     *
     * <p>The cached transform is used directly as the replay root:
     * {@code ScriptRenderManager.invoke} replays every draw call against
     * its own pooled snapshot (see {@link com.lx862.mtrscripting.core.util.render.TransformCapturePool}),
     * so a shared base is neither mutated nor cross-contaminated and needs
     * no per-frame {@code copy()}.
     */
    private static final class TransformCache {
        final StoredMatrixTransformations transform = new StoredMatrixTransformations();
        int px, py, pz;
        float tx, ty, tz, rx, ry, rz;
        Direction facing;
        boolean initialized;

        boolean matches(BlockEyeCandy.BlockEntity be, Direction facing) {
            BlockPos pos = be.getPos2();
            return this.initialized
                    && this.px == pos.getX() && this.py == pos.getY() && this.pz == pos.getZ()
                    && this.tx == be.getTranslateX() && this.ty == be.getTranslateY() && this.tz == be.getTranslateZ()
                    && this.rx == be.getRotateX() && this.ry == be.getRotateY() && this.rz == be.getRotateZ()
                    && this.facing == facing;
        }

        void rebuild(BlockEyeCandy.BlockEntity be, Direction facing) {
            final StoredMatrixTransformations transform = this.transform;
            final BlockPos pos = be.getPos2();
            /* Primitives only — see the capture discipline note above. */
            final double bx = 0.5 + pos.getX(), by = pos.getY(), bz = 0.5 + pos.getZ();
            final float ltx = be.getTranslateX(), lty = be.getTranslateY(), ltz = be.getTranslateZ();
            final float lrx = be.getRotateX(), lry = be.getRotateY(), lrz = be.getRotateZ();
            final float facingRot = -(float) Math.toRadians(facing.asRotation()) + (float) Math.PI;
            transform.add(gh -> {
                gh.translate(bx, by, bz);
                gh.translate(ltx, lty, ltz);
                gh.rotateYRadians(facingRot);
                gh.rotateXRadians(lrx + (float) Math.PI);
                gh.rotateYRadians(lry);
                gh.rotateZRadians(lrz);
            });
            this.px = pos.getX();
            this.py = pos.getY();
            this.pz = pos.getZ();
            this.tx = ltx;
            this.ty = lty;
            this.tz = ltz;
            this.rx = lrx;
            this.ry = lry;
            this.rz = lrz;
            this.facing = facing;
            this.initialized = true;
        }
    }

    private static final Map<BlockEyeCandy.BlockEntity, TransformCache> TRANSFORMS = new WeakHashMap<>();

    private static StoredMatrixTransformations transformOf(BlockEyeCandy.BlockEntity blockEntity, Direction facing) {
        TransformCache cache = TRANSFORMS.get(blockEntity);
        if (cache == null) {
            cache = new TransformCache();
            TRANSFORMS.put(blockEntity, cache);
        }
        if (!cache.matches(blockEntity, facing)) {
            ((com.lx862.jcm.nativeapi.NativeTransformScope) (Object) cache.transform).jcm$clearTransformations();
            cache.rebuild(blockEntity, facing);
        }
        return cache.transform;
    }

    @Inject(method = "render(Lorg/mtr/mod/block/BlockEyeCandy$BlockEntity;FLorg/mtr/mapping/mapper/GraphicsHolder;II)V", at = @At(value = "INVOKE", target = "Lorg/mtr/mod/block/BlockEyeCandy$BlockEntity;getModelId()Ljava/lang/String;"), cancellable = true)
    public void jsblock$executeAndRenderScript(BlockEyeCandy.BlockEntity blockEntity, float tickDelta, GraphicsHolder graphicsHolder, int light, int overlay, CallbackInfo ci) {
        World world = blockEntity.getWorld2();
        if(world == null) return;
        if(world.getBlockEntity(blockEntity.getPos2()) == null) return; // Cached BE can continue to render outside the render distance

        String eyecandyId = blockEntity.getModelId();
        EyecandyResourceProvider.EyecandyScriptConfiguration eyecandyScriptEntry = MtrScriptingResourceManager.eyecandy.getScriptEntry(eyecandyId);
        if(eyecandyScriptEntry == null) return;
        String nativeId=MtrScriptingResourceManager.eyecandy.getScriptEntryId(eyecandyId);
        if(com.lx862.jcm.nativeapi.NativeScriptManager.isLoaded(nativeId)) {
            Direction facing=IBlock.getStatePropertySafe(world,blockEntity.getPos2(),BlockEyeCandy.FACING);
            /* Cached base (rebuilt only on NBT change): invoke replays every
               draw call against pooled snapshots seeded from this base, so
               the cached instance serves directly as the replay root. */
            StoredMatrixTransformations transforms=transformOf(blockEntity, facing);
            com.lx862.jcm.nativeapi.NativeContentDriver.eyecandy(nativeId,blockEntity,world,transforms,facing,light);
            if(MtrScriptingResourceManager.eyecandy.isScriptControlledRendering(eyecandyId)) ci.cancel();
            /* Yanyang: mixed entries (JS + C++ modules under one script id)
               keep running — the JS part below executes in addition to the
               native modules above; ci.cancel() only suppresses MTR's
               built-in model, not the rest of this injector. */
        }
        ParsedScript parsedScript = eyecandyScriptEntry.parsedScript();
        if(parsedScript == null) return; /* native-only entry: nothing left to drive */

        ScriptInstance<EyecandyBlockEntityWrapper> scriptInstance = MTRContentScripting.getScriptManager().getInstanceManager().getInstance(new UniqueKey("eyecandy", eyecandyId, blockEntity.getPos2().getX(), blockEntity.getPos2().getY(), blockEntity.getPos2().getZ()), () -> new EyeCandyScriptInstance(blockEntity, new EyeCandyScriptContext(blockEntity), parsedScript));
        if(!(scriptInstance instanceof EyeCandyScriptInstance)) return;

        if(MtrScriptingResourceManager.eyecandy.isScriptControlledRendering(eyecandyId)) {
            ci.cancel();
        }

        EyeCandyScriptInstance eyeCandyScriptInstance = (EyeCandyScriptInstance) scriptInstance;
        EyeCandyScriptContext eyeCandyScriptContext = (EyeCandyScriptContext)scriptInstance.getContextObject();

        if(!OptimizedRenderer.renderingShadows()) {
            EyecandyBlockEntityWrapper eyecandyWrapper = new EyecandyBlockEntityWrapper(blockEntity);
            scriptInstance.setWrapperObject(eyecandyWrapper);
            scriptInstance.getScript().invokeRenderFunctions(scriptInstance, () -> {
                eyeCandyScriptInstance.updateRenderer(eyeCandyScriptContext.getRenderManager());
                eyeCandyScriptInstance.updateSound(eyeCandyScriptContext.getSoundManager());
                eyeCandyScriptContext.resetForNextRun();
            });
        }

        Direction facing = IBlock.getStatePropertySafe(blockEntity.getWorld2(), blockEntity.getPos2(), BlockEyeCandy.FACING);

        /* Same cached base as the native path (see above). */
        StoredMatrixTransformations storedMatrixTransformations = transformOf(blockEntity, facing);

        eyeCandyScriptInstance.getRenderManager().invoke(world, storedMatrixTransformations, facing, light);
        eyeCandyScriptInstance.getSoundManager().invoke(world, new ScriptVector3f(blockPos));
    }
}
