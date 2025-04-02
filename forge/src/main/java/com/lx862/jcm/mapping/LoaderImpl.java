package com.lx862.jcm.mapping;

import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import org.mtr.mapping.holder.*;
import org.mtr.mapping.mapper.ScreenExtension;

/**
 * Forge implementation via Mojang mapping
 */
public class LoaderImpl {
    public static boolean isRainingAt(World world, BlockPos pos) {
        return world.data.isRainingAt(pos.data);
    }

    public static void openURLScreen(ScreenExtension parentScreen, String url) {
        MinecraftClient mc = MinecraftClient.getInstance();

        #if MC_VERSION <= "11605"
            mc.openScreen(
                    new Screen(new ConfirmOpenLinkScreen((confirmed) -> {
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
