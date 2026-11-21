package com.lx862.jcm.mixin.mtrscripting;

import com.lx862.jcm.mod.scripting.psd.PSDRuns;
import org.mtr.mapping.holder.BlockPos;
import org.mtr.mapping.holder.BlockState;
import org.mtr.mapping.holder.Direction;
import org.mtr.mapping.holder.World;
import org.mtr.mapping.mapper.GraphicsHolder;
import org.mtr.mod.block.BlockPSDAPGBase;
import org.mtr.mod.block.BlockPSDAPGGlassBase;
import org.mtr.mod.block.BlockPSDTop;
import org.mtr.mod.block.IBlock;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Yanyang/PSD × Tianjin Metro: the Tianjin Metro mod ({@code tjmetro}) renders
 * its PSD content through its <b>own copy</b> of MTR's RenderRouteBase
 * pipeline, and its renderers even override {@code render()} with the copied
 * body, so JCM's {@code RenderRouteBaseMixin} on MTR's class never sees it.
 * When a script is bound to a glass segment / psd_top group of a Tianjin PSD
 * wall, the mod's own original content (route map strip, station name strip,
 * direction arrows, next-station displays) must stop rendering for exactly
 * that segment/group — the same rule JCM applies to MTR's own content.
 *
 * <p><b>Relocated package names (2026-11-21 fix):</b> the Tianjin Metro mod
 * is a multi-loader build — its published jars carry the classes under
 * {@code fabric.ziyue.tjmetro.*} (Fabric) and {@code forge.ziyue.tjmetro.*}
 * (Forge), NOT the {@code ziyue.tjmetro.*} names its GitHub sources use.
 * The original hook targeted the unrelocated names, which never exist at
 * runtime, so the mixin silently never applied and Tianjin walls kept
 * rendering their original content behind script canvases (the 2026-11-21
 * report). Targets are now the relocated names for both loaders; only the
 * installed loader's set exists at runtime, the other half of the
 * {@code @Pseudo} multi-target list simply never loads.</p>
 *
 * <p>Targets are string-based and the mixin is {@code @Pseudo} — JCM does not
 * depend on the mod, so its classes stay off the compile classpath (the
 * annotation processor must not try to resolve them either). The config
 * plugin ({@code JcmMixinPlugin}) only registers this mixin when every
 * target class of the MATCHING loader prefix is present, and every injection
 * is {@code require = 0}, so a future Tianjin Metro build that renames a
 * renderer or changes a {@code render} signature degrades to a log warning
 * instead of a crash.</p>
 *
 * <p>The {@code @Coerce}d first parameter covers each target's own entity
 * type — they all extend MTR's {@code BlockPSDTop.BlockEntityBase} (Tianjin
 * reuses MTR's block-entity base for its PSD tops and APG glass; MTR's own
 * classes are NOT relocated).</p>
 */
@Mixin(targets = {
        "fabric.ziyue.tjmetro.mod.render.RenderRouteBase",
        "fabric.ziyue.tjmetro.mod.render.RenderPSDTopTianjin",
        "fabric.ziyue.tjmetro.mod.render.RenderPSDTopTianjinBMT",
        "fabric.ziyue.tjmetro.mod.render.RenderAPGGlassTianjinBMT",
        "fabric.ziyue.tjmetro.mod.render.RenderAPGGlassTianjinJinjing",
        "fabric.ziyue.tjmetro.mod.render.RenderAPGGlassTianjinTRT",
        "forge.ziyue.tjmetro.mod.render.RenderRouteBase",
        "forge.ziyue.tjmetro.mod.render.RenderPSDTopTianjin",
        "forge.ziyue.tjmetro.mod.render.RenderPSDTopTianjinBMT",
        "forge.ziyue.tjmetro.mod.render.RenderAPGGlassTianjinBMT",
        "forge.ziyue.tjmetro.mod.render.RenderAPGGlassTianjinJinjing",
        "forge.ziyue.tjmetro.mod.render.RenderAPGGlassTianjinTRT",
}, remap = false)
@Pseudo
public class TianjinRenderRouteBaseMixin {

    @Inject(method = "render", at = @At("HEAD"), cancellable = true, require = 0)
    private void jsblock$cancelForScriptedTianjinPsd(@Coerce BlockPSDTop.BlockEntityBase entity, float tickDelta, GraphicsHolder graphicsHolder, int light, int overlay, CallbackInfo ci) {
        final World world = entity.getWorld2();
        if (world == null) return;
        final BlockPos pos = entity.getPos2();
        final BlockState state = world.getBlockState(pos);
        final Object block = state.getBlock().data;
        if (block instanceof BlockPSDTop) {
            final Direction facing = IBlock.getStatePropertySafe(state, BlockPSDTop.FACING);
            if (PSDRuns.topGroupHasScriptBinding(world, pos, facing)) {
                ci.cancel();
            }
        } else if (block instanceof BlockPSDAPGGlassBase) {
            final Direction facing = IBlock.getStatePropertySafe(state, BlockPSDAPGBase.FACING);
            if (PSDRuns.glassRunHasScriptBinding(world, pos, facing)) {
                ci.cancel();
            }
        }
    }
}
