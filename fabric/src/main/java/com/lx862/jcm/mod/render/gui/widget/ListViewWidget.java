package com.lx862.jcm.mod.render.gui.widget;

import com.lx862.jcm.mod.Constants;
import com.lx862.jcm.mod.render.ClipStack;
import com.lx862.jcm.mod.render.GuiHelper;
import com.lx862.jcm.mod.render.RenderHelper;
import org.mtr.mapping.holder.MutableText;
import org.mtr.mapping.mapper.GraphicsHolder;
import org.mtr.mapping.mapper.GuiDrawing;

import java.util.ArrayList;
import java.util.List;

public class ListViewWidget extends AbstractScrollViewWidget implements RenderHelper, GuiHelper {
    public static final int ENTRY_PADDING = 5;
    private final List<AbstractListItem> displayedEntryList = new ArrayList<>();
    private final List<AbstractListItem> entryList = new ArrayList<>();
    private float elapsed;
    private String searchTerm = "";
    public ListViewWidget() {
        super(0, 0, 0, 0);
    }

    public void setXYSize(int x, int y, int width, int height) {
        setX2(x);
        setY2(y);
        setWidth2(width);
        setHeightMapped(height);
        setScroll(currentScroll);
        positionWidgets(currentScroll);
        refreshDisplayedList();
    }

    public void add(MutableText text, MappedWidget widget) {
        add(new ContentItem(text, widget));
    }

    public void add(AbstractListItem listItem) {
        entryList.add(listItem);
        refreshDisplayedList();
    }

    public void addCategory(MutableText text) {
        entryList.add(new CategoryItem(text));
        refreshDisplayedList();
    }

    public void reset() {
        this.entryList.clear();
        refreshDisplayedList();
    }

    public void setSearchTerm(String searchTerm) {
        this.searchTerm = searchTerm;
        refreshDisplayedList();
    }

    private void refreshDisplayedList() {
        displayedEntryList.clear();
        entryList.forEach(e -> {
            if(e.matchQuery(searchTerm)) {
                e.shown();
                displayedEntryList.add(e);
            } else {
                e.hidden();
            }
        });
        positionWidgets();

        // Update scrolling so that it will rubber-band back if scrolled area are beyond viewport
        setScroll(currentScroll);
    }

    @Override
    public void render(GraphicsHolder graphicsHolder, int mouseX, int mouseY, float tickDelta) {
        GuiHelper.drawRectangle(new GuiDrawing(graphicsHolder), getX2(), getY2(), width, height, 0x4F4C4C4C);
        super.render(graphicsHolder, mouseX, mouseY, tickDelta);
    }

    @Override
    public void renderContent(GraphicsHolder graphicsHolder, int mouseX, int mouseY, float tickDelta) {
        GuiDrawing guiDrawing = new GuiDrawing(graphicsHolder);
        elapsed += (tickDelta / Constants.MC_TICK_PER_SECOND);

        int incY = 0;
        for(int i = 0; i < displayedEntryList.size(); i++) {
            ClipStack.ensureStateCorrect();
            AbstractListItem listItem = displayedEntryList.get(i);
            int entryX = getX2();
            int entryY = getY2() + incY - (int)currentScroll;
            boolean widgetVisible = false;

            if(listItem instanceof ContentItem) {
                ContentItem ci = (ContentItem) listItem;
                boolean topLeftVisible = inRectangle(ci.widget.getX(), ci.widget.getY(), getX2(), getY2(), getWidth2(), getHeight2());
                boolean bottomRightVisible = inRectangle(ci.widget.getX() + ci.widget.getWidth(), ci.widget.getY() + ci.widget.getHeight(), getX2(), getY2(), getWidth2(), getHeight2());
                widgetVisible = topLeftVisible && bottomRightVisible;
            }

            listItem.draw(graphicsHolder, guiDrawing, entryX, entryY, width - getScrollbarOffset(), height, mouseX, mouseY, widgetVisible, elapsed, tickDelta);
            incY += listItem.height;
        }
    }

    @Override
    public void setScroll(double newScroll) {
        super.setScroll(newScroll);
        positionWidgets();
    }

    @Override
    protected int getContentHeight() {
        int entryHeight = 0;
        for(AbstractListItem entry : displayedEntryList) {
            entryHeight += entry.height;
        }
        return entryHeight;
    }

    public void positionWidgets() {
        positionWidgets(currentScroll);
    }

    private void positionWidgets(double scroll) {
        int startX = getX2();
        int startY = getY2();

        int incY = 0;
        for(int i = 0; i < displayedEntryList.size(); i++) {
            AbstractListItem listItem = displayedEntryList.get(i);
            int entryY = startY + incY;
            int x = (startX + width - getScrollbarOffset()) - ENTRY_PADDING;
            listItem.positionChanged(x, (int)-scroll + entryY);
            incY += listItem.height;
        }
    }
}
