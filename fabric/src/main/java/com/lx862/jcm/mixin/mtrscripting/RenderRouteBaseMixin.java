package com.lx862.jcm.mixin.mtrscripting;

import com.lx862.jcm.mod.block.APGGlassDRL;
import com.lx862.jcm.mod.block.APGGlassEndDRL;
import com.lx862.jcm.mod.scripting.psd.PSDRuns;
import org.mtr.mapping.holder.BlockPos;
import org.mtr.mapping.holder.BlockState;
import org.mtr.mapping.holder.Direction;
import org.mtr.mapping.holder.World;
import org.mtr.mapping.mapper.GraphicsHolder;
import org.mtr.mod.block.BlockPSDTop;
import org.mtr.mod.block.IBlock;
import org.mtr.mod.render.RenderRouteBase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Yanyang/PSD: when a script is bound to a psd_top block, the original
 * content (direction arrow / route map strip and the platform color strip)
 * must no longer render on <b>the whole contiguous psd_top run</b> — the
 * script canvas spans the entire band (see {@code PSDScriptRenderer}), so
 * MTR's own per-block slices must all go, not just the bound block's. The
 * run walk matches MTR's own {@code RenderRouteBase.getTextureNumber} chain
 * (contiguous psd_top blocks), so exactly the band the route map would have
 * spanned is silenced.
 *
 * <p>Also cancels for <b>stale MTR block entities on JCM DRL glass</b>:
 * worlds that placed {@code jsblock:apg_glass_drl} /
 * {@code jsblock:apg_glass_end_drl} with an older JCM — before those blocks
 * overrode {@code createBlockEntity} with their own
 * {@code jsblock:apg_glass_1} type — still carry MTR
 * {@code mtr:apg_glass} block entities in their chunk NBT. Those keep
 * rendering through MTR's RenderAPGGlass (renderer selection follows the BE
 * type), painting the arrow/route map, station-name strip and color strip at
 * the top of the upper glass half — but the DRL model has no pane there
 * (just a ~2/16 light strip), so the content floats in mid-air. No MTR
 * route content is ever wanted on a JCM DRL glass block, so the whole
 * RenderRouteBase pass is dropped whenever the block at the entity's
 * position is one of ours. (Those stale entities also can't tick — vanilla
 * logs "invalid for ticking" for them — which is harmless: the shared
 * PSD/APG block entity tick is a no-op.)</p>
 */
@Mixin(value = RenderRouteBase.class, remap = false)
public class RenderRouteBaseMixin {
    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void jsblock$cancelForScriptedTop(BlockPSDTop.BlockEntityBase entity, float tickDelta, GraphicsHolder graphicsHolder, int light, int overlay, CallbackInfo ci) {
        if (entity instanceof BlockPSDTop.BlockEntity) {
            final World world = entity.getWorld2();
            final BlockPos pos = entity.getPos2();
            if (world != null) {
                final BlockState state = world.getBlockState(pos);
                if (state.getBlock().data instanceof BlockPSDTop) {
                    final Direction facing = IBlock.getStatePropertySafe(state, BlockPSDTop.FACING);
                    if (PSDRuns.topRunHasScriptBinding(world, pos, facing)) {
                        ci.cancel();
                        return;
                    }
                }
            }
        }

        /* Stale MTR apg_glass/apg_glass_end BEs sitting on JCM DRL glass
           blocks (old worlds, see class doc) — MTR's own route content must
           not float over the DRL's open upper half. */
        final World world = entity.getWorld2();
        if (world != null) {
            final Object block = world.getBlockState(entity.getPos2()).getBlock().data;
            if (block instanceof APGGlassDRL || block instanceof APGGlassEndDRL) {
                ci.cancel();
            }
        }
    }
}
