package com.lx862.jcm.mod.render.gui.widget;

import com.lx862.jcm.mod.Constants;
import com.lx862.jcm.mod.render.GuiHelper;
import com.lx862.jcm.mod.render.RenderHelper;
import com.lx862.jcm.mod.render.gui.ScrollViewWidget;
import org.mtr.mapping.holder.MutableText;
import org.mtr.mapping.mapper.*;

import java.util.ArrayList;
import java.util.List;

public class ListViewWidget extends ScrollViewWidget implements RenderHelper, GuiHelper {
    public static final int ENTRY_PADDING = 5;
    private final List<ListItem> entryList = new ArrayList<>();
    private float elapsed;
    public ListViewWidget() {
        super(0, 0, 0, 0);
    }

    public void setXYSize(int x, int y, int width, int height) {
        setX2(x);
        setY2(y);
        setWidth2(width);
        setHeightMapped(height);
        if(getContentHeight() <= getHeight2()) {
            currentScroll = 0;
        }
        positionWidgets(currentScroll);
    }

    public void add(MutableText text, MappedWidget widget) {
        add(new ListItem(text, widget));
    }

    public void add(ListItem listItem) {
        entryList.add(listItem);
    }

    public void addCategory(MutableText text) {
        entryList.add(new ListItem(text, null, true));
    }

    public void reset() {
        this.entryList.clear();
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

        // Background
        int incY = 0;
        for(int i = 0; i < entryList.size(); i++) {
            ListItem listItem = entryList.get(i);
            int entryX = getX2();
            int entryY = getY2() + incY - (int)currentScroll;
            boolean widgetVisible = false;
            if(listItem.widget != null) {
                boolean topLeftVisible = inRectangle(listItem.widget.getX(), listItem.widget.getY(), getX2(), getY2(), getWidth2(), getHeight2());
                boolean bottomRightVisible = inRectangle(listItem.widget.getX() + listItem.widget.getWidth(), listItem.widget.getY() + listItem.widget.getHeight(), getX2(), getY2(), getWidth2(), getHeight2());
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
        for(ListItem entry : entryList) {
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

        for(int i = 0; i < entryList.size(); i++) {
            ListItem listItem = entryList.get(i);
            if(!listItem.isCategory) {
                int entryY = startY + incY;
                int x = (startX + width - getScrollbarOffset()) - (listItem.widget.getWidth()) - (ENTRY_PADDING);
                int y = (int)(-scroll + entryY) + ((listItem.height - listItem.widget.getHeight()) / 2);

                listItem.widget.setX(x);
                listItem.widget.setY(y);
            }

            incY += listItem.height;
        }
    }
}
