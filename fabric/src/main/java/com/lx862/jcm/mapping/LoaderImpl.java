package com.lx862.jcm.mapping;

import org.mtr.mapping.holder.*;
import org.mtr.mapping.mapper.ScreenExtension;

/**
 * Fabric implementation via Yarn mapping
 */
public class LoaderImpl {
    public static boolean isRainingAt(World world, BlockPos pos) {
        return world.data.hasRain(pos.data);
    }

    /** Get a block settings forcing it to be solid, as we don't want water to break our block. */
    public static BlockSettings getSolidBlockSettings(BlockSettings settings) {
        #if MC_VERSION >= "12001"
            return new BlockSettings(settings.data.solid());
        #else
            return settings;
        #endif
    }

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
}
