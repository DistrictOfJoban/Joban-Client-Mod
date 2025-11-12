package com.lx862.jcm.mapping;

import com.lx862.jcm.mixin.minecraft.NativeImageAccessor;
import org.mtr.mapping.holder.*;
import org.mtr.mapping.mapper.ScreenExtension;

/**
 * Fabric implementation via Yarn mapping
 */
public class LoaderImplClient {

    public static void openURLScreen(ScreenExtension parentScreen, String url) {
        MinecraftClient mc = MinecraftClient.getInstance();

        #if MC_VERSION <= "11802"
            mc.openScreen(
                    new Screen(new net.minecraft.client.gui.screen.ConfirmChatLinkScreen((confirmed) -> {
                        if(confirmed) {
                            Util.getOperatingSystem().open(url);
                        }
                        mc.openScreen(new Screen(parentScreen));
                    }, url, true)
                )
            );
        #else
        mc.openScreen(
                new Screen(new net.minecraft.client.gui.screen.ConfirmLinkScreen((confirmed) -> {
                    if(confirmed) {
                        Util.getOperatingSystem().open(url);
                    }
                    mc.openScreen(new Screen(parentScreen));
                }, url, true)
                )
        );
        #endif
    }

    public static long getNativeImagePointer(NativeImage nativeImage) {
        return ((NativeImageAccessor)(Object)nativeImage.data).getPointer();
    }
}
