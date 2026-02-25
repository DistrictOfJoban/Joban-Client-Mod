package com.lx862.jcm.mapping;

import com.lx862.jcm.mixin.minecraft.NativeImageAccessor;
import org.mtr.mapping.holder.*;
import org.mtr.mapping.mapper.ScreenExtension;

/**
 * Forge implementation via Mojang mapping
 */
public class LoaderImplClient {
    public static void openURLScreen(ScreenExtension parentScreen, String url) {
        MinecraftClient mc = MinecraftClient.getInstance();
        mc.openScreen(
                new Screen(new net.minecraft.client.gui.screens.ConfirmLinkScreen((confirmed) -> {
                    if(confirmed) {
                        Util.getOperatingSystem().open(url);
                    }
                    mc.openScreen(new Screen(parentScreen));
                }, url, true)
            )
        );
    }

    public static long getNativeImagePointer(NativeImage nativeImage) {
        return ((NativeImageAccessor)(Object)nativeImage.data).getPixels();
    }
}
