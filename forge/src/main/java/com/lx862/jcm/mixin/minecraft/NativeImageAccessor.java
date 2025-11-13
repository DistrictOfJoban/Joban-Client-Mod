package com.lx862.jcm.mixin.minecraft;

#if MC_VERSION < 11701
import net.minecraft.client.renderer.texture.NativeImage;
#else
import com.mojang.blaze3d.platform.NativeImage;
#endif

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(NativeImage.class)
public interface NativeImageAccessor {
    @Accessor
    public long getPixels();
}
