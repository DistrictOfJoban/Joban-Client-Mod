package com.lx862.jcm.mixin.mtrscripting;

import com.lx862.jcm.nativeapi.NativeTransformScope;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.mtr.mapping.mapper.GraphicsHolder;
import org.mtr.mod.render.StoredMatrixTransformations;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import java.util.function.Consumer;

/**
 * Exposes list surgery on MTR's {@link StoredMatrixTransformations}: the
 * scripting replay path ({@code TransformCapturePool}, the eyecandy
 * transform cache, pooled native draw calls) clears and re-seeds recycled
 * holders in place instead of paying the per-call {@code copy()} (new
 * object + new ObjectArrayList + element copy per draw call per car per
 * frame — the single largest steady allocation multiplier in the scripted
 * render path). {@code jcm$mark}/{@code jcm$truncate} are kept for addons
 * and the regression test; the replay path itself no longer truncates a
 * shared list (the deferred render callbacks read it at flush time — see
 * TransformCapturePool's javadoc for the yanyang.4 regression that taught
 * us this).
 *
 * <p>{@code jcm$copyWorldAnchorFrom} writes the four anchor fields
 * ({@code useDefaultOffset} + {@code initialTranslateX/Y/Z}) of a recycled
 * holder — {@code @Mutable} shadows, the standard mixin write pattern for
 * final instance fields ({@code PUTFIELD} carries no runtime finality
 * check; instance finals are never JIT constant-folded). The anchor is what
 * {@code transform()} turns into {@code translate(initialTranslate - offset)}
 * at flush time; {@code TransformCapturePool.acquire} re-seeds it from the
 * base every handout, replicating {@code copy()}'s field semantics — the
 * yanyang.8 pool dropped it, and every scripted PIDS text followed the
 * camera instead of its block.
 *
 * <p>Applied to every StoredMatrixTransformations, including instances MTR
 * itself creates; the added methods are additive and change no behaviour.
 */
@Mixin(value = StoredMatrixTransformations.class, remap = false)
public abstract class StoredMatrixTransformationsMixin implements NativeTransformScope {

    @Shadow
    @Final
    private ObjectArrayList<Consumer<GraphicsHolder>> transformations;

    @Shadow
    @Final
    @Mutable
    public boolean useDefaultOffset;

    @Shadow
    @Final
    @Mutable
    private double initialTranslateX;

    @Shadow
    @Final
    @Mutable
    private double initialTranslateY;

    @Shadow
    @Final
    @Mutable
    private double initialTranslateZ;

    @Override
    @Unique
    public int jcm$mark() {
        return transformations.size();
    }

    @Override
    @Unique
    public void jcm$truncate(int mark) {
        if (mark >= 0 && transformations.size() > mark) {
            /* fastutil ObjectArrayList.size(int) discards the trailing
               elements without allocating. */
            transformations.size(mark);
        }
    }

    @Override
    @Unique
    public void jcm$clearTransformations() {
        transformations.clear();
    }

    @Override
    @Unique
    public void jcm$copyWorldAnchorFrom(StoredMatrixTransformations base) {
        final StoredMatrixTransformationsMixin from = (StoredMatrixTransformationsMixin) (Object) base;
        this.useDefaultOffset = from.useDefaultOffset;
        this.initialTranslateX = from.initialTranslateX;
        this.initialTranslateY = from.initialTranslateY;
        this.initialTranslateZ = from.initialTranslateZ;
    }
}
