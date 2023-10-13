package com.lx862.jcm.mod.gui;

import com.lx862.jcm.mod.gui.widget.ListViewWidget;
import com.lx862.jcm.mod.util.TextUtil;
import org.mtr.mapping.holder.BlockPos;
import org.mtr.mapping.holder.MutableText;
import org.mtr.mapping.mapper.ButtonWidgetExtension;
import org.mtr.mapping.mapper.GraphicsHolder;

public abstract class BlockConfigurationScreenBase extends BasicScreenBase {
    public static final int MAX_CONTENT_WIDTH = 400;
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
        int contentWidth = (int)Math.min((width * 0.75), MAX_CONTENT_WIDTH);
        int listViewHeight = (int)((height - 60) * 0.75);
        int startX = (width - contentWidth) / 2;
        int startY = TEXT_PADDING * 6;

        listViewWidget.reset();
        addConfigEntries();
        listViewWidget.setXYSize(startX, startY, contentWidth, listViewHeight);

        int bottomWidgetsStartY = startY + listViewHeight + TEXT_PADDING;
        saveButton.setPosition(startX, bottomWidgetsStartY);
        saveButton.setWidth(contentWidth / 2);
        discardButton.setPosition(startX + (contentWidth / 2), bottomWidgetsStartY);
        discardButton.setWidth(contentWidth / 2);

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
