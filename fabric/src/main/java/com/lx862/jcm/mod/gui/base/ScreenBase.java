package com.lx862.jcm.mod.gui.base;

import org.mtr.mapping.holder.MinecraftClient;
import org.mtr.mapping.holder.Screen;
import org.mtr.mapping.mapper.ScreenExtension;

public abstract class ScreenBase extends ScreenExtension {
    private Screen previousScreen = null;
    public ScreenBase() {
        super();
    }
    public ScreenBase withPreviousScreen(Screen screen) {
        this.previousScreen = screen;
        return this;
    }

    @Override
    public void onClose2() {
        MinecraftClient.getInstance().openScreen(previousScreen);
    }

}
