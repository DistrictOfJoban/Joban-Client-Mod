package com.lx862.jcm.nativeapi;

import org.mtr.mapping.holder.Frustum;
import org.mtr.mapping.holder.MinecraftClient;

import java.util.function.Supplier;

/**
 * Loader-neutral frustum state for {@link VehicleCulling}.
 *
 * <p>Kept free of any loader API on purpose: {@code com/lx862/jcm/nativeapi} is
 * copied verbatim into the Forge source tree by the {@code setupFiles} task, so
 * the shared code must not import Fabric classes. The loader-specific part
 * (which event fires when the world render setup is complete, and wrapping the
 * loader's frustum in MTR's {@link Frustum}) lives in
 * {@code com.lx862.jcm.entrypoint.FabricVehicleCulling} (capture-per-pass) or
 * {@code com.lx862.jcm.entrypoint.ForgeVehicleCulling} (live read, see below).
 *
 * <p>Two publication modes:
 * <ul>
 * <li><b>Capture</b> (Fabric): the render-setup event pushes the frustum for
 * this pass via {@link #set}, and the end event clears it. Simple, but needs a
 * loader event that carries the frustum — which Forge 1.17.1 does not have.</li>
 * <li><b>Live</b> (Forge): {@link #setLive} installs a supplier that reads the
 * vanilla {@code LevelRenderer#cullingFrustum} field through an accessor mixin
 * at every query. {@link VehicleCulling#shouldRender} only runs from inside the
 * vehicle render pass, which happens after that field received the current
 * frame's frustum, so a live read is always current-frame — and needs no event
 * at all. The supplier hands back the MTR-wrapped holder; the loader side
 * caches the wrap per raw frustum instance (stable for the whole pass), so the
 * holder is built once per frame, not once per vehicle.</li>
 * </ul>
 */
public final class NativeCullFrustum {
    private NativeCullFrustum() {}

    /* Capture mode (Fabric). */
    private static volatile Frustum frustum;
    private static volatile Object world;

    /* Live mode (Forge). Render-thread confined: shouldRender callers all run
       on the render thread, the supplier is installed once at client init. */
    private static volatile Supplier<Frustum> liveFrustum;

    /**
     * Publishes the frustum captured for the current world render pass.
     *
     * @param captured the frustum wrapped in MTR's holder, or {@code null} to
     *                 clear it and make culling inert.
     * @param rawWorld the client world the frustum belongs to.
     */
    public static void set(Frustum captured, Object rawWorld) {
        frustum = captured;
        world = captured == null ? null : rawWorld;
    }

    /** Clears the capture-mode frustum at the end of a world render pass. */
    public static void clear() {
        frustum = null;
        world = null;
    }

    /**
     * Installs (or with {@code null} removes) a live frustum supplier — the
     * Forge mode; see the class javadoc. The supplier must return the
     * MTR-holder-wrapped frustum or {@code null}, never throw.
     */
    public static void setLive(Supplier<Frustum> frustumSupplier) {
        liveFrustum = frustumSupplier;
    }

    /** The frustum, or {@code null} when none was captured this pass. */
    public static Frustum frustum() {
        final Supplier<Frustum> supplier = liveFrustum;
        if(supplier != null) {
            return supplier.get();
        }
        return frustum;
    }

    /** The world the captured frustum belongs to, or {@code null}. */
    public static Object world() {
        if(liveFrustum != null) {
            /* Live mode: the frustum is read mid-pass, so the world it
               belongs to is by definition the world being rendered. */
            try {
                return MinecraftClient.getInstance().getWorldMapped().data;
            } catch(Throwable t) {
                return null;
            }
        }
        return world;
    }
}
