package com.lx862.jcm.nativeapi;

import org.mtr.mapping.holder.Frustum;

/**
 * Loader-neutral frustum state for {@link VehicleCulling}.
 *
 * <p>Kept free of any loader API on purpose: {@code com/lx862/jcm/nativeapi} is
 * copied verbatim into the Forge source tree by the {@code setupFiles} task, so
 * the shared code must not import Fabric classes. The loader-specific part
 * (which event fires when the world render setup is complete, and wrapping the
 * loader's frustum in MTR's {@link Frustum}) lives in
 * {@code com.lx862.jcm.entrypoint.FabricVehicleCulling}.
 */
public final class NativeCullFrustum {
    private NativeCullFrustum() {}

    private static volatile Frustum frustum;
    private static volatile Object world;

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

    public static void clear() {
        frustum = null;
        world = null;
    }

    /** The frustum, or {@code null} when none was captured this pass. */
    public static Frustum frustum() {
        return frustum;
    }

    /** The world the captured frustum belongs to, or {@code null}. */
    public static Object world() {
        return world;
    }
}
