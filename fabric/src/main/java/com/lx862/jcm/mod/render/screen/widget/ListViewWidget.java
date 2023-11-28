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
    private double currentScroll = 0;
    private final int entryHeight;
    private float elapsed;
    private final List<ListEntry> entryList = new ArrayList<>();
    private final List<Double> entryHighlightAnimation = new ArrayList<>();
    public ListViewWidget(int entryHeight) {
        super(0, 0, 0, 0);
        this.entryHeight = entryHeight;
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
        drawRectangle(guiDrawing, getX2(), getY2(), width, height, 0x4F4C4C4C);

        for(int i = 0; i < entryList.size(); i++) {
            ListEntry listEntry = entryList.get(i);
            int entryX = getX2();
            int entryY = getY2() + (i * entryHeight) - (int)currentScroll;

            if(listEntry.isCategory) {
                drawListCategory(graphicsHolder, guiDrawing, listEntry, entryX, entryY);
            } else {
                drawListEntry(graphicsHolder, guiDrawing, listEntry, entryX, entryY, mouseX, mouseY, i, tickDelta);
            }
        }
        drawScrollbar(guiDrawing);
        ClipStack.pop();
    }

    @Override
    public boolean mouseScrolled3(double mouseX, double mouseY, double amount) {
        amount *= 6;
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
        double scrollableArea = Math.max(0, getMaxScrollPosition() - getHeight2());
        double scrollbarHeight = getHeight2() - scrollableArea;
        int scrollBarWidth = getScrollbarWidth();
        drawRectangle(guiDrawing, getX2() + getWidth2() - scrollBarWidth, getY2() + currentScroll, scrollBarWidth, scrollbarHeight, 0xFFC0C0C0);

        // Border
        drawRectangle(guiDrawing, getX2() + getWidth2() - 1, getY2() + currentScroll, 1, scrollbarHeight, 0xFF808080);
        drawRectangle(guiDrawing, getX2() + getWidth2() - scrollBarWidth, getY2() + currentScroll + scrollbarHeight - 1, scrollBarWidth, 1, 0xFF808080);
    }

    private int getMaxScrollPosition() {
        return entryHeight * entryList.size();
    }

    private int getScrollbarWidth() {
        return (getMaxScrollPosition() > getHeight2()) ? SCROLLBAR_WIDTH : 0;
    }

    private void drawListCategory(GraphicsHolder graphicsHolder, GuiDrawing guiDrawing, ListEntry listEntry, int entryX, int entryY) {
        drawRectangle(guiDrawing, entryX, entryY, width - getScrollbarWidth(), entryHeight, 0x99999999);
        graphicsHolder.drawCenteredText(listEntry.title, (entryX + width / 2), entryY + (entryHeight / 4), ARGB_WHITE);
    }

    private void drawListEntry(GraphicsHolder graphicsHolder, GuiDrawing guiDrawing, ListEntry entry, int entryX, int entryY, int mouseX, int mouseY, int entryIndex, float tickDelta) {
        double highlightFadeSpeed = (tickDelta / 4);
        boolean entryHovered = inRectangle(mouseX, mouseY, entryX, entryY, width, entryHeight);
        if(entryHovered) {
            entryHighlightAnimation.set(entryIndex, Math.min(1, entryHighlightAnimation.get(entryIndex) + highlightFadeSpeed));
        } else {
            entryHighlightAnimation.set(entryIndex, Math.max(0, entryHighlightAnimation.get(entryIndex) - highlightFadeSpeed));
        }

        drawListEntryHighlight(guiDrawing, entryIndex, entryX, entryY);

        if(entry.widget != null) {
            boolean topLeftVisible = inRectangle(entry.widget.getX(), entry.widget.getY(), getX2(), getY2(), getWidth2(), getHeight2());
            boolean topRightVisible = inRectangle(entry.widget.getX() + entry.widget.getWidth(), entry.widget.getY() + entry.widget.getHeight(), getX2(), getY2(), getWidth2(), getHeight2());

            if(!topLeftVisible || !topRightVisible) {
                entry.widget.setVisible(false);
            } else {
                entry.widget.setVisible(true);
            }

            // We have to draw our widget (Right side) again after rendering the highlight so it doesn't get covered.
            entry.widget.render(graphicsHolder, mouseX, mouseY, tickDelta);
        }

        drawListEntryDescription(graphicsHolder, entry, entryX, entryY);
    }

    private void drawListEntryDescription(GraphicsHolder graphicsHolder, ListEntry entry, int entryX, int entryY) {
        int textHeight = 9;
        boolean hasIcon = entry.hasIcon();
        int iconSize = hasIcon ? entryHeight - ENTRY_PADDING : 0;
        int widgetWidth = entry.widget == null ? 0 : entry.widget.getWidth();
        int availableTextWidth = width - widgetWidth - ENTRY_PADDING - iconSize;
        int textY = (entryHeight / 2) - (textHeight / 2);

        graphicsHolder.push();
        graphicsHolder.translate(entryX, entryY, 0);
        graphicsHolder.translate(ENTRY_PADDING, 0, 0);

        if(hasIcon) {
            entry.drawIconCallback.accept(new GuiDrawing(graphicsHolder), entryX + ENTRY_PADDING, entryY + ((entryHeight - iconSize) / 2), iconSize, iconSize);
            // Shift the text to the right
            graphicsHolder.translate(iconSize + ENTRY_PADDING, 0, 0);
        }

        GuiHelper.drawScrollableText(graphicsHolder, entry.title, elapsed, entryX + ENTRY_PADDING + iconSize, 0, textY, availableTextWidth - iconSize - ENTRY_PADDING - ENTRY_PADDING - ENTRY_PADDING, ARGB_WHITE, true);
        graphicsHolder.pop();
    }
    private void drawListEntryHighlight(GuiDrawing guiDrawing, int entryIndex, int x, int y) {
        int highlightAlpha = (int)(100 * entryHighlightAnimation.get(entryIndex));
        int highlightColor = (highlightAlpha << 24) | (150 << 16) | (150 << 8) | 150;

        if(entryHighlightAnimation.get(entryIndex) > 0) {
            drawRectangle(guiDrawing, x, y, width - getScrollbarWidth(), entryHeight, highlightColor);
        }
    }

    private void positionWidgets(double scroll) {
        int startX = getX2();
        int startY = getY2();

        for(int i = 0; i < entryList.size(); i++) {
            ListEntry listEntry = entryList.get(i);
            if(listEntry.isCategory) continue;
            int entryY = startY + (i * entryHeight);
            int x = (startX + width - getScrollbarWidth()) - (listEntry.widget.getWidth()) - (ENTRY_PADDING);
            int y = (int)(-scroll + entryY) + ((entryHeight - listEntry.widget.getHeight()) / 2);
            listEntry.widget.setX(x);
            listEntry.widget.setY(y);
        }
    }
}
