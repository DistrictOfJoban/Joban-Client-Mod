package com.lx862.amberui.gui.screen;

import com.lx862.amberui.gui.GuiHelper;
import com.lx862.amberui.gui.widget.WidgetSet;
import com.lx862.jcm.mod.util.TextCategory;
import com.lx862.jcm.mod.util.TextUtil;
import org.mtr.mapping.holder.ClickableWidget;
import org.mtr.mapping.mapper.ButtonWidgetExtension;

public abstract class SavableScreen extends TitledScreen implements GuiHelper {
    protected ButtonWidgetExtension saveButton;
    protected ButtonWidgetExtension discardButton;
    protected boolean discardConfig = false;

    public SavableScreen(boolean animatable) {
        super(animatable);
    }

    protected void addBottomRowEntry(int x, int y, int width, int height) {
        WidgetSet bottomEntryWidget = new WidgetSet(20);

        this.saveButton = new ButtonWidgetExtension(0, 0, 0, 20, TextUtil.translatable(TextCategory.GUI, "block_config.save"), (btn) -> {
            onClose2();
        });

        this.discardButton = new ButtonWidgetExtension(0, 0, 0, 20, TextUtil.translatable(TextCategory.GUI, "block_config.discard"), (btn) -> {
            discardConfig = true;
            onClose2();
        });

        addChild(new ClickableWidget(saveButton));
        addChild(new ClickableWidget(discardButton));

        bottomEntryWidget.addWidget(saveButton);
        bottomEntryWidget.addWidget(discardButton);
        bottomEntryWidget.setXYSize(x, y, width, height);
        addChild(new ClickableWidget(bottomEntryWidget));
    }

    @Override
    public void onClose2() {
        // Save config by default, unless explicitly requested not to
        if(!discardConfig) {
            onSave();
        } else {
            onDiscard();
        }
        super.onClose2();
    }

    protected abstract void onSave();

    protected void onDiscard() {
    }

    @Override
    public boolean isPauseScreen2() {
        return false;
    }
}
