package com.lx862.jcm.mixin.mtrscripting;

import com.lx862.jcm.mod.scripting.psd.PSDScriptRenderer;
import org.mtr.mapping.holder.Vector3d;
import org.mtr.mapping.mapper.GraphicsHolder;
import org.mtr.mod.render.MainRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Yanyang/PSD: drives the PSD script render pass at the head of MTR's frame
 * render — the queued script draws join the same queued-layer flush MTR's own
 * PSD renderers schedule into, after the block-entity phase drew (and, for
 * bound tops, was cancelled by {@link RenderRouteBaseMixin}) and before
 * vehicles. The camera {@code offset} is forwarded: the glass pane's white
 * backing is drawn synchronously (private immediate, see
 * {@code PSDScriptRenderer}'s flush-discipline note) and needs it to place
 * its quads camera-relative.
 */
@Mixin(value = MainRenderer.class, remap = false)
public class MainRendererPSDMixin {
    @Inject(method = "render(Lorg/mtr/mapping/mapper/GraphicsHolder;Lorg/mtr/mapping/holder/Vector3d;)V", at = @At("HEAD"))
    private static void jsblock$renderPsdScripts(GraphicsHolder graphicsHolder, Vector3d offset, CallbackInfo ci) {
        PSDScriptRenderer.renderAll(offset);
    }
}
