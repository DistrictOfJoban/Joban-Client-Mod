package com.lx862.jcm.mapping;

import net.minecraft.client.gui.screen.ConfirmLinkScreen;
import org.mtr.mapping.holder.*;
import org.mtr.mapping.mapper.ScreenExtension;

/**
 * Fabric implementation via Yarn mapping
 */
public class LoaderImpl {
    public static boolean isRainingAt(World world, BlockPos pos) {
        return world.data.hasRain(pos.data);
    }

    public static void openURLScreen(ScreenExtension parentScreen, String url) {
        MinecraftClient mc = MinecraftClient.getInstance();

        #if MC_VERSION <= "11802"
            mc.openScreen(
                    new Screen(new ConfirmChatLinkScreen((confirmed) -> {
                        if(confirmed) {
                            Util.getOperatingSystem().open(url);
                        }
                        mc.openScreen(new Screen(parentScreen));
                    }, url, true)
                )
            );
        #else
        mc.openScreen(
                new Screen(new ConfirmLinkScreen((confirmed) -> {
                    if(confirmed) {
                        Util.getOperatingSystem().open(url);
                    }
                    mc.openScreen(new Screen(parentScreen));
                }, url, true)
                )
        );
        #endif
    }
}
