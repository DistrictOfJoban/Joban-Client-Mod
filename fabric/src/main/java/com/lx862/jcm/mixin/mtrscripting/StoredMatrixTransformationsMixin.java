package com.lx862.jcm.mixin.mtrscripting;

import com.lx862.jcm.nativeapi.NativeTransformScope;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.mtr.mapping.mapper.GraphicsHolder;
import org.mtr.mod.render.StoredMatrixTransformations;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import java.util.function.Consumer;

/**
 * Exposes mark/truncate on MTR's {@link StoredMatrixTransformations} so the
 * scripting replay path can isolate per-draw-call additions without the
 * per-call {@code copy()} (new object + new ObjectArrayList + element copy
 * per draw call per car per frame — the single largest steady allocation
 * multiplier in the scripted render path).
 *
 * <p>Applied to every StoredMatrixTransformations, including instances MTR
 * itself creates; the added methods are additive and change no behaviour.
 */
@Mixin(value = StoredMatrixTransformations.class, remap = false)
public abstract class StoredMatrixTransformationsMixin implements NativeTransformScope {

    @Shadow
    @Final
    private ObjectArrayList<Consumer<GraphicsHolder>> transformations;

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
}
