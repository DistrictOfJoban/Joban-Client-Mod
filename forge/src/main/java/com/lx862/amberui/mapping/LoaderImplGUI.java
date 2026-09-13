package com.lx862.amberui.mapping;

import org.mtr.mapping.holder.*;
import org.mtr.mapping.mapper.ScreenExtension;

import java.util.List;

/**
 * Forge implementation via Mojang mapping
 */
public class LoaderImplGUI {
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

    public static void setTooltip(Screen screen, OrderedText orderedText) {
        #if MC_VERSION >= "11903"
        screen.data.setTooltipForNextRenderPass(List.of(orderedText.data));
        #endif
    }

    public static OrderedText asOrderedText(MutableText mutableText) {
        return new OrderedText(mutableText.data.getVisualOrderText());
    }
}
