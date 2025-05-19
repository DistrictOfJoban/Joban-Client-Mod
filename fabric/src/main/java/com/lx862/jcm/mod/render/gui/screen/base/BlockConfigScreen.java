package com.lx862.jcm.mod.render.gui.screen.base;

import com.lx862.jcm.mod.render.GuiHelper;
import com.lx862.jcm.mod.render.gui.widget.ListViewWidget;
import com.lx862.jcm.mod.render.gui.widget.WidgetSet;
import com.lx862.jcm.mod.util.TextCategory;
import com.lx862.jcm.mod.util.TextUtil;
import org.mtr.core.data.Station;
import org.mtr.mapping.holder.BlockPos;
import org.mtr.mapping.holder.ClickableWidget;
import org.mtr.mapping.holder.MutableText;
import org.mtr.mapping.mapper.ButtonWidgetExtension;
import org.mtr.mod.InitClient;
import org.mtr.mod.data.IGui;

/**
 * GUI Screen for configuring block settings, you should extend this class for your own block config screen
 */
public abstract class BlockConfigScreen extends BlockConfigRawScreen implements GuiHelper {
    protected final ListViewWidget listViewWidget;

    public BlockConfigScreen(BlockPos blockPos) {
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
