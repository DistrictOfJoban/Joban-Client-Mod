package com.lx862.amberui.mapping;

import org.mtr.mapping.holder.*;
import org.mtr.mapping.mapper.ScreenExtension;

import java.util.List;

/**
 * Fabric implementation via Yarn mapping
 */
public class LoaderImplGUI {

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

    public static void setTooltip(Screen screen, OrderedText orderedText) {
        #if MC_VERSION >= "11903"
        screen.data.setTooltip(List.of(orderedText.data));
        #endif
    }

    public static OrderedText asOrderedText(MutableText mutableText) {
        return new OrderedText(mutableText.data.asOrderedText());
    }
}
