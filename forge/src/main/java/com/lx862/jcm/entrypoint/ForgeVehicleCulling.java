package com.lx862.jcm.entrypoint;

import com.lx862.jcm.mixin.mtrscripting.LevelRendererCullingAccessor;
import com.lx862.jcm.nativeapi.NativeCullFrustum;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;

/**
 * Forge half of native vehicle-script frustum culling.
 *
 * <p>Installs a live frustum supplier into {@link NativeCullFrustum} — the
 * vanilla {@code LevelRenderer#cullingFrustum} field, read through the
 * {@link LevelRendererCullingAccessor} mixin whenever culling asks (the shared
 * {@code com.lx862.jcm.nativeapi.VehicleCulling} only asks from inside the
 * vehicle render pass, after the field received the current frame's frustum).
 *
 * <p>Why not an event like the Fabric side ({@code FabricVehicleCulling}):
 * Forge 1.17.1 has no frustum-carrying render event, and the
 * RenderLevelStageEvent that exists from 1.18 onward changed its stage set
 * across the supported versions — a single live read needs none of that and
 * works identically on every version. See {@link NativeCullFrustum}'s javadoc
 * for the two publication modes.
 *
 * <p>Fails open by construction: any throwable inside the supplier yields
 * {@code null}, which {@code VehicleCulling} treats as "no frustum — render
 * everything", exactly the pre-culling behavior.
 */
public final class ForgeVehicleCulling {
    private ForgeVehicleCulling() {}

    /* Render-thread confined (the supplier only runs inside the vehicle
       render pass): the raw frustum instance is stable for a whole pass, so
       the MTR holder wrap happens once per frame, not once per vehicle. */
    private static Object lastRaw;
    private static org.mtr.mapping.holder.Frustum lastHolder;

    public static void init() {
        NativeCullFrustum.setLive(() -> {
            try {
                final LevelRenderer levelRenderer = Minecraft.getInstance().levelRenderer;
                if (levelRenderer == null) return null;
                final net.minecraft.client.renderer.culling.Frustum raw =
                        ((LevelRendererCullingAccessor) levelRenderer).getJcmCullingFrustum();
                if (raw == null) return null;
                if (raw != lastRaw) {
                    lastRaw = raw;
                    lastHolder = new org.mtr.mapping.holder.Frustum(raw);
                }
                return lastHolder;
            } catch(Throwable t) {
                /* A culling aid must never break world rendering. */
                return null;
            }
        });
    }
}
