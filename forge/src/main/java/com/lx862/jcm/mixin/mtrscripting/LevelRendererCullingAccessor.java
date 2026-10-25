package com.lx862.jcm.mixin.mtrscripting;

import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.culling.Frustum;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Forge half of native vehicle-script frustum culling: exposes the vanilla
 * {@code LevelRenderer#cullingFrustum} field for
 * {@code com.lx862.jcm.entrypoint.ForgeVehicleCulling}'s live supplier.
 *
 * <p>The field name {@code cullingFrustum} is verified present with the same
 * type on every supported Minecraft version's official mappings (1.17.1
 * through 1.20.4) — Forge compiles and reobfuscates against those mappings, so
 * the refmap carries the right runtime name per version automatically.
 *
 * <p>Forge 1.17.1 has no render event that carries a frustum (the Fabric-side
 * WorldRenderEvents equivalent, RenderLevelStageEvent, only exists from 1.18
 * and its stage set changed across versions), which is why the culling is a
 * live read through this accessor instead of an event capture: no
 * version-conditional event handling anywhere.
 */
@Mixin(LevelRenderer.class)
public interface LevelRendererCullingAccessor {

    @Accessor("cullingFrustum")
    Frustum getJcmCullingFrustum();
}
