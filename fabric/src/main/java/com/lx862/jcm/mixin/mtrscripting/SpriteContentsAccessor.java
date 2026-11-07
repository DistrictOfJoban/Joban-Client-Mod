package com.lx862.jcm.mixin.mtrscripting;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

#if MC_VERSION >= "12001"
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.SpriteContents;
import net.minecraft.util.Identifier;

/**
 * Yanyang/PSD: read-only access to {@link SpriteContents}' original image
 * and texture id. The PSD glass window-bounds scan
 * ({@code PSDWindowBounds}) needs the per-pixel ALPHA of the block's
 * actual glass texture — including alpha values between fully transparent
 * and fully opaque, which the public {@code isPixelTransparent} (opacity
 * == 0 only) cannot express — to find where a resource pack's tinted
 * "window" region ends and its frame begins. The image is only ever read,
 * never mutated or closed.
 */
@Mixin(SpriteContents.class)
public interface SpriteContentsAccessor {

    @Accessor("image")
    NativeImage jsblock$getImage();

    @Accessor("id")
    Identifier jsblock$getId();
}
#else
/* Pre-1.20.1 stub: those texture APIs have no SpriteContents class (the
 * baked-sprite pixel scan is compiled out there and PSDWindowBounds falls
 * back to the full face). One mixins.json ships in every MC build, so the
 * class itself must exist on all of them — as a member-less interface
 * mixin against NativeImage it is a strict no-op that merely tags the
 * class with an empty interface. */
@Mixin(net.minecraft.client.texture.NativeImage.class)
public interface SpriteContentsAccessor {
}
#endif
