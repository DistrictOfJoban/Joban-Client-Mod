package com.lx862.jcm.entrypoint;

import com.lx862.jcm.nativeapi.NativeCullFrustum;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import org.mtr.mapping.holder.Frustum;

/**
 * Fabric half of native vehicle-script frustum culling.
 *
 * <p>Publishes the world render pass's frustum into
 * {@link NativeCullFrustum} for {@code com.lx862.jcm.nativeapi.VehicleCulling}
 * to test against.
 *
 * <p><b>Why this class lives in the entrypoint package:</b> the {@code setupFiles}
 * task copies {@code com/lx862/jcm/nativeapi} and {@code com/lx862/jcm/mod}
 * verbatim into the Forge source tree, so those packages must stay free of
 * Fabric API. This package is Fabric-only, which lets the shared culling code
 * remain loader-neutral.
 *
 * <p>The hook is the same one MTR builds its own block-entity culling frustum
 * from ({@code org.mtr.mod.render.BlockEntityRenderCulling#init} registers
 * {@code WorldRenderEvents.AFTER_SETUP} as well), so a vehicle is culled under
 * exactly the frustum MTR would reject a box with there.
 *
 * <p>A Forge (or any other loader) port only has to call
 * {@link NativeCullFrustum#set} from its equivalent render-setup event and
 * {@link NativeCullFrustum#clear} when the pass ends.
 */
public final class FabricVehicleCulling {
    private FabricVehicleCulling() {}

    public static void init() {
        WorldRenderEvents.AFTER_SETUP.register(FabricVehicleCulling::captureFrustum);
        WorldRenderEvents.END.register(context -> NativeCullFrustum.clear());
    }

    private static void captureFrustum(WorldRenderContext context) {
        try {
            /* 'var' on purpose: the raw Minecraft type behind the frustum is
               not on the shared code's compile classpath, only MTR's wrapper
               is — so the wrap happens here, where the type is inferable. */
            final var raw = context.frustum();
            NativeCullFrustum.set(raw == null ? null : new Frustum(raw), context.world());
        } catch (Throwable t) {
            /* A culling aid must never break world rendering. */
            NativeCullFrustum.clear();
        }
    }
}
