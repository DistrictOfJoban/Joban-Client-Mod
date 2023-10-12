package com.lx862.jcm.mod.gui;

import com.lx862.jcm.mod.gui.widget.ListViewWidget;
import com.lx862.jcm.mod.util.TextUtil;
import org.mtr.mapping.holder.BlockPos;
import org.mtr.mapping.holder.MutableText;
import org.mtr.mapping.mapper.ButtonWidgetExtension;
import org.mtr.mapping.mapper.GraphicsHolder;

public abstract class BlockConfigurationScreenBase extends BasicScreenBase {
    protected final BlockPos blockPos;
    protected final ListViewWidget listViewWidget;
    private final ButtonWidgetExtension saveButton;
    private final ButtonWidgetExtension discardButton;
    private boolean discardConfig = false;
    public BlockConfigurationScreenBase(BlockPos blockPos) {
        super(false);
        this.blockPos = blockPos;

        this.saveButton = new ButtonWidgetExtension(0, 0, 0, 20, TextUtil.translatable(TextUtil.TextCategory.GUI, "block_config.save"), (btn) -> {
            onClose2();
        });

        this.discardButton = new ButtonWidgetExtension(0, 0, 0, 20, TextUtil.translatable(TextUtil.TextCategory.GUI, "block_config.discard"), (btn) -> {
            discardConfig = true;
            onClose2();
        });

        this.listViewWidget = new ListViewWidget(20);
    }

    @Override
    protected void init2() {
        super.init2();
        int listWidgetWidth = (int)(width * 0.75);
        int listWidgetHeight = (int)((height - 60) * 0.75);
        int startX = (width - listWidgetWidth) / 2;
        int startY = 60;

        listViewWidget.reset();
        addConfigEntries();
        listViewWidget.setXYSize(startX, startY, listWidgetWidth, listWidgetHeight);

        int bottomBtnY = (height - 10 - ((height - listWidgetHeight - 60) / 2));
        saveButton.setPosition(startX, bottomBtnY);
        saveButton.setWidth(listWidgetWidth / 2);
        discardButton.setPosition(startX + (listWidgetWidth / 2), bottomBtnY);
        discardButton.setWidth(listWidgetWidth / 2);

        addDrawableChild2(saveButton);
        addDrawableChild2(discardButton);
    }

    public abstract void addConfigEntries();
    public abstract void onSave();

    @Override
    public MutableText getScreenSubtitle() {
        return TextUtil.translatable(TextUtil.TextCategory.GUI,
                "block_config.subtitle_near",
                blockPos.getX(), blockPos.getY(), blockPos.getZ(),
                "中環 Central" //TODO: Get real data
        );
    }

    @Override
    public void render(GraphicsHolder graphicsHolder, int mouseX, int mouseY, float delta) {
        super.render(graphicsHolder, mouseX, mouseY, delta);
        listViewWidget.render(graphicsHolder, mouseX, mouseY, delta);
    }

    @Override
    public void onClose2() {
        // Save config by default, unless explicitly requested not to
        if(!discardConfig) {
            onSave();
        }
        super.onClose2();
    }
}
