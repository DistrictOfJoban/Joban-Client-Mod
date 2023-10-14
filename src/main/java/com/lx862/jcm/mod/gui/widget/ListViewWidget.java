package com.lx862.jcm.mod.gui.widget;

import com.lx862.jcm.mod.render.Renderable;
import net.minecraft.client.gui.widget.Widget;
import org.mtr.mapping.holder.MutableText;
import org.mtr.mapping.mapper.ClickableWidgetExtension;
import org.mtr.mapping.mapper.GraphicsHolder;
import org.mtr.mapping.mapper.GuiDrawing;

import java.util.ArrayList;
import java.util.List;

public class ListViewWidget extends ClickableWidgetExtension implements Renderable {
    public static final int ENTRY_PADDING = 5;
    private final int entryHeight;
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
        setHeight2(height);
        positionWidgets();
    }

    public void add(MutableText text, Widget widget) {
        entryList.add(new ListEntry(text, widget));
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
        double animationSpeed = (tickDelta / 4);

        // Background
        drawRectangle(guiDrawing, getX2(), getY2(), width, height, 0x09FFFFFF);

        for(int i = 0; i < entryList.size(); i++) {
            ListEntry listEntry = entryList.get(i);
            int entryX = getX2();
            int entryY = getY2() + (i * entryHeight);

            if(listEntry.isCategory) {
                drawListCategory(graphicsHolder, listEntry, entryX, entryY);
            } else {
                boolean hovered = inRectangle(mouseX, mouseY, entryX, entryY, width, entryHeight);
                if(hovered) {
                    entryHighlightAnimation.set(i, Math.min(1, entryHighlightAnimation.get(i) + animationSpeed));
                } else {
                    entryHighlightAnimation.set(i, Math.max(0, entryHighlightAnimation.get(i) - animationSpeed));
                }

                drawEntryHighlight(guiDrawing, i, entryX, entryY, listEntry.widget.getWidth());
                drawListEntryText(graphicsHolder, listEntry, entryX, entryY, tickDelta);
            }
        }
    }

    private void drawListEntryText(GraphicsHolder graphicsHolder, ListEntry entry, int entryX, int entryY, double tickDelta) {
        int entryTextWidth = width - entry.widget.getWidth() - ENTRY_PADDING;

        graphicsHolder.push();
        graphicsHolder.translate(entryX + ENTRY_PADDING, entryY, 0);
        scaleToFitBoundary(graphicsHolder, GraphicsHolder.getTextWidth(entry.title) + ENTRY_PADDING, entryTextWidth, false);
        graphicsHolder.drawText(entry.title, 0, (entryHeight / 4), 0xFFFFFFFF, true, MAX_RENDER_LIGHT);
        graphicsHolder.pop();
    }

    private void drawListCategory(GraphicsHolder graphicsHolder, ListEntry listEntry, int entryX, int entryY) {
        drawRectangle(new GuiDrawing(graphicsHolder), entryX, entryY, width, entryHeight, 0xAA999999);
        graphicsHolder.drawCenteredText(listEntry.title, (entryX + width / 2), entryY + (entryHeight / 4), 0xFFFFFFFF);
    }
    private void drawEntryHighlight(GuiDrawing guiDrawing, int entryIndex, int x, int y, int widgetWidth) {
        int highlightAlpha = (int)(80 * entryHighlightAnimation.get(entryIndex));
        int highlightColor = (highlightAlpha << 24) | (190 << 16) | (190 << 8) | 190;

        if(entryHighlightAnimation.get(entryIndex) > 0) {
            drawRectangle(guiDrawing, x, y, width - widgetWidth, entryHeight, highlightColor);
        }
    }

    private void positionWidgets() {
        int x = getX2();
        int y = getY2();

        for(int i = 0; i < entryList.size(); i++) {
            ListEntry listEntry = entryList.get(i);
            if(listEntry.isCategory) continue;

            int entryY = y + (i * entryHeight);
            listEntry.widget.setX((x + width) - listEntry.widget.getWidth());
            listEntry.widget.setY(entryY + ((entryHeight - listEntry.widget.getHeight()) / 2));
        }
    }
}
