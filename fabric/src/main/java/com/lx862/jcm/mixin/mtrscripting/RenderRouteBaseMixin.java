package com.lx862.jcm.mixin.mtrscripting;

import com.lx862.jcm.mod.scripting.psd.PSDBinding;
import com.lx862.jcm.mod.scripting.psd.PSDClientBindings;
import com.lx862.jcm.mod.scripting.psd.PSDWrapper;
import org.mtr.mapping.mapper.GraphicsHolder;
import org.mtr.mod.block.BlockPSDTop;
import org.mtr.mod.render.RenderRouteBase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Yanyang/PSD: when a script is bound to a psd_top block, its original
 * content (direction arrow / route map strip and the platform color strip)
 * must no longer render — the script fully controls what the panel shows
 * (drawn by {@code PSDScriptRenderer} through the MainRenderer pass).
 *
 * <p>Only psd_top block entities are affected; the other
 * {@link RenderRouteBase} subclass (RenderAPGGlass) passes through
 * untouched.</p>
 */
@Mixin(value = RenderRouteBase.class, remap = false)
public class RenderRouteBaseMixin {
    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void jsblock$cancelForScriptedTop(BlockPSDTop.BlockEntityBase entity, float tickDelta, GraphicsHolder graphicsHolder, int light, int overlay, CallbackInfo ci) {
        if (entity instanceof BlockPSDTop.BlockEntity) {
            final PSDBinding binding = PSDClientBindings.get(entity.getPos2());
            if (binding != null && binding.type == PSDWrapper.PSDType.TOP) {
                ci.cancel();
            }
        }
    }
}
