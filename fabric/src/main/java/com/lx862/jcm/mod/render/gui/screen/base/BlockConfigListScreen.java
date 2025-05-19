package com.lx862.jcm.mod.render.gui.screen.base;

import com.lx862.jcm.mod.render.GuiHelper;
import com.lx862.jcm.mod.render.gui.widget.ListViewWidget;
import org.mtr.mapping.holder.BlockPos;
import org.mtr.mapping.holder.ClickableWidget;

/**
 * Block Config Screen with a GUI list, override {@link BlockConfigListScreen#addConfigEntries()} to add items to the list.
 */
public abstract class BlockConfigListScreen extends BlockConfigScreen implements GuiHelper {
    protected final ListViewWidget listViewWidget;

    public BlockConfigListScreen(BlockPos blockPos) {
        super(blockPos);
        this.listViewWidget = new ListViewWidget();
    }

    @Override
    protected void init2() {
        super.init2();
        int contentWidth = getContentWidth();
        int listViewHeight = Math.max(160, (int)((height - 60) * 0.75));
        int startX = (width - contentWidth) / 2;
        int startY = TEXT_PADDING * 5;
        int bottomEntryHeight = (height - startY - listViewHeight - (BOTTOM_ROW_MARGIN * 2));

        listViewWidget.reset();

        listViewWidget.setXYSize(startX, startY, contentWidth, listViewHeight);
        addConfigEntries();
        addBottomRowEntry(startX, startY + listViewHeight + BOTTOM_ROW_MARGIN, contentWidth, bottomEntryHeight);
        listViewWidget.positionWidgets();
        addChild(new ClickableWidget(listViewWidget));
    }

    public abstract void addConfigEntries();
}
