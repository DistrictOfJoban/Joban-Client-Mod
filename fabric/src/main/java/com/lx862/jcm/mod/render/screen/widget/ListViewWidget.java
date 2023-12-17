package com.lx862.jcm.mod.render.screen.widget;

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
    private final List<ListEntry> entryList = new ArrayList<>();
    private final List<Double> entryHighlightAnimation = new ArrayList<>();
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
        add(new ListEntry(text, widget));
    }

    public void add(ListEntry listEntry) {
        entryList.add(listEntry);
        entryHighlightAnimation.add(0.0);
    }

    public void addCategory(MutableText text) {
        entryList.add(new ListEntry(text, null, true));
        entryHighlightAnimation.add(0.0);
    }

    public void reset() {
        this.entryList.clear();
        this.entryHighlightAnimation.clear();
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
            ListEntry listEntry = entryList.get(i);
            int entryX = getX2();
            int entryY = getY2() + incY - (int)currentScroll;

            if(listEntry.isCategory) {
                drawListCategory(graphicsHolder, guiDrawing, listEntry, entryX, entryY);
            } else {
                drawListEntry(graphicsHolder, guiDrawing, listEntry, entryX, entryY, mouseX, mouseY, i, tickDelta);
            }
            incY += listEntry.height;
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
        for(ListEntry entry : entryList) {
            entryHeight += entry.height;
        }
        return entryHeight;
    }

    private int getScrollbarWidth() {
        return (getMaxScrollPosition() > getHeight2()) ? SCROLLBAR_WIDTH : 0;
    }

    private void drawListCategory(GraphicsHolder graphicsHolder, GuiDrawing guiDrawing, ListEntry listEntry, int entryX, int entryY) {
        GuiHelper.drawRectangle(guiDrawing, entryX, entryY, width - getScrollbarWidth(), listEntry.height, 0x99999999);
        graphicsHolder.drawCenteredText(listEntry.title, (entryX + width / 2), entryY - (8/2) + (listEntry.height / 2), ARGB_WHITE);
    }

    private void drawListEntry(GraphicsHolder graphicsHolder, GuiDrawing guiDrawing, ListEntry entry, int entryX, int entryY, int mouseX, int mouseY, int entryIndex, float tickDelta) {
        double highlightFadeSpeed = (tickDelta / 4);
        boolean entryHovered = inRectangle(mouseX, mouseY, entryX, entryY, width, entry.height);
        if(entryHovered) {
            entryHighlightAnimation.set(entryIndex, Math.min(1, entryHighlightAnimation.get(entryIndex) + highlightFadeSpeed));
        } else {
            entryHighlightAnimation.set(entryIndex, Math.max(0, entryHighlightAnimation.get(entryIndex) - highlightFadeSpeed));
        }

        drawListEntryHighlight(guiDrawing, entryIndex, entryX, entryY, entry.height);

        if(entry.widget != null) {
            boolean topLeftVisible = inRectangle(entry.widget.getX(), entry.widget.getY(), getX2(), getY2(), getWidth2(), getHeight2());
            boolean topRightVisible = inRectangle(entry.widget.getX() + entry.widget.getWidth(), entry.widget.getY() + entry.widget.getHeight(), getX2(), getY2(), getWidth2(), getHeight2());

            entry.widget.setVisible(topLeftVisible && topRightVisible);

            // We have to draw our widget (Right side) again after rendering the highlight so it doesn't get covered.
            entry.widget.render(graphicsHolder, mouseX, mouseY, tickDelta);
        }

        if(entry.title != null) {
            drawListEntryDescription(graphicsHolder, entry, entryX, entryY);
        }
    }

    private void drawListEntryDescription(GraphicsHolder graphicsHolder, ListEntry entry, int entryX, int entryY) {
        int textHeight = 9;
        boolean hasIcon = entry.hasIcon();
        int iconSize = hasIcon ? entry.height - ENTRY_PADDING : 0;
        int widgetWidth = entry.widget == null ? 0 : entry.widget.getWidth();
        int availableTextWidth = width - widgetWidth - ENTRY_PADDING - iconSize;
        int textY = (entry.height / 2) - (textHeight / 2);

        graphicsHolder.push();
        graphicsHolder.translate(entryX, entryY, 0);
        graphicsHolder.translate(ENTRY_PADDING, 0, 0);

        if(hasIcon) {
            entry.drawIconCallback.accept(new GuiDrawing(graphicsHolder), entryX + ENTRY_PADDING, entryY + ((entry.height - iconSize) / 2), iconSize, iconSize);
            // Shift the text to the right
            graphicsHolder.translate(iconSize + ENTRY_PADDING, 0, 0);
        }

        GuiHelper.drawScrollableText(graphicsHolder, entry.title, elapsed, entryX + ENTRY_PADDING + iconSize, 0, textY, availableTextWidth - iconSize - ENTRY_PADDING - ENTRY_PADDING - ENTRY_PADDING, ARGB_WHITE, true);
        graphicsHolder.pop();
    }
    private void drawListEntryHighlight(GuiDrawing guiDrawing, int entryIndex, int x, int y, int height) {
        int highlightAlpha = (int)(100 * entryHighlightAnimation.get(entryIndex));
        int highlightColor = (highlightAlpha << 24) | (150 << 16) | (150 << 8) | 150;

        if(entryHighlightAnimation.get(entryIndex) > 0) {
            GuiHelper.drawRectangle(guiDrawing, x, y, width - getScrollbarWidth(), height, highlightColor);
        }
    }

    public void positionWidgets() {
        positionWidgets(currentScroll);
    }

    private void positionWidgets(double scroll) {
        int startX = getX2();
        int startY = getY2();

        int incY = 0;

        for(int i = 0; i < entryList.size(); i++) {
            ListEntry listEntry = entryList.get(i);
            if(!listEntry.isCategory) {
                int entryY = startY + incY;
                int x = (startX + width - getScrollbarWidth()) - (listEntry.widget.getWidth()) - (ENTRY_PADDING);
                int y = (int)(-scroll + entryY) + ((listEntry.height - listEntry.widget.getHeight()) / 2);

                listEntry.widget.setX(x);
                listEntry.widget.setY(y);
            }

            incY += listEntry.height;
        }
    }
}
