package com.lx862.jcm.mod.gui.base;

import com.lx862.jcm.mod.gui.GuiHelper;
import com.lx862.jcm.mod.gui.widget.ButtonSetsWidget;
import com.lx862.jcm.mod.gui.widget.ListViewWidget;
import com.lx862.jcm.mod.util.TextCategory;
import com.lx862.jcm.mod.util.TextUtil;
import org.mtr.mapping.holder.BlockPos;
import org.mtr.mapping.holder.ClickableWidget;
import org.mtr.mapping.holder.MutableText;
import org.mtr.mapping.mapper.ButtonWidgetExtension;
import org.mtr.mapping.mapper.GraphicsHolder;

public abstract class BlockConfigurationScreenBase extends BasicScreenBase implements GuiHelper {
    protected final BlockPos blockPos;
    protected final ListViewWidget listViewWidget;
    protected final ButtonSetsWidget bottomEntryWidget;
    private final ButtonWidgetExtension saveButton;
    private final ButtonWidgetExtension discardButton;
    private boolean discardConfig = false;
    public BlockConfigurationScreenBase(BlockPos blockPos) {
        super(false);
        this.blockPos = blockPos;

        this.saveButton = new ButtonWidgetExtension(0, 0, 0, 20, TextUtil.translatable(TextCategory.GUI, "block_config.save"), (btn) -> {
            onClose2();
        });

        this.discardButton = new ButtonWidgetExtension(0, 0, 0, 20, TextUtil.translatable(TextCategory.GUI, "block_config.discard"), (btn) -> {
            discardConfig = true;
            onClose2();
        });

        this.listViewWidget = new ListViewWidget(22);
        this.bottomEntryWidget = new ButtonSetsWidget(20);
    }

    @Override
    protected void init2() {
        super.init2();
        int contentWidth = (int)Math.min((width * 0.75), MAX_CONTENT_WIDTH);
        int listViewHeight = (int)((height - 60) * 0.75);
        int startX = (width - contentWidth) / 2;
        int startY = TEXT_PADDING * 5;
        int bottomEntryHeight = (height - startY - listViewHeight - (BOTTOM_ROW_MARGIN * 2));

        listViewWidget.reset();
        addConfigEntries();
        listViewWidget.setXYSize(startX, startY, contentWidth, listViewHeight);

        bottomEntryWidget.reset();
        addBottomRowButtons();
        bottomEntryWidget.setXYSize(startX, startY + listViewHeight + BOTTOM_ROW_MARGIN, contentWidth, bottomEntryHeight);
    }

    public abstract void addConfigEntries();
    public abstract void onSave();

    protected void addBottomRowButtons() {
        addChild(new ClickableWidget(saveButton));
        addChild(new ClickableWidget(discardButton));

        bottomEntryWidget.addWidget(saveButton);
        bottomEntryWidget.addWidget(discardButton);
    }

    @Override
    public MutableText getScreenSubtitle() {
        return TextUtil.translatable(TextCategory.GUI,
                "block_config.subtitle_near",
                blockPos.getX(), blockPos.getY(), blockPos.getZ(),
                "中環 Central" //TODO: Get real data
        );
    }

    @Override
    public void render(GraphicsHolder graphicsHolder, int mouseX, int mouseY, float tickDelta) {
        super.render(graphicsHolder, mouseX, mouseY, tickDelta);
        listViewWidget.render(graphicsHolder, mouseX, mouseY, tickDelta);
    }

    @Override
    public void onClose2() {
        // Save config by default, unless explicitly requested not to
        if(!discardConfig) {
            onSave();
        }
        super.onClose2();
    }

    @Override
    public boolean isPauseScreen2() {
        return false;
    }
}
