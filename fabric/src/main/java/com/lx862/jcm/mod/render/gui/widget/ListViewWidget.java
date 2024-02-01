package com.lx862.jcm.mod.render.gui.widget;

import com.lx862.jcm.mod.Constants;
import com.lx862.jcm.mod.render.fundamental.ClipStack;
import com.lx862.jcm.mod.render.GuiHelper;
import com.lx862.jcm.mod.render.RenderHelper;
import org.mtr.mapping.holder.MutableText;
import org.mtr.mapping.mapper.*;

import java.util.ArrayList;
import java.util.List;

public class ListViewWidget extends ClickableWidgetExtension implements RenderHelper, GuiHelper {
    public static final int SCROLLBAR_WIDTH = 6;
    public static final int ENTRY_PADDING = 5;
    private final List<ListItem> entryList = new ArrayList<>();
    private double currentScroll = 0;
    private float elapsed;
    public ListViewWidget() {
        super(0, 0, 0, 0);
    }

    public void setXYSize(int x, int y, int width, int height) {
        setX2(x);
        setY2(y);
        setWidth2(width);
        setHeightMapped(height);
        if(getMaxScrollPosition() <= getHeight2()) {
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
        GuiDrawing guiDrawing = new GuiDrawing(graphicsHolder);
        elapsed += (tickDelta / Constants.MC_TICK_PER_SECOND);
        ClipStack.push(getX2(), getY2(), getWidth2(), getHeight2());

        // Background
        GuiHelper.drawRectangle(guiDrawing, getX2(), getY2(), width, height, 0x4F4C4C4C);

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

            listItem.draw(graphicsHolder, guiDrawing, entryX, entryY, width - getScrollbarWidth(), height, mouseX, mouseY, widgetVisible, elapsed, tickDelta);
            incY += listItem.height;
        }
        drawScrollbar(guiDrawing);
        ClipStack.pop();
    }

    @Override
    public boolean mouseScrolled3(double mouseX, double mouseY, double amount) {
        amount *= 15;
        double scrollableArea = Math.max(0, getMaxScrollPosition() - getHeight2());
        if(amount > 0) {
            currentScroll = Math.max(0, currentScroll - amount);
        } else {
            currentScroll = Math.min(scrollableArea, currentScroll - amount);
        }
        positionWidgets(currentScroll);
        return true;
    }

    private void drawScrollbar(GuiDrawing guiDrawing) {
        int fullHeight = getMaxScrollPosition();
        int visible = getHeight2();

        if(visible >= fullHeight) return;
        double scrollbarHeight = ((double)visible / fullHeight) * visible;
        double yOffset = (currentScroll / (fullHeight - visible)) * (visible - scrollbarHeight);
        int scrollBarWidth = getScrollbarWidth();
        GuiHelper.drawRectangle(guiDrawing, getX2() + getWidth2() - scrollBarWidth, getY2() + yOffset, scrollBarWidth, scrollbarHeight, 0xFFC0C0C0);

        // Border
        GuiHelper.drawRectangle(guiDrawing, getX2() + getWidth2() - 1, getY2() + yOffset, 1, scrollbarHeight, 0xFF808080);
        GuiHelper.drawRectangle(guiDrawing, getX2() + getWidth2() - scrollBarWidth, getY2() + yOffset + scrollbarHeight - 1, scrollBarWidth, 1, 0xFF808080);
    }

    private int getMaxScrollPosition() {
        int entryHeight = 0;
        for(ListItem entry : entryList) {
            entryHeight += entry.height;
        }
        return entryHeight;
    }

    private int getScrollbarWidth() {
        return (getMaxScrollPosition() > getHeight2()) ? SCROLLBAR_WIDTH : 0;
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
                int x = (startX + width - getScrollbarWidth()) - (listItem.widget.getWidth()) - (ENTRY_PADDING);
                int y = (int)(-scroll + entryY) + ((listItem.height - listItem.widget.getHeight()) / 2);

                listItem.widget.setX(x);
                listItem.widget.setY(y);
            }

            incY += listItem.height;
        }
    }
}
